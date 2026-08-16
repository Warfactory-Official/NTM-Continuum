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

sourceSets["main"].java {
    exclude("com/hbm/platform/fabric/**", "com/hbm/platform/forge/**")
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
            // 26.1 splits datagen into clientData/serverData; NeoForge 21.1 has only the combined
            // `data` type, and asking for a type the version does not define fails the IDE sync
            // (`Trying to prepare unknown run: clientData`), not just the run itself.
            type = if (mcVersion.substringBefore('.').toInt() >= 26) "clientData" else "data"
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

/**
 * Dev-only mods that must be real mod FILES rather than classpath entries.
 *
 * FML discovers a jar on the run classpath and lists it as a mod, but registers only its code — not its
 * mixin configs ("passed in as source" in the JarSelector warning). A mixin-driven mod therefore loads
 * and then does nothing at all: Sodium appeared in the mod list while never initialising, writing no
 * config and replacing no renderer. Dropping the jar into the run directory's `mods` folder makes FML
 * treat it as a normal mod file, mixins included.
 *
 * Deliberately NOT folded into `runtimeClasspath`: a jar present both there and in `mods` is discovered
 * twice and fails the run as a duplicate mod. Mods that need no mixins (JEI) stay on `localRuntime`,
 * where they already work.
 */
val devModJar: Configuration by configurations.creating

val syncDevMods = tasks.register<Sync>("syncDevMods") {
    description = "Places dev-only mod jars in the client run's mods folder."
    from(devModJar)
    into(layout.projectDirectory.dir("runs/client/mods"))
}

tasks.matching { it.name == "runClient" }.configureEach { dependsOn(syncDevMods) }

dependencies {
    // Golem Core supplies the cross-version API facades. settings.gradle.kts includes it as a
    // composite build, so this resolves against the local checkout with no publish cycle.
    implementation("dev.golem:golemcore-neoforge-$mcVersion:${prop("deps.golemcore")}+$mcVersion")

    compileOnly("org.spongepowered:mixin:0.8.5")
    // Both loaders bundle mixinextras, so it is safe to use in shared code.
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    // JEI is per-node optional: a node with no JEI coordinates simply builds without the integration
    // (the plugin classes are guarded off by the `jei` constant) instead of failing to resolve.
    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        compileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
        compileOnly("mezz.jei:jei-$jei-neoforge-api:$jeiVer")
        localRuntime("mezz.jei:jei-$jei-neoforge:$jeiVer")
    }

    // Sodium declares itself client-side, so it loads only during the client run. It goes on
    // `devModJar` rather than `localRuntime` — see that configuration for why.
    propOrNull("deps.sodium_neoforge")?.let { devModJar("maven.modrinth:sodium:$it") }
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
        "mixin_compatibility" to if (javaVersion <= 17) "JAVA_17" else "JAVA_21",
        "pack_format" to prop("pack_format"),
        // pack.mcmeta's version field, whose very name changed: 26.1 declares a supported RANGE
        // (min_format/max_format) where older versions declare a single pack_format. Keep in step
        // with the fabric buildscript — the resource is shared.
        "pack_format_entry" to if (mcVersion.substringBefore('.').toInt() >= 26)
            "\"min_format\": ${prop("pack_format")}, \"max_format\": ${prop("pack_format")}"
        else
            "\"pack_format\": ${prop("pack_format")}",
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta", "*.mixins.json")) { expand(props) }
    exclude("fabric.mod.json", "$modId.fabric.mixins.json", "data/$modId/forge/**")
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "${it}_${prop("mod.name")}" } }
}

tasks.named("createMinecraftArtifacts") { dependsOn(tasks.named("stonecutterGenerate")) }

// Stonecutter rewrites the shared src/ tree in place for the active node. Both compilation and
// resource processing must wait for that rewrite, or they consume the previous node's source state.
tasks.compileJava { dependsOn(tasks.named("stonecutterGenerate")) }
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

// Recipe ingredients are written in the 26.1 shape and converted downward for older nodes.
extra["mc_version"] = mcVersion
extra["java_version"] = javaVersion.toString()
apply(from = rootProject.file("gradle/run-toolchain.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-recipes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))

apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))

dependencies {
    // NeoForge ships jspecify transitively on 26.1 but not on older versions, and HBM uses @Nullable in
    // ~156 files. Declared explicitly so the shared tree compiles on every node; annotations-only jar,
    // no runtime component.
    compileOnly("org.jspecify:jspecify:1.0.0")
}

// javac stops reporting after 100 errors by default, which hides the tail while porting a new node.
tasks.compileJava { options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000")) }
