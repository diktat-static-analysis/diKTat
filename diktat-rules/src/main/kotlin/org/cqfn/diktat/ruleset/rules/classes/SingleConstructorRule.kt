package org.cqfn.diktat.ruleset.rules.classes

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.SECONDARY_CONSTRUCTOR
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.EmitType
import org.cqfn.diktat.ruleset.constants.Warnings.SINGLE_CONSTRUCTOR_SHOULD_BE_PRIMARY
import org.cqfn.diktat.ruleset.utils.KotlinParser
import org.cqfn.diktat.ruleset.utils.getAllChildrenWithType
import org.cqfn.diktat.ruleset.utils.getIdentifierName
import org.cqfn.diktat.ruleset.utils.isGoingAfter
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * This rule ensures that if a class has a single constructor, this constructor is primary.
 * Secondary constructor is converted into primary, statements that are not assignments are moved into an `init` block.
 */
class SingleConstructorRule(private val config: List<RulesConfig>) : Rule("single-constructor") {
    private var isFixMode: Boolean = false
    private val kotlinParser by lazy { KotlinParser() }
    private lateinit var emitWarn: EmitType

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: EmitType
    ) {
        emitWarn = emit
        isFixMode = autoCorrect

        if (node.elementType == CLASS) {
            handleClassConstructors(node)
        }
    }

    private fun handleClassConstructors(node: ASTNode) {
        if (node.findChildByType(PRIMARY_CONSTRUCTOR) == null) {
            // class has no primary constructor, need to count secondary constructors
            node
                .findChildByType(CLASS_BODY)
                ?.getAllChildrenWithType(SECONDARY_CONSTRUCTOR)
                ?.singleOrNull()
                ?.let { secondaryCtor ->
                    SINGLE_CONSTRUCTOR_SHOULD_BE_PRIMARY.warnAndFix(
                        config, emitWarn, isFixMode, "in class <${node.getIdentifierName()?.text}>",
                        node.startOffset, node
                    ) {
                        convertConstructorToPrimary(node, secondaryCtor)
                    }
                }
        }
    }

    /**
     * This method does the following:
     * - Inside the single secondary constructor find all assignments.
     * - Some of assigned values will have `this` qualifier, they are definitely class properties.
     * - For other assigned variables that are not declared in the same scope we check if they are properties and whether they depend only on constructor parameters.
     * - Create primary constructor moving all properties that we collected.
     * - Create init block with other statements from the secondary constructor, including initialization of properties that require local variables or complex calls.
     * - Finally, remove the secondary constructor.
     */
    private fun convertConstructorToPrimary(classNode: ASTNode, secondaryCtor: ASTNode) {
        val secondaryCtorArguments = (secondaryCtor.psi as KtSecondaryConstructor).valueParameters

        // split all statements into assignments and all other statements (including comments)
        val (assignments, otherStatements) = (secondaryCtor.psi as KtSecondaryConstructor)
            .bodyBlockExpression
            ?.statements
            ?.partition { it is KtBinaryExpression && it.asAssignment() != null }
            ?.run { first.map { it as KtBinaryExpression } to second }
            ?: emptyList<KtBinaryExpression>() to emptyList()

        val classProperties = (classNode.psi as KtClass).getProperties()
        val localProperties = secondaryCtor.psi.collectDescendantsOfType<KtProperty> { it.isLocal }
        // find all references to class properties that are getting assigned in a constructor
        val assignmentsToReferences = assignments.associateWithAssignedReference(localProperties, classProperties)

        // Split all assignments into trivial (that are just assigned from a constructor parameter) and non-trivial.
        // Logic for non-trivial assignments should than be kept and moved into a dedicated `init` block.
        val (trivialAssignments, nonTrivialAssignments) = assignmentsToReferences
            .toList()
            .partition { (assignment, _) ->
                assignment.right.let { rhs ->
                    rhs is KtNameReferenceExpression && rhs.getReferencedName() in secondaryCtorArguments.map { it.name }
                }
            }
            .let { it.first.toMap() to it.second.toMap() }

        // find corresponding properties' declarations
        val declarationsAssignedInCtor = trivialAssignments
            .mapNotNull { (_, reference) ->
                (classNode.psi as KtClass).getProperties()
                    .firstOrNull { it.nameIdentifier?.text == reference.getReferencedName() }
            }
            .distinct()

        classNode.convertSecondaryConstructorToPrimary(secondaryCtor, declarationsAssignedInCtor, nonTrivialAssignments, otherStatements)
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun List<KtBinaryExpression>.associateWithAssignedReference(localProperties: List<KtProperty>, classProperties: List<KtProperty>) =
        associateWith {
            // non-null assert is safe because of predicate in partitioning
            it.asAssignment()!!.left!!
        }
            .filterValues { left ->
                // we keep only statements where property is referenced via this (like `this.foo = ...`)
                left is KtDotQualifiedExpression && left.receiverExpression is KtThisExpression && left.selectorExpression is KtNameReferenceExpression ||
                        // or directly (like `foo = ...`)
                        left is KtNameReferenceExpression && localProperties.none {
                    // check for shadowing
                    left.node.isGoingAfter(it.node) && it.name == left.name
                }
            }
            .mapValues { (_, left) ->
                when (left) {
                    is KtDotQualifiedExpression -> left.selectorExpression as KtNameReferenceExpression
                    is KtNameReferenceExpression -> left
                    else -> error("Unexpected psi class ${left::class} with text ${left.text}")
                }
            }
            .filterValues { left -> left.getReferencedName() in classProperties.mapNotNull { it.name } }

    @Suppress("UnsafeCallOnNullableType", "NestedBlockDepth")
    private fun ASTNode.convertSecondaryConstructorToPrimary(
        secondaryCtor: ASTNode,
        declarationsAssignedInCtor: List<KtProperty>,
        nonTrivialAssignments: Map<KtBinaryExpression, KtNameReferenceExpression>,
        otherStatements: List<KtExpression>
    ) {
        require(elementType == CLASS)

        val localProperties = secondaryCtor.psi.collectDescendantsOfType<KtProperty> { it.isLocal }
        // find all arguments that are not directly assigned into properties
        val neededSecondaryCtorArguments = (secondaryCtor.psi as KtSecondaryConstructor).valueParameters.run {
            val dependencies = nonTrivialAssignments.keys
                .flatMap { it.left!!.collectDescendantsOfType<KtNameReferenceExpression>() }
                .filterNot { ref ->
                    localProperties.any { ref.node.isGoingAfter(it.node) && ref.getReferencedName() == it.name }
                }
                .map { it.getReferencedName() }
            filter {
                it.name in dependencies
            }
        }

        val primaryCtorNode = kotlinParser.createPrimaryConstructor(
            (secondaryCtor
                .findChildByType(MODIFIER_LIST)
                ?.text
                ?.plus(" constructor ")
                ?: "") +
                    "(${
                        declarationsAssignedInCtor.run {
                            joinToString(
                                ", ",
                                postfix = if (isNotEmpty() && neededSecondaryCtorArguments.isNotEmpty()) ", " else ""
                            ) { it.text }
                        }
                    }" +
                    "${neededSecondaryCtorArguments.joinToString(", ") { it.text }})"
        ).node
        addChild(primaryCtorNode, findChildByType(CLASS_BODY))
        declarationsAssignedInCtor.forEach { ktProperty ->
            ktProperty.node.let { treeParent.removeChild(it) }
        }

        if (otherStatements.isNotEmpty() || nonTrivialAssignments.isNotEmpty()) {
            findChildByType(CLASS_BODY)?.run {
                val classInitializer = kotlinParser.createNode(
                    """
                    |init {
                    |    ${(otherStatements + nonTrivialAssignments.keys).joinToString("\n") { it.text }}
                    |}
                """.trimMargin()
                )
                addChild(classInitializer, secondaryCtor)
                addChild(PsiWhiteSpaceImpl("\n"), secondaryCtor)
            }
        }

        findChildByType(CLASS_BODY)?.removeChild(secondaryCtor)
    }
}
