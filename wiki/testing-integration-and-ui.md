> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Integration and UI Testing

Repository integration tests (data-source coordination and DTO mapping), Compose Multiplatform UI
tests, Android UI Automator tests, and the test-organization conventions — directory structure,
source-set layout, naming conventions and worked test-method names. Load this when the test crosses
a component boundary or drives real UI, or when you need to decide where a new test file goes and
what to call it.

The shorter companion for Compose UI assertions and semantics is the
[`compose-ui-testing-patterns`](../.claude/skills/compose-ui-testing-patterns/SKILL.md) skill.
Unit-level patterns: [`testing-unit-patterns.md`](testing-unit-patterns.md).

Router: [`.claude/agents/tester.md`](../.claude/agents/tester.md).

______________________________________________________________________

## Integration Testing

### Repository Integration Tests

Test the full data flow from network to domain model.

```kotlin
package app.syarah.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

class JobRepositoryIntegrationTest {

    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private lateinit var dataSource: JobNetworkDataSource
    private lateinit var repository: JobRepository

    @BeforeTest
    fun setup() {
        mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/jobs" -> respond(
                    content = """[
                        {"id": "1", "title": "Job 1", "status": "PENDING"},
                        {"id": "2", "title": "Job 2", "status": "ASSIGNED"}
                    ]""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("Not found", HttpStatusCode.NotFound)
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }

        dataSource = JobNetworkDataSourceImpl(JobApiService(httpClient))
        repository = JobRepositoryImpl(
            dataSource = dataSource,
            errorManager = FakeErrorManager(),
            ioDispatcher = StandardTestDispatcher()
        )
    }

    @Test
    fun `fetches and maps jobs from API`() = runTest {
        repository.getJobs().test {
            val result = awaitItem()
            result.isSuccess shouldBe true

            val jobs = result.getOrNull()!!
            jobs.size shouldBe 2
            jobs[0].title shouldBe "Job 1"
            jobs[0].status shouldBe JobStatus.PENDING
            jobs[1].status shouldBe JobStatus.ASSIGNED

            awaitComplete()
        }
    }

    @AfterTest
    fun tearDown() {
        httpClient.close()
    }
}
```

______________________________________________________________________

## UI Testing

### Compose UI Testing (Multiplatform)

```kotlin
package app.syarah.features.jobs

import androidx.compose.ui.test.*
import kotlin.test.Test

class JobListScreenTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `displays loading indicator when loading`() = runComposeUiTest {
        setContent {
            JobListContent(
                uiState = JobListUiState(isLoading = true),
                onEvent = {}
            )
        }

        onNodeWithTag("loading_indicator").assertIsDisplayed()
        onNodeWithTag("job_list").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `displays jobs when loaded`() = runComposeUiTest {
        val testJobs = listOf(
            Job(id = "1", title = "Inspection A", status = JobStatus.PENDING),
            Job(id = "2", title = "Inspection B", status = JobStatus.ASSIGNED)
        )

        setContent {
            JobListContent(
                uiState = JobListUiState(jobs = testJobs, isLoading = false),
                onEvent = {}
            )
        }

        onNodeWithTag("job_list").assertIsDisplayed()
        onNodeWithText("Inspection A").assertIsDisplayed()
        onNodeWithText("Inspection B").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `clicking job triggers event`() = runComposeUiTest {
        var clickedJobId: String? = null
        val testJobs = listOf(Job(id = "job-1", title = "Test Job", status = JobStatus.PENDING))

        setContent {
            JobListContent(
                uiState = JobListUiState(jobs = testJobs),
                onEvent = { event ->
                    if (event is JobListEvent.JobClicked) {
                        clickedJobId = event.jobId
                    }
                }
            )
        }

        onNodeWithText("Test Job").performClick()

        clickedJobId shouldBe "job-1"
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `displays error message when error occurs`() = runComposeUiTest {
        setContent {
            JobListContent(
                uiState = JobListUiState(error = "Network connection failed"),
                onEvent = {}
            )
        }

        onNodeWithText("Network connection failed").assertIsDisplayed()
        onNodeWithTag("retry_button").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `retry button triggers reload event`() = runComposeUiTest {
        var retryTriggered = false

        setContent {
            JobListContent(
                uiState = JobListUiState(error = "Error"),
                onEvent = { event ->
                    if (event is JobListEvent.RetryClicked) {
                        retryTriggered = true
                    }
                }
            )
        }

        onNodeWithTag("retry_button").performClick()

        retryTriggered shouldBe true
    }
}
```

