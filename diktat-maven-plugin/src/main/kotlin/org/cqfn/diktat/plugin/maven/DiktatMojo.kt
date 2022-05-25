/**
 * MOJOs for goals of diktat plugin
 */

package org.cqfn.diktat.plugin.maven

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import org.apache.maven.plugins.annotations.Mojo

import java.io.File

/**
 * Main [Mojo] that call [DiktatRuleSetProvider]'s rules on [inputs] files
 */
@Mojo(name = "check")
@Suppress("unused")
class DiktatCheckMojo : DiktatBaseMojo() {
    /**
     * @param params instance of [KtLint.ExperimentalParams] used in analysis
     */
    @OptIn(FeatureInAlphaState::class)
    override fun runAction(params: KtLint.ExperimentalParams) {
        KtLint.lint(params)
    }
}

/**
 * Main [Mojo] that call [DiktatRuleSetProvider]'s rules on [inputs] files
 * and fixes discovered errors
 */
@Mojo(name = "fix")
@Suppress("unused")
class DiktatFixMojo : DiktatBaseMojo() {
    /**
     * @param params instance of [KtLint.Params] used in analysis
     */
    @OptIn(FeatureInAlphaState::class)
    override fun runAction(params: KtLint.ExperimentalParams) {
        val fileName = params.fileName
        val filePath = params.userData["file_path"] ?: error("File path should be provided")
        val fileContent = File(filePath).readText(charset("UTF-8"))
        val formattedText = KtLint.format(params)
        if (fileContent != formattedText) {
            log.info("Original and formatted content differ, writing to $fileName...")
            File(filePath).writeText(formattedText, charset("UTF-8"))
        }
    }
}
