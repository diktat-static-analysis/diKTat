/**
 * The file contains main method
 */

package com.saveourtool.diktat

import com.saveourtool.diktat.api.DiktatProcessorListener
import com.saveourtool.diktat.cli.DiktatMode
import com.saveourtool.diktat.cli.DiktatProperties
import com.saveourtool.diktat.ktlint.DiktatBaselineFactoryImpl
import com.saveourtool.diktat.ktlint.DiktatProcessorFactoryImpl
import com.saveourtool.diktat.ktlint.DiktatReporterFactoryImpl
import com.saveourtool.diktat.ruleset.rules.DiktatRuleConfigReaderImpl
import com.saveourtool.diktat.ruleset.rules.DiktatRuleSetFactoryImpl

import mu.KotlinLogging

import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.absolutePathString

private val log = KotlinLogging.logger { }

private val loggingListener = object : DiktatProcessorListener {
    override fun before(file: Path) {
        log.debug {
            "Start processing the file: $file"
        }
    }
}

fun main(args: Array<String>) {
    val diktatRunnerFactory = DiktatRunnerFactory(
        DiktatRuleConfigReaderImpl(),
        DiktatRuleSetFactoryImpl(),
        DiktatProcessorFactoryImpl(),
        DiktatBaselineFactoryImpl(),
        DiktatReporterFactoryImpl(),
    )
    val properties = DiktatProperties.parse(diktatRunnerFactory.diktatReporterFactory, args)
    properties.configureLogger()

    log.debug {
        "Loading diktatRuleSet using config ${properties.config}"
    }
    val currentFolder = Paths.get(".").toAbsolutePath().normalize()
    val diktatRunnerArguments = properties.toRunnerArguments(
        sourceRootDir = currentFolder,
        loggingListener = loggingListener,
    )

    val diktatRunner = diktatRunnerFactory(diktatRunnerArguments)
    when (properties.mode) {
        DiktatMode.CHECK -> diktatRunner.checkAll(diktatRunnerArguments)
        DiktatMode.FIX -> diktatRunner.fixAll(diktatRunnerArguments) { updatedFile ->
            log.warn {
                "Original and formatted content differ, writing to ${updatedFile.absolutePathString()}..."
            }
        }
    }
}
