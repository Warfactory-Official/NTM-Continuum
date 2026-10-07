plugins {
    id("java-library")
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-forge-$mcVersion"

java { toolchain.languageVersion = JavaLanguageVersion.of(javaVersion) }

sourceSets["main"].java {
    exclude("com/hbm/platform/fabric/**", "com/hbm/platform/neoforge/**")
}

legacyForge {
    version = prop("deps.forge")
    runs {
        configureEach {
            systemProperty("forge.enabledGameTestNamespaces", modId)
            ideName = "Forge ${name.replaceFirstChar { it.uppercase() }} ($mcVersion)"
        }
        register("client") {
            client()
            gameDirectory = file("runs/client")
            // `-PquickPlayWorld=<save name>` joins a singleplayer world straight from launch. JEI does
            // not build its runtime — categories, recipes, catalysts — at the title screen, so a plain
            // runClient can only ever prove that JEI loaded, never that this mod's plugin registered
            // anything. This is the difference between "no errors" and an actual positive signal.
            propOrNull("quickPlayWorld")?.let { programArguments.addAll("--quickPlaySingleplayer", it) }
        }
        register("data") {
            data()
            gameDirectory = file("runs/data")
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", rootProject.file("src/main/resources").absolutePath,
            )
        }
        register("server") {
            server()
            gameDirectory = file("runs/server")
        }
        // Headless GameTest runner: `gradlew :1.20.1-forge:runGameTestServer` executes every hbm:* test
        // through Golem's @GameTestGenerator (the pre-1.21.4 framework discovers tests by reflection).
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = file("runs/gametest")
            // -Pgametest.filter=<regex> runs only the matching test ids (see Golem's GameTests.addLegacy).
            jvmArgument("-Dgolem.gametest.filter=${providers.gradleProperty("gametest.filter").orElse("").get()}")
        }
    }
    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
    }
}

sourceSets["main"].resources.srcDir("src/generated/resources")

// The legacy GameTest server writes fresh level data but reuses whatever region files the previous run
// left in runs/gametest/world, so craters, fills and ash from earlier runs sit under the new plots and
// tests fail on ground they never touched. Start every run from an empty world, as 26.1's runner does.
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst {
        delete(layout.projectDirectory.dir("runs/gametest/world"))
        // Forge's GameTestMain is the dedicated-server main with a test hook, so the world comes from
        // server.properties rather than from vanilla's FLAT preset: without this line the plots sit 125
        // blocks under normal terrain (a sat link found grass at y=65 in its sky column, and the crater
        // benchmark carved 24,000 blocks of hillside).
        val properties = layout.projectDirectory.file("runs/gametest/server.properties").asFile
        properties.parentFile.mkdirs()
        val lines = if (properties.exists()) properties.readLines().filterNot {
            it.startsWith("level-type=") || it.startsWith("level-seed=") || it.startsWith("max-tick-time=")
        } else emptyList()
        properties.writeText((lines + listOf("level-type=minecraft:flat", "level-seed=0", "max-tick-time=-1"))
            .joinToString(System.lineSeparator()) + System.lineSeparator())
    }
}

// A one-off smoke launch must not leave Quick Play in ModDev's cached argument file.
tasks.matching { it.name == "prepareClientRun" }.configureEach {
    inputs.property("hbmQuickPlayWorld", providers.gradleProperty("quickPlayWorld").orElse(""))
}

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

