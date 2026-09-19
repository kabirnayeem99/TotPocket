# TotPocket R8 rules. Libraries (Compose, kotlinx.serialization, coroutines, CameraX) ship their own
# consumer rules, so only app-specific ones live here.

# Readable crash stack traces: keep line numbers, hide the real source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# No logging at all in release builds: every android.util.Log call — ours and the libraries' — is
# removed, at every level. isLoggable() answers false, so guarded log blocks are dropped too, and
# the fixed return values let R8 drop calls whose result is used (CameraX does this).
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int) return false;
    public static int v(...) return 0;
    public static int d(...) return 0;
    public static int i(...) return 0;
    public static int w(...) return 0;
    public static int e(...) return 0;
    public static int wtf(...) return 0;
    public static int println(...) return 0;
    public static java.lang.String getStackTraceString(java.lang.Throwable) return "";
}
