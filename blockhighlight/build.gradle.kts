plugins {
    kotlin("jvm")
    id("dev.architectury.loom")
    id("maven-publish")
}

base {
    archivesName.set("hugoutils-blockhighlight")
}

architectury {
    fabric()
}

val common = configurations.create("common")
configurations.compileClasspath.get().extendsFrom(common)
configurations.runtimeClasspath.get().extendsFrom(common)
configurations["developmentFabric"].extendsFrom(common)

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${rootProject.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("kotlin_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_version")}")
    common(project(":core", configuration = "namedElements")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", rootProject.property("minecraft_version")!!)
    inputs.property("loader_version", rootProject.property("loader_version")!!)
    inputs.property("kotlin_loader_version", rootProject.property("kotlin_loader_version")!!)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version.toString(),
            "minecraft_version" to rootProject.property("minecraft_version").toString(),
            "loader_version" to rootProject.property("loader_version").toString(),
            "kotlin_loader_version" to rootProject.property("kotlin_loader_version").toString()
        )
    }
}

java {
    withSourcesJar()
}
