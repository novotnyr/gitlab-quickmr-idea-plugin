plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = "com.github.novotnyr"
version = "26-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
    implementation( "com.squareup.okhttp3:okhttp:3.14.9")
    implementation("org.apache.commons:commons-lang3:3.18.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:2.23.4")

    intellijPlatform {
        intellijIdeaCommunity("2025.1.3")
        bundledPlugin("Git4Idea")
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "223"
            untilBuild = "252.*"
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
    pluginVerification {
        ides {
            recommended()
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}