# Preserve generic signatures and annotations for reflection & Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Gson library & TypeTokens
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter

# Keep models, entities, and document core
-keep class com.digitalchopda.app.data.local.entity.** { *; }
-keep class com.digitalchopda.app.domain.model.** { *; }
-keep class com.digitalchopda.app.core.document.** { *; }

# Keep Network DTOs for Gson reflection
-keep class com.digitalchopda.app.core.network.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# WorkManager Worker reflection
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Jetpack Compose and Coroutines R8 Optimizations
-dontwarn androidx.compose.**
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# ZXing Barcode Scanning & QR Generation
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Retrofit 2 Keep Rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }


