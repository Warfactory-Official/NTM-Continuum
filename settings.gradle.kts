pluginManagement {
    includeBuild("../Golem-Core/build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
    }
}

plugins {
    id("dev.golem.settings")
}

rootProject.name = "hbm"

golem {
    upstream("../Golem-Core")
}
