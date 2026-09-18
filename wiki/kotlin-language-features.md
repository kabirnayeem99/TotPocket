> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Kotlin Language Features Reference

The language-feature toolbox: extension functions and properties, class and property delegation,
scope functions, generics and variance, `inline`/`reified`, sequences vs collections, sealed
hierarchies, value classes, and DSL construction — each with when-to-use guidance, idiomatic usage
and its own anti-patterns. Load this when choosing between two language constructs, or when
designing an API surface (an extension, a delegate, a DSL, a wrapper type).

For value classes specifically (including their Compose stability implications) the
[`kotlin-types-value-class`](../.claude/skills/kotlin-types-value-class/SKILL.md) skill is the
faster read. Coroutines are in [`kotlin-coroutines.md`](kotlin-coroutines.md).

Router: [`.claude/agents/kotlin.md`](../.claude/agents/kotlin.md).

______________________________________________________________________

## EXTENSION FUNCTIONS

### When to Use Extension Functions

| Scenario                                          | Use Extension? | Why                                                 |
| ------------------------------------------------- | -------------- | --------------------------------------------------- |
| Adding utility to a type you don't own            | YES            | Can't modify the source class                       |
| Operations that don't need private state          | YES            | Extensions can't access private members             |
| Domain-specific operations on common types        | YES            | `String.toUserId()`, `List<Job>.activeOnly()`       |
| Making APIs more fluent/readable                  | YES            | `user.toDisplayName()` vs `formatDisplayName(user)` |
| Replacing static utility classes                  | YES            | `StringUtils.capitalize(s)` -> `s.capitalize()`     |
| Operations requiring access to private state      | NO             | Use member functions instead                        |
| Core behavior of the class                        | NO             | Should be a member function                         |
| When it creates confusion about where logic lives | NO             | Prefer clarity over cleverness                      |

### Extension Function Best Practices

```kotlin
// GOOD - Clear, focused, discoverable
fun String.isValidEmail(): Boolean =
    isNotBlank() && contains("@") && contains(".")

// GOOD - Domain-specific extension
fun List<Job>.activeOnly(): List<Job> =
    filter { it.status == JobStatus.ACTIVE }

// GOOD - Nullable receiver for safe operations
fun String?.orEmpty(): String = this ?: ""

// GOOD - Generic extension with constraint
fun <T : Comparable<T>> List<T>.isSorted(): Boolean =
    zipWithNext().all { (a, b) -> a <= b }
```

### Extension Function Anti-Patterns

```kotlin
// BAD - Too generic, pollutes autocomplete
fun Any.log() = println(this)

// BAD - Overusing extensions where a function is clearer
fun Int.fetchUser(): User = userRepository.getById(this)  // Confusing receiver

// BAD - Extension that should be a member (needs private state)
fun MyClass.processInternal() {
    // Can't access private members - this should be a member function
}

// BAD - Stateful extension hiding side effects
fun MutableList<String>.addAndLog(item: String) {
    add(item)
    logger.info("Added $item")  // Hidden side effect
}
```

### Member Extension Functions (Scoped Extensions)

```kotlin
class Connection(val host: Host, val port: Int) {
    // Extension only available within Connection scope
    fun Host.getConnectionString(): String = "$hostname:$port"

    fun connect() {
        val connStr = host.getConnectionString()  // Available here
    }
}
// host.getConnectionString()  // NOT available outside Connection
```

### Extension Properties

```kotlin
// GOOD - Computed property that reads naturally
val String.wordCount: Int
    get() = split("\\s+".toRegex()).size

// GOOD - Domain extension property
val UserProfile.displayInitials: String
    get() = name.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

// NOTE: Extension properties CANNOT have backing fields
// val String.cachedHash: Int = hashCode()  // ERROR - no backing field allowed
```

______________________________________________________________________

## DELEGATION PATTERNS

### Class Delegation (Composition over Inheritance)

```kotlin
// PREFER - Delegation via 'by' keyword
interface Logger {
    fun log(message: String)
    fun error(message: String)
}

class ConsoleLogger : Logger {
    override fun log(message: String) = println("LOG: $message")
    override fun error(message: String) = println("ERROR: $message")
}

// Delegates all Logger methods to 'logger', can override selectively
class TaggedLogger(
    private val tag: String,
    private val logger: Logger
) : Logger by logger {
    override fun log(message: String) = logger.log("[$tag] $message")
    // error() is automatically delegated
}
```

**When to use class delegation:**

