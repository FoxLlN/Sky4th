plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // 仅编译时依赖，运行时从 Core 插件获取经济 API
    compileOnly(project(":SkyCore"))
}

tasks {
    runServer {
        minecraftVersion("1.21")
    }
}

tasks.shadowJar {
    archiveBaseName.set("CreditEconomy")
    archiveClassifier.set("")
    mergeServiceFiles()
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    // 不打包 SkyCore，避免类重复加载导致经济数据不走 Core
    dependencies {
        exclude(project(":SkyCore"))
    }
    exclude("sky4th/core/**")
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
