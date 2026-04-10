plugins {
    kotlin("jvm") version "2.3.20-Beta1" apply false
    id("com.gradleup.shadow") version "8.3.0" apply false
    id("xyz.jpenilla.run-paper") version "2.3.1" apply false
    id("io.izzel.taboolib") version "2.0.26" apply false
}

// 所有子项目的通用配置
subprojects {
    group = "Sky4th"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
        // 添加 TabooLib 仓库
        maven("https://repo.tabooproject.org/repository/releases")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}
