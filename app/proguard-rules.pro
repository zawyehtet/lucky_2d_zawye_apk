# Proguard rules for Lucky2D & Offline License Security
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn com.google.errorprone.annotations.**

# Obfuscate security internals while keeping public entry points if needed
-repackageclasses 'o'
-allowaccessmodification

