package org.cqfn.diktat.ruleset.rules.kdoc

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_SECTION
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG_NAME
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.BLANK_LINE_AFTER_KDOC
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NEWLINES_BEFORE_BASIC_TAGS
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_EMPTY_KDOC
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NO_DEPRECATED_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NO_EMPTY_TAGS
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NO_NEWLINES_BETWEEN_BASIC_TAGS
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NO_NEWLINE_AFTER_SPECIAL_TAGS
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WRONG_SPACES_AFTER_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WRONG_TAGS_ORDER
import org.cqfn.diktat.ruleset.utils.*
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * Formatting visitor for Kdoc:
 * 1) removing all blank lines between Kdoc and the code it's declaring
 * 2) ensuring there are no tags with empty content
 * 3) ensuring there is only one white space between tag and it's body
 * 4) ensuring tags @apiNote, @implSpec, @implNote have one empty line after their body
 * 5) ensuring tags @param, @return, @throws are arranged in this order
 */
@Suppress("ForbiddenComment")
class KdocFormatting(private val configRules: List<RulesConfig>) : Rule("kdoc-formatting") {
    private val basicTagsList = listOf(KDocKnownTag.PARAM, KDocKnownTag.RETURN, KDocKnownTag.THROWS)
    private val specialTagNames = setOf("implSpec", "implNote", "apiNote")

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false
    private var fileName: String = ""

    override fun visit(node: ASTNode,
                       autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        isFixMode = autoCorrect
        emitWarn = emit
        fileName = node.getRootNode().getUserData(KtLint.FILE_PATH_USER_DATA_KEY)!!

        val declarationTypes = setOf(CLASS, FUN, PROPERTY)

        if (declarationTypes.contains(node.elementType)) {
            checkBlankLineAfterKdoc(node)
        }

        if (node.elementType == KDOC && checkKdocNotEmpty(node)) {
            checkNoDeprecatedTag(node)
            checkEmptyTags(node.kDocTags())
            checkSpaceAfterTag(node.kDocTags())
            node.kDocBasicTags()?.let { checkEmptyLineBeforeBasicTags(it) }
            node.kDocBasicTags()?.let { checkEmptyLinesBetweenBasicTags(it) }
            checkBasicTagsOrder(node)
            checkNewLineAfterSpecialTags(node)
        }
    }

    private fun checkBlankLineAfterKdoc(node: ASTNode) {
        val kdoc = node.getFirstChildWithType(KDOC)
        val nodeAfterKdoc = kdoc?.treeNext
        val name = node.getFirstChildWithType(ElementType.IDENTIFIER)
        if (nodeAfterKdoc?.elementType == WHITE_SPACE && nodeAfterKdoc.text.countSubStringOccurrences("\n") > 1) {
            BLANK_LINE_AFTER_KDOC.warnAndFix(configRules, emitWarn, isFixMode, name!!.text, nodeAfterKdoc.startOffset, nodeAfterKdoc) {
                nodeAfterKdoc.leaveOnlyOneNewLine()
            }
        }
    }

    private fun checkKdocNotEmpty(node: ASTNode): Boolean {
        val isKdocNotEmpty = node.getFirstChildWithType(KDOC_SECTION)
                ?.hasChildMatching {
                    it.elementType != KDOC_LEADING_ASTERISK && it.elementType != WHITE_SPACE
                } ?: false
        if (!isKdocNotEmpty) {
            KDOC_EMPTY_KDOC.warn(configRules, emitWarn, isFixMode,
                node.treeParent.getIdentifierName()?.text
                    ?: node.nextSibling { it.elementType in KtTokens.KEYWORDS }?.text
                    ?: fileName, node.startOffset, node)
        }
        return isKdocNotEmpty
    }

