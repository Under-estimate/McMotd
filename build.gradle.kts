plugins {
    val kotlinVersion = "1.7.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.13.2"
}

group = "org.zrnq"
version = "1.1.20"
val ktor_version = "2.3.1"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}


dependencies {
    implementation(kotlin("reflect"))
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("dnsjava:dnsjava:3.5.0")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("org.gnu.inet:libidn:1.15")
}

tasks.create("CopyToLib", Copy::class) {
    into("${buildDir}/output/libs")
    from(configurations.runtimeClasspath)
}