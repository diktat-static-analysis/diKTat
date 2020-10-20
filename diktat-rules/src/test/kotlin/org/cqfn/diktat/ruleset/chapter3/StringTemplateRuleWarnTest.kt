package org.cqfn.diktat.ruleset.chapter3

import com.pinterest.ktlint.core.LintError
import generated.WarningNames.STRING_TEMPLATE_CURLY_BRACES
import generated.WarningNames.STRING_TEMPLATE_QUOTES
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.rules.StringTemplateFormatRule
import org.cqfn.diktat.util.LintTestBase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class StringTemplateRuleWarnTest : LintTestBase(::StringTemplateFormatRule) {

    private val ruleId = "$DIKTAT_RULE_SET_ID:string-template-format"

    @Test
    @Tag(STRING_TEMPLATE_CURLY_BRACES)
    fun `long string template good example`() {
        lintMethod(
                """
                    |class Some { 
                    |   val template = "${'$'}{::String} ${'$'}{asd.moo()}"
                    |   val some = "${'$'}{foo as Foo}"
                    |}
                """.trimMargin()
        )
    }

    @Test
    @Tag(STRING_TEMPLATE_CURLY_BRACES)
    fun `long string template bad example`() {
        lintMethod(
                """
                    |class Some { 
                    |   val template = "${'$'}{a} ${'$'}{asd.moo()}"
                    |   val some = "${'$'}{1.0}"
                    |   val another = "${'$'}{1}"
                    |}
                """.trimMargin(),
                LintError(2, 20, ruleId, "${Warnings.STRING_TEMPLATE_CURLY_BRACES.warnText()} ${'$'}{a}", true),
                LintError(3, 16, ruleId, "${Warnings.STRING_TEMPLATE_CURLY_BRACES.warnText()} ${'$'}{1.0}", true),
                LintError(4, 19, ruleId, "${Warnings.STRING_TEMPLATE_CURLY_BRACES.warnText()} ${'$'}{1}", true)
        )
    }

    @Test
    @Tag(STRING_TEMPLATE_QUOTES)
    fun `short string template bad example`() {
        lintMethod(
                """
                    |class Some { 
                    |   val template = "${'$'}a"
                    |   val z = a
                    |}
                """.trimMargin(),
                LintError(2, 20, ruleId, "${Warnings.STRING_TEMPLATE_QUOTES.warnText()} ${'$'}a", true)
        )
    }

    @Test
    @Tag(STRING_TEMPLATE_CURLY_BRACES)
    fun `should not trigger on dot after braces`() {
        lintMethod(
                """
                    |class Some {
                    |   fun some() {
                    |       val s = "abs"
                    |       println("${'$'}{s}.length is ${'$'}{s.length}")
                    |   }
                    |}
                """.trimMargin()
        )
    }

    @Test
    @Tag(STRING_TEMPLATE_QUOTES)
    fun `should not trigger`() {
        lintMethod(
                """
                    |class Some {
                    |   fun some() {
                    |       val price = ""${'"'}
                    |       ${'$'}9.99
                    |       ""${'"'}
                    |       val some = "${'$'}{index + 1}"
                    |   }
                    |}
                """.trimMargin()
        )
    }

    @Test
    @Tag(STRING_TEMPLATE_CURLY_BRACES)
    fun `underscore after braces - braces should not be removed`() {
        lintMethod(
                """
                    |class Some {
                    |   fun some() {
                    |       val copyTestFile = File("${'$'}{testFile()} copy ${'$'}{testFile}_copy")
                    |   }
                    |}
                """.trimMargin()
        )
    }
}
