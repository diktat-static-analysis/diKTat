package org.cqfn.diktat.ruleset.rules.chapter6

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.findAllDescendantsWithSpecificType
import org.cqfn.diktat.ruleset.utils.isGoingAfter

import org.jetbrains.kotlin.KtNodeTypes.BLOCK
import org.jetbrains.kotlin.KtNodeTypes.CALL_EXPRESSION
import org.jetbrains.kotlin.KtNodeTypes.DOT_QUALIFIED_EXPRESSION
import org.jetbrains.kotlin.KtNodeTypes.IDENTIFIER
import org.jetbrains.kotlin.KtNodeTypes.PROPERTY
import org.jetbrains.kotlin.KtNodeTypes.PROPERTY_ACCESSOR
import org.jetbrains.kotlin.KtNodeTypes.REFERENCE_EXPRESSION
import org.jetbrains.kotlin.KtNodeTypes.THIS_EXPRESSION
import org.jetbrains.kotlin.KtNodeTypes.TYPE_REFERENCE
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtProperty

/**
 * Rule check that never use the name of a variable in the custom getter or setter
 */
class PropertyAccessorFields(configRules: List<RulesConfig>) : DiktatRule(
    NAME_ID,
    configRules,
    listOf(WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR)
) {
    override fun logic(node: ASTNode) {
        if (node.elementType == PROPERTY_ACCESSOR) {
            checkPropertyAccessor(node)
        }
    }

    // fixme should use shadow-check when it will be done
    private fun checkPropertyAccessor(node: ASTNode) {
        val leftValue = node.treeParent.findChildByType(IDENTIFIER) ?: return
        val isNotExtensionProperty = leftValue.treePrev?.treePrev?.elementType != TYPE_REFERENCE
        val firstReferenceWithSameName = node
            .findAllDescendantsWithSpecificType(REFERENCE_EXPRESSION)
            .mapNotNull { it.findChildByType(IDENTIFIER) }
            .firstOrNull {
                it.text == leftValue.text &&
                        (it.treeParent.treeParent.elementType != DOT_QUALIFIED_EXPRESSION ||
                                it.treeParent.treeParent.firstChildNode.elementType == THIS_EXPRESSION)
            }
        val isContainLocalVarSameName = node
            .findChildByType(BLOCK)
            ?.getChildren(TokenSet.create(PROPERTY))
            ?.filter { (it.psi as KtProperty).nameIdentifier?.text == leftValue.text }
            ?.none { firstReferenceWithSameName?.isGoingAfter(it) ?: false } ?: true
        val isNotCallExpression = firstReferenceWithSameName?.treeParent?.treeParent?.elementType != CALL_EXPRESSION
        if (firstReferenceWithSameName != null && isContainLocalVarSameName && isNotCallExpression && isNotExtensionProperty) {
            WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR.warn(configRules, emitWarn, isFixMode, node.text, node.startOffset, node)
        }
    }

    companion object {
        const val NAME_ID = "getter-setter-fields"
    }
}
