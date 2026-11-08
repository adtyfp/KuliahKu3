# Firebase Firestore POJO — jaga nama field agar deserialisasi tetap bekerja [citation:18]
-keepclassmembers class com.kuliahku.data.model.** {
    <fields>;
    <init>(...);
}
-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Apache POI
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.**

# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class com.kuliahku.data.remote.** { *; }

# Hilt
-dontwarn dagger.hilt.**

# Vico
-dontwarn com.patrykandpatrick.vico.**

# Jsoup
-dontwarn org.jsoup.**

# Gemalto JP2
-dontwarn com.gemalto.jp2.JP2Decoder