- Implement an interface by wrapping an existing implementation
- Decorator pattern without boilerplate
- Composition while satisfying an interface contract
- Override only specific methods while forwarding the rest

### Property Delegation

```kotlin
// lazy - Thread-safe by default, computed once on first access
val expensiveResult: ExpensiveObject by lazy {
    computeExpensiveResult()
}

// lazy with mode - Choose synchronization strategy
val fastLazy: String by lazy(LazyThreadSafetyMode.NONE) {
    // Use NONE when single-threaded access is guaranteed (better performance)
    "computed"
}

// observable - React to property changes
var selectedRole: UserRole by Delegates.observable(UserRole.INSPECTOR) { _, old, new ->
    println("Role changed: $old -> $new")
}

// vetoable - Validate before allowing change
var age: Int by Delegates.vetoable(0) { _, _, newValue ->
    newValue >= 0  // Reject negative values
}

// Map delegation - Properties backed by a map
class Config(private val map: Map<String, Any?>) {
    val host: String by map
    val port: Int by map
    val debug: Boolean by map
}
val config = Config(mapOf("host" to "localhost", "port" to 8080, "debug" to true))
```

#### Custom Property Delegates

```kotlin
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LoggingDelegate<T>(private var value: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        println("Reading ${property.name}: $value")
        return value
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        println("Writing ${property.name}: ${this.value} -> $value")
        this.value = value
    }
}

var username: String by LoggingDelegate("default")
```

### Delegation vs Inheritance

| Factor                          | Inheritance             | Delegation                       |
| ------------------------------- | ----------------------- | -------------------------------- |
| "is-a" relationship             | YES                     | NO                               |
| "has-a" / "uses-a" relationship | NO                      | YES                              |
| Need to override many methods   | Consider                | YES (override only what changes) |
| Multiple behavior compositions  | NO (single inheritance) | YES (multiple delegates)         |
| Runtime behavior swapping       | NO                      | YES                              |

______________________________________________________________________

## SCOPE FUNCTIONS

### Quick Reference

| Function | Object ref | Return value   | Use case                           |
| -------- | ---------- | -------------- | ---------------------------------- |
| `let`    | `it`       | Lambda result  | Null checks, transformations       |
| `run`    | `this`     | Lambda result  | Object config + compute result     |
| `with`   | `this`     | Lambda result  | Grouping calls on an object        |
| `apply`  | `this`     | Context object | Object configuration (builder)     |
| `also`   | `it`       | Context object | Side effects (logging, validation) |

### Idiomatic Usage

```kotlin
// let - Null-safe transformations
val length: Int? = nullableString?.let { it.length }

// run - Configure and compute
val hexColor = Color.RED.run {
    "#${red.toString(16)}${green.toString(16)}${blue.toString(16)}"
}

// with - Group operations (non-null)
val report = with(StringBuilder()) {
    appendLine("Report Title")
    appendLine("=".repeat(40))
    data.forEach { appendLine(it) }
    toString()
}

// apply - Object initialization
val client = HttpClient().apply {
    timeout = 30_000
    retries = 3
    baseUrl = "https://api.example.com"
}

// also - Side effects without modifying chain
val user = createUser(name)
    .also { logger.info("Created user: ${it.id}") }
```

### Scope Function Anti-Patterns

```kotlin
// BAD - Nested scope functions
user?.let { u ->
    u.address?.let { addr ->
        addr.city?.let { city -> println(city) }
    }
}

// GOOD - Chain directly
val city = user?.address?.city
city?.let { println(it) }

// BAD - Scope function where simple expression works
val name = user?.let { it.name } ?: "Unknown"

// GOOD
val name = user?.name ?: "Unknown"
```

______________________________________________________________________

## GENERICS AND VARIANCE

### Declaration-Site Variance

```kotlin
// Producer (out) - Only produces T, never consumes
interface Source<out T> {
    fun next(): T
    // fun add(item: T)  // ERROR
}

// Consumer (in) - Only consumes T, never produces
interface Sink<in T> {
    fun accept(item: T)
    // fun get(): T  // ERROR
}
```

### Generic Constraints

```kotlin
// Single upper bound
fun <T : Comparable<T>> sort(list: List<T>): List<T> = list.sorted()

// Multiple upper bounds with 'where'
fun <T> ensureValid(item: T): T where T : Serializable, T : Comparable<T> = item

// Reified type parameters (inline only)
inline fun <reified T> isType(value: Any): Boolean = value is T

inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson(json, T::class.java)
```

