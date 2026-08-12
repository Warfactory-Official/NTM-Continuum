plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-neoforge-$mcVersion"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

neoForge {
    version = prop("deps.neoforge")

    val at = rootProject.file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) accessTransformers.from(at)

    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            ideName = "NeoForge ${name.replaceFirstChar { it.uppercase() }} ($mcVersion)"
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
        }
        register("data") {
            clientData()
            gameDirectory = file("runs/data")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", rootProject.file("src/main/resources").absolutePath,
            )
        }
        // Headless GameTest runner: `gradlew :26.1-neoforge:runGameTestServer` executes every hbm:* test.
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = file("runs/gametest")
        }
        register("server") {
            server()
            gameDirectory = file("runs/server")
        }
    }

    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
    }
}

sourceSets["main"].resources.srcDir("src/generated/resources")

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

// MDG for this MC version exposes no per-run additional classpath, so dev-only mods go on a
// `localRuntime` configuration folded into the dev runtimeClasspath — never compiled against, never
// published, never bundled into the jar.
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") { extendsFrom(localRuntime) }

dependencies {
    // Golem Core supplies the cross-version API facades. settings.gradle.kts includes it as a
    // composite build, so this resolves against the local checkout with no publish cycle.
    implementation("dev.golem:golemcore-neoforge-$mcVersion:${prop("deps.golemcore")}+$mcVersion")

    compileOnly("org.spongepowered:mixin:0.8.5")
    // Both loaders bundle mixinextras, so it is safe to use in shared code.
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    val jei = "${prop("deps.jei_mc")}"
    val jeiVer = prop("deps.jei")
    compileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
    compileOnly("mezz.jei:jei-$jei-neoforge-api:$jeiVer")
    localRuntime("mezz.jei:jei-$jei-neoforge:$jeiVer")

    // Sodium declares itself client-side, so it loads only during the client run.
    propOrNull("deps.sodium_neoforge")?.let { localRuntime("maven.modrinth:sodium:$it") }
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_version" to prop("mod.version"),
        "mod_author" to prop("mod.author"),
        "mod_license" to prop("mod.license"),
        "mod_credits" to prop("mod.credits"),
        "mod_description" to prop("mod.description"),
        "minecraft_range" to prop("deps.minecraft_range"),
        "neoforge_version" to prop("deps.neoforge"),
        "neoforge_loader_range" to prop("deps.neoforge_loader_range"),
        "java_version" to javaVersion.toString(),
        "pack_format" to prop("pack_format"),
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta", "*.mixins.json")) { expand(props) }
    exclude("fabric.mod.json", "$modId.fabric.mixins.json")
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "${it}_${prop("mod.name")}" } }
}

tasks.named("createMinecraftArtifacts") { dependsOn(tasks.named("stonecutterGenerate")) }

// Stonecutter rewrites the shared src/ tree in place for the active node. Both compilation and
// resource processing must wait for that rewrite, or they consume the previous node's source state.
tasks.compileJava { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))
