> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Kotlin Coroutines Reference

Everything this repo expects of coroutine code: main-thread protection, dispatcher selection and
injection, structured concurrency, ViewModel coroutine patterns, main-safe Flow pipelines and
operators, cooperative cancellation, exception handling, coroutine testing, the coroutine
anti-pattern list and performance tips. Load this whenever you write or review a `suspend` function,
a `Flow`, a `CoroutineScope`, or anything that launches work.

Shorter, focused companions:
[`kotlin-coroutines-structured-concurrency`](../.claude/skills/kotlin-coroutines-structured-concurrency/SKILL.md)
for scope/launch/cancellation discipline, and
[`kotlin-flow-state-event-modeling`](../.claude/skills/kotlin-flow-state-event-modeling/SKILL.md)
for StateFlow/SharedFlow/Channel API shape. Load this file when you need the full treatment or the
worked examples.

Router: [`.claude/agents/kotlin.md`](../.claude/agents/kotlin.md).

______________________________________________________________________

## COROUTINES: COMPREHENSIVE GUIDE

### CRITICAL RULE: Main Thread Protection

The main thread renders UI at 60fps (16ms per frame). ANY blocking operation on the main thread
causes jank or ANR (Application Not Responding after 5 seconds). **ALL suspend functions MUST be
main-safe.**

```kotlin
// FORBIDDEN - Blocking the main thread
class BadRepository {
    suspend fun loadData(): Data {
        return heavyDatabaseQuery()  // BLOCKS MAIN THREAD!
    }
}

// REQUIRED - Main-safe with withContext
class GoodRepository(
    private val ioDispatcher: CoroutineDispatcher  // INJECTED, not hardcoded
) {
    suspend fun loadData(): Data = withContext(ioDispatcher) {
        heavyDatabaseQuery()  // Runs on IO thread pool
    }
}
```

### Dispatcher Selection (MUST Follow)

| Dispatcher               | Use For                 | Thread Pool         | Example                          |
| ------------------------ | ----------------------- | ------------------- | -------------------------------- |
| `Dispatchers.Main`       | UI updates ONLY         | Single UI thread    | Updating StateFlow, navigation   |
| `Dispatchers.IO`         | Network, disk, database | 64+ shared threads  | API calls, file reads, DataStore |
| `Dispatchers.Default`    | CPU-heavy computation   | CPU cores pool      | Sorting, parsing JSON, regex     |
| `Dispatchers.Unconfined` | Testing ONLY            | No thread switching | Never in production code         |

```kotlin
// CORRECT dispatcher usage per operation type
class JobRepository(
    private val ioDispatcher: CoroutineDispatcher,      // For network/disk
    private val defaultDispatcher: CoroutineDispatcher   // For CPU work
) {
    // Network call → IO
    suspend fun fetchJobs(): List<Job> = withContext(ioDispatcher) {
        apiService.getJobs()
    }

    // CPU-intensive parsing → Default
    suspend fun parseJobsFromCsv(csv: String): List<Job> = withContext(defaultDispatcher) {
        csv.lines().map { parseJobLine(it) }
    }

    // Combined: fetch on IO, transform on Default
    suspend fun fetchAndProcessJobs(): List<ProcessedJob> {
        val raw = withContext(ioDispatcher) { apiService.getJobs() }
        return withContext(defaultDispatcher) { raw.map { processJob(it) } }
    }
}
```

### RULE: Always Inject Dispatchers (NEVER Hardcode)

```kotlin
// FORBIDDEN - Hardcoded dispatcher, untestable
class BadRepository {
    suspend fun loadData() = withContext(Dispatchers.IO) { /* ... */ }
}

// REQUIRED - Injected dispatcher, testable
class GoodRepository(
    private val ioDispatcher: CoroutineDispatcher  // Inject via Koin/constructor
) {
    suspend fun loadData() = withContext(ioDispatcher) { /* ... */ }
}

// Koin DI setup
val dispatcherModule = module {
    single<CoroutineDispatcher>(named("io")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("default")) { Dispatchers.Default }
    single<CoroutineDispatcher>(named("main")) { Dispatchers.Main }
}

// In tests - use TestDispatcher
class RepositoryTest {
    @Test
    fun `test loads data`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repo = GoodRepository(ioDispatcher = testDispatcher)
        // Deterministic, no real threading
    }
}
```

