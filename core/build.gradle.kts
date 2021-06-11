plugins {
    kotlin("jvm") version "1.5.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.9.0")
    implementation("org.json:json:20210307")

    testImplementation("junit:junit:4.13")
}