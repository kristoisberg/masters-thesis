plugins {
    id("application")
    id("com.gradleup.shadow") version "9.4.1"
}

group = "xyz.kristoi.jooq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.28.0")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "xyz.kristoi.jooq.CliApplication"
    }
}
