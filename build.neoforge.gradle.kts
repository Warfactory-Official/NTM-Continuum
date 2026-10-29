plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
apply(from = rootProject.file("../Golem-Core/gradle/mod/gemrender-local.gradle.kts"))
val localGemRender = extra.properties["golem.gemrender.local"]
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-neoforge"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

sourceSets["main"].java {
    exclude("com/hbm/platform/fabric/**", "com/hbm/platform/forge/**")
}

// The NeoForge this build runs on: deps.neoforge, or deps.neoforge_kubejs in a -Pkubejs.run=true dev run (KubeJS may need
// an older build; see versions/*/gradle.properties). Release jars always use deps.neoforge.
val neoforgeVersion = propOrNull("deps.neoforge_kubejs")?.takeIf { propOrNull("kubejs.run") == "true" } ?: prop("deps.neoforge")
// The oldest NeoForge the jar accepts (neoforge.mods.toml): deps.neoforge_floor where a node sets one, else the build's.
val neoforgeFloor = propOrNull("deps.neoforge_floor") ?: neoforgeVersion

neoForge {
    version = neoforgeVersion

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
        forRepository { maven("https://cursemaven.com") { name = "CurseMaven" } }
        filter { includeGroup("curse.maven") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.ryanhcode.dev/releases") { name = "RyanHCode" } }
        filter {
            includeGroup("dev.ryanhcode.sable")
            includeGroup("dev.ryanhcode.sable-companion")
        }
    }
    // KubeJS and Rhino (the optional `kubejs` peer, dev runs only with -Pkubejs.run=true; see deps.kubejs).
    exclusiveContent {
        forRepository { maven("https://maven.latvian.dev/releases") { name = "LatvianDev" } }
        filter { includeGroup("dev.latvian.mods") }
    }
    // REI and its Cloth Config / Architectury dependencies (the optional `rei` peer, see deps.rei).
    exclusiveContent {
        forRepository { maven("https://maven.shedaniel.me") { name = "Shedaniel" } }
        filter {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
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

// The IDE's run configurations do not run `runClient`: they run ModDevGradle's `prepareClientRun` and start the game
// directly, so the mods folder is filled from there as well.
tasks.matching { it.name == "runClient" || it.name == "prepareClientRun" }.configureEach { dependsOn(syncDevMods) }

// CC: Tweaked (`-Pcomputercraft.run=true`) and Sable / Create / Create Aeronautics (`-Psable.run=true`) for the
// server and GameTest runs; empty otherwise, so a run without the flags also clears jars an earlier flagged run
// left behind. One sync per run folder: two would each delete the other's jars. Not a `Sync` task: Gradle skips a
// copy task with no source files (NO-SOURCE) without running it, so an unflagged run used to leave the previous
// flagged run's jars in place and run with them (seen with Sable, 2026-10-05).
val ccServerMod: Configuration by configurations.creating
val sableServerMod: Configuration by configurations.creating
val kubejsServerMod: Configuration by configurations.creating
val ae2ServerMod: Configuration by configurations.creating
val rsServerMod: Configuration by configurations.creating
for ((run, task) in listOf("server" to "runServer", "gametest" to "runGameTestServer")) {
    val modsDir = layout.projectDirectory.dir("runs/$run/mods")
    val sync = tasks.register("syncComputerCraft${run.replaceFirstChar { it.uppercase() }}") {
        description = "Places the opt-in peer mods in the $run run's mods folder (-Pcomputercraft.run, -Psable.run, -Pkubejs.run, -Pae2.run, -Prefinedstorage.run)."
        inputs.files(ccServerMod, sableServerMod, kubejsServerMod, ae2ServerMod, rsServerMod)
        outputs.dir(modsDir)
        doLast {
            project.sync {
                from(ccServerMod)
                from(sableServerMod)
                from(kubejsServerMod)
                from(ae2ServerMod)
                from(rsServerMod)
                into(modsDir)
            }
        }
    }
    // `prepare<Run>Run` is what the IDE's run configuration runs before launching.
    val prepare = "prepare${task.removePrefix("run")}Run"
    tasks.matching { it.name == task || it.name == prepare }.configureEach { dependsOn(sync) }
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
    // NeoForge and Fabric Loader bundle MixinExtras; on Forge 1.20.1 Golem Core nests it. Safe in shared code.
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

    // REI (Roughly Enough Items) is an optional peer like JEI: HBM's recipe-viewer declarations
    // (com.hbm.handler.jei.HbmRecipeViewer) feed it through Golem Core's dev.golem.core.rei, from the thin plugins in
    // com.hbm.handler.rei (guarded by the `rei` constant). Compiled against on every node, never shipped or required.
    // `-Prei.run=true` puts REI (with Cloth Config and Architectury) in the client run, beside JEI, so a run with it is
    // also the "both viewers installed" case; off by default so the recipe baselines stay REI-free. Architectury
    // carries mixins, so the three go to the run's mods folder (`devModJar`).
    propOrNull("deps.rei")?.let { rei ->
        compileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:$rei")
        if (propOrNull("rei.run") == "true") devModJar("me.shedaniel:RoughlyEnoughItems-neoforge:$rei")
    }

    // Jade, dev runs only (`-Pjade.run=true`): HBM's overlay declarations (com.hbm.compat.HbmBlockInfo) reach it
    // through Golem Core's dev.golem.core.jade, so HBM has no Jade dependency. Jade carries mixins, hence `devModJar`.
    if (propOrNull("jade.run") == "true") propOrNull("deps.jade")?.let { devModJar("maven.modrinth:jade:$it") }
    // ImmediatelyFast, dev runs only (`-Pimmediatelyfast.run=true`): HUD batching that has hidden the hotbar before.
    if (propOrNull("immediatelyfast.run") == "true") propOrNull("deps.immediatelyfast")?.let { devModJar("maven.modrinth:immediatelyfast:$it") }
    // Just Enough Characters, dev runs only (`-Pjecharacters.run=true`): pinyin matching in HBM's search bars (Core TextSearch).
    if (propOrNull("jecharacters.run") == "true") propOrNull("deps.jecharacters")?.let { devModJar("maven.modrinth:justenoughcharacters:$it") }

    // Applied Energistics 2 and its required GuideME, dev runs only (`-Pae2.run=true`): HBM's machines reach AE2's
    // pattern providers through Golem Core's dev.golem.core.autocraft, so HBM has no AE2 dependency. Both carry mixins.
    if (propOrNull("ae2.run") == "true") {
        for (conf in listOf(devModJar, ae2ServerMod)) {
            propOrNull("deps.ae2")?.let { add(conf.name, "maven.modrinth:ae2:$it") { isTransitive = false } }
            propOrNull("deps.ae2_guideme")?.let { add(conf.name, "maven.modrinth:guideme:$it") { isTransitive = false } }
        }
    }

    // Refined Storage, dev runs only (`-Prefinedstorage.run=true`): it reaches HBM's machines through the item and
    // fluid capabilities Golem Core exposes, so HBM has no Refined Storage dependency.
    if (propOrNull("refinedstorage.run") == "true") {
        for (conf in listOf(devModJar, rsServerMod)) {
            propOrNull("deps.refinedstorage")?.let { add(conf.name, "maven.modrinth:refined-storage:$it") { isTransitive = false } }
        }
    }

    // Holo Inventory Modern, client dev runs only (`-Pholoinventory.run=true`): reads HBM blocks through the item capability.
    if (propOrNull("holoinventory.run") == "true") {
        propOrNull("deps.holoinventory")?.let { devModJar("curse.maven:holo-inventory-modern-1720961:$it") }
    }

    // Mo' Bends, client dev runs only (`-Pmobends.run=true`), with the Architectury API it requires. HBM's armour follows
    // its bent skeleton through Golem Core (dev.golem.core.render.BodyAnimations); nothing here compiles against it.
    if (propOrNull("mobends.run") == "true") {
        propOrNull("deps.mobends_run")?.let { devModJar("maven.modrinth:mo-bends-unofficial-modern-port:$it") { isTransitive = false } }
        propOrNull("deps.mobends_architectury")?.let { devModJar("maven.modrinth:architectury-api:$it") { isTransitive = false } }
    }

    // GemRender (with Flywheel nested or provided) instances gem-declared machines through Golem Core; every machine
    // keeps its vanilla BER. A mods-folder jar (`devModJar`) so its nested Flywheel and mixin configs load.
    // `-Pgemrender.run=false` launches without it, as a player without GemRender would.
    // `-Pgemrender.jar=<path>` runs that GemRender jar instead; `gemrender.dir` is Golem Core's gemrender-local script.
    if (propOrNull("gemrender.run") != "false") {
        val gemJar = propOrNull("gemrender.jar")
        if (gemJar != null) devModJar(files(gemJar))
        else if (localGemRender != null) devModJar(localGemRender)
        else propOrNull("deps.gemrender")?.let { devModJar("curse.maven:gemrender-1735539:$it") }
    }

    // Sodium declares itself client-side, so it loads only during the client run. It goes on
    // `devModJar` rather than `localRuntime` — see that configuration for why.
    propOrNull("deps.sodium_neoforge")?.let { devModJar("maven.modrinth:sodium:$it") }
    // Iris (Oculus on Forge 1.20.1) for testing under shader packs: `-Pshaders.run=true` puts it in the client run.
    // Client-side, dev runtime only, never compiled against or published; off by default so the standard runs
    // and baselines keep vanilla's pipeline. Place a pack in the run's shaderpacks folder and select it in game.
    if (propOrNull("shaders.run") == "true") propOrNull("deps.iris")?.let { devModJar("maven.modrinth:iris:$it") }

    // Pipez is a real third-party energy mod, on `localRuntime` so it is present for the gametest
    // server run and not only the client: it ships no mixins, so it needs no `mods` folder placement.
    // Nothing compiles against it — the tests that drive it reach its one public setter reflectively.
    propOrNull("deps.pipez")?.let { localRuntime("maven.modrinth:pipez:$it") }

    // KubeJS is an optional peer served entirely by Golem Core's dev.golem.core.kubejs adapter (HBM has no KubeJS code:
    // its recipe tables, script bindings and events reach KubeJS through Core). Never compiled against, shipped or
    // required. `-Pkubejs.run=true` puts KubeJS and Rhino in the dev runs (mod files: both carry mixins or mod
    // metadata) and copies harness/kubejs into each run's kubejs folder; off by default so the baselines stay KubeJS-free.
    propOrNull("deps.kubejs")?.let { kubejs ->
        if (propOrNull("kubejs.run") == "true") {
            for (mod in listOfNotNull(propOrNull("deps.kubejs_bat")?.let { "dev.latvian.mods:better-advanced-tooltips:$it" }, "dev.latvian.mods:kubejs-neoforge:$kubejs", "dev.latvian.mods:rhino:${prop("deps.kubejs_rhino")}")) {
                devModJar(mod) { isTransitive = false }
                kubejsServerMod(mod) { isTransitive = false }
            }
        }
    }

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
    // Sable (moving block sub-levels, the engine under Create Aeronautics) is an optional peer on the nodes that
    // declare it. HBM's side of it is its own source root, src/sable (com.hbm.compat.sable), added only here; the
    // shared tree reaches sub-levels only through Golem Core's neutral dev.golem.core.world.SubLevels. Compiled
    // against, never shipped or required. `-Psable.run=true` puts Sable, Create and Create Aeronautics in the dev
    // runs; off by default so the recipe baselines stay Create-free.
    val sableMc = propOrNull("deps.sable_mc")
    val sableVer = propOrNull("deps.sable")
    val sableCompanionVer = propOrNull("deps.sable_companion")
    if (sableMc != null && sableVer != null && sableCompanionVer != null) {
        compileOnly("dev.ryanhcode.sable-companion:sable-companion-common-$sableMc:$sableCompanionVer") { isTransitive = false }
        compileOnly("dev.ryanhcode.sable:sable-common-$sableMc:$sableVer") { isTransitive = false }
        if (propOrNull("sable.run") == "true") {
            for (mod in listOfNotNull(
                propOrNull("deps.sable_run")?.let { "maven.modrinth:sable:$it" },
                propOrNull("deps.create_run")?.let { "maven.modrinth:create:$it" },
                propOrNull("deps.aeronautics_run")?.let { "maven.modrinth:create-aeronautics:$it" },
            )) {
                devModJar(mod) { isTransitive = false }
                sableServerMod(mod) { isTransitive = false }
            }
        }
    }
}

if (propOrNull("deps.sable") != null) {
    sourceSets["main"].java.srcDir(rootProject.file("src/sable/java"))
    sourceSets["main"].java.srcDir(rootProject.file("src/sable-neoforge/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("src/sable/resources"))
}

// Happy ghasts exist from Minecraft 1.21.6, so only the 26.1 nodes carry the happy-ghast Fat Man (a happy ghast carries
// a rigged Fat Man as it carries a boat). It is its own source root, src/happyghast (com.hbm.compat.happyghast), plus
// this loader's entry points in src/happyghast-neoforge, added only on nodes that set `feature.happy_ghast=true`.
if (propOrNull("feature.happy_ghast") == "true") {
    sourceSets["main"].java.srcDir(rootProject.file("src/happyghast/java"))
    sourceSets["main"].java.srcDir(rootProject.file("src/happyghast-neoforge/java"))
}

// The importer for NTM: Decentralized Edition worlds (1.21.1 NeoForge only) is its own source root, src/deimport
// (com.hbm.deimport), added only where `feature.de_import=true`. Nothing outside it names it, so turning the flag off
// and deleting the root removes it; `deimportBoundary` fails the build if anything else refers to it.
if (propOrNull("feature.de_import") == "true") {
    sourceSets["main"].java.srcDir(rootProject.file("src/deimport/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("src/deimport/resources"))
}
// Its legacy layer (registry aliases for DE ids in other mods' storage) outlives the converter: src/delegacy
// (com.hbm.delegacy), added only where `feature.de_legacy=true`.
// The converter uses the legacy layer's stack conversion, so it cannot be built without it.
if (propOrNull("feature.de_import") == "true" && propOrNull("feature.de_legacy") != "true")
    throw GradleException("feature.de_import needs feature.de_legacy")
if (propOrNull("feature.de_legacy") == "true") {
    sourceSets["main"].java.srcDir(rootProject.file("src/delegacy/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("src/delegacy/resources"))
}
tasks.register("deimportBoundary") {
    description = "Fails if anything outside its own root names the DE importer or its legacy layer."
    val roots = listOf("src/main", "src/sable", "src/sable-neoforge", "src/happyghast", "src/happyghast-neoforge")
        .map { rootProject.file(it) }.filter { it.exists() }
    inputs.files(roots.map { fileTree(it) })
    doLast {
        val names = listOf("com.hbm.deimport", "hbm_deimport", "com.hbm.delegacy", "hbm_delegacy")
        val hits = roots.flatMap { root -> root.walkTopDown().filter { it.isFile }
            .filter { file -> file.readText().let { text -> names.any { text.contains(it) } } }.toList() }
        if (hits.isNotEmpty()) throw GradleException("DE importer named outside src/deimport and src/delegacy: $hits")
    }
}
tasks.named("check") { dependsOn("deimportBoundary") }

val resourceProps = mapOf(
    "mod_id" to modId,
    "mod_name" to prop("mod.name"),
    "mod_version" to prop("mod.version"),
    "mod_author" to prop("mod.author"),
    "mod_license" to prop("mod.license"),
    "mod_credits" to prop("mod.credits"),
    "mod_description" to prop("mod.description"),
    "minecraft_range" to prop("deps.minecraft_range"),
    "neoforge_version" to neoforgeFloor,
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

/** The node's resource templating, shared by the mod's resources and the recipe generator's copy of them. */
fun ProcessResources.expandModResources() {
    inputs.properties(resourceProps)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta", "*.mixins.json")) { expand(resourceProps) }
    // Only Forge generates a refmap; naming an absent one leaves the jar pointing at a file it lacks.
    filesMatching("*.mixins.json") { filter { if (it.trimStart().startsWith("\"refmap\"")) null else it } }
    // NeoForge reads only neoforge.mods.toml; the Forge template would ship unexpanded.
    exclude("fabric.mod.json", "$modId.fabric.mixins.json", "data/$modId/forge/**", "META-INF/mods.toml")
}

tasks.processResources { expandModResources() }

tasks.jar {
    from(files(rootProject.file("LICENSE"), rootProject.file("LICENSE.LESSER"), rootProject.file("CREDITS.txt"))) { rename { "${it}_${prop("mod.id")}" } }
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
// result keeps its count on 1.21.1 where vanilla (and Fabric) drop it; see ../Golem-Core/gradle/mod/legacy-recipes.gradle.kts.
extra["loader_cooking_result_count"] = true
// Golem Core's shared build layer; HBM's parameters for it (golem.build.*) are in gradle.properties.
apply(from = rootProject.file("../Golem-Core/gradle/mod/run-toolchain.gradle.kts"))
// Visual test harness launch capture (shared by every Golem Core mod); see harness.json and ../Golem-Core/harness/README.md.
apply(from = rootProject.file("../Golem-Core/gradle/mod/harness-spec.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-recipes.gradle.kts"))
extra["loader"] = "neoforge"
// Orders the mod before KubeJS (optional dependency) so its script groups and bindings reach the startup scripts.
apply(from = rootProject.file("../Golem-Core/gradle/mod/kubejs-order.gradle.kts"))
// Neutral "golem:conditions" in data JSON, rewritten to this loader's spelling; see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/recipe-conditions.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-assets.gradle.kts"))
// HBM's own item-model resource checks; after legacy-assets, whose parentModelOf export they read.
apply(from = rootProject.file("gradle/asset-checks.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/dejson.gradle.kts"))
// HBM's machine recipe tables, generated from src/recipes on 26.1 NeoForge (build.neoforge.gradle.kts); see the script.
apply(from = rootProject.file("../Golem-Core/gradle/mod/generated-data.gradle.kts"))

apply(from = rootProject.file("../Golem-Core/gradle/mod/mixin-validation.gradle.kts"))

// The uploaded jar: no GameTests or harness queries (see the script).
apply(from = rootProject.file("../Golem-Core/gradle/mod/release-jar.gradle.kts"))
// Credits and links in the loader metadata, from the mod.* properties.
apply(from = rootProject.file("../Golem-Core/gradle/mod/mod-metadata.gradle.kts"))
// Modrinth + CurseForge upload of this node's jar; run every node with the root `publishAllMods`.
apply(from = rootProject.file("../Golem-Core/gradle/mod/publish.gradle.kts"))

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

// Machine recipe tables, written in Java (src/recipes: one transliteration of 1.7's registerDefaults() per table, listed in
// com.hbm.recipes.HbmDataSources) and generated into datapack files on this node only, by Golem Core's DataGenerator in
// a headless server run: the rows are the runtime row types, written with the tables' own codecs, which need a started
// server. Every node's processResources depends on the run and packs its output (../Golem-Core/gradle/mod/generated-data
// .gradle.kts), so nothing is ever run by hand; Gradle reruns it only when its classpath (src/recipes, HBM, Golem Core) or
// resources changed. src/recipes is not shipped.
if (project.name == "26.1-neoforge") {
    val main = sourceSets["main"]
    val generatedDataRoot = rootProject.layout.buildDirectory.dir("generated/data")
    val recipes = sourceSets.create("recipes") {
        java.setSrcDirs(listOf(rootProject.file("src/recipes/java")))
        resources.setSrcDirs(listOf<File>())
        compileClasspath += main.compileClasspath + main.output.classesDirs
    }
    configurations["recipesRuntimeClasspath"].extendsFrom(configurations.runtimeClasspath.get())
    // One mod group for the run: HBM's classes and resources plus the table sources. The resources are what processResources
    // packs (the mod's source folders and Dejson's output), templated the same way, but not processResources itself,
    // whose output includes the generated tables and would make the run depend on its own product.
    recipes.output.dir(mapOf("builtBy" to tasks.named("compileJava")), main.java.destinationDirectory)
    tasks.named<ProcessResources>("processRecipesResources") {
        dependsOn(tasks.named("stonecutterGenerate"), tasks.named("dejsonGenerate"))
        from(provider { main.resources.srcDirs })
        from(layout.buildDirectory.dir("generated/dejson"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        expandModResources()
    }
    recipes.runtimeClasspath = recipes.output + configurations["recipesRuntimeClasspath"]

    neoForge {
        mods { register("hbmRecipes") { sourceSet(recipes) } }
        runs {
            // The generator's mod group repeats HBM's classes and resources without the generated tables: every other
            // run loads HBM's own group only, or it would hold two copies of the mod and could pick the one without them.
            matching { it.name != "generateData" }.configureEach { loadedMods.set(setOf(mods.getByName(modId))) }
            // The headless GameTest server, as runGenerateIds: the rows hold item stacks, and 26.1 binds item components
            // only once a server has loaded its registries (a data run never does). Golem Core's DataGenerator writes
            // when the server has started and halts it before any test runs.
            register("generateData") {
                type = "gameTestServer"
                sourceSet = recipes
                loadedMods.set(setOf(mods.getByName("hbmRecipes")))
                gameDirectory = file("runs/generatedata")
                ideName = "NeoForge Generate Data ($mcVersion)"
                systemProperty("golem.data.output", generatedDataRoot.get().asFile.absolutePath)
                systemProperty("golem.data.sources", "com.hbm.recipes.HbmDataSources,com.hbm.recipes.MinecraftDataSources")
                systemProperty("golem.gametest.filter", "^$")
            }
        }
    }

    tasks.named<JavaExec>("runGenerateData") {
        group = "build"
        description = "Generates the machine recipe tables from src/recipes into build/generated/data."
        inputs.files(recipes.output.classesDirs, main.output.classesDirs).withNormalizer(ClasspathNormalizer::class.java)
        outputs.dir(generatedDataRoot)
        outputs.cacheIf { false }
        // The halted server's exit code means nothing; the manifest is written last, only once every row was encoded.
        isIgnoreExitValue = true
        val manifest = generatedDataRoot.map { it.file("manifest.txt") }
        // A fresh world every run: the run stops without saving, and never needs one that existed before.
        doFirst {
            delete(manifest)
            delete(layout.projectDirectory.dir("runs/generatedata/world"))
        }
        doLast {
            check(manifest.get().asFile.isFile) {
                "$path wrote no ${manifest.get().asFile.name}: the generator never finished. Read the run's output above."
            }
        }
    }
}

// KubeJS dev scripts (`-Pkubejs.run=true`): harness/kubejs (startup_scripts, server_scripts, client_scripts) is copied
// into each run's kubejs folder (runs/harness too: the harness clients' game folder). A copy, not a sync: KubeJS writes its own config and exports beside the scripts.
if (propOrNull("kubejs.run") == "true" && propOrNull("deps.kubejs") != null) {
    for ((run, task) in listOf("client" to "runClient", "harness" to "runClient", "server" to "runServer", "gametest" to "runGameTestServer")) {
        val copy = tasks.register<Copy>("copyKubeJsScripts${run.replaceFirstChar { it.uppercase() }}") {
            description = "Copies harness/kubejs into the $run run's kubejs folder (-Pkubejs.run)."
            from(rootProject.file("harness/kubejs")) { exclude("README.md") }
            into(layout.projectDirectory.dir("runs/$run/kubejs"))
        }
        val prepare = "prepare${task.removePrefix("run")}Run"
        tasks.matching { it.name == task || it.name == prepare }.configureEach { dependsOn(copy) }
    }
}
