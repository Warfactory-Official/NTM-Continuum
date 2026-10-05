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
            // Quick Play is opt-in through -PquickPlayWorld. Do not inherit HBM_QUICKPLAY from a
            // previous visual fixture into ordinary IDE launches.
            providers.gradleProperty("quickPlayWorld").orNull?.let {
                programArguments.addAll("--quickPlaySingleplayer", it)
            }
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
            // -Pgametest.filter=<regex> runs only the matching test ids (see Golem's GameTests.selected).
            systemProperty("golem.gametest.filter", providers.gradleProperty("gametest.filter").orElse("").get())
        }
        // Rewrites src/generated/java from the live registry, then halts before any test runs. Borrows
        // the gameTestServer type because it is the headless server that needs no EULA, and the tag
        // roster the generator reads only exists once data packs are loaded.
        register("generateIds") {
            type = "gameTestServer"
            // Its own directory, not the test runner's: this run stops the server without saving, and
            // a GameTest world is never reset between runs, so leaving a half-written one behind is
            // something the next test run would inherit.
            gameDirectory = file("runs/generateids")
            systemProperty("hbm.generateIds", rootProject.file("src/generated/java").absolutePath)
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
    maven("https://maven.squiddev.cc") {
        name = "SquidDev"
        content { includeGroup("cc.tweaked") }
    }
    maven("https://maven.createmod.net") {
        name = "CreateMod"
        content { includeGroup("dev.engine-room.flywheel") }
    }
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

// CC: Tweaked for the server and GameTest runs (`-Pcomputercraft.run=true`); empty otherwise, so a run without
// the flag also clears a jar an earlier flagged run left behind.
val ccServerMod: Configuration by configurations.creating
for ((run, task) in listOf("server" to "runServer", "gametest" to "runGameTestServer")) {
    val sync = tasks.register<Sync>("syncComputerCraft${run.replaceFirstChar { it.uppercase() }}") {
        description = "Places CC: Tweaked in the $run run's mods folder when -Pcomputercraft.run=true."
        from(ccServerMod)
        into(layout.projectDirectory.dir("runs/$run/mods"))
    }
    tasks.matching { it.name == task }.configureEach { dependsOn(sync) }
}
// ModDev writes run arguments to build/moddev/clientRunProgramArgs.txt. Without this input, a
// one-off HBM_QUICKPLAY launch remains cached and every later normal client run keeps auto-joining it.
tasks.matching { it.name == "prepareClientRun" }.configureEach {
    inputs.property("hbmQuickPlayWorld", providers.gradleProperty("quickPlayWorld").orElse(""))
}

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

    // Published Flywheel supplies 1.21.1; the 26.1 node leaves this blank and starts Core's vendored
    // engine instead (switched off with config/golemcore-flywheel.json or `/flywheel backend off`).
    // `-Pflywheel.run=false` leaves the engine jar out of the dev run (compile stays), so the dev client
    // can be launched as a player without Flywheel would see it: every machine through its vanilla
    // BER. The instanced visuals are optional; this is how that claim is tested on these nodes.
    propOrNull("deps.flywheel")?.let { fw ->
        compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-$mcVersion:$fw")
        if (propOrNull("flywheel.run") != "false") devModJar("dev.engine-room.flywheel:flywheel-neoforge-$mcVersion:$fw")
    }

    // Sodium declares itself client-side, so it loads only during the client run. It goes on
    // `devModJar` rather than `localRuntime` — see that configuration for why.
    propOrNull("deps.sodium_neoforge")?.let { devModJar("maven.modrinth:sodium:$it") }

    // Pipez is a real third-party energy mod, on `localRuntime` so it is present for the gametest
    // server run and not only the client: it ships no mixins, so it needs no `mods` folder placement.
    // Nothing compiles against it — the tests that drive it reach its one public setter reflectively.
    propOrNull("deps.pipez")?.let { localRuntime("maven.modrinth:pipez:$it") }

    // CC: Tweaked is an optional peer: HBM's 1.7 OpenComputers components are served to it as peripherals
    // (com.hbm.compat.computercraft, guarded by the `computercraft` constant). Compiled against on every node,
    // never shipped or required. `-Pcomputercraft.run=true` puts the mod in the dev runs as well; it is off by
    // default so the recipe baselines stay CC-free.
    // It carries mixins and nested jars, so the run takes it as a mod file (`devModJar`, and `ccServerMod` for
    // the server and GameTest runs, whose mods folders nothing else syncs).
    val ccMc = propOrNull("deps.cc_mc")
    val ccVer = propOrNull("deps.cc")
    if (ccMc != null && ccVer != null) {
        // HBM's peripherals need the peripheral API alone (core-api, no Minecraft types); registration is Core's.
        compileOnly("cc.tweaked:cc-tweaked-$ccMc-core-api:$ccVer") { isTransitive = false }
        if (propOrNull("computercraft.run") == "true") {
            devModJar("cc.tweaked:cc-tweaked-$ccMc-forge:$ccVer") { isTransitive = false }
            ccServerMod("cc.tweaked:cc-tweaked-$ccMc-forge:$ccVer") { isTransitive = false }
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
// Loader fact: NeoForge patches SimpleCookingSerializer to read the result with ItemStack.CODEC, so a cooking
// result keeps its count on 1.21.1 where vanilla (and Fabric) drop it; see gradle/legacy-recipes.gradle.kts.
extra["loader_cooking_result_count"] = true
apply(from = rootProject.file("gradle/run-toolchain.gradle.kts"))
// Visual test harness launch capture (shared by every Golem Core mod); see harness.json and ../Golem-Core/harness/README.md.
apply(from = rootProject.file("../Golem-Core/gradle/harness-spec.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-recipes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))
apply(from = rootProject.file("gradle/dejson.gradle.kts"))

apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))

dependencies {
    // NeoForge ships jspecify transitively on 26.1 but not on older versions, and HBM uses @Nullable in
    // ~156 files. Declared explicitly so the shared tree compiles on every node; annotations-only jar,
    // no runtime component.
    compileOnly("org.jspecify:jspecify:1.0.0")
}

// javac stops reporting after 100 errors by default, which hides the tail while porting a new node.
tasks.compileJava { options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000")) }

// The id constants recipe declarations are written against. Generated from the live registry by
// `runGenerateIds` and committed, because the plain-JVM build step the declarations run in cannot
// load half the registry classes; see com.hbm.lib.IdConstants.
sourceSets["main"].java.srcDir(rootProject.file("src/generated/java"))

// The generating run stops the server the moment it has written the files, which a GameTest server
// reports as a failure because it never got to run a test. The files are the whole product of the
// task, and they are written before the halt.
tasks.named<JavaExec>("runGenerateIds") { isIgnoreExitValue = true }

// Match Forge/Fabric: an aborted test world can retain armed machines and live blasts. Loading it
// into the next run lets those old fixtures tick alongside the new suite (including DFC detonations).
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst { delete(layout.projectDirectory.dir("runs/gametest/world")) }
}
