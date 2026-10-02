# ==========================================================
# REGLAS DE PROGUARD PARA KYMUSIC - OPTIMIZADAS
# ==========================================================

# ✅ Mantener solo los CAMPOS de las clases de datos (para Gson/JSON)
# NO mantener las clases completas, solo sus miembros
-keepclassmembers class com.kyoten.kymusic.data.model.** {
    <fields>;
    <init>(...);
}
-keep class com.kyoten.kymusic.data.model.Song { *; }
-keep class com.kyoten.kymusic.data.model.Playlist { *; }

# ✅ Mantener solo los métodos que usa el framework en ViewModels
-keepclassmembers class com.kyoten.kymusic.viewmodel.** {
    public <methods>;
    <init>(...);
}

# ✅ Mantener ExoPlayer (Media3) - Solo lo necesario
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ✅ Mantener Google Ads (AdMob) - Solo lo necesario
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# ✅ Mantener Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ✅ Mantener Coil
-keep class coil.** { *; }
-dontwarn coil.**

# ✅ Mantener Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ✅ Mantener enums (necesario para JSON)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ✅ Mantener Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ✅ Mantener Activities, Services, Receivers (necesario para el sistema)
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }

# ✅ Mantener MediaSessionService (crítico)
-keep class com.kyoten.kymusic.player.service.** { *; }

# ✅ Mantener clases de audio (crítico para ecualizador)
-keep class com.kyoten.kymusic.audio.** { *; }

# ✅ Suprimir warnings que no son críticos
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**
-dontwarn javax.annotation.**

# ✅ Mantener nombres de archivos de recursos
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ✅ Eliminar código de AndroidX no usado
-dontwarn androidx.**
-keep class androidx.core.app.CoreComponentFactory { *; }

# ✅ Eliminar logs en release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ✅ Eliminar código de debug de Compose
-assumenosideeffects class androidx.compose.runtime.** {
    public static *** trace(...);
}

# ✅ Optimizar Kotlin
-dontwarn kotlin.**
-dontwarn kotlinx.**