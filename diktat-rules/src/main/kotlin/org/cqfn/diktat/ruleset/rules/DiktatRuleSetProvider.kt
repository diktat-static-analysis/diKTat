package org.cqfn.diktat.ruleset.rules

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.cqfn.diktat.common.config.rules.DIKTAT_COMMON
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.RulesConfigReader
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.rules.calculations.AccurateCalculationsRule
import org.cqfn.diktat.ruleset.rules.classes.DataClassesRule
import org.cqfn.diktat.ruleset.rules.classes.SingleInitRule
import org.cqfn.diktat.ruleset.rules.comments.CommentsRule
import org.cqfn.diktat.ruleset.rules.comments.HeaderCommentRule
import org.cqfn.diktat.ruleset.rules.files.BlankLinesRule
import org.cqfn.diktat.ruleset.rules.files.FileSize
import org.cqfn.diktat.ruleset.rules.files.FileStructureRule
import org.cqfn.diktat.ruleset.rules.files.IndentationRule
import org.cqfn.diktat.ruleset.rules.files.NewlinesRule
import org.cqfn.diktat.ruleset.rules.files.WhiteSpaceRule
import org.cqfn.diktat.ruleset.rules.identifiers.LocalVariablesRule
import org.cqfn.diktat.ruleset.rules.kdoc.CommentsFormatting
import org.cqfn.diktat.ruleset.rules.kdoc.KdocComments
import org.cqfn.diktat.ruleset.rules.kdoc.KdocFormatting
import org.cqfn.diktat.ruleset.rules.kdoc.KdocMethods
import org.jetbrains.kotlin.org.jline.utils.Levenshtein
import org.slf4j.LoggerFactory
import java.io.File

/**
 * this constant will be used everywhere in the code to mark usage of Diktat ruleset
 */
const val DIKTAT_RULE_SET_ID = "diktat-ruleset"

class DiktatRuleSetProvider(private val diktatConfigFile: String = "diktat-analysis.yml") : RuleSetProvider {
    @Suppress("LongMethod")
    override fun get(): RuleSet {
        log.debug("Will run $DIKTAT_RULE_SET_ID with $diktatConfigFile (it can be placed to the run directory or the default file from resources will be used)")
        if (!File(diktatConfigFile).exists()) {
            log.warn("Configuration file $diktatConfigFile not found in file system, the file included in jar will be used. " +
                    "Some configuration options will be disabled or substituted with defaults. " +
                    "Custom configuration file should be placed in diktat working directory if run from CLI " +
                    "or provided as configuration options in plugins."
            )
        }
        val configRules = RulesConfigReader(javaClass.classLoader)
            .readResource(diktatConfigFile)
            ?.onEach(::validate)
            ?: emptyList()
        val rules = listOf(
                ::CommentsRule,
                ::KdocComments,
                ::KdocMethods,
                ::KdocFormatting,
                ::FileNaming,
                ::PackageNaming,
                ::StringTemplateFormatRule,
                ::FileSize,
                ::DataClassesRule,
                ::IdentifierNaming,
                ::LocalVariablesRule,
                ::ClassLikeStructuresOrderRule,
                ::BracesInConditionalsAndLoopsRule,
                ::BlockStructureBraces,
                ::EmptyBlock,
                ::SmartCastRule,
                ::PropertyAccessorFields,
                ::EnumsSeparated,
                ::VariableGenericTypeDeclarationRule,
                ::SingleLineStatementsRule,
                ::CommentsFormatting,
                ::ConsecutiveSpacesRule,
                ::LongNumericalValuesSeparatedRule,
                ::NestedFunctionBlock,
                ::MultipleModifiersSequence,
                ::AnnotationNewLineRule,
                ::HeaderCommentRule,
                ::SortRule,
                ::StringConcatenationRule,
                ::AccurateCalculationsRule,
                ::LineLength,
                ::TypeAliasRule,
                ::OverloadingArgumentsFunction,
                ::FunctionLength,
                ::LambdaParameterOrder,
                ::FunctionArgumentsSize,
                ::BlankLinesRule,
                ::NullableTypeRule,
                ::SingleInitRule,
                ::WhenMustHaveElseRule,
                ::ImmutableValNoVarRule,
                ::AvoidNestedFunctionsRule,
                ::WhiteSpaceRule,  // this rule should be after other rules that can cause wrong spacing
                ::FileStructureRule,  // this rule should be right before indentation because it should operate on already valid code
                ::NewlinesRule,  // newlines need to be inserted right before fixing indentation
                ::IndentationRule  // indentation rule should be the last because it fixes formatting after all the changes done by previous rules
        )
                .map {
                    it.invoke(configRules)
                }
                .toTypedArray()
        return RuleSet(
                DIKTAT_RULE_SET_ID,
                *rules
        )
    }

    private fun validate(config: RulesConfig) =
        require(config.name == DIKTAT_COMMON || config.name in Warnings.names) {
            val closestMatch = Warnings.names.minBy { Levenshtein.distance(it, config.name) }
            "Warning name <${config.name}> in configuration file is invalid, did you mean <$closestMatch>?"
        }

    companion object {
        private val log = LoggerFactory.getLogger(DiktatRuleSetProvider::class.java)
    }
}
