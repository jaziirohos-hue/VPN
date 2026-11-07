# WireGuard backend uses JNI and reflection.
-keep class com.wireguard.android.backend.** { *; }
-keep class com.wireguard.crypto.** { *; }
-keep class com.wireguard.config.** { *; }
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.xalane.vpn.data.** { *** Companion; }
-keep,includedescriptorclasses class com.xalane.vpn.data.**$$serializer { *; }