### Structured Concurrency

**Every coroutine must have a parent scope.** When the parent is cancelled, all children are
cancelled. This prevents leaks.

```kotlin
// CORRECT - Structured concurrency with coroutineScope
suspend fun loadDashboard(): Dashboard = coroutineScope {
    // All three run in parallel, all cancelled if any fails
    val profile = async { profileRepository.load() }
    val stats = async { statsRepository.load() }
    val notifications = async { notificationRepository.load() }

    Dashboard(
        profile = profile.await(),
        stats = stats.await(),
        notifications = notifications.await()
    )
}

// CORRECT - supervisorScope when you want partial failure
suspend fun loadDashboardGraceful(): Dashboard = supervisorScope {
    val profile = async { profileRepository.load() }
    val stats = async {
        try { statsRepository.load() }
        catch (e: Exception) { emptyStats() }  // Don't cancel siblings
    }
    val notifications = async {
        try { notificationRepository.load() }
        catch (e: Exception) { emptyList() }
    }

    Dashboard(
        profile = profile.await(),
        stats = stats.await(),
        notifications = notifications.await()
    )
}

// FORBIDDEN - GlobalScope (unstructured, leaks, untestable)
fun loadData() {
    GlobalScope.launch { /* NEVER DO THIS */ }
}
```

### ViewModel Coroutine Patterns

```kotlin
class JobListViewModel(
    private val getJobs: GetJobsUseCase,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState.asStateFlow()

    // CORRECT - ViewModel creates coroutines, owns the scope
    fun loadJobs() {
        viewModelScope.launch {  // Tied to ViewModel lifecycle
            _uiState.update { it.copy(isLoading = true) }
            try {
                val jobs = getJobs()  // Must be main-safe internally
                _uiState.update { it.copy(jobs = jobs, isLoading = false) }
            } catch (e: CancellationException) {
                throw e  // NEVER swallow CancellationException
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    // CORRECT - Collecting flows
    init {
        viewModelScope.launch {
            getJobs.observeJobs()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { jobs -> _uiState.update { it.copy(jobs = jobs) } }
        }
    }

    // FORBIDDEN - Exposing suspend functions from ViewModel
    // suspend fun loadJobs() = getJobs()  // BAD - who launches this?
}
```

### Flow: Main-Safe Pipelines

```kotlin
// CORRECT - Repository exposes Flow with proper dispatcher
class JobRepositoryImpl(
    private val apiService: JobApiService,
    private val ioDispatcher: CoroutineDispatcher,
    private val errorManager: ErrorManager
) : JobRepository {

    // One-shot as Flow with proper threading
    override fun getJobs(): Flow<Result<List<Job>>> = flow {
        val networkJobs = apiService.fetchJobs()
        emit(networkJobs.map { it.asExternalModel() })
    }
        .flowOn(ioDispatcher)        // Upstream runs on IO
        .toResult()                   // Wraps in Result
        .withErrorHandler(errorManager)

    // Continuous observation with polling
    override fun observeJobs(): Flow<List<Job>> = flow {
        while (true) {
            emit(apiService.fetchJobs().map { it.asExternalModel() })
            delay(30_000)  // Suspends, doesn't block
        }
    }.flowOn(ioDispatcher)

    // Combining multiple flows
    override fun observeJobsWithStatus(): Flow<List<JobWithStatus>> =
        combine(
            observeJobs(),
            statusRepository.observeStatuses()
        ) { jobs, statuses ->
            jobs.map { job ->
                JobWithStatus(job, statuses[job.id])
            }
        }
        .flowOn(Dispatchers.Default)  // CPU work on Default
        .distinctUntilChanged()
}
```

