> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Unit Testing Patterns

The bulk of this repo's test-writing material: ViewModel, UseCase and Repository unit tests; fakes,
stubs and shared test fixtures; Flow testing with Turbine; and mocking with Mockative. Load this
whenever you are writing a test below the integration line — which, per the test pyramid, is most of
them.

Integration and UI tests: [`testing-integration-and-ui.md`](testing-integration-and-ui.md).
Dependencies, anti-patterns and the assertion quick reference:
[`testing-setup-and-antipatterns.md`](testing-setup-and-antipatterns.md).

Router: [`.claude/agents/tester.md`](../.claude/agents/tester.md).

______________________________________________________________________

## Unit Testing Patterns

### ViewModel Testing

ViewModels are the primary target for unit tests. They coordinate UI state and business logic.

```kotlin
package app.syarah.features.jobs

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeJobRepository: FakeJobRepository
    private lateinit var viewModel: JobListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeJobRepository = FakeJobRepository()
        viewModel = JobListViewModel(fakeJobRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty with no loading`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            initialState.jobs shouldBe emptyList()
            initialState.isLoading shouldBe false
            initialState.error.shouldBeNull()
        }
    }

    @Test
    fun `when load jobs succeeds, state contains jobs`() = runTest {
        // Given
        val expectedJobs = listOf(
            Job(id = "1", title = "Inspection A", status = JobStatus.PENDING),
            Job(id = "2", title = "Inspection B", status = JobStatus.ASSIGNED)
        )
        fakeJobRepository.jobs = expectedJobs

        // When & Then
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.onEvent(JobListEvent.LoadJobs)

            // Loading state
            awaitItem().isLoading shouldBe true

            // Advance coroutines
            testDispatcher.scheduler.advanceUntilIdle()

            // Success state
            val successState = awaitItem()
            successState.isLoading shouldBe false
            successState.jobs shouldBe expectedJobs
            successState.error.shouldBeNull()
        }
    }

    @Test
    fun `when load jobs fails, state contains error`() = runTest {
        // Given
        fakeJobRepository.shouldFail = true
        fakeJobRepository.errorMessage = "Network error"

        // When & Then
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.onEvent(JobListEvent.LoadJobs)
            awaitItem() // Loading

            testDispatcher.scheduler.advanceUntilIdle()

            val errorState = awaitItem()
            errorState.isLoading shouldBe false
            errorState.error shouldBe "Network error"
            errorState.jobs shouldBe emptyList()
        }
    }

    @Test
    fun `filter by status updates filtered jobs`() = runTest {
        // Given
        val allJobs = listOf(
            Job(id = "1", title = "Job A", status = JobStatus.PENDING),
            Job(id = "2", title = "Job B", status = JobStatus.COMPLETED),
            Job(id = "3", title = "Job C", status = JobStatus.PENDING)
        )
        fakeJobRepository.jobs = allJobs

        // Load jobs first
        viewModel.onEvent(JobListEvent.LoadJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.uiState.test {
            skipItems(3) // Initial, loading, success

            viewModel.onEvent(JobListEvent.FilterByStatus(JobStatus.PENDING))

            val filteredState = awaitItem()
            filteredState.filteredJobs.size shouldBe 2
            filteredState.filteredJobs.all { it.status == JobStatus.PENDING } shouldBe true
        }
    }
}
```

### UseCase Testing

UseCases contain business logic and should have thorough tests for all scenarios.

```kotlin
package app.syarah.domain.usecase

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.result.shouldBeFailure
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class StartInspectionUseCaseTest {

    private lateinit var fakeJobRepository: FakeJobRepository
    private lateinit var fakeInspectionRepository: FakeInspectionRepository
    private lateinit var useCase: StartInspectionUseCase

    @BeforeTest
    fun setup() {
        fakeJobRepository = FakeJobRepository()
        fakeInspectionRepository = FakeInspectionRepository()
        useCase = StartInspectionUseCase(
            jobRepository = fakeJobRepository,
            inspectionRepository = fakeInspectionRepository
        )
    }

    @Test
    fun `when job is assigned, inspection starts successfully`() = runTest {
        // Given
        val assignedJob = Job(
            id = "job-1",
            title = "Test Job",
            status = JobStatus.ASSIGNED,
            assignedTo = "inspector-1"
        )
        fakeJobRepository.jobs = listOf(assignedJob)

        val expectedInspection = Inspection(
            id = "inspection-1",
            jobId = "job-1",
            status = InspectionStatus.IN_PROGRESS
        )
        fakeInspectionRepository.inspectionToReturn = expectedInspection

        // When
        val result = useCase("job-1")

        // Then
        result.shouldBeSuccess()
        result.getOrNull() shouldBe expectedInspection
    }

    @Test
    fun `when job is not assigned, returns failure`() = runTest {
        // Given
        val pendingJob = Job(
            id = "job-1",
            title = "Test Job",
            status = JobStatus.PENDING,  // Not assigned
            assignedTo = null
        )
        fakeJobRepository.jobs = listOf(pendingJob)

        // When
        val result = useCase("job-1")

        // Then
        result.shouldBeFailure()
        result.exceptionOrNull()?.message shouldBe
            "Cannot start inspection for job with status: PENDING"
    }

    @Test
    fun `when job not found, returns failure`() = runTest {
        // Given - empty repository
        fakeJobRepository.jobs = emptyList()

        // When
        val result = useCase("nonexistent-job")

        // Then
        result.shouldBeFailure()
        result.exceptionOrNull() shouldBe IllegalArgumentException::class
    }

    @Test
    fun `when job already completed, returns failure`() = runTest {
        // Given
        val completedJob = Job(
            id = "job-1",
            title = "Test Job",
            status = JobStatus.COMPLETED,
            assignedTo = "inspector-1"
        )
        fakeJobRepository.jobs = listOf(completedJob)

        // When
        val result = useCase("job-1")

        // Then
        result.shouldBeFailure()
    }
}
```

### Repository Testing

Test repository implementations to verify data source coordination and DTO mapping.

```kotlin
package app.syarah.data.repository

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LoginRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNetworkDataSource: FakeLoginNetworkDataSource
    private lateinit var fakeErrorManager: FakeErrorManager
    private lateinit var repository: LoginRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeNetworkDataSource = FakeLoginNetworkDataSource()
        fakeErrorManager = FakeErrorManager()
        repository = LoginRepositoryImpl(
            loginNetworkDataSource = fakeNetworkDataSource,
            errorManager = fakeErrorManager,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `signIn returns mapped domain model on success`() = runTest(testDispatcher) {
        // Given
        val networkProfile = NetworkUserProfileData(
            photographer = null,
            inspector = NetworkInspectorProfile(
                id = 123,
                name = "John Doe",
                authKey = "auth-key-123"
            )
        )
        fakeNetworkDataSource.profileToReturn = networkProfile

        // When
        repository.signIn("username", "password").test {
            // Then - verify mapping via asExternalModel()
            val result = awaitItem()
            result.isSuccess shouldBe true

            val profile = result.getOrNull()
            profile.shouldNotBeNull()
            profile.inspector.shouldNotBeNull()
            profile.inspector!!.name shouldBe "John Doe"
            profile.inspector!!.authKey shouldBe "auth-key-123"

            awaitComplete()
        }
    }

    @Test
    fun `signIn emits error to ErrorManager on failure`() = runTest(testDispatcher) {
        // Given
        fakeNetworkDataSource.shouldFail = true
        fakeNetworkDataSource.errorToThrow = Exception("Network error")

        // When
        repository.signIn("username", "password").test {
            val result = awaitItem()

            // Then
            result.isFailure shouldBe true
            fakeErrorManager.lastError.shouldNotBeNull()
            fakeErrorManager.lastError!!.message shouldBe "Network error"

            awaitComplete()
        }
    }

    @Test
    fun `signIn executes on IO dispatcher`() = runTest(testDispatcher) {
        // Given
        fakeNetworkDataSource.profileToReturn = createTestProfile()

        // When - collect on test dispatcher
        val result = repository.signIn("user", "pass").first()

        // Then - should complete without hanging (proves flowOn works)
        result.isSuccess shouldBe true
    }
}
```

______________________________________________________________________

## Fake/Stub Patterns

### Creating Fakes for Testing

Fakes are preferred over mocks for KMP compatibility. Create fakes that implement repository
interfaces.

```kotlin
package app.syarah.testing.fakes

