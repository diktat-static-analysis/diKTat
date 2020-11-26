package org.cqfn.diktat.ruleset.generation

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.rules.comments.HeaderCommentRule.Companion.afterCopyrightRegex
import org.cqfn.diktat.ruleset.rules.comments.HeaderCommentRule.Companion.curYear
import org.cqfn.diktat.ruleset.rules.comments.HeaderCommentRule.Companion.hyphenRegex
import java.io.PrintWriter

private val autoGenerationComment =
        """
            | This document was auto generated, please don't modify it.
            | This document contains all enum properties from Warnings.kt as Strings.
        """.trimMargin()

fun main() {
    generateCodeStyle()
}

private fun generateWarningNames() {
    val enumValNames = Warnings.values().map { it.name }

    val propertyList = enumValNames.map {
        PropertySpec
                .builder(it, String::class)
                .addModifiers(KModifier.CONST)
                .initializer("\"$it\"")
                .build()
    }

    val fileBody = TypeSpec
            .objectBuilder("WarningNames")
            .addProperties(propertyList)
            .build()

    val kotlinFile = FileSpec
            .builder("generated", "WarningNames")
            .addType(fileBody)
            .indent("    ")
            .addComment(autoGenerationComment)
            .build()

    kotlinFile.writeTo(File("diktat-rules/src/main/kotlin"))  // fixme: need to add it to pom
}

private fun generateCodeStyle() {
    val file = File("info/guide/diktat-coding-convention.md")
    val tempFile = File("info/guide/test.tex")
    tempFile.createNewFile()
    val lines = mutableListOf<String>()
    file.forEachLine { lines.add(it) }
    tempFile.printWriter().use { writer ->
        val iterator = lines.iterator()
        writer.writeln("\\lstMakeShortInline`")
        while (iterator.hasNext()) {
            var line = iterator.next()
            if (line.contains("<!--"))
                continue
            if (line.startsWith("#")) {
                val number = Regex("\"([a-z0-9.]*)\"").find(line)?.value?.trim('"')?.substring(1)
                val name = Regex("(</a>[A-Za-z 0-9.-]*)").find(line)?.value?.removePrefix("</a>")?.trim()
                if (name.isNullOrEmpty() || number.isNullOrEmpty())
                    throw NullPointerException("String starts with # but has no number or name - $line")
                when(number.filter { it == '.' }.count()) {
                    0 -> writer.writeln("""\section*{\textbf{$name}}""")
                    1 -> writer.writeln("""\subsection*{\textbf{$name}}""")
                    2 -> writer.writeln("""\subsubsection*{\textbf{$name}}${"\n"}\leavevmode\newline""")
                }
                continue
            }
            if (iterator.hasNext() && line.trim().startsWith("```")) {
                writer.writeln("""\begin{lstlisting}[language=Kotlin]""")
                line = iterator.next()
                while (!line.trim().startsWith("```")) {
                    writer.writeln(line)
                    line = iterator.next()
                }
                writer.writeln("""\end{lstlisting}""")
                continue
            }

            if (line.trim().startsWith("|")) {
                val columnNumber = line.filter { it =='|' }.count() - 1
                val createTable = "|p{3cm}".repeat(columnNumber).plus("|") // May be we will need to calculate column width
                writer.writeln("""\begin{center}""")
                writer.writeln("""\begin{tabular}{ $createTable }""")
                writer.writeln("""\hline""")
                val columnNames = Regex("""[A-Za-z ]*""")
                        .findAll(line)
                        .filter { it.value.isNotEmpty() }
                        .map { it.value.trim() }
                        .toList()
                writer.write(columnNames.joinToString(separator = "&"))
                writer.writeln("""\\""")
                writer.writeln("\\hline")
                iterator.next()
                line = iterator.next()
                while(iterator.hasNext() && line.trim().startsWith("|")) {
                    writer.writeln(line
                            .replace('|', '&')
                            .drop(1)
                            .dropLast(1)
                            .plus("""\\""")
                            .replace("_", "\\_")
                            .replace(Regex("""\(#(.*)\)"""), ""))
                    line = iterator.next()
                }
                writer.writeln("""\hline""")
                writer.writeln("\\end{tabular}")
                writer.writeln("\\end{center}")
            } else {
                var correctedString = findBoldOrItalicText(line, Regex("""\*\*([^*]+)\*\*"""), FindType.BOLD)
                correctedString = findBoldOrItalicText(correctedString, Regex("""\*([A-Za-z ]+)\*"""), FindType.ITALIC)
                correctedString = findBoldOrItalicText(correctedString, Regex("""`([^`]*)`"""), FindType.BACKTICKS)
                correctedString = correctedString.replace(Regex("""\(#(.*)\)"""), "")
                correctedString = correctedString.replace("#", "\\#")
                correctedString = correctedString.replace("&", "\\&")
                correctedString = correctedString.replace("_", "\\_")
                writer.writeln(correctedString)
            }
        }
    }
}

enum class FindType {
    BOLD,
    ITALIC,
    BACKTICKS
}

private fun findBoldOrItalicText(line: String, regex: Regex, type: FindType) : String {
    val allRegex = regex.findAll(line).map { it.value }.toMutableList()
    var correctedLine = line.replace(regex, "R_EP_LA_CE_ME")
    allRegex.forEachIndexed {index, value ->
        when (type) {
            FindType.BOLD -> allRegex[index] = "\\textbf{${value.replace("**", "")}}"
            FindType.ITALIC -> allRegex[index] = "\\textit{${value.replace("*", "")}}"
            FindType.BACKTICKS -> allRegex[index] = value
        }
    }
    allRegex.forEach {
        correctedLine = correctedLine.replaceFirst("R_EP_LA_CE_ME", it)
    }
    return correctedLine
}

private fun PrintWriter.writeln(text: String) {
    write(text.plus("\n"))
}

private fun validateYear() {
    val file = File("diktat-rules/src/test/resources/test/paragraph2/header/CopyrightDifferentYearExpected.kt")
    val tempFile = createTempFile()
    tempFile.printWriter().use { writer ->
        file.forEachLine { line ->
            writer.println(when {
                hyphenRegex.matches(line) -> hyphenRegex.replace(line) {
                    val years = it.value.split("-")
                    val validYears = "${years[0]}-${curYear}"
                    line.replace(hyphenRegex, validYears)
                }
                afterCopyrightRegex.matches(line) -> afterCopyrightRegex.replace(line) {
                    val copyrightYears = it.value.split("(c)", "(C)", "©")
                    val validYears = "${copyrightYears[0]}-${curYear}"
                    line.replace(afterCopyrightRegex, validYears)
                }
                else -> line
            })
        }
    }
    file.delete()
    tempFile.renameTo(file)
}
