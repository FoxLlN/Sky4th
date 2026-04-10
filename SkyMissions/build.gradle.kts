plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // 依赖 SkyCore
    implementation(project(":SkyCore"))
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }
}

tasks.shadowJar {
    archiveBaseName.set("SkyMissions")
    archiveClassifier.set("")
    mergeServiceFiles()
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    dependsOn(project(":SkyCore").tasks.shadowJar)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.jar {
    enabled = false
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