______________________________________________________________________

## INLINE FUNCTIONS AND REIFIED TYPES

### When to Use Inline

| Scenario                              | Use `inline`?  | Why                                 |
| ------------------------------------- | -------------- | ----------------------------------- |
| Function with lambda parameters       | YES            | Eliminates lambda object allocation |
| Small utility called frequently       | YES            | Removes call overhead               |
| Functions needing reified type params | YES (required) | Reified needs inlining              |
| Large function bodies                 | NO             | Code bloat                          |
| Functions without lambda params       | RARELY         | Minimal benefit                     |
| Recursive functions                   | NO             | Cannot inline recursion             |

```kotlin
// GOOD - Inline with lambda
inline fun <T> measureAndReturn(block: () -> T): Pair<T, Long> {
    val start = System.nanoTime()
    val result = block()
    return result to (System.nanoTime() - start)
}

// noinline - When lambda needs to be stored as object
inline fun executeWithCallback(
    action: () -> Unit,
    noinline callback: () -> Unit  // Can be stored/passed
) {
    action()
    scheduleCallback(callback)
}

// crossinline - Lambda used in another execution context
inline fun runInContext(crossinline block: () -> Unit) {
    val runnable = Runnable { block() }
    executor.execute(runnable)
}
```

______________________________________________________________________

## SEQUENCES vs COLLECTIONS

### Decision Guide

| Factor                             | Collection (`List`)            | Sequence (`Sequence`) |
| ---------------------------------- | ------------------------------ | --------------------- |
| Evaluation                         | Eager (all at once)            | Lazy (one at a time)  |
| Intermediate results               | New list per operation         | No intermediate lists |
| Short-circuiting (`first`, `take`) | Processes ALL elements         | Stops at first match  |
| Small collections (< 100)          | FASTER                         | Slower (overhead)     |
| Large collections (1000+)          | More memory                    | FASTER, less memory   |
| Multiple chained operations        | Each creates intermediate list | Single pass           |
| Single operation                   | PREFERRED                      | Unnecessary overhead  |

```kotlin
val people: List<Person> = loadThousandsOfPeople()

// BAD for large lists - 3 intermediate lists
val result = people
    .filter { it.age >= 18 }
    .map { it.name }
    .take(10)

// GOOD for large lists - Single pass, stops after 10
val result = people.asSequence()
    .filter { it.age >= 18 }
    .map { it.name }
    .take(10)
    .toList()

// Operation order matters!
// GOOD - filter first to reduce work
people.asSequence()
    .filter { it.name.length > 5 }
    .map { it.name }
```

### Sequence Builder

```kotlin
fun fibonacci(): Sequence<Long> = sequence {
    var a = 0L; var b = 1L
    while (true) {
        yield(a)
        val next = a + b; a = b; b = next
    }
}
fibonacci().take(10).toList()
```

______________________________________________________________________

## SEALED CLASSES AND INTERFACES

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

// Exhaustive when - compiler enforces all branches
fun <T> handleResult(result: Result<T>) = when (result) {
    is Result.Success -> process(result.data)
    is Result.Error -> showError(result.exception)
    Result.Loading -> showSpinner()
    // No 'else' needed
}
```

______________________________________________________________________

## VALUE CLASSES (Zero-Overhead Type Safety)

```kotlin
@JvmInline
value class UserId(val value: Long)

@JvmInline
value class Email(val address: String) {
    init { require(address.contains("@")) { "Invalid email: $address" } }
    val domain: String get() = address.substringAfter("@")
}

// Type safety without runtime cost
fun sendEmail(to: Email, from: Email) { /* ... */ }
// Compiler prevents: sendEmail(from = userId, to = email)
```

______________________________________________________________________

## DSL CONSTRUCTION

```kotlin
@DslMarker
annotation class ConfigDsl

@ConfigDsl
class NetworkConfig {
    var baseUrl: String = ""
    var timeout: Long = 30_000
    var retries: Int = 3
    private val interceptors = mutableListOf<Interceptor>()

    fun logging(block: LoggingConfig.() -> Unit) {
        interceptors += LoggingInterceptor(LoggingConfig().apply(block))
    }
}

fun networkConfig(block: NetworkConfig.() -> Unit): NetworkConfig =
    NetworkConfig().apply(block)

val config = networkConfig {
    baseUrl = "https://api.example.com"
    timeout = 60_000
    logging {
        level = LogLevel.BODY
        redactHeaders += "Authorization"
    }
}
```
