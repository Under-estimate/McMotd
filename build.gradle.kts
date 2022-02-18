plugins {
    val kotlinVersion = "1.5.30"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.2"
}

group = "org.zrnq"
version = "1.0.2"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}


dependencies {
    implementation(kotlin("reflect"))
    implementation("com.alibaba:fastjson:1.2.79")
    implementation("dnsjava:dnsjava:3.5.0")
}
