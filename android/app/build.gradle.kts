plugins {
    id("com.android.application")
}

android {
    namespace = "com.melofy.app"
    compileSdk = 36

    val youtubeApiKey = ((project.findProperty("YOUTUBE_API_KEY") as String?)
        ?: System.getenv("YOUTUBE_API_KEY")
        ?: "").replace("\"", "\\\"")

    defaultConfig {
        applicationId = "com.melofy.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

val syncWebFrontend by tasks.registering(Sync::class) {
    from(file("../../backend/frontend"))
    into(file("src/main/assets/www"))
}

tasks.named("preBuild") {
    dependsOn(syncWebFrontend)
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.core:core:1.15.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