    private fun checkNoDeprecatedTag(node: ASTNode) {
        val kDocTags = node.kDocTags()
        kDocTags?.find { it.name == "deprecated" }
            ?.let { kDocTag ->
                KDOC_NO_DEPRECATED_TAG.warnAndFix(configRules, emitWarn, isFixMode, kDocTag.text, kDocTag.node.startOffset, kDocTag.node) {
                    val kDocSection = kDocTag.node.treeParent
                    val deprecatedTagNode = kDocSection.getChildren(TokenSet.create(KDOC_TAG))
                        .find { "@deprecated" in it.text }!!
                    kDocSection.removeRange(deprecatedTagNode.prevSibling { it.elementType == WHITE_SPACE }!!,
                        deprecatedTagNode.nextSibling { it.elementType == WHITE_SPACE }
                    )
                    node.treeParent.addChild(LeafPsiElement(ElementType.ANNOTATION,
                        "@Deprecated(message = \"${kDocTag.getContent()}\")"), node.treeNext)
                    // copy to get all necessary indents
                    node.treeParent.addChild(node.nextSibling { it.elementType == WHITE_SPACE }!!.clone() as PsiWhiteSpaceImpl, node.treeNext) }
                }
    }

    private fun checkEmptyTags(kDocTags: Collection<KDocTag>?) {
        kDocTags?.filter {
            it.getSubjectName() == null && it.getContent().isEmpty()
        }?.forEach {
            KDOC_NO_EMPTY_TAGS.warn(configRules, emitWarn, isFixMode, "@${it.name!!}", it.node.startOffset, it.node)
        }
    }

    private fun checkSpaceAfterTag(kDocTags: Collection<KDocTag>?) {
        // tags can have 'parameters' and content, either can be missing
        // we always can find white space after tag name, but after tag parameters only if content is present
        kDocTags?.filter { tag ->
            val hasSubject = tag.getSubjectName()?.isNotBlank() ?: false
            if (!hasSubject && tag.getContent().isBlank()) return@filter false

            val (isSpaceBeforeContentError, isSpaceAfterTagError) = findBeforeAndAfterSpaces(tag)

            hasSubject && isSpaceBeforeContentError || isSpaceAfterTagError
        }?.forEach { tag ->
            KDOC_WRONG_SPACES_AFTER_TAG.warnAndFix(configRules, emitWarn, isFixMode,
                "@${tag.name!!}", tag.node.startOffset, tag.node) {
                tag.node.findChildBefore(KDOC_TEXT, WHITE_SPACE)
                        ?.let { tag.node.replaceChild(it, LeafPsiElement(WHITE_SPACE, " ")) }
                tag.node.findChildAfter(KDOC_TAG_NAME, WHITE_SPACE)
                        ?.let { tag.node.replaceChild(it, LeafPsiElement(WHITE_SPACE, " ")) }
            }
        }
    }

    private fun findBeforeAndAfterSpaces(tag: KDocTag): Pair<Boolean, Boolean> {
        return Pair(tag.node.findChildBefore(KDOC_TEXT, WHITE_SPACE).let {
            it?.text != " "
                    && !(it?.isWhiteSpaceWithNewline() ?: false)
        },
                tag.node.findChildAfter(KDOC_TAG_NAME, WHITE_SPACE).let {
                    it?.text != " "
                            && !(it?.isWhiteSpaceWithNewline() ?: false)
                }
        )
    }