### Android UI Automator Tests

For Android-specific end-to-end tests with UI Automator.

```kotlin
package app.syarah.inspection

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowE2ETest {

    private lateinit var device: UiDevice
    private val timeout = 5000L

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Launch app
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = context.packageManager.getLaunchIntentForPackage("app.syarah.inspection")
        context.startActivity(intent)

        // Wait for app to launch
        device.wait(Until.hasObject(By.pkg("app.syarah.inspection")), timeout)
    }

    @Test
    fun loginFlow_withValidCredentials_navigatesToJobList() {
        // Enter username
        device.findObject(By.res("app.syarah.inspection:id/username_field"))
            .text = "testuser"

        // Enter password
        device.findObject(By.res("app.syarah.inspection:id/password_field"))
            .text = "password123"

        // Click login button
        device.findObject(By.res("app.syarah.inspection:id/login_button"))
            .click()

        // Wait for job list to appear
        val jobList = device.wait(
            Until.hasObject(By.res("app.syarah.inspection:id/job_list")),
            timeout
        )

        assert(jobList) { "Job list should be displayed after login" }
    }

    @Test
    fun loginFlow_withInvalidCredentials_showsError() {
        // Enter invalid credentials
        device.findObject(By.res("app.syarah.inspection:id/username_field"))
            .text = "invalid"
        device.findObject(By.res("app.syarah.inspection:id/password_field"))
            .text = "wrong"

        // Click login
        device.findObject(By.res("app.syarah.inspection:id/login_button"))
            .click()

        // Wait for error message
        val errorMessage = device.wait(
            Until.hasObject(By.textContains("Invalid credentials")),
            timeout
        )

        assert(errorMessage) { "Error message should be displayed" }
    }
}
```

______________________________________________________________________

## Test Organization

### Directory Structure

```
module/
├── src/
│   ├── commonMain/kotlin/           # Production code
│   ├── commonTest/kotlin/           # Shared unit tests
│   │   └── app/syarah/module/
│   │       ├── ViewModelTest.kt
│   │       ├── UseCaseTest.kt
│   │       └── fakes/               # Fake implementations
│   │           └── FakeRepository.kt
│   ├── androidHostTest/kotlin/      # Android JVM tests
│   │   └── app/syarah/module/
│   │       └── IntegrationTest.kt
│   └── androidDeviceTest/kotlin/    # Android instrumented tests
│       └── app/syarah/module/
│           ├── ScreenTest.kt        # Compose UI tests
│           └── E2ETest.kt           # UI Automator tests
```

### Naming Conventions

| Test Type   | File Naming                   | Method Naming                      |
| ----------- | ----------------------------- | ---------------------------------- |
| Unit Test   | `{Class}Test.kt`              | `when condition, then expectation` |
| Integration | `{Feature}IntegrationTest.kt` | `feature_scenario_expectedResult`  |
| UI Test     | `{Screen}Test.kt`             | `uiElement_action_expectedState`   |
| E2E Test    | `{Flow}E2ETest.kt`            | `flow_scenario_expectedOutcome`    |

### Test Method Naming Examples

```kotlin
// Unit tests - backtick style with descriptive language
@Test
fun `when user clicks login with valid credentials, then navigates to home`()

@Test
fun `when network fails, then error state is emitted`()

// Integration tests - underscore style
@Test
fun loginRepository_withValidToken_returnsUserProfile()

// UI tests - describes UI interaction
@Test
fun loginButton_whenFormIsValid_isEnabled()
```
