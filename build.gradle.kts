import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.10" apply false
    id("architectury-plugin") version "3.5.170" apply false
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("maven-publish")
}

subprojects {
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    group = rootProject.property("maven_group") as String
    version = rootProject.property("mod_version") as String

    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootProject.file("THIRD_PARTY_NOTICES.md"))
        if (project.name != "fastitems") {
            from(rootProject.file("LICENSE.txt"))
            from(rootProject.file("NOTICE"))
        }
    }
}

gradle.projectsEvaluated {
    val remapJars = subprojects
        .filter { it.name != "fabric" }
        .mapNotNull { it.tasks.findByName("remapJar") }
    remapJars.windowed(2).forEach { (first, second) ->
        second.mustRunAfter(first)
    }
}
