plugins {
    kotlin("jvm")
    id("dev.architectury.loom")
    id("maven-publish")
}

base {
    archivesName.set("HugoUtils")
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

    include(project(":core"))
    include(project(":itemglow"))
    include(project(":playerglow"))
    include(project(":fastitems"))

    // Load the bundled modules as classpath mods in the development runtime
    common(project(":core", configuration = "namedElements")) { isTransitive = false }
    common(project(":itemglow", configuration = "namedElements")) { isTransitive = false }
    common(project(":playerglow", configuration = "namedElements")) { isTransitive = false }
    common(project(":fastitems", configuration = "namedElements")) { isTransitive = false }
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "hugoutils"
            from(components["java"])
        }
    }
}
