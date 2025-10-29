-dontwarn android.support.**
-dontwarn com.android.**
-dontwarn androidx.**
-keep class com.xapps.media.xmusic.** { *; } # Keep your main application package



-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgent
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}


-keep class com.xapps.media.xmusic.activity.** { *; } # Explicitly keep your activities
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}


-keep class **.R$* {
  <fields>;
}


-keepclassmembers class * extends android.view.View {
    <init>(android.content.Context);
    <init>(android.content.Context, android.util.AttributeSet);
    <init>(android.content.Context, android.util.AttributeSet, int);
    void set*(...);
}


-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}


-keepclasseswithmembernames class * {
    native <methods>;
}


-keepattributes Signature
-keepattributes *Annotation*
-keep class * implements java.io.Serializable



-dontwarn androidx.annotation.**
-dontwarn androidx.appcompat.**
-dontwarn androidx.core.**
-dontwarn androidx.fragment.**
-dontwarn androidx.lifecycle.**
-dontwarn androidx.recyclerview.**
-dontwarn androidx.cardview.**
-dontwarn androidx.viewpager.**
-dontwarn androidx.slidingpanelayout.**
-dontwarn com.google.android.material.**


-dontwarn java.nio.file.**
-dontwarn sun.misc.Unsafe
-dontwarn org.apache.harmony.awt.internal.nls.Messages
-dontwarn org.eclipse.jdt.internal.compiler.lookup.SyntheticArgumentBinding
-dontwarn kotlin.jvm.internal.**
-dontwarn kotlinx.coroutines.**





-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; } # Example, replace with your model package
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class ** {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.google.gson.internal.$Gson$Types {
  <methods>;
}
-keep class com.google.gson.reflect.TypeToken { *; }


-dontwarn okio.**
-dontwarn retrofit2.platform.**
-keepattributes Signature
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface ** { *; }


-keep class **.model.** { *; } # Keep your Room entities and POJOs
-keep class **.dao.** { *; } # Keep your Room DAOs
-keep class **.database.** { *; } # Keep your Room database class


-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile
-keepattributes LineNumberTable

-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.Continuation # For coroutines



-keepclassmembers public final class **.model.data.** {
    public <fields>;
    public <methods>;
}


-keep class * extends java.lang.annotation.Annotation { *; }