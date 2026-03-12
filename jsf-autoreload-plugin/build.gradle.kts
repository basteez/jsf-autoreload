plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

val runtimeJar by configurations.creating

dependencies {
    implementation(gradleApi())
    implementation("io.methvin:directory-watcher:0.18.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    compileOnly("io.openliberty.tools:liberty-gradle-plugin:3.9.0")

    runtimeJar(project(":jsf-autoreload-runtime"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.java-websocket:Java-WebSocket:1.5.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("jsfAutoreload") {
            id = "it.bstz.jsf-autoreload"
            implementationClass = "it.bstz.jsfautoreload.JsfAutoreloadPlugin"
        }
    }
}

tasks.jar {
    from(runtimeJar) {
        into("META-INF/jsf-autoreload")
        rename { "jsf-autoreload-runtime.jar" }
    }
}

tasks.shadowJar {
    relocate("org.java_websocket", "it.bstz.shaded.java_websocket")
    relocate("io.methvin", "it.bstz.shaded.methvin")
    from(runtimeJar) {
        into("META-INF/jsf-autoreload")
        rename { "jsf-autoreload-runtime.jar" }
    }
}
