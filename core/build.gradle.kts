plugins {
    kotlin("jvm") version "1.5.10"
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.9.0")
    implementation("org.json:json:20210307")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}



publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ml.glassmc"
            artifactId = "loader"
            version = "0.0.1"

            from(components["java"])
        }
    }
}
