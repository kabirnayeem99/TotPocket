> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Kotlin Clean Code and Performance Reference

The ten clean-code rules (val over var, expression bodies, named arguments, default parameters,
`when` over `if-else` chains, no `!!`, destructuring, preconditions, type aliases, string
templates), the language-level performance guidance (object creation, string building, collection
performance, lazy initialization) and the general Kotlin anti-patterns. Load this while writing or
reviewing ordinary Kotlin — it is the "is this idiomatic and cheap" reference, not the concurrency
or API-design one.

Naming conventions and the pre-completion checklist stay in the router,
[`.claude/agents/kotlin.md`](../.claude/agents/kotlin.md). Coroutines:
[`kotlin-coroutines.md`](kotlin-coroutines.md). Language constructs:
[`kotlin-language-features.md`](kotlin-language-features.md).

______________________________________________________________________

## KOTLIN CLEAN CODE RULES

### Rule 1: Prefer `val` Over `var`

```kotlin
val name = "Alice"        // GOOD - immutable
var retryCount = 0        // OK only when mutation is truly needed
```

### Rule 2: Expression Bodies for Simple Functions

```kotlin
fun isAdult(age: Int): Boolean = age >= 18
```

### Rule 3: Named Arguments for Clarity

```kotlin
// BAD
createUser("Alice", true, false, true)
// GOOD
createUser(name = "Alice", isAdmin = true, isVerified = false, sendWelcomeEmail = true)
```

### Rule 4: Default Parameters Over Overloads

```kotlin
fun connect(host: String, port: Int = 443, timeout: Int = 30_000) { /* ... */ }
```

### Rule 5: `when` Over `if-else` Chains

```kotlin
fun describe(obj: Any): String = when (obj) {
    is Int -> "Integer: $obj"
    is String -> "String: $obj"
    is List<*> -> "List of size ${obj.size}"
    else -> "Unknown"
}
```

### Rule 6: Avoid `!!` - Use Safe Alternatives

```kotlin
val length1 = str?.length ?: 0                           // Elvis with default
val length2 = str?.length ?: return                      // Elvis with early return
val length3 = requireNotNull(str) { "must not be null" }.length  // Explicit contract
```

### Rule 7: Destructuring

```kotlin
val (lat, lng) = getLocation()
for ((key, value) in map) { println("$key -> $value") }
```

### Rule 8: Preconditions

```kotlin
fun processAge(age: Int) {
    require(age >= 0) { "Age must be non-negative, got $age" }
    check(isInitialized) { "Must initialize before processing" }
    val user = findUser(id) ?: error("User $id not found")
}
```

### Rule 9: Type Aliases for Complex Types

```kotlin
typealias EventHandler = (Event) -> Unit
typealias UserCache = Map<UserId, User>
typealias Predicate<T> = (T) -> Boolean
```

### Rule 10: String Templates

```kotlin
val message = "Hello, ${user.name}! You have $count messages."
val query = """
    |SELECT * FROM users
    |WHERE age >= $minAge
""".trimMargin()
```

______________________________________________________________________

## PERFORMANCE OPTIMIZATION

### Object Creation

```kotlin
// BAD - Regex compiled every call
fun isValid(input: String): Boolean = input.matches(Regex("[a-zA-Z]+"))

// GOOD - Compile once
private val ALPHA_REGEX = "[a-zA-Z]+".toRegex()
fun isValid(input: String): Boolean = input.matches(ALPHA_REGEX)
```

### String Building

```kotlin
// BAD
var result = ""
for (item in items) { result += "$item, " }

// GOOD
val result = items.joinToString(", ")
// or
val result = buildString { items.forEach { append(it); append(", ") } }
```

### Collection Performance

```kotlin
val lookupTable: Map<String, User> = users.associateBy { it.id }  // O(1) lookup
val uniqueItems: Set<String> = items.toSet()                       // O(1) contains
val intArray = IntArray(1000)   // Primitive int[], no boxing
// vs List<Int> which boxes every element
```

### Lazy Initialization

```kotlin
val config: AppConfig by lazy { loadConfig() }
val cache: Map<String, Any> by lazy(LazyThreadSafetyMode.NONE) { buildCache() }
```

______________________________________________________________________

## ANTI-PATTERNS TO AVOID

### Platform Types Leaking

```kotlin
// BAD
val name: String = javaObject.getName()  // Crash if null
// GOOD
val name: String = javaObject.getName() ?: "Unknown"
```

### Abusing `data class` Copy

```kotlin
// BAD - copy is shallow
val copy = original.copy()
copy.items.add("C")  // Mutates original too!
// GOOD - Use immutable collections
data class Order(val items: List<String>)
```

### Using Exceptions for Control Flow

```kotlin
// BAD
try { return repository.get(id) } catch (e: NotFoundException) { return null }
// GOOD
fun findUser(id: String): User? = repository.getOrNull(id)
```
