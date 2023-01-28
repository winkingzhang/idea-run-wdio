import org.jetbrains.changelog.Changelog
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
	id("org.jetbrains.intellij") version "1.12.0"
	// gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
	id("org.jetbrains.changelog") version "2.0.0"
	// Java support
	java
	// Kotlin support
	kotlin("jvm") version "1.8.0"
}

fun properties(key: String) = project.findProperty(key).toString()

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
	if (!System.getenv("USE_ALI_REPO").isNullOrEmpty()) {
		maven {
			setUrl("https://maven.aliyun.com/nexus/content/groups/public/")
		}
	}
	mavenCentral()
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation("junit", "junit", "4.12")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
	pluginName.set(properties("pluginName"))
	version.set(properties("platformVersion"))
	type.set(properties("platformType")) // Target IDE Platform
	downloadSources.set(properties("platformDownloadSources").toBoolean())
	updateSinceUntilBuild.set(true)

	// Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
	plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))

}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
	groups.set(emptyList())
	version.set(properties("pluginVersion"))
	repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
	jvmToolchain(17)
}

tasks {
	wrapper {
		gradleVersion = "7.6"
	}

	withType<KotlinCompile> {
		kotlinOptions.jvmTarget = "17"
		kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
	}

	patchPluginXml {
		version.set(properties("pluginVersion"))
		sinceBuild.set(properties("pluginSinceBuild"))
		untilBuild.set(properties("pluginUntilBuild"))

		// Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
		val start = "<!-- Plugin description -->"
		val end = "<!-- Plugin description end -->"
		pluginDescription.set(
		  file("README.md").readText().lines().run {
				if (!containsAll(listOf(start, end)))
				{
					throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
				}
				subList(indexOf(start) + 1, indexOf(end))
			}.joinToString("\n").run { markdownToHTML(this) }
		)

		// Get the latest available change notes from the changelog file
		changeNotes.set(provider {
			with(changelog) {
				renderItem(
				  getOrNull(properties("pluginVersion")) ?: kotlin.runCatching { getLatest() }.getOrElse { getUnreleased() },
				  Changelog.OutputType.HTML,
				)
			}
		  }
		)
	}

	// Configure UI tests plugin
	// Read more: https://github.com/JetBrains/intellij-ui-test-robot
	runIdeForUiTests {
		systemProperty("robot-server.port", "8082")
		systemProperty("ide.mac.message.dialogs.as.sheets", "false")
		systemProperty("jb.privacy.policy.text", "<!--999.999-->")
		systemProperty("jb.consents.confirmation.enabled", "false")
	}

	runPluginVerifier {
		ideVersions.set(
		  properties("pluginVerifierIdeVersions").split(',')
			.map(String::trim)
			.filter(String::isNotEmpty)
		)
	}

	signPlugin {
		certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
		privateKey.set(System.getenv("PRIVATE_KEY"))
		password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
	}

	publishPlugin {
		dependsOn("patchChangelog")
		token.set(System.getenv("PUBLISH_TOKEN"))

		// pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
		// Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
		// https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
		channels.set(listOf("stable"))
	}
}
