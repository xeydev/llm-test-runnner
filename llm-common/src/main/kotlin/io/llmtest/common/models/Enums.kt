package io.llmtest.common.models

enum class ActionType(val value: String) {
    CLICK("click"),
    LONG_CLICK("longClick"),
    DOUBLE_CLICK("doubleClick"),
    TYPE_TEXT("typeText"),
    CLEAR_TEXT("clearText"),
    SCROLL_TO("scrollTo"),
    ASSERT_VISIBLE("assertVisible"),
    ASSERT_TEXT("assertText"),
    ASSERT_CONTAINS("assertContains");

    companion object {
        fun fromString(value: String): ActionType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown action: $value")
        }
    }
}

enum class MatcherType(val value: String) {
    TEST_TAG("testTag"),
    HIERARCHY("hierarchy"),
    TEXT("text"),
    CONTENT_DESCRIPTION("contentDescription");

    companion object {
        fun fromString(value: String): MatcherType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown matcher type: $value")
        }
    }
}

data class TestCommand(
    val action: ActionType,
    val value: String?,
    val matcher: Matcher
)

data class Matcher(
    val type: MatcherType,
    val value: String,
    val rationale: String? 
)

