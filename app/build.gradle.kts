import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URI
import java.util.Collections

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

fun detectHostLanIp(): String? {
    return try {
        val interfacesEnumeration = NetworkInterface.getNetworkInterfaces() ?: return null
        val interfaces = Collections.list(interfacesEnumeration)
        val candidateIps = mutableListOf<String>()

        for (iface in interfaces) {
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = Collections.list(iface.inetAddresses)
            for (addr in addresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress) {
                    val ip = addr.hostAddress
                    if (!ip.isNullOrBlank()) {
                        candidateIps.add(ip)
                    }
                }
            }
        }

        candidateIps.firstOrNull { it.startsWith("192.168.") }
            ?: candidateIps.firstOrNull { it.startsWith("10.") }
            ?: candidateIps.firstOrNull { it.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) }
            ?: candidateIps.firstOrNull()
    } catch (_: Throwable) {
        null
    }
}

fun resolveDevelopmentBaseUrl(configured: String?): String {
    val trimmed = configured?.trim().orEmpty()
    val isExplicitCustom = trimmed.isNotEmpty() &&
        !trimmed.equals("auto", ignoreCase = true) &&
        trimmed != "http://10.0.2.2:8080/"

    if (isExplicitCustom) {
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    val lanIp = detectHostLanIp()
    return if (lanIp != null) {
        "http://$lanIp:8080/"
    } else {
        "http://10.0.2.2:8080/"
    }
}

val rawDevBaseUrl = providers.gradleProperty("DEV_BASE_URL").orNull
val resolvedDevelopmentBaseUrl = resolveDevelopmentBaseUrl(rawDevBaseUrl)

val productionBaseUrl = providers.gradleProperty("PROD_BASE_URL")
    .orElse(providers.environmentVariable("PROD_BASE_URL"))
val releaseRequested = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
val releaseStorePath = providers.gradleProperty("RELEASE_STORE_FILE").orElse(providers.environmentVariable("RELEASE_STORE_FILE")).orNull
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD")).orNull
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orElse(providers.environmentVariable("RELEASE_KEY_ALIAS")).orNull
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD")).orNull
val releaseSigningReady = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

if (releaseRequested) {
    val url = productionBaseUrl.orNull
    val parsedUrl = url?.let { candidate -> runCatching { URI(candidate) }.getOrNull() }
    val host = parsedUrl?.host?.lowercase()
    require(!url.isNullOrBlank() && parsedUrl?.scheme.equals("https", ignoreCase = true) &&
        !host.isNullOrBlank() && parsedUrl?.userInfo == null &&
        host != "localhost" && host != "127.0.0.1" && host != "10.0.2.2" &&
        !host.endsWith(".localhost") && !host.endsWith(".invalid")) {
        "PROD_BASE_URL wajib berupa URL HTTPS production non-lokal"
    }
    require(releaseSigningReady) {
        "RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, dan RELEASE_KEY_PASSWORD wajib untuk build release"
    }
    require(file(releaseStorePath!!).isFile) { "RELEASE_STORE_FILE tidak ditemukan" }
}

android {
    namespace = "com.tbterminal.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.tbterminal.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"${resolvedDevelopmentBaseUrl}\"")
        }
        release {
            val releaseUrl = productionBaseUrl.orNull ?: "https://production-url-required.invalid/"
            buildConfigField("String", "BASE_URL", "\"$releaseUrl\"")
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.39.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.39.0")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.zxing:core:3.5.3")
}
