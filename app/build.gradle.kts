plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.google.devtools.ksp")
	id("androidx.room")
}

// git の出力を返す。git がない場合（ソースの zip から開いたなど）は null
fun git(vararg args: String): String? = try {
	providers.exec {
		commandLine("git", *args)
		isIgnoreExitValue = true
	}.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
	null
}

// バージョン名は「gradle.properties の appVersion + 最後の数字」。
// 最後の数字は appVersion の行を最後に変えたコミットからのコミット数（0 から数える）
val appVersionBase: String = providers.gradleProperty("appVersion").get()
val appVersionPatch: Int = git("log", "-n1", "--format=%H", "-G^appVersion=", "--", "gradle.properties")
	?.let { git("rev-list", "--count", "$it..HEAD") }
	?.toIntOrNull() ?: 0
val appVersionName = "$appVersionBase.$appVersionPatch"

// versionCode はコミット数の合計。コミットするたびに増えるので、いつ Play にアップロードしても前回より大きくなる
val appVersionCode: Int = git("rev-list", "--count", "HEAD")?.toIntOrNull() ?: 1

// リリースのワークフローがバージョンを読み取るためのタスク
tasks.register("printVersion") {
	val name = appVersionName
	val code = appVersionCode
	doLast { println("versionName=$name\nversionCode=$code") }
}

android {
	namespace = "com.gukos.battery_graph"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.gukos.battery_graph"
		minSdk = 24
		targetSdk = 36
		versionCode = appVersionCode
		versionName = appVersionName

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
		vectorDrawables {
			useSupportLibrary = true
		}
	}
	signingConfigs {
		create("release") {
			storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
			storePassword = System.getenv("KEYSTORE_PASSWORD")
			keyAlias = System.getenv("KEY_ALIAS")
			keyPassword = System.getenv("KEY_PASSWORD")
		}
	}
	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
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
	buildFeatures {
		compose = true
	}
	androidResources {
		// 端末設定の「アプリの言語」（Android 13 以降）に en / ja を表示する
		generateLocaleConfig = true
		// ライブラリが含む他言語のリソースを除き、アプリが対応する言語だけを残す
		localeFilters += listOf("en", "ja")
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
}

room {
	// マイグレーションテスト用にスキーマを JSON で保存する（git 管理する）
	schemaDirectory("$projectDir/schemas")
}

kotlin {
	compilerOptions {
		jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
	}
}

dependencies {

	implementation("androidx.core:core-ktx:1.18.0")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
	implementation("androidx.activity:activity-compose:1.13.0")
	implementation(platform("androidx.compose:compose-bom:2026.03.01"))
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.room:room-runtime:2.8.4")
	implementation("androidx.room:room-ktx:2.8.4")
	implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
	implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    testImplementation("junit:junit:4.13.2")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
	androidTestImplementation("androidx.test.ext:junit:1.3.0")
	androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
	androidTestImplementation("androidx.room:room-testing:2.8.4")
	androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
	androidTestImplementation(platform("androidx.compose:compose-bom:2026.03.01"))
	androidTestImplementation("androidx.compose.ui:ui-test-junit4")
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
	ksp("androidx.room:room-compiler:2.8.4")

	constraints {
		// room-testing は serialization-json 1.8.1 を使うが、アプリ側の core は 1.7.3 に固定され
		// テスト実行時に AbstractMethodError になるため、core のバージョンをそろえる
		implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
	}
}
