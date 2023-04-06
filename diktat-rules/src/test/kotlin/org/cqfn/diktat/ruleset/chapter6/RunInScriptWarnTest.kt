package org.cqfn.diktat.ruleset.chapter6

import org.cqfn.diktat.common.config.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.constants.Warnings.RUN_IN_SCRIPT
import org.cqfn.diktat.ruleset.rules.chapter6.RunInScript
import org.cqfn.diktat.util.LintTestBase

import com.pinterest.ktlint.core.LintError
import generated.WarningNames
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RunInScriptWarnTest : LintTestBase(::RunInScript) {
    private val ruleId: String = "$DIKTAT_RULE_SET_ID:${RunInScript.NAME_ID}"

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check simple example`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                class A {}

                fun foo() {
                }

                diktat {}

                diktat({})

                foo/*df*/()

                foo( //dfdg
                    10
                )
                println("hello")

                w.map { it -> it }

                tasks.register("a") {
                    dependsOn("b")
                    doFirst {
                        generateCodeStyle(file("rootDir/guide"), file("rootDir/../wp"))
                    }
                }

            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/Example.kts",
            LintError(10, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} foo/*df*/()", true),
            LintError(12, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} foo( //dfdg...", true),
            LintError(15, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} println(\"hello\")", true),
            LintError(17, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} w.map { it -> it }", true),
            LintError(19, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} tasks.register(\"a\") {...", true)
        )
    }

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check correct examples`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                run {
                    println("hello")
                }

                run{println("hello")}

                val task = tasks.register("a") {
                }

            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/Example.kts"
        )
    }

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check correct with custom wrapper`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                custom {
                    println("hello")
                }

                oneMore{println("hello")}

                another {
                    println("hello")
                }
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/Example.kts"
        )
    }

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check gradle file`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                class A {}

                fun foo() {
                }

                if(true) {
                    goo()
                }

                diktat {}

                diktat({})

                foo/*df*/()

                foo( //dfdg
                    10
                )
                println("hello")

                w.map { it -> it }

                (tasks.register("a") {
                    dependsOn("b")
                    doFirst {
                        generateCodeStyle(file("rootDir/guide"), file("rootDir/../wp"))
                    }
                })

            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/builds.gradle.kts",
            LintError(6, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} if(true) {...", true)
        )
    }

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check gradle script with eq expression`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                version = "0.1.0-SNAPSHOT"

                diktat {}

                diktat({})

                foo/*df*/()

                foo().goo()
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/builds.gradle.kts"
        )
    }

    @Test
    @Tag(WarningNames.RUN_IN_SCRIPT)
    fun `check kts script with eq expression`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                version = "0.1.0-SNAPSHOT"

                diktat {}

                diktat({})

                foo/*df*/()
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "src/main/kotlin/org/cqfn/diktat/Example.kts",
            LintError(1, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} version = \"0.1.0-SNAPSHOT\"", true),
            LintError(7, 17, ruleId, "${RUN_IN_SCRIPT.warnText()} foo/*df*/()", true)
        )
    }
}
