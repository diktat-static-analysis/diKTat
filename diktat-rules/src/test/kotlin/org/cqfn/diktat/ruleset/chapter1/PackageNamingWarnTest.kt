package org.cqfn.diktat.ruleset.chapter1

import org.cqfn.diktat.common.config.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.INCORRECT_PACKAGE_SEPARATOR
import org.cqfn.diktat.ruleset.constants.Warnings.PACKAGE_NAME_INCORRECT_CASE
import org.cqfn.diktat.ruleset.constants.Warnings.PACKAGE_NAME_INCORRECT_PATH
import org.cqfn.diktat.ruleset.constants.Warnings.PACKAGE_NAME_INCORRECT_PREFIX
import org.cqfn.diktat.ruleset.constants.Warnings.PACKAGE_NAME_INCORRECT_SYMBOLS
import org.cqfn.diktat.ruleset.constants.Warnings.PACKAGE_NAME_MISSING
import org.cqfn.diktat.ruleset.rules.chapter1.PackageNaming
import org.cqfn.diktat.util.LintTestBase
import org.cqfn.diktat.util.TEST_FILE_NAME

import com.pinterest.ktlint.core.LintError
import generated.WarningNames
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PackageNamingWarnTest : LintTestBase(::PackageNaming) {
    private val ruleId: String = "$DIKTAT_RULE_SET_ID:${PackageNaming.NAME_ID}"
    private val rulesConfigList: List<RulesConfig> = listOf(
        RulesConfig("DIKTAT_COMMON", true, mapOf("domainName" to "org.cqfn.diktat"))
    )
    private val rulesConfigListEmptyDomainName: List<RulesConfig> = listOf(
        RulesConfig("DIKTAT_COMMON", true, mapOf("domainName" to ""))
    )
    private val rulesConfigSourceDirectories: List<RulesConfig> = listOf(
        RulesConfig("DIKTAT_COMMON", true, mapOf(
            "domainName" to "org.cqfn.diktat",
            "srcDirectories" to "nativeMain, mobileMain",
            "testDirs" to "nativeTest"
        ))
    )

    @Test
    @Tag(WarningNames.PACKAGE_NAME_MISSING)
    fun `missing package name (check)`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = TEST_FILE_NAME,
            LintError(1, 1, ruleId, "${PACKAGE_NAME_MISSING.warnText()} $TEST_FILE_NAME", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_MISSING)
    fun `missing package name with annotation (check)`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                @file:Suppress("CONSTANT_UPPERCASE")

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = TEST_FILE_NAME,
            LintError(1, 37, ruleId, "${PACKAGE_NAME_MISSING.warnText()} $TEST_FILE_NAME", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_MISSING)
    fun `don't add the package name to the file in buildSrc path`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/buildSrc/src/main/kotlin/Version.kt",
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_CASE)
    fun `package name should be in a lower case (check)`() {
        lintMethod(
            """
                package /* AAA */ org.cqfn.diktat.SPECIALTEST.test

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            LintError(1, 35, ruleId, "${PACKAGE_NAME_INCORRECT_CASE.warnText()} SPECIALTEST", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PREFIX)
    fun `package name should start from domain name (check)`() {
        lintMethod(

            """
                package some.incorrect.domain.test

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PREFIX.warnText()} org.cqfn.diktat", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.INCORRECT_PACKAGE_SEPARATOR)
    fun `underscore exceptions - incorrect underscore case`() {
        lintMethod(

            """
                package org.cqfn.diktat.domain.test_

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            LintError(1, 32, ruleId, "${INCORRECT_PACKAGE_SEPARATOR.warnText()} test_", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_SYMBOLS)
    fun `incorrect symbol in package name`() {
        lintMethod(

            """
                package org.cqfn.diktat.domain.testш

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            LintError(1, 32, ruleId, "${PACKAGE_NAME_INCORRECT_SYMBOLS.warnText()} testш"),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_SYMBOLS)
    fun `underscore exceptions - positive case - keyword`() {
        lintMethod(

            """
                package org.cqfn.diktat.domain.int_

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_SYMBOLS)
    fun `test source config`(@TempDir tempDir: Path) {
        lintMethodWithFile(

            """
                package org.cqfn.diktat.domain

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/nativeMain/kotlin/org/cqfn/diktat/domain/BlaBla.kt",
            rulesConfigList = rulesConfigSourceDirectories
        )

        lintMethodWithFile(

            """
                package org.cqfn.diktat.domain

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/main/kotlin/org/cqfn/diktat/domain/BlaBla.kt",
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.main.kotlin.org.cqfn.diktat.domain", true),
            rulesConfigList = rulesConfigSourceDirectories
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_SYMBOLS)
    fun `test directories for test config`(@TempDir tempDir: Path) {
        lintMethodWithFile(

            """
                package org.cqfn.diktat.domain

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/nativeTest/kotlin/org/cqfn/diktat/domain/BlaBla.kt",
            rulesConfigList = rulesConfigSourceDirectories
        )

        lintMethodWithFile(

            """
                package org.cqfn.diktat.domain

                import org.cqfn.diktat.a.b.c

                /**
                 * testComment
                 */
                class TestPackageName {  }

            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/test/kotlin/org/cqfn/diktat/domain/BlaBla.kt",
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.test.kotlin.org.cqfn.diktat.domain", true),
            rulesConfigList = rulesConfigSourceDirectories
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `regression - incorrect warning on file under test directory`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                    package org.cqfn.diktat.ruleset.chapter1
            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/test/kotlin/org/cqfn/diktat/ruleset/chapter1/EnumValueCaseTest.kt",
            rulesConfigList = rulesConfigList
        )

        lintMethodWithFile(
            """
                    package org.cqfn.diktat.chapter1
            """.trimIndent(),
            tempDir = tempDir,
            fileName = "diktat/diktat-rules/src/test/kotlin/org/cqfn/diktat/ruleset/chapter1/EnumValueCaseTest.kt",
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.ruleset.chapter1", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `regression - should not remove special words from file path`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                    |package org.cqfn.diktat.test.processing
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/module/src/test/kotlin/org/cqfn/diktat/test/processing/SpecialPackageNaming.kt",
            rulesConfigList = rulesConfigList
        )

        lintMethodWithFile(
            """
                    |package kotlin.collections
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/module/src/main/kotlin/kotlin/collections/Collections.kt",
            rulesConfigList = listOf(
                RulesConfig("DIKTAT_COMMON", true, mapOf("domainName" to "kotlin"))
            )
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `should respect KMP project structure - positive example`(@TempDir tempDir: Path) {
        listOf("main", "test", "jvmMain", "jvmTest", "androidMain", "androidTest", "iosMain", "iosTest", "jsMain", "jsTest", "commonMain", "commonTest").forEach {
            lintMethodWithFile(
                """
                    |package org.cqfn.diktat
                """.trimMargin(),
                tempDir = tempDir,
                fileName = "project/src/$it/kotlin/org/cqfn/diktat/Example.kt",
                rulesConfigList = rulesConfigList
            )
        }
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `should respect KMP project structure`(@TempDir tempDir: Path) {
        listOf("main", "test", "jvmMain", "jvmTest", "androidMain", "androidTest", "iosMain", "iosTest", "jsMain", "jsTest", "commonMain", "commonTest").forEach {
            lintMethodWithFile(
                """
                    |package org.cqfn.diktat
                """.trimMargin(),
                tempDir = tempDir,
                fileName = "project/src/$it/kotlin/org/cqfn/diktat/example/Example.kt",
                LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.example", true),
                rulesConfigList = rulesConfigList
            )
        }
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `should respect KMP project structure - illegal source set name`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                |package org.cqfn.diktat
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/src/myProjectMain/kotlin/org/cqfn/diktat/example/Example.kt",
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.myProjectMain.kotlin.org.cqfn.diktat.example", true),
            rulesConfigList = rulesConfigList
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `should warn if there is empty domain name`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                |package org.cqfn.diktat
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/src/main/kotlin/org/cqfn/diktat/example/Example.kt",
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PREFIX.warnText()} ", true),
            LintError(1, 9, ruleId, "${PACKAGE_NAME_INCORRECT_PATH.warnText()} org.cqfn.diktat.example", true),
            rulesConfigList = rulesConfigListEmptyDomainName
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `shouldn't trigger if path contains dot`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                |package org.cqfn.diktat.test.utils
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/src/main/kotlin/org/cqfn/diktat/test.utils/Example.kt",
        )
    }

    @Test
    @Tag(WarningNames.PACKAGE_NAME_INCORRECT_PATH)
    fun `shouldn't trigger for gradle script`(@TempDir tempDir: Path) {
        lintMethodWithFile(
            """
                |import org.cqfn.diktat.generation.docs.generateAvailableRules
            """.trimMargin(),
            tempDir = tempDir,
            fileName = "project/build.gradle.kts",
        )
    }
}
