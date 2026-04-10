plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    //maven("https://repo.codemc.io/repository/maven-releases/") // PacketEvents
    maven("https://repo.papermc.io/repository/maven-snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    //maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("com.zaxxer:HikariCP:5.1.0")
    //compileOnly("net.dmulloy2:ProtocolLib:5.4.0") // ProtocolLib
    //compileOnly("com.github.retrooper:packetevents-spigot:2.11.2") // PacketEvents
}

tasks.shadowJar {
    archiveBaseName.set("SkyCore")
    archiveClassifier.set("")
    mergeServiceFiles()
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    exclude("net/minecraft/**", "com/mojang/**", "io/papermc/**", "org/bukkit/craftbukkit/**")
}

tasks.build { dependsOn(tasks.shadowJar) }
tasks.jar { enabled = false }

tasks.processResources {
    filesMatching("plugin.yml") { expand(mapOf("version" to version)) }
    filteringCharset = "UTF-8"
}
