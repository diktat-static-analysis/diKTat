/**
 * This is a file, that contains utility methods for latex generation
 */

package org.cqfn.diktat.ruleset.utils

import java.io.PrintWriter

@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val NUMBER_IN_TAG = Regex("\"([a-z0-9.]*)\"")  // finds "r1.0.2"
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val RULE_NAME = Regex("(</a>[A-Za-z 0-9.-]*)")  // get's rule name from ### <a>...</a> Rule name
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val BOLD_TEXT = Regex("""\*\*([^*]+)\*\*""")  // finds bold text in regular lines
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val ITALIC_TEXT = Regex("""\*([A-Za-z ]+)\*""")  // finds italic text in regular lines
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val BACKTICKS_TEXT = Regex("""`([^`]*)`""")  // finds backtick in regular text (not used for now, may be we will need to use it in future)
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val ANCHORS = Regex("""\(#(.*)\)""")  // finds anchors on rules and deletes them
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
val TABLE_COLUMN_NAMES = Regex("""[A-Za-z ]*""")  // used to find column names in tables only
const val REGEX_PLACEHOLDER = "RE_PL_AC_E_ME"
@Suppress("CONSTANT_UPPERCASE")
const val A4_PAPER_WIDTH = 15f

/**
 * Writes text on a new line and adds one blank line
 *
 * @param text String to be written
 */
fun PrintWriter.writeln(text: String) {
    write(text.plus("\n\n"))
}

/**
 * Writes text on a new line. It is used to write code in latex
 *
 * @param code code lines to be printed
 */
fun PrintWriter.writeCode(code: String) {
    write(code.plus("\n"))
}

/**
 * Transforms float number to float with only one digit after dot
 *
 * @param digits Float number to be transformed
 */
fun Float.format(digits: Int) = "%.${digits}f".format(this)