    private fun checkBasicTagsOrder(node: ASTNode) {
        val kDocTags = node.kDocTags()
        // distinct basic tags which are present in current KDoc, in proper order
        val basicTagsOrdered = basicTagsList.filter { basicTag ->
            kDocTags?.find { it.knownTag == basicTag } != null
        }
        // all basic tags from curent KDoc
        val basicTags = kDocTags?.filter { basicTagsOrdered.contains(it.knownTag) }
        val isTagsInCorrectOrder = basicTags
                ?.fold(mutableListOf<KDocTag>()) { acc, kDocTag ->
                    if (acc.size > 0 && acc.last().knownTag != kDocTag.knownTag) acc.add(kDocTag)
                    else if (acc.size == 0) acc.add(kDocTag)
                    acc
                }
                ?.map { it.knownTag }?.equals(basicTagsOrdered)

        if (kDocTags != null && !isTagsInCorrectOrder!!) {
            KDOC_WRONG_TAGS_ORDER.warnAndFix(configRules, emitWarn, isFixMode,
                basicTags.joinToString(", ") { "@${it.name}" }, basicTags.first().node.startOffset, basicTags.first().node) {
                val kDocSection = node.getFirstChildWithType(KDOC_SECTION)!!
                val basicTagChildren = kDocTags
                        .filter { basicTagsOrdered.contains(it.knownTag) }
                        .map { it.node }

                basicTagsOrdered.forEachIndexed { index, tag ->
                    val tagNode = kDocTags.find { it.knownTag == tag }?.node
                    kDocSection.addChild(tagNode!!.clone() as CompositeElement, basicTagChildren[index])
                    kDocSection.removeChild(basicTagChildren[index])
                }
            }
        }
    }

    private fun checkEmptyLineBeforeBasicTags(basicTags: List<KDocTag>) {
        val firstBasicTag = basicTags.firstOrNull()
        if (firstBasicTag != null) {
            val hasContentBefore = firstBasicTag.node.allSiblings(true)
                    .let { it.subList(0, it.indexOf(firstBasicTag.node)) }
                    .any { it.elementType !in arrayOf(WHITE_SPACE, KDOC_LEADING_ASTERISK) && it.text.isNotBlank() }

            val previousTag = firstBasicTag.node.prevSibling { it.elementType == KDOC_TAG }
            val hasEmptyLineBefore = previousTag?.hasEmptyLineAfter()
                    ?: (firstBasicTag.node.previousAsterisk()
                            ?.previousAsterisk()
                            ?.treeNext?.elementType == WHITE_SPACE)

            if (hasContentBefore xor hasEmptyLineBefore) {
                KDOC_NEWLINES_BEFORE_BASIC_TAGS.warnAndFix(configRules, emitWarn, isFixMode,
                    "@${firstBasicTag.name!!}", firstBasicTag.node.startOffset, firstBasicTag.node) {
                    if (hasContentBefore) {
                        if (previousTag != null) {
                            previousTag.applyToPrevSibling(KDOC_LEADING_ASTERISK) {
                                previousTag.addChild(treePrev.clone() as ASTNode, null)
                                previousTag.addChild(this.clone() as ASTNode, null)
                            }
                        } else {
                            firstBasicTag.node.applyToPrevSibling(KDOC_LEADING_ASTERISK) {
                                treeParent.addChild(treePrev.clone() as ASTNode, this)
                                treeParent.addChild(this.clone() as ASTNode, treePrev)
                            }
                        }
                    } else {
                        firstBasicTag.node.apply {
                            val asteriskNode = previousAsterisk()!!
                            treeParent.removeChild(asteriskNode.treePrev)
                            treeParent.removeChild(asteriskNode.treePrev)
                        }
                    }
                }
            }
        }
    }

    private fun checkEmptyLinesBetweenBasicTags(basicTags: List<KDocTag>) {
        val tagsWithRedundantEmptyLines = basicTags.dropLast(1).filterNot { tag ->
            val nextWhiteSpace = tag.node.nextSibling { it.elementType == WHITE_SPACE }
            val noEmptyKdocLines = tag.node.getChildren(TokenSet.create(KDOC_LEADING_ASTERISK))
                    .filter { it.treeNext == null || it.treeNext.elementType == WHITE_SPACE }
                    .count() == 0
            nextWhiteSpace?.text?.count { it == '\n' } == 1 && noEmptyKdocLines
        }

        tagsWithRedundantEmptyLines.forEach { tag ->
            KDOC_NO_NEWLINES_BETWEEN_BASIC_TAGS.warnAndFix(configRules, emitWarn, isFixMode,
                "@${tag.name}", tag.startOffset, tag.node) {
                tag.node.nextSibling { it.elementType == WHITE_SPACE }?.leaveOnlyOneNewLine()
                // the first asterisk before tag is not included inside KDOC_TAG node
                // we look for the second and take its previous which should be WHITE_SPACE with newline
                tag.node.getAllChildrenWithType(KDOC_LEADING_ASTERISK).firstOrNull()
                        ?.let { tag.node.removeRange(it.treePrev, null) }
            }
        }
    }

