package org.cqfn.diktat.ruleset.chapter3

import org.cqfn.diktat.ruleset.rules.chapter3.BlockStructureBraces
import org.cqfn.diktat.util.FixTestBase

import org.cqfn.diktat.ruleset.constants.WarningsNames
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class BlockStructureBracesFixTest : FixTestBase ("test/paragraph3/block_brace", ::BlockStructureBraces) {
    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in if-else expression`() {
        fixAndCompare("IfElseBracesExpected.kt", "IfElseBracesTest.kt")
    }

    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in class expression`() {
        fixAndCompare("ClassBracesExpected.kt", "ClassBracesTest.kt")
    }

    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in do-while expression`() {
        fixAndCompare("DoWhileBracesExpected.kt", "DoWhileBracesTest.kt")
    }

    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in loops expression`() {
        fixAndCompare("LoopsBracesExpected.kt", "LoopsBracesTest.kt")
    }

    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in when expression`() {
        fixAndCompare("WhenBranchesExpected.kt", "WhenBranchesTest.kt")
    }

    @Test
    @Tag(WarningNames.BRACES_BLOCK_STRUCTURE_ERROR)
    fun `should fix open and close brace in try-catch-finally expression`() {
        fixAndCompare("TryBraceExpected.kt", "TryBraceTest.kt")
    }
}
