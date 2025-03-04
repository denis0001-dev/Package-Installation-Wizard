plugins {
    kotlin("jvm") version "2.0.21"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

group = "ru.morozovit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

javafx {
    version = "17.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ru.morozovit:logging:1.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}


tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}