    private fun checkNewLineAfterSpecialTags(node: ASTNode) {
        val presentSpecialTagNodes = node.getFirstChildWithType(KDOC_SECTION)
                ?.getAllChildrenWithType(KDOC_TAG)
                ?.filter { (it.psi as KDocTag).name in specialTagNames }

        val poorlyFormattedTagNodes = presentSpecialTagNodes?.filterNot { specialTagNode ->
            // empty line with just * followed by white space or end of block
            specialTagNode.lastChildNode.elementType == KDOC_LEADING_ASTERISK
                    && (specialTagNode.treeNext == null || specialTagNode.treeNext.elementType == WHITE_SPACE
                    && specialTagNode.treeNext.text.count { it == '\n' } == 1)
                    // and with no empty line before
                    && specialTagNode.lastChildNode.treePrev.elementType == WHITE_SPACE
                    && specialTagNode.lastChildNode.treePrev.treePrev.elementType != KDOC_LEADING_ASTERISK
        }

        if (presentSpecialTagNodes != null && poorlyFormattedTagNodes!!.isNotEmpty()) {
            KDOC_NO_NEWLINE_AFTER_SPECIAL_TAGS.warnAndFix(configRules, emitWarn, isFixMode,
                poorlyFormattedTagNodes.joinToString(", ") { "@${(it.psi as KDocTag).name!!}" },
                poorlyFormattedTagNodes.first().startOffset, node) {
                poorlyFormattedTagNodes.forEach { node ->
                    while (node.lastChildNode.elementType == KDOC_LEADING_ASTERISK
                            && node.lastChildNode.treePrev.treePrev.elementType == KDOC_LEADING_ASTERISK) {
                        node.removeChild(node.lastChildNode)  // KDOC_LEADING_ASTERISK
                        node.removeChild(node.lastChildNode)  // WHITE_SPACE
                    }
                    if (node.lastChildNode.elementType != KDOC_LEADING_ASTERISK) {
                        val indent = node.prevSibling { it.elementType == WHITE_SPACE }
                                ?.text?.substringAfter('\n')?.count { it == ' ' } ?: 0
                        node.addChild(LeafPsiElement(WHITE_SPACE, "\n${" ".repeat(indent)}"), null)
                        node.addChild(LeafPsiElement(KDOC_LEADING_ASTERISK, "*"), null)
                    }
                }
            }
        }
    }

    // fixme this method can be improved and extracted to utils
    private fun ASTNode.hasEmptyLineAfter(): Boolean {
        require(this.elementType == KDOC_TAG) { "This check is only for KDOC_TAG" }
        return lastChildNode.elementType == KDOC_LEADING_ASTERISK
                && (treeNext == null || treeNext.elementType == WHITE_SPACE
                && treeNext.text.count { it == '\n' } == 1)
    }

    private fun ASTNode.kDocBasicTags() = kDocTags()?.filter { basicTagsList.contains(it.knownTag) }

    private fun ASTNode.previousAsterisk() = prevSibling { it.elementType == KDOC_LEADING_ASTERISK }

    private fun ASTNode.applyToPrevSibling(elementType: IElementType, consumer: ASTNode.() -> Unit) {
        prevSibling { it.elementType == elementType }?.apply(consumer)
    }
}
