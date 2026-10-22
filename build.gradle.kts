import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("net.fabricmc.fabric-loom")
    id("com.github.gmazzo.buildconfig")
    java
}

val mod_version: String by project
val maven_group: String by project
val mod_name: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val loader_version: String by project
val fabric_version: String by project
val catformat_version: String by project
val imgui_version: String by project

val USING_GIT = Paths.get(rootDir.absolutePath).resolve(".git").toFile().exists()
val GIT_HASH = execute("git rev-parse --short HEAD")

version = mod_version
group = maven_group

buildConfig {
    packageName("$group.util")
    useJavaOutput()
    generateAtSync.set(true)

    val typeString = String::class.java
    val typeBool = Boolean::class.java

    val buildTime = SimpleDateFormat("MM/dd/yyyy HH:mm").format(Date())

    buildConfigField(typeString, "NAME", mod_name)
    buildConfigField(typeString, "GROUP", maven_group)
    buildConfigField(typeString, "VERSION", mod_version)
    buildConfigField(typeString, "BUILD_TIME", buildTime)
    buildConfigField(typeBool, "USING_GIT", USING_GIT)
    buildConfigField(typeString, "HASH", GIT_HASH)
    buildConfigField(typeString, "BRANCH", execute("git branch --show-current"))
}

sourceSets.main {
    java.srcDirs("build/gen/buildconfig/src/main")
}

base {
    archivesName.set("$archives_base_name-$GIT_HASH")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

loom {
    accessWidenerPath.set(file("src/main/resources/oyvey.accesswidener"))

    runs {
        named("client") {
            // fail fast on any broken mixin in dev instead of on first class load
            vmArg("-Doyvey.mixinAudit=true")
            // OYVEY_QUICKPLAY="World Name" ./gradlew runClient joins that world directly
            System.getenv("OYVEY_QUICKPLAY")?.let { programArgs("--quickPlaySingleplayer", it) }
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft_version")
    implementation("net.fabricmc:fabric-loader:$loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_version")

    include(implementation("com.github.cattyngmd.catformat:catformat-core:$catformat_version")!!)

    // Dear ImGui, LWJGL itself is provided by Minecraft
    include(implementation("io.github.spair:imgui-java-binding:$imgui_version")!!)
    include(implementation("io.github.spair:imgui-java-lwjgl3:$imgui_version") {
        exclude(group = "org.lwjgl")
    })
    for (platform in listOf("windows", "linux", "macos")) {
        include(runtimeOnly("io.github.spair:imgui-java-natives-$platform:$imgui_version")!!)
    }
}

tasks.processResources {
    inputs.property("version", version)
    inputs.property("minecraft_version", minecraft_version)
    inputs.property("loader_version", loader_version)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "minecraft_version" to minecraft_version,
            "loader_version" to loader_version
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    dependsOn("generateBuildConfig")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("generateBuildConfig")
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_$archives_base_name" }
    }
}

fun execute(cmd: String): String = providers.exec {
    commandLine(cmd.split(" "))
    isIgnoreExitValue = true
}.standardOutput.asText.getOrElse("nogit").trim().replace("\n", "")
