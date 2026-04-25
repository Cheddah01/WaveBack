plugins {
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
