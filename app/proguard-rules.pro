# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep your app's main classes
-keep class com.notifiy.noticeboard.** { *; }

# Keep data classes and models
-keep class * extends kotlinx.coroutines.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep navigation classes
-keep class androidx.navigation.** { *; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep ZXing QR code library classes
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# Keep Gson classes for JSON parsing
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep data models for Gson serialization
-keep class com.notifiy.noticeboard.data.model.** { <fields>; }
-keep class com.notifiy.noticeboard.utils.QRCodeUtils$QRBoardData { <fields>; }

# Keep Camera and ActivityResult APIs
-keep class androidx.activity.result.** { *; }
-keep class androidx.camera.** { *; }

# Prevent obfuscation of custom QR scanner activity
-keep class com.notifiy.noticeboard.ui.components.PortraitCaptureActivity { *; }