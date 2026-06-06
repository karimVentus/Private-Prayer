-keep class com.prayertime.domain.model.** { *; }
-keep class com.prayertime.data.remote.** { *; }
# AppWidgetManager instantiates providers by class name from the manifest.
-keep class com.prayertime.widget.PrayerTimeWidgetProvider { *; }
-keep class com.prayertime.widget.PrayerTimeWidgetProviderSmallTall { *; }
-keep class com.prayertime.widget.PrayerTimeWidgetProviderSmallWide { *; }
-keep class com.prayertime.widget.PrayerTimeWidgetProviderLarge { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson (online flavor — Retrofit converter)
-keepattributes InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# WorkManager + Hilt workers
-keep class * extends androidx.work.ListenableWorker
-keep class com.prayertime.worker.** { *; }
-keep class * extends androidx.hilt.work.HiltWorker
-keepclassmembers class * {
  @dagger.assisted.AssistedInject <init>(...);
}

# Manifest-instantiated alarms / notifications
-keep class com.prayertime.alarm.** { *; }
-keep class com.prayertime.notification.** { *; }

