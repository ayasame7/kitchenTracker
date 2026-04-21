# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep your data models so Firebase can deserialize them
-keep class com.example.fridgetracker.KitchenItem { *; }
-keep class com.example.fridgetracker.KitchenNote { *; }
-keep class com.example.fridgetracker.PredefinedItem { *; }

# Keep Compose internal classes
-keep class androidx.compose.** { *; }

# Preserve common attributes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
