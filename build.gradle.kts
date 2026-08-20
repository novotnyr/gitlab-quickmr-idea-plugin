plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

group = "com.github.novotnyr"
version = "28-SNAPSHOT"

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
    implementation( "com.squareup.okhttp3:okhttp:3.14.9")
    implementation("org.apache.commons:commons-lang3:3.18.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:2.23.4")

    intellijPlatform {
        intellijIdeaCommunity("2025.1.3")
        bundledPlugin("Git4Idea")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
        }
        changeNotes = """
            <ul>
            <li>Support JetBrains IDE 2026.1</li>
            <li>Fix reordering of default assignees in Settings</li>
            </ul>
        """.trimIndent()
    }
    publishing {
        val intellijPublishToken: String by project
        token = intellijPublishToken
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}