> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Test Setup, Anti-Patterns and Quick Reference

Test infrastructure and the things that go wrong: version-catalog test dependencies and KMP
source-set build configuration, the four common test anti-patterns (testing implementation details,
real delays, shared mutable state, missing error cases), and the quick reference for Kotest
assertions, Turbine commands and the test dispatcher. Load this when setting up testing in a module
that has none, when a test is flaky or slow, or as an assertion cheat sheet while writing.

The [`testing-setup`](../.claude/skills/testing-setup/SKILL.md) skill covers generic test-harness
setup; this file is the repo's own catalog wiring and rules.

Router: [`.claude/agents/tester.md`](../.claude/agents/tester.md).

______________________________________________________________________

## Test Dependencies

### Version Catalog Configuration

```toml
# gradle/libs.versions.toml
[versions]
kotest = "5.9.1"
turbine = "1.2.0"
coroutines = "1.9.0"
mockative = "3.0.1"
composeTest = "1.10.0"

[libraries]
# Testing Core
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }
kotest-assertions-core = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
kotest-framework-engine = { module = "io.kotest:kotest-framework-engine", version.ref = "kotest" }

# Coroutine Testing
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

# Flow Testing
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

# Mocking
mockative = { module = "io.mockative:mockative", version.ref = "mockative" }
mockative-processor = { module = "io.mockative:mockative-processor", version.ref = "mockative" }

# Compose UI Testing
compose-ui-test = { module = "org.jetbrains.compose.ui:ui-test", version.ref = "composeTest" }
compose-ui-test-junit4 = { module = "org.jetbrains.compose.ui:ui-test-junit4", version.ref = "composeTest" }
```

### Build Configuration

```kotlin
// build.gradle.kts (KMP module)
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotest.framework.engine)
                implementation(libs.mockative)
            }
        }

        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.compose.ui.test.junit4)
            }
        }
    }
}

// For Mockative KSP
dependencies {
    add("kspAndroidTest", libs.mockative.processor)
}
```

______________________________________________________________________

## Common Anti-Patterns to Avoid

### 1. Testing Implementation Details

```kotlin
// WRONG - Tests internal state
@Test
fun `test internal list size`() {
    viewModel.loadJobs()
    viewModel._internalJobsList.size shouldBe 5  // Don't access private state
}

// CORRECT - Tests observable behavior
@Test
fun `when jobs loaded, state contains jobs`() {
    viewModel.uiState.test {
        viewModel.onEvent(JobListEvent.LoadJobs)
        awaitItem().jobs.size shouldBe 5
    }
}
```

### 2. Using Real Delays

```kotlin
// WRONG - Flaky, slow test
@Test
fun `test with delay`() = runTest {
    viewModel.loadJobs()
    delay(1000)  // Don't use real delays
    viewModel.uiState.value.isLoading shouldBe false
}

// CORRECT - Use test dispatcher
@Test
fun `test with dispatcher`() = runTest {
    viewModel.loadJobs()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.uiState.value.isLoading shouldBe false
}
```

### 3. Shared Mutable State

```kotlin
// WRONG - Tests affect each other
class BadTest {
    companion object {
        val sharedFake = FakeRepository()  // Shared between tests
    }
}

// CORRECT - Fresh state per test
class GoodTest {
    private lateinit var fakeRepository: FakeRepository

    @BeforeTest
    fun setup() {
        fakeRepository = FakeRepository()  // Fresh for each test
    }
}
```

### 4. Missing Error Cases

```kotlin
// WRONG - Only tests happy path
@Test
fun `loads jobs successfully`() { ... }

// CORRECT - Test error scenarios too
@Test
fun `loads jobs successfully`() { ... }

@Test
fun `handles network error`() { ... }

@Test
fun `handles empty response`() { ... }

@Test
fun `handles malformed data`() { ... }
```

______________________________________________________________________

## Quick Reference

### Kotest Assertions

| Assertion                 | Usage               |
| ------------------------- | ------------------- |
| `shouldBe`                | Equality check      |
| `shouldNotBe`             | Inequality check    |
| `shouldBeNull()`          | Null check          |
| `shouldNotBeNull()`       | Not null check      |
| `shouldBeInstanceOf<T>()` | Type check          |
| `shouldContain`           | Collection contains |
| `shouldBeEmpty()`         | Empty collection    |
| `shouldHaveSize(n)`       | Collection size     |
| `shouldBeSuccess()`       | Result success      |
| `shouldBeFailure()`       | Result failure      |

### Turbine Commands

| Command            | Usage                  |
| ------------------ | ---------------------- |
| `test { }`         | Start flow testing     |
| `awaitItem()`      | Wait for next emission |
| `awaitComplete()`  | Wait for completion    |
| `awaitError()`     | Wait for error         |
| `skipItems(n)`     | Skip n emissions       |
| `expectNoEvents()` | Assert no emissions    |
| `cancel()`         | Cancel collection      |

### Test Dispatcher

| Command                                       | Usage                      |
| --------------------------------------------- | -------------------------- |
| `StandardTestDispatcher()`                    | Create test dispatcher     |
| `Dispatchers.setMain(testDispatcher)`         | Set main dispatcher        |
| `Dispatchers.resetMain()`                     | Reset main dispatcher      |
| `testDispatcher.scheduler.advanceUntilIdle()` | Run all pending coroutines |
| `testDispatcher.scheduler.advanceTimeBy(ms)`  | Advance virtual time       |
