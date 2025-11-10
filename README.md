# LLM Test Runner

An AI-powered Android UI testing framework that allows you to write natural language test scenarios for Jetpack Compose apps. The framework uses LLMs (OpenAI GPT or Anthropic Claude) to parse natural language instructions into structured test commands.

## 🏗️ Architecture

The system consists of three main components:

```
┌─────────────────────┐
│   Android Test      │
│   (Instrumentation) │
└──────────┬──────────┘
           │ HTTP
           ▼
┌─────────────────────┐
│   Bridge Server     │
│   (Kotlin/Ktor)     │
└──────────┬──────────┘
           │ API
           ▼
┌─────────────────────┐
│   LLM Provider      │
│  (OpenAI/Claude)    │
└─────────────────────┘
```

### Components

1. **Android App** (`app/`)
   - Jetpack Compose UI with test instrumentation
   - Test orchestrator that caches artifacts and manages test flow
   - Test adapter that communicates with bridge server
   - DSL for writing natural language tests
   - Artifact collection (screen context, generated commands)

2. **Bridge Server** (`bridge-server/`)
   - Kotlin/Ktor server that acts as intermediary
   - Translates natural language → LLM prompts → structured commands
   - Supports OpenAI GPT-4o-mini and Anthropic Claude Sonnet
   - Caches responses for performance
   - Manages test artifacts

3. **LLM Provider**
   - Parses natural language test steps
   - Analyzes screen hierarchy
   - Returns structured JSON commands with optimal matchers

## 🚀 How It Works

1. **Write Natural Language Test**
   ```kotlin
   llmTest("Submit form test") {
       step("Click on the email field")
       step("Type 'user@example.com'")
       step("Click Submit button")
       step("Check that 'Success!' message is displayed")
   }
   ```

2. **Test Execution Flow**
   - Test runner captures current screen hierarchy (semantics tree)
   - Sends `user_step` + `screen_hierarchy` to bridge server
   - Bridge server sends to LLM with specialized prompt
   - LLM returns structured JSON with action and matcher
   - Test adapter executes the action on Compose UI
   - Artifacts (screenshots, hierarchy) collected on each step

3. **LLM Response Example**
   ```json
   {
     "status": "OK",
     "actions": [{
       "action": "assertText",
       "value": "Success!",
       "matcher": {
         "type": "testTag",
         "value": "success_message",
         "rationale": "testTag is most stable matcher available"
       }
     }]
   }
   ```

## 📋 Prerequisites

- **Java 17+** (for Gradle and Kotlin)
- **Android SDK** with API 34+
- **Android Device/Emulator** (for running tests)
- **API Key**: Either `OPENAI_API_KEY` or `ANTHROPIC_API_KEY`

## ⚙️ Setup

### 1. Set API Key

Choose one LLM provider and set the environment variable:

**Option A: OpenAI (GPT-4o-mini)**
```bash
export OPENAI_API_KEY="sk-..."
```

**Option B: Anthropic (Claude Sonnet)**
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

### 2. Build And Run Bridge Server

```bash
./gradlew runBridge
cd ..
```

### 3: Setup ADB Port Forwarding

In a separate terminal:

```bash
adb reverse tcp:37546 tcp:37546
```

This allows the Android emulator/device to reach `localhost:37546` on your machine.

### 4: Run Android Tests

**Option A: Using Gradle**
```bash
./gradlew app:connectedDebugAndroidTest
```

**Option B: Using Android Studio**
1. Open the project in Android Studio
2. Navigate to `app/src/androidTest/`
3. Right-click on a test file
4. Select "Run"

## 📝 Writing Tests

### Basic Test Structure

```kotlin
@Test
fun myNaturalLanguageTest() = runTest {
    llmTest("My Test Name") {
        step("Click on login button")
        step("Type 'user@test.com' in email field")
        step("Type 'password123' in password field")
        step("Click submit")
        step("Check that 'Welcome!' is displayed")
    }
}
```

### Natural Language Examples

**Clicks:**
- `"Click on the Submit button"`
- `"Tap the menu icon"`
- `"Long press the item"`
- `"Double click refresh button"`

**Text Input:**
- `"Type 'hello@example.com' in email field"`
- `"Enter 'MyPassword123'"`
- `"Clear the search box"`

**Assertions:**
- `"Check that 'Success!' is displayed"` (text + visibility)
- `"Verify submit button is visible"` (just visibility)
- `"Assert error message contains 'invalid'"` (partial match)

**Scrolling:**
- `"Scroll to the Terms section"`

## 🔧 Configuration

### LLM Model Selection

Edit `bridge-server/src/main/kotlin/io/llmtest/bridge/LLMServiceFactory.kt`:

**OpenAI:**
```kotlin
model = "gpt-4o-mini" // or "gpt-4o", "gpt-4-turbo"
```

**Anthropic:**
```kotlin
model = "claude-3-5-sonnet-20241022" // or other Claude models
```

### Test Tags (Recommended)

For most stable tests, add testTags to your Compose UI:

```kotlin
Button(
    onClick = { /* ... */ },
    modifier = Modifier.testTag("submit_button")
) {
    Text("Submit")
}
```

The LLM prioritizes matchers in this order:
1. `testTag` (most stable)
2. `contentDescription`
3. `hierarchy` path
4. `text` (least stable)

## 📊 Test Artifacts

After each **successful** test run, artifacts are automatically saved to your local machine via the bridge server.

### Configuration

The artifact save location is configured in `build.gradle.kts`:

```kotlin
// Configuration for bridge server
val bridgeServerPort = 37546
val artifactsDirectory = file("app/src/androidTest/artifacts")
```

You can change `artifactsDirectory` to any path on your local machine.

### Artifact Structure

```
artifacts/
├── basic_submit.json          # Test execution log
├── multiple_submissions.json
└── ...
```

**Artifact Contents:**
- LLM commands and responses
- Test execution timeline

### Test Fails to Find Element

The LLM might have chosen a wrong matcher. Check artifacts:
1. Open `artifacts/test_name.json`
2. Look at the `screen_hierarchy` for that step
3. Verify the matcher in the LLM response
4. Adjust your natural language or add testTags