/**
 * Fake implementation of JobRepository for testing.
 * Allows controlling behavior through public properties.
 */
class FakeJobRepository : JobRepository {

    // Control data
    var jobs: List<Job> = emptyList()
    var jobsById: MutableMap<String, Job> = mutableMapOf()

    // Control behavior
    var shouldFail: Boolean = false
    var errorMessage: String = "Test error"
    var delayMillis: Long = 0

    // Track calls
    var getJobsCallCount: Int = 0
        private set
    var lastRequestedJobId: String? = null
        private set

    override suspend fun getJobs(): List<Job> {
        getJobsCallCount++
        if (delayMillis > 0) delay(delayMillis)
        if (shouldFail) throw Exception(errorMessage)
        return jobs
    }

    override suspend fun getJobById(id: String): Job? {
        lastRequestedJobId = id
        if (shouldFail) throw Exception(errorMessage)
        return jobsById[id] ?: jobs.find { it.id == id }
    }

    override fun observeJobs(): Flow<List<Job>> = flow {
        if (shouldFail) throw Exception(errorMessage)
        emit(jobs)
    }

    // Test helpers
    fun reset() {
        jobs = emptyList()
        jobsById.clear()
        shouldFail = false
        errorMessage = "Test error"
        delayMillis = 0
        getJobsCallCount = 0
        lastRequestedJobId = null
    }
}

