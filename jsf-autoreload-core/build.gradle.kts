plugins {
    `java-library`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.methvin:directory-watcher:0.18.0")
    api("org.java-websocket:Java-WebSocket:1.5.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
