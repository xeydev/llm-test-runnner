package io.llmttestrunner.adapter

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import io.llmttestrunner.artifact.Matcher
import io.llmttestrunner.artifact.MatcherType
import io.llmttestrunner.artifact.TestCommand

/**
 * Test framework adapter for Jetpack Compose Testing.
 *
 * Provides integration with androidx.compose.ui.test APIs.
 */
class ComposeTestAdapter(
    private val composeTestRule: ComposeTestRule
) : BaseTestFrameworkAdapter() {

    override fun captureViewHierarchy(): String {
        return composeTestRule.onRoot(useUnmergedTree = true).printToString()
    }

    override fun executeClick(command: TestCommand) {
        withNode(command) { performClick() }
    }

    override fun executeLongClick(command: TestCommand) {
        withNode(command) {
            performTouchInput { longClick() }
        }
    }

    override fun executeDoubleClick(command: TestCommand) {
        withNode(command) {
            performTouchInput { doubleClick() }
        }
    }

    override fun executeTypeText(command: TestCommand) {
        val text = command.value ?: error("TYPE_TEXT action requires a non-null value")
        withNode(command) {
            try {
                performTextReplacement(text)
            } catch (_: AssertionError) {
                performTextInput(text)
            }
        }
    }

    override fun executeClearText(command: TestCommand) {
        withNode(command) { performTextClearance() }
    }

    @OptIn(ExperimentalTestApi::class)
    override fun executeScrollTo(command: TestCommand) {
        withNode(command) { performScrollTo() }
    }

    override fun executeAssertVisible(command: TestCommand) {
        withNode(command) { assertIsDisplayed() }
    }

    override fun executeAssertText(command: TestCommand) {
        val expected = command.value ?: error("ASSERT_TEXT action requires a non-null value")
        withNode(command) {
            val lines = expected.split('\n')
            assertTextEquals(*lines.toTypedArray())
        }
    }

    override fun executeAssertContains(command: TestCommand) {
        val expected = command.value ?: error("ASSERT_CONTAINS action requires a non-null value")
        withNode(command) { assertTextContains(expected, substring = true) }
    }

    private fun withNode(
        command: TestCommand,
        block: SemanticsNodeInteraction.() -> Unit
    ) {
        composeTestRule.waitForIdle()
        val interaction = findNode(command.matcher)
        interaction.assertExists()
        interaction.block()
        composeTestRule.waitForIdle()
    }

    private fun findNode(matcher: Matcher): SemanticsNodeInteraction {
        return when (matcher.type) {
            MatcherType.TEST_TAG -> composeTestRule.onNodeWithTag(matcher.value, useUnmergedTree = true)
            MatcherType.TEXT -> composeTestRule.onNodeWithText(matcher.value)
            MatcherType.CONTENT_DESCRIPTION -> composeTestRule.onNodeWithContentDescription(matcher.value)
            MatcherType.HIERARCHY -> findNodeByHierarchy(matcher.value)
        }
    }

    private fun findNodeByHierarchy(rawValue: String): SemanticsNodeInteraction {
        val normalized = rawValue.trim()
        val segments = parseHierarchyPath(normalized)

        require(segments.isNotEmpty()) { "Hierarchy matcher value cannot be empty" }
        require(segments.first().name.equals("Root", ignoreCase = true)) {
            "Hierarchy path must start with 'Root': $normalized"
        }

        val expectedIndices = segments.drop(1).mapIndexed { depth, segment ->
            segment.index ?: error(
                "Hierarchy segment '${segment.name}' at depth ${depth + 1} must include an index, e.g. '${segment.name}[0]'. Path: $normalized"
            )
        }

        val matcherDescription = "Hierarchy path equals '$normalized'"
        val matcher = SemanticsMatcher(matcherDescription) { node ->
            computeHierarchyIndices(node) == expectedIndices
        }

        return composeTestRule.onNode(matcher, useUnmergedTree = true)
    }

    private fun parseHierarchyPath(rawPath: String): List<HierarchySegment> {
        if (rawPath.isBlank()) return emptyList()

        return rawPath.split('>').mapNotNull { segment ->
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            val match = HIERARCHY_SEGMENT_REGEX.matchEntire(trimmed)
                ?: throw IllegalArgumentException("Invalid hierarchy segment '$trimmed' in path '$rawPath'")

            val name = match.groupValues[1].trim()
            val index = match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toIntOrNull()

            HierarchySegment(name = name, index = index)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun computeHierarchyIndices(node: SemanticsNode): List<Int> {
        if (node.parent == null) {
            return emptyList()
        }

        val indices = mutableListOf<Int>()
        var current: SemanticsNode? = node

        while (true) {
            val parent = current?.parent ?: break
            val children = parent.children
            val currentId = current.id

            val index = children.indexOfFirst { it.id == currentId }
            if (index == -1) {
                return emptyList()
            }

            indices += index
            current = parent
        }

        return indices.asReversed()
    }

    private data class HierarchySegment(
        val name: String,
        val index: Int?
    )

    companion object {
        private val HIERARCHY_SEGMENT_REGEX = Regex("^([^\\[]+?)(\\[(\\d+)])?$")
    }
}