/**
 * Fake implementation of LoginNetworkDataSource.
 */
class FakeLoginNetworkDataSource : LoginNetworkDataSource {

    var profileToReturn: NetworkUserProfileData? = null
    var shouldFail: Boolean = false
    var errorToThrow: Throwable = Exception("Network error")

    override suspend fun signInWithUsernameAndPassword(
        username: String,
        password: String
    ): NetworkUserProfileData {
        if (shouldFail) throw errorToThrow
        return profileToReturn ?: throw IllegalStateException("No profile configured")
    }
}

/**
 * Fake ErrorManager for testing error emission.
 */
class FakeErrorManager : ErrorManager {

    var lastError: AppError? = null
        private set
    var errors: MutableList<AppError> = mutableListOf()
        private set

    override fun emit(error: AppError) {
        lastError = error
        errors.add(error)
    }

    override fun observeErrors(): Flow<AppError> = flow {
        errors.forEach { emit(it) }
    }

    fun reset() {
        lastError = null
        errors.clear()
    }
}
```

### Test Fixtures

Create reusable test data factories.

```kotlin
package app.syarah.testing.fixtures

/**
 * Factory for creating test Job instances.
 */
object JobFixtures {

    fun createJob(
        id: String = "job-${System.currentTimeMillis()}",
        title: String = "Test Job",
        description: String = "Test description",
        status: JobStatus = JobStatus.PENDING,
        assignedTo: String? = null,
        scheduledAt: Instant = Clock.System.now()
    ) = Job(
        id = id,
        title = title,
        description = description,
        status = status,
        assignedTo = assignedTo,
        scheduledAt = scheduledAt,
        location = createLocation()
    )

    fun createAssignedJob(
        id: String = "job-${System.currentTimeMillis()}",
        inspectorId: String = "inspector-1"
    ) = createJob(
        id = id,
        status = JobStatus.ASSIGNED,
        assignedTo = inspectorId
    )

    fun createCompletedJob(id: String = "job-${System.currentTimeMillis()}") =
        createJob(id = id, status = JobStatus.COMPLETED)

    fun createLocation(
        address: String = "123 Test Street",
        latitude: Double = 24.7136,
        longitude: Double = 46.6753
    ) = Location(address = address, latitude = latitude, longitude = longitude)

    fun createJobList(count: Int = 5): List<Job> =
        (1..count).map { createJob(id = "job-$it", title = "Job $it") }
}

/**
 * Factory for creating test User instances.
 */
object UserFixtures {

    fun createInspectorProfile(
        id: Long = 1L,
        name: String = "John Inspector",
        authKey: String = "auth-key-${System.currentTimeMillis()}"
    ) = InspectorProfile(id = id, name = name, authKey = authKey)

    fun createNetworkInspectorProfile(
        id: Long = 1L,
        name: String = "John Inspector",
        authKey: String = "auth-key-${System.currentTimeMillis()}"
    ) = NetworkInspectorProfile(id = id, name = name, authKey = authKey)
}
```

______________________________________________________________________

## Flow Testing with Turbine

Turbine is essential for testing Kotlin Flows and StateFlows.

### Basic Flow Testing

```kotlin
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.kotest.matchers.shouldBe

