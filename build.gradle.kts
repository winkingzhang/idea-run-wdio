import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
	repositories {
//		// use Aliyun mirror to resolve network issue in China
//		maven("https://maven.aliyun.com/repository/public")
		mavenCentral()
		maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
	}
}

plugins {
	// gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
	id("org.jetbrains.intellij") version "1.7.0"
	// gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
	id("org.jetbrains.changelog") version "1.3.0"
	// Java support
	java
	// Kotlin support
	kotlin("jvm") version "1.5.31"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project
val pluginVerifierIdeVersions: String by project

val platformType: String by project
val platformVersion: String by project
val platformPlugins: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
	mavenCentral()
	jcenter()

	maven("https://www.jetbrains.com/intellij-repository/releases")
	maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation("junit", "junit", "4.12")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
	pluginName.set(pluginName_)
	version.set(platformVersion)
	type.set(platformType)
	downloadSources.set(platformDownloadSources.toBoolean())
	updateSinceUntilBuild.set(true)

	// Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
	plugins.set(
	  platformPlugins.split(',')
		.map(String::trim)
		.filter(String::isNotEmpty)
	)
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
	//	version = pluginVersion
	setProperty("version", pluginVersion)
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

tasks {
	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
		kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility")
	}

	patchPluginXml {
		version.set(pluginVersion)
		sinceBuild.set(pluginSinceBuild)
		untilBuild.set(pluginUntilBuild)

		// Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
		pluginDescription.set(
		  File(project.rootDir.absolutePath + File.separator + "README.md")
			.readText()
			.lines()
			.run {
				val start = "<!-- Plugin description -->"
				val end = "<!-- Plugin description end -->"

				if (!containsAll(listOf(start, end)))
				{
					throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
				}
				subList(indexOf(start) + 1, indexOf(end))
			}.joinToString("\n").run { markdownToHTML(this) }
		)

		// Get the latest available change notes from the changelog file
		changeNotes.set(
		  provider {
			  changelog.getLatest().toHTML()
		  }
		)
	}

	runPluginVerifier {
		ideVersions.set(
		  pluginVerifierIdeVersions.split(',')
			.map(String::trim)
			.filter(String::isNotEmpty)
		)
	}

	publishPlugin {
		dependsOn("patchChangelog")
		token.set(System.getenv("PUBLISH_TOKEN"))
		// pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
		// Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
		// https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
//		channels.set(""
//		  .split('-')
//		  .getOrElse(1) { "default" }
//		  .split('.')
//		  .first())
	}
}
