# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /path-to-android-sdk/tools/proguard/proguard-android.txt

# Keep Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment

# Keep Room entities
-keep class com.example.arts.models.** { *; }

# Keep data classes
-keepclassmembers class com.example.arts.models.** {
    public <init>(...);
}