@Test
fun `flow emits expected values`() = runTest {
    val flow = flowOf(1, 2, 3)

    flow.test {
        awaitItem() shouldBe 1
        awaitItem() shouldBe 2
        awaitItem() shouldBe 3
        awaitComplete()
    }
}

@Test
fun `flow handles errors`() = runTest {
    val flow = flow<Int> {
        emit(1)
        throw Exception("Test error")
    }

    flow.test {
        awaitItem() shouldBe 1
        val error = awaitError()
        error.message shouldBe "Test error"
    }
}
```

### StateFlow Testing

```kotlin
@Test
fun `stateFlow reflects state changes`() = runTest {
    val viewModel = JobListViewModel(FakeJobRepository())

    viewModel.uiState.test {
        // Initial state
        awaitItem().isLoading shouldBe false

        // Trigger action
        viewModel.onEvent(JobListEvent.LoadJobs)

        // Loading state
        awaitItem().isLoading shouldBe true

        // Complete loading
        testDispatcher.scheduler.advanceUntilIdle()

        // Success state
        awaitItem().isLoading shouldBe false
    }
}
```

### Multiple Flow Testing

```kotlin
import app.cash.turbine.turbineScope

@Test
fun `multiple flows can be tested together`() = runTest {
    turbineScope {
        val jobsFlow = repository.observeJobs().testIn(backgroundScope)
        val userFlow = repository.observeUser().testIn(backgroundScope)

        jobsFlow.awaitItem() shouldBe emptyList()
        userFlow.awaitItem().shouldBeNull()

        // Trigger update
        repository.addJob(testJob)

        jobsFlow.awaitItem().size shouldBe 1

        jobsFlow.cancel()
        userFlow.cancel()
    }
}
```

______________________________________________________________________

## Mocking with Mockative

**Reality check (verified across `core/data`, `features/login`, `features/addcar/presentation`,
`core/care`):** no module in this repo actually has Mockative/KSP wired, despite the version catalog
listing `mockative`/`mockative-processor` aliases. The real, consistently-used convention is **fakes
only** (see "Fake/Stub Patterns" above). Do not add Mockative to a module's `build.gradle.kts` to
satisfy a test — write a fake instead, even for large interfaces (e.g. an 18-method
`RemoteConfigProvider`-style provider). If the class under test is `androidMain` and its constructor
requires a real `android.content.Context`/`ContentResolver` with no mocking library available,
follow `features/addcar/presentation`'s `VinScannerLauncherAndroidTest` precedent: extract the
framework-free logic into its own function/class that takes plain Kotlin types (or plain lambdas,
see `core/care`'s `CareAttachmentHandler`) and test that seam directly in `androidHostTest` — don't
fabricate a `Context` and don't skip the module's test coverage because of it. If no such seam
exists yet, it's fine to skip that file and say so explicitly rather than inventing one under
test-writing time pressure (extracting a seam is a production-code change, out of scope for a
tests-only pass).

The Mockative sample below is retained as a reference for if/when this repo actually adopts it —
treat it as aspirational, not current convention.

Mockative provides KMP-compatible mocking via KSP.

### Setup

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    commonTestImplementation(libs.mockative)
    add("kspCommonMainMetadata", libs.mockative.processor)
}
```

### Basic Mocking

```kotlin
import io.mockative.*

class JobListViewModelMockTest {

    @Mock
    private lateinit var jobRepository: JobRepository

    private lateinit var viewModel: JobListViewModel

    @BeforeTest
    fun setup() {
        // Configure mock behavior
        every { jobRepository.getJobs() }.returns(flowOf(Result.success(testJobs)))

        viewModel = JobListViewModel(jobRepository)
    }

    @Test
    fun `calls repository when loading jobs`() = runTest {
        viewModel.onEvent(JobListEvent.LoadJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { jobRepository.getJobs() }.wasInvoked(exactly = 1)
    }

    @Test
    fun `handles repository error`() = runTest {
        every { jobRepository.getJobs() }
            .returns(flowOf(Result.failure(Exception("Error"))))

        viewModel.onEvent(JobListEvent.LoadJobs)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.value.error shouldBe "Error"
    }
}
```
