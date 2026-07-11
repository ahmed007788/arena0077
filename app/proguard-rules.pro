# Arena0077 ProGuard rules

# Keep BuildConfig
-keep class com.arena0077.app.BuildConfig { *; }

# Keep data models (used by kotlinx.serialization)
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arena0077.app.**$$serializer { *; }
-keepclassmembers class com.arena0077.app.** {
    *** Companion;
}

# Keep Hilt generated
-keep class **_HiltModules { *; }
-keep class **_HiltComponents$* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep class * extends androidx.lifecycle.ViewModel { *; }

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# WebView JavaScript bridge
-keepclassmembers class com.arena0077.app.webview.JsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep @android.webkit.JavascriptInterface class * { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
