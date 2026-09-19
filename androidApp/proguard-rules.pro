# TotPocket R8 rules. Libraries (Compose, kotlinx.serialization, coroutines, CameraX) ship their own
# consumer rules, so only app-specific ones live here.

# Readable crash stack traces: keep line numbers, hide the real source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip verbose/debug/info logging from release builds (warnings and errors stay).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
