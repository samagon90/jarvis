# Правила ProGuard для JarvisMaster

# Room
-keep class com.jarvis.master.data.db.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# DataStore
-keep class com.jarvis.master.data.settings.** { *; }
