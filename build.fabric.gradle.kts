plugins {
    id("java-library")
    id("net.fabricmc.fabric-loom") version "1.15.5"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-fabric-$mcVersion"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

// Whether the target Minecraft is obfuscated decides which Loom configurations exist. 26.1+ ships
// unobfuscated, so Loom runs with remapping off and never creates the `mod*` configurations; plain
// `implementation` is correct and no mappings layer is needed. Older nodes need `modImplementation`
// plus an explicit Mojmap layer. This is an ordinary Kotlin conditional, not a Stonecutter `//?`
// comment: Stonecutter only preprocesses files under src/, never build scripts.
val obfuscated = mcVersion.substringBefore('.').toInt() < 26
val modCfg = if (obfuscated) "modImplementation" else "implementation"
// Loom's `localRuntime` is not remapped either way; with obfuscation off the plain config is correct.
val devRuntime = if (obfuscated) "modLocalRuntime" else "localRuntime"

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("deps.minecraft")}")
    if (obfuscated) "mappings"(loom.officialMojangMappings())

    modCfg("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modCfg("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    // Golem Core supplies the cross-version API facades, resolved from the composite build.
    modCfg("dev.golem:golemcore-fabric-$mcVersion:${prop("deps.golemcore")}+$mcVersion")

    val jei = prop("deps.jei_mc")
    val jeiVer = prop("deps.jei")
    "compileOnly"("mezz.jei:jei-$jei-common-api:$jeiVer")
    "compileOnly"("mezz.jei:jei-$jei-fabric-api:$jeiVer")
    devRuntime("mezz.jei:jei-$jei-fabric:$jeiVer")

    // Sodium declares `environment: client`, so Fabric loads it only during the client run.
    propOrNull("deps.sodium_fabric")?.let { devRuntime("maven.modrinth:sodium:$it") }
}

loom {
    val aw = rootProject.file("src/main/resources/$modId.accesswidener")
    if (aw.exists()) accessWidenerPath = aw

    runs {
        named("client") {
            client()
            configName = "Fabric Client ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/client"
        }
        named("server") {
            server()
            configName = "Fabric Server ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/server"
        }
    }
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to prop("mod.name"),
        "mod_version" to prop("mod.version"),
        "mod_author" to prop("mod.author"),
        "mod_license" to prop("mod.license"),
        "mod_credits" to prop("mod.credits"),
        "mod_description" to prop("mod.description").replace("\n", "\\n"),
        "minecraft_range" to prop("deps.minecraft_range"),
        "fabric_loader" to prop("deps.fabric_loader"),
        "java_version" to javaVersion.toString(),
        "pack_format" to prop("pack_format"),
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "pack.mcmeta", "*.mixins.json")) { expand(props) }
    // NeoForge-only metadata and the NeoForge biome_modifier data pack, which Fabric injects in code.
    exclude("META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg", "data/$modId/neoforge/**")
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "${it}_${prop("mod.name")}" } }
}

// Stonecutter rewrites the shared src/ tree in place for the active node. Both compilation and
// resource processing must wait for that rewrite, or they consume the previous node's source state.
tasks.compileJava { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))
