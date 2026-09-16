# ==============================================================================
# Production Release ProGuard / R8 Optimization Rules
# Namespace: com.example
# ==============================================================================

# --- Base rules (ALWAYS include) ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-keep class kotlin.Metadata { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class * extends android.app.Application
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- Material Components ---
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Moshi codegen ---
-keep class com.squareup.moshi.** { *; }
-keep class *JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass *;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# --- Firebase & Google Services ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Biometric ---
-keep class androidx.biometric.** { *; }

# --- Glance / AppWidget ---
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# --- App-specific data models and classes (Namespace: com.example) ---
-keep class com.example.data.** { *; }
-keep class com.example.**$Companion { *; }
-keepclassmembers class com.example.data.** { *; }

-keep class com.example.data.db.AppDatabase { *; }
-keep class com.example.data.db.NoteDao { *; }
-keep class com.example.data.model.NoteEntity { *; }
-keep class com.example.data.sync.HtmlSyncWorker { *; }
-keep class com.example.widget.GlassNotesWidgetReceiver { *; }
-keep class com.example.widget.GlassNotesWidget { *; }

