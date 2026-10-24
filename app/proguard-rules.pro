# ========================================================
# Gomobile / Go JNI bindings
# Обязательно сохранить все классы atlant и go, иначе JNI крашится
# ========================================================
-keep class atlant.** { *; }
-keep interface atlant.** { *; }
-keep class go.** { *; }
-keep interface go.** { *; }

# ========================================================
# Kotlin Standard Library & Collections
# Предотвращает IllegalAccessError на разных версиях Android
# ========================================================
-dontrepackageclasses
-keep class kotlin.** { *; }
-keep class kotlin.collections.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ========================================================
# AndroidX Startup & Lifecycle
# ========================================================
-keep class androidx.startup.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.startup.Initializer { *; }
-dontwarn androidx.**

# Общие правила для сохранения аннотаций и сигнатур
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions
