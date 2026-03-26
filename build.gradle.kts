import java.util.Properties

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.springframework.boot") version "4.0.4"
    id("io.spring.dependency-management") version "1.1.7"
    application
}

application {
    mainClass.set("com.jakobmenke.bootrestgenerator.MainKt")
}

group = "com.jakobmenke.bootrestgenerator"
version = "1.0"

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val targetFolder: String by lazy {
    val props = Properties()
    file("src/main/resources/config.properties").reader().use(props::load)
    props.getProperty("target.folder", "build/")
}

val cleanTargetFolder by tasks.registering(Delete::class) {
    delete(targetFolder)
}

tasks.named("bootRun") {
    dependsOn(cleanTargetFolder)
}
