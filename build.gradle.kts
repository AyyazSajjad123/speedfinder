plugins {
    // Purani lines (Unhein mat chherna)
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // ⬇️ YE LINE ADD KARNI HAI (Crashlytics Plugin)
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
}