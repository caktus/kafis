plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.slf4j:slf4j-nop:2.0.16")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("commons-io:commons-io:2.6")
    implementation("commons-codec:commons-codec:1.14")
    implementation("com.machinezoo.sourceafis:sourceafis:3.18.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("com.smartelect.AppKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

val copyRuntimeLibs = task<Copy>("copyRuntimeLibs") {
    into("$buildDir/libs")
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") })
}

tasks.getByName("assemble").finalizedBy(copyRuntimeLibs)

fun getClasspath() = "libs/" + file("$buildDir/libs").list()?.joinToString(":libs/")

// Build native image using GraalVM
task<Exec>("buildNativeImage") {
    description = "Build native image using GraalVM"
    dependsOn(tasks.getByName("build"))
    workingDir(buildDir)
    commandLine(
        "${System.getenv("GRAALVM_HOME")}/bin/native-image",
        "-cp", getClasspath(),
        "-H:+ReportExceptionStackTraces",
        "-H:+AddAllCharsets",
        // "--report-unsupported-elements-at-runtime",
        // "--allow-incomplete-classpath",
        "--initialize-at-build-time=com.github.ajalt.mordant.internal.nativeimage.NativeImagePosixMppImpls",
        "--no-server",
        "--no-fallback",
        "--enable-http",
        "--enable-https",
        "com.smartelect.AppKt",
        "kafis"
    )
}

// Create a fat jar for distribution
tasks.register<Jar>("uberJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
