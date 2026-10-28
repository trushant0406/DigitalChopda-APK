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

# Keep Room Database, Entities, DAOs & Converters
-keep class com.digitalchopda.app.data.local.entity.** { *; }
-keep class com.digitalchopda.app.data.local.dao.** { *; }
-keep class com.digitalchopda.app.data.local.database.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep models and core engines
-keep class com.digitalchopda.app.domain.model.** { *; }
-keep class com.digitalchopda.app.core.document.** { *; }
-keep class com.digitalchopda.app.core.printer.** { *; }
-keep class com.digitalchopda.app.core.sound.** { *; }
-keep class com.digitalchopda.app.core.security.** { *; }

# Keep Network DTOs, API Services & Retrofit 2
-keep class com.digitalchopda.app.core.network.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# SQLCipher Database Security
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

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
    public static int i(...);
}

# ZXing Barcode Scanning & QR Generation
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**


