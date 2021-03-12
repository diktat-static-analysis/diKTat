package org.cqfn.diktat.ruleset.rules.chapter3

import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.constants.Warnings.EMPTY_BLOCK_STRUCTURE_ERROR
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.*

import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Rule that checks if empty code blocks (`{  }`) are used and checks their formatting.
 */
class EmptyBlock(configRules: List<RulesConfig>) : DiktatRule(
    "empty-block-structure",
    configRules,
    listOf(EMPTY_BLOCK_STRUCTURE_ERROR)) {
    override fun logic(node: ASTNode) {
        val configuration = EmptyBlockStyleConfiguration(
            configRules.getRuleConfig(EMPTY_BLOCK_STRUCTURE_ERROR)?.configuration ?: emptyMap()
        )
        searchNode(node, configuration)
    }

    private fun searchNode(node: ASTNode, configuration: EmptyBlockStyleConfiguration) {
        val newNode = node.findLBrace()?.treeParent ?: return
        checkEmptyBlock(newNode, configuration)
    }

    private fun isEmptyWhiteSpace(node: ASTNode) =
            node.findChildByType(WHITE_SPACE)?.text?.contains("\n") ?: true

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun checkEmptyBlock(node: ASTNode, configuration: EmptyBlockStyleConfiguration) {
        if (node.treeParent.isOverridden() || isAnonymousSamClass(node)) {
            return
        }
        if (node.isBlockEmpty()) {
            node.findParentNodeWithSpecificType(ElementType.LAMBDA_ARGUMENT)?.let {
                // Lambda body is always has a BLOCK -> run { } - (LBRACE, WHITE_SPACE, BLOCK "", RBRACE)
                if (!configuration.emptyBlockExist && isEmptyWhiteSpace(node)) {
                    val freeText = "do not put newlines in empty lambda"
                    EMPTY_BLOCK_STRUCTURE_ERROR.warnAndFix(configRules, emitWarn, isFixMode, freeText, node.startOffset, node) {
                        val whiteSpaceNode = node.findChildByType(WHITE_SPACE)
                        whiteSpaceNode?.leaveExactlyNumNewLines(0)
                    }
                }
                return
            }
            if (!configuration.emptyBlockExist) {
                EMPTY_BLOCK_STRUCTURE_ERROR.warn(configRules, emitWarn, isFixMode, "empty blocks are forbidden unless it is function with override keyword",
                    node.startOffset, node)
            } else {
                val space = node.findChildByType(RBRACE)!!.treePrev
                if (configuration.emptyBlockNewline && !space.text.contains("\n")) {
                    EMPTY_BLOCK_STRUCTURE_ERROR.warnAndFix(configRules, emitWarn, isFixMode, "different style for empty block",
                        node.startOffset, node) {
                        if (space.elementType == WHITE_SPACE) {
                            (space.treeNext as LeafPsiElement).replaceWithText("\n")
                        } else {
                            node.addChild(PsiWhiteSpaceImpl("\n"), space.treeNext)
                        }
                    }
                } else if (!configuration.emptyBlockNewline && space.text.contains("\n")) {
                    EMPTY_BLOCK_STRUCTURE_ERROR.warnAndFix(configRules, emitWarn, isFixMode, "different style for empty block",
                        node.startOffset, node) {
                        node.removeChild(space)
                    }
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType", "WRONG_WHITESPACE")
    private fun isAnonymousSamClass(node: ASTNode) : Boolean =
            if (node.elementType == FUNCTION_LITERAL && node.hasParent(CALL_EXPRESSION)) {
                // We are checking identifier because it is not class in AST
                // , SAM conversions are indistinguishable from lambdas.
                // So we just verify that identifier is in PascalCase
                val valueArgument = node.findParentNodeWithSpecificType(CALL_EXPRESSION)!!
                valueArgument.findLeafWithSpecificType(IDENTIFIER)?.text?.isPascalCase() ?: false
            } else {
                false
            }

    /**
     * [RuleConfiguration] for empty blocks formatting
     */
    class EmptyBlockStyleConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        /**
         * Whether empty code blocks should be allowed
         */
        val emptyBlockExist = config["allowEmptyBlocks"]?.toBoolean() ?: false

        /**
         * Whether a newline after `{` is required in an empty block
         */
        val emptyBlockNewline = config["styleEmptyBlockWithNewline"]?.toBoolean() ?: true
    }
}
