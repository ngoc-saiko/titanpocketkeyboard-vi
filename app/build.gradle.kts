import java.util.Properties

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
	val f = rootProject.file("local.properties")
	if (f.exists()) load(f.inputStream())
}

android {
	namespace = "io.github.oin.titanpocketkeyboard"
	compileSdk = 34

	defaultConfig {
		applicationId = "io.github.oin.titanpocketkeyboard"
		minSdk = 29
		targetSdk = 33
		versionCode = 1
		versionName = "1.0"

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	signingConfigs {
		create("release") {
			storeFile = file(localProps["RELEASE_STORE_FILE"] as String)
			storePassword = localProps["RELEASE_STORE_PASSWORD"] as String
			keyAlias = localProps["RELEASE_KEY_ALIAS"] as String
			keyPassword = localProps["RELEASE_KEY_PASSWORD"] as String
		}
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".debug"
			versionNameSuffix = "-debug"
		}
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
			signingConfig = signingConfigs.getByName("release")
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

dependencies {

	implementation("androidx.core:core-ktx:1.9.0")
	implementation("androidx.appcompat:appcompat:1.6.1")
	implementation("com.google.android.material:material:1.11.0")
	implementation("androidx.preference:preference-ktx:1.2.1")
	testImplementation("junit:junit:4.13.2")
	testImplementation("io.mockk:mockk:1.13.5")
	androidTestImplementation("androidx.test.ext:junit:1.1.5")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}