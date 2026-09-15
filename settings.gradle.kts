pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "hugoutils"

include("core")
include("ui")
include("itemglow")
include("playerglow")
include("blockhighlight")
include("fastitems")
include("fabric")
