package org.cqfn.diktat.ruleset.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings.TYPE_ALIAS
import org.cqfn.diktat.ruleset.utils.*
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class TypeAliasRule(private val configRules: List<RulesConfig>) : Rule("type-alias") {

    companion object {
        const val TYPE_REFERENCE_MAX_LENGTH = 25
    }

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(node: ASTNode,
                       autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        emitWarn = emit
        isFixMode = autoCorrect

        if (node.elementType == TYPE_REFERENCE) {
            checkProperty(node, TypeAliasConfiguration(configRules.getRuleConfig(TYPE_ALIAS)?.configuration ?: mapOf()))
        }
    }

    private fun checkProperty(node: ASTNode, config: TypeAliasConfiguration) {
        if (node.textLength > config.typeReferenceLength)
            if (node.findAllNodesWithSpecificType(LT).size > 1 || node.findAllNodesWithSpecificType(VALUE_PARAMETER).size > 1)
                TYPE_ALIAS.warn(configRules, emitWarn, isFixMode, "too long type reference", node.startOffset)
    }

    class TypeAliasConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        val typeReferenceLength = config["typeReferenceLength"]?.toIntOrNull() ?: TYPE_REFERENCE_MAX_LENGTH
    }
}
