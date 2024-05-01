import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.0.0-SNAPSHOT"
}

group = "com.github.novotnyr"
version = "25-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.4")
    implementation( "com.squareup.okhttp3:okhttp:3.14.9")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:2.23.4")

    intellijPlatform {
        intellijIdeaCommunity("2022.3.3")
        bundledPlugin("Git4Idea")
        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223"
            untilBuild = "241.*"
        }
        changeNotes = """
            <ul>
            <li></li>
            </ul>
        """.trimIndent()
    }
    publishing {
        val intellijPublishToken: String by project
        token = intellijPublishToken
    }
    verifyPlugin {
        ides {
            recommended()
            ide("2022.3")
            ide("2024.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}