### Flow Operators for Clean Pipelines

```kotlin
// flowOn - Switch upstream dispatcher (NEVER downstream)
flow { emit(heavyComputation()) }
    .flowOn(Dispatchers.Default)  // heavyComputation runs on Default
    .collect { updateUi(it) }     // collect runs on caller's dispatcher

// map/filter/transform - Standard transformations
jobsFlow
    .map { jobs -> jobs.filter { it.isActive } }
    .map { activeJobs -> activeJobs.sortedBy { it.dueDate } }

// debounce - For search/input (avoid rapid API calls)
searchQueryFlow
    .debounce(300)  // Wait 300ms after last emission
    .distinctUntilChanged()
    .flatMapLatest { query -> searchRepository.search(query) }

// catch - Handle errors without crashing the flow
dataFlow
    .catch { e ->
        if (e is IOException) emit(cachedData)
        else throw e  // Re-throw unexpected errors
    }

// retry - Automatic retry with backoff
flow { emit(apiService.fetchData()) }
    .retry(retries = 3) { cause ->
        cause is IOException && delay(1000).let { true }
    }

// stateIn - Convert cold Flow to hot StateFlow
val jobs: StateFlow<List<Job>> = jobRepository.observeJobs()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

// shareIn - Share a cold Flow among multiple collectors
val sharedEvents: SharedFlow<Event> = eventSource.events()
    .shareIn(
        scope = applicationScope,
        started = SharingStarted.Lazily,
        replay = 0
    )
```

### Cancellation: Cooperative and Correct

```kotlin
// RULE: All suspend functions in kotlinx.coroutines are cancellable
// (delay, withContext, yield, etc.)

// For CPU-bound loops, check cancellation manually
suspend fun processLargeList(items: List<Item>) = withContext(Dispatchers.Default) {
    for (item in items) {
        ensureActive()  // Throws CancellationException if cancelled
        processItem(item)
    }
}

// CRITICAL: NEVER swallow CancellationException
try {
    suspendingOperation()
} catch (e: CancellationException) {
    throw e  // ALWAYS re-throw
} catch (e: Exception) {
    handleError(e)
}

// Cleanup on cancellation with finally
suspend fun downloadFile(url: String) {
    val tempFile = createTempFile()
    try {
        writeToFile(tempFile, downloadStream(url))
    } finally {
        // Runs even on cancellation
        if (!isActive) tempFile.delete()  // Cleanup
    }
}

// NonCancellable - For critical cleanup that MUST complete
suspend fun saveAndCleanup() {
    try {
        apiService.save(data)
    } finally {
        withContext(NonCancellable) {
            // This block completes even if coroutine is cancelled
            database.markAsSaved()
            cache.invalidate()
        }
    }
}
```

### Exception Handling in Coroutines

```kotlin
// launch: Exceptions propagate to parent (crash if unhandled)
// async: Exceptions stored until .await() is called

// Pattern 1: try-catch inside launch
viewModelScope.launch {
    try {
        repository.saveData(data)
    } catch (e: CancellationException) { throw e }
    catch (e: IOException) { showError("Network error") }
    catch (e: Exception) { showError("Unexpected error: ${e.message}") }
}

// Pattern 2: CoroutineExceptionHandler (last resort, not for async)
val handler = CoroutineExceptionHandler { _, exception ->
    logger.error("Unhandled coroutine exception", exception)
}
scope.launch(handler) {
    riskyOperation()
}

// Pattern 3: supervisorScope for independent children
suspend fun loadMultipleSources() = supervisorScope {
    // If one fails, others continue
    val source1 = async {
        try { api1.fetch() } catch (e: Exception) { fallback1() }
    }
    val source2 = async {
        try { api2.fetch() } catch (e: Exception) { fallback2() }
    }
    CombinedResult(source1.await(), source2.await())
}

// Pattern 4: Result wrapper in repositories
override fun signIn(username: String, password: String): Flow<Result<UserProfile>> =
    flow {
        val data = networkDataSource.signIn(username, password)
        emit(data.asExternalModel())
    }
    .toResult()                      // Catches exceptions → Result.Error
    .withErrorHandler(errorManager)  // Reports to error manager
    .flowOn(ioDispatcher)            // All upstream on IO
```

