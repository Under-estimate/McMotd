plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.2"
}

group = "org.zrnq"
version = "1.1.7"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}


dependencies {
    implementation(kotlin("reflect"))
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("dnsjava:dnsjava:3.5.0")
    implementation("org.slf4j:slf4j-nop:1.7.36")
}

tasks.create("CopyToLib", Copy::class) {
    into("${buildDir}/output/libs")
    from(configurations.runtimeClasspath)
}