dependencies {
    modImplementation("dev.golem:golemcore-forge-$mcVersion:${prop("deps.golemcore")}+$mcVersion")
    compileOnly("org.spongepowered:mixin:0.8.5")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // Forge 1.20.1 does not bundle MixinExtras; Golem Core nests mixinextras-forge (Jar-in-Jar) and its dev runtime
    // carries it here, so HBM can compile against it without shipping a copy.
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        modCompileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
        modCompileOnly("mezz.jei:jei-$jei-forge-api:$jeiVer")
        modRuntimeOnly("mezz.jei:jei-$jei-forge:$jeiVer")
    }

    // Published Flywheel is optional at runtime.
    // `-Pflywheel.run=false` leaves the engine jar out of the dev run (compile stays), so the dev client
    // can be launched as a player without Flywheel would see it: every machine through its vanilla
    // BER. The instanced visuals are optional; this is how that claim is tested on these nodes.
    propOrNull("deps.flywheel")?.let { fw ->
        modCompileOnly("dev.engine-room.flywheel:flywheel-forge-api-$mcVersion:$fw")
        if (propOrNull("flywheel.run") != "false") modRuntimeOnly("dev.engine-room.flywheel:flywheel-forge-$mcVersion:$fw")
    }

    // Embeddium is the Forge port of Sodium — Sodium itself ships no Forge build, so this is the
    // equivalent dev renderer for this node. Client-side only, dev runtime only, never published.
    propOrNull("deps.embeddium")?.let { modRuntimeOnly("maven.modrinth:embeddium:$it") }
    // Iris (Oculus on Forge 1.20.1) for testing under shader packs: `-Pshaders.run=true` puts it in the client run.
    // Client-side, dev runtime only, never compiled against or published; off by default so the standard runs
    // and baselines keep vanilla's pipeline. Place a pack in the run's shaderpacks folder and select it in game.
    if (propOrNull("shaders.run") == "true") propOrNull("deps.oculus")?.let { modRuntimeOnly("maven.modrinth:oculus:$it") }

    // Pipez: the real third-party energy peer the foreign-bridge gametests run against. `modRuntimeOnly`
    // so the production jar is remapped for this node's dev mappings; nothing compiles against it.
    propOrNull("deps.pipez")?.let { modRuntimeOnly("maven.modrinth:pipez:$it") }

    // CC: Tweaked is an optional peer: HBM's 1.7 OpenComputers components are served to it as peripherals
    // (com.hbm.compat.computercraft, guarded by the `computercraft` constant). Compiled against on every node,
    // never shipped or required. `-Pcomputercraft.run=true` puts the mod in the dev runs as well; it is off by
    // default so the recipe baselines stay CC-free.
    val ccMc = propOrNull("deps.cc_mc")
    val ccVer = propOrNull("deps.cc")
    if (ccMc != null && ccVer != null) {
        // HBM's peripherals need the peripheral API alone (core-api, no Minecraft types); registration is Core's.
        compileOnly("cc.tweaked:cc-tweaked-$ccMc-core-api:$ccVer") { isTransitive = false }
        if (propOrNull("computercraft.run") == "true") {
            modRuntimeOnly("cc.tweaked:cc-tweaked-$ccMc-forge:$ccVer") { isTransitive = false }
        }
    }
}

mixin {
    add(sourceSets["main"], "$modId.refmap.json")
    config("$modId.mixins.json")
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
        "forge_loader_range" to prop("deps.forge_loader_range"),
        "java_version" to javaVersion.toString(),
        "mixin_compatibility" to if (javaVersion <= 17) "JAVA_17" else "JAVA_21",
        "pack_format" to prop("pack_format"),
        "pack_format_entry" to "\"pack_format\": ${prop("pack_format")}",
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta", "*.mixins.json")) { expand(props) }
    exclude(
        "fabric.mod.json", "$modId.fabric.mixins.json", "META-INF/neoforge.mods.toml",
        "data/$modId/neoforge/**", "data/neoforge/**",
    )
}

tasks.jar {
    manifest.attributes["MixinConfigs"] = "$modId.mixins.json"
    from(files(rootProject.file("LICENSE"), rootProject.file("LICENSE.LESSER"))) { rename { "${it}_${prop("mod.id")}" } }
}

tasks.compileJava {
    dependsOn(tasks.named("stonecutterGenerate"))
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000"))
}
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

extra["mc_version"] = mcVersion
extra["java_version"] = javaVersion.toString()
// Forge 1.20.1's community tag namespace is `forge:`, not the `c:` this mod's data is authored in —
// `c:` only became universal with NeoForge 1.21. The legacy resource pass republishes the convention
// tags under this namespace and repoints recipes at them; see gradle/legacy-recipes.gradle.kts.
// Forge nodes at 1.21+ (should any be added) would drop this line rather than change it.
extra["convention_namespace"] = "forge"
apply(from = rootProject.file("gradle/run-toolchain.gradle.kts"))
// Visual test harness launch capture (shared by every Golem Core mod); see harness.json and ../Golem-Core/harness/README.md.
apply(from = rootProject.file("../Golem-Core/gradle/harness-spec.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-recipes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))
apply(from = rootProject.file("gradle/dejson.gradle.kts"))
apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))

dependencies { compileOnly("org.jspecify:jspecify:1.0.0") }

// The id constants recipe declarations are written against. Generated from the live registry by
// `runGenerateIds` and committed, because the plain-JVM build step the declarations run in cannot
// load half the registry classes; see com.hbm.lib.IdConstants.
sourceSets["main"].java.srcDir(rootProject.file("src/generated/java"))
