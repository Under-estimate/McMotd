import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    val kotlinVersion = "1.8.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
}

group = "org.zrnq"
version = "1.2.0"
val ktor_version = "2.3.12"

repositories {
    mavenCentral()
}

/* Note: clean resources folder to update version number.
 * Or gradle will consider ProcessResources to be UP-TO-DATE.
 * https://github.com/gradle/gradle/issues/861
 */
tasks.withType(ProcessResources::class) {
    filter(ReplaceTokens::class, "tokens" to hashMapOf("version" to version))
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("com.charleskorn.kaml:kaml:0.54.0")
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