### Coroutine Testing Patterns

```kotlin
class LoginViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)  // Replace Main dispatcher
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success updates state`() = runTest {
        val fakeRepo = FakeLoginRepository(Result.success(mockProfile))
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onEvent(LoginEvent.LoginClicked)
        advanceUntilIdle()  // Process all pending coroutines

        viewModel.uiState.value.isLoggedIn shouldBe true
    }

    @Test
    fun `flow emissions collected correctly`() = runTest {
        val repo = FakeJobRepository()
        val viewModel = JobListViewModel(repo, StandardTestDispatcher(testScheduler))

        viewModel.uiState.test {  // Turbine
            awaitItem().isLoading shouldBe false
            repo.emitJobs(listOf(mockJob))
            awaitItem().jobs shouldBe listOf(mockJob)
        }
    }
}
```

### Coroutine Anti-Patterns

```kotlin
// ANTI-PATTERN 1: runBlocking in production code
fun getData(): Data = runBlocking {  // BLOCKS the calling thread!
    repository.fetchData()
}

// ANTI-PATTERN 2: GlobalScope
GlobalScope.launch { saveData() }  // Leaks! No lifecycle!

// ANTI-PATTERN 3: Hardcoded dispatchers
suspend fun load() = withContext(Dispatchers.IO) { /* untestable */ }

// ANTI-PATTERN 4: Swallowing CancellationException
try { delay(1000) } catch (e: Exception) { /* Swallowed cancellation! */ }

// ANTI-PATTERN 5: Creating scope inside suspend function
suspend fun bad() {
    CoroutineScope(Dispatchers.IO).launch { /* Unstructured! Leaks! */ }
}

// ANTI-PATTERN 6: Not using flowOn, doing work on Main
fun getJobs(): Flow<List<Job>> = flow {
    emit(heavyDatabaseQuery())  // Running on Main thread!
}

// ANTI-PATTERN 7: Collecting flow on wrong dispatcher
viewModelScope.launch(Dispatchers.IO) {  // DON'T - UI state updates need Main
    repository.observeData().collect { _uiState.value = it }
}

// CORRECT - Collect on Main, flow does its work on IO internally
viewModelScope.launch {  // Main by default
    repository.observeData().collect { _uiState.value = it }  // UI update on Main
}

// ANTI-PATTERN 8: Using async without await (fire and forget)
coroutineScope {
    async { saveData() }  // Deferred result ignored! Use launch instead
}

// ANTI-PATTERN 9: Nested withContext (unnecessary)
withContext(Dispatchers.IO) {
    withContext(Dispatchers.IO) {  // Redundant! Already on IO
        loadData()
    }
}
```

### Coroutine Performance Tips

```kotlin
// 1. Use Dispatchers.IO.limitedParallelism() for rate limiting
private val dbDispatcher = Dispatchers.IO.limitedParallelism(4)
// Only 4 concurrent database operations

// 2. Use SharingStarted.WhileSubscribed with timeout
val state = flow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),  // Keep alive 5s after last subscriber
    initialValue = default
)

// 3. Use conflate() for fast producers / slow consumers
sensorDataFlow
    .conflate()  // Drop intermediate values, keep latest
    .collect { updateChart(it) }

// 4. Use buffer() for pipeline parallelism
producerFlow
    .buffer(Channel.BUFFERED)  // Producer and consumer run in parallel
    .map { transform(it) }
    .collect { save(it) }

// 5. Use flatMapMerge for concurrent processing
idsFlow
    .flatMapMerge(concurrency = 4) { id ->
        flow { emit(fetchDetails(id)) }
    }
    .collect { details -> process(details) }
```
