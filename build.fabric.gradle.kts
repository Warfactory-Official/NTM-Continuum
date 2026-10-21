import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    id("java-library")
    id("dev.kikugie.loom-back-compat")
}

fun prop(name: String): String = property(name) as String
fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

/**
 * A dev-runtime copy of CC: Tweaked's Fabric jar without its `Fabric-Loom-Version` manifest line. Loom 1.16 refuses to
 * remap a jar built by a newer Loom (CC's 1.20.1/1.21.1 jars name Loom 1.17); the jar itself needs nothing newer, its
 * mixins being statically remapped (`Fabric-Loom-Mixin-Remap-Type: static`). Fetched straight from CC's maven into
 * the build directory: resolving a configuration this early would disturb Loom's own repositories. Its nested
 * libraries (`META-INF/jars`: Cobalt, NightConfig, ...) are unpacked beside it, because a mod given as a file is not
 * searched for nested jars in a dev run; [computerCraftLibraries] lists them. Dev runs only, behind
 * `-Pcomputercraft.run=true`; a jar that really needed the newer Loom fails loudly at client start.
 */
fun loomCompatibleComputerCraft(mc: String, version: String): File {
    val name = "cc-tweaked-$mc-fabric-$version.jar"
    val target = layout.buildDirectory.file("loom-compatible/$name").get().asFile
    if (target.isFile) return target
    target.parentFile.mkdirs()
    val source = File(target.parentFile, "$name.download")
    uri("https://maven.squiddev.cc/cc/tweaked/cc-tweaked-$mc-fabric/$version/$name").toURL().openStream().use { input ->
        source.outputStream().use { input.copyTo(it) }
    }
    ZipFile(source).use { zip ->
        ZipOutputStream(target.outputStream()).use { out ->
            for (entry in zip.entries()) {
                out.putNextEntry(ZipEntry(entry.name))
                if (entry.name.startsWith("META-INF/jars/") && entry.name.endsWith(".jar")) {
                    val nested = File(target.parentFile, "nested/" + entry.name.substringAfterLast('/'))
                    nested.parentFile.mkdirs()
                    zip.getInputStream(entry).use { input -> nested.outputStream().use { input.copyTo(it) } }
                }
                if (entry.name == "META-INF/MANIFEST.MF") {
                    val manifest = Manifest(zip.getInputStream(entry))
                    manifest.mainAttributes.remove(Attributes.Name("Fabric-Loom-Version"))
                    manifest.write(out)
                } else if (!entry.isDirectory) {
                    zip.getInputStream(entry).use { it.copyTo(out) }
                }
                out.closeEntry()
            }
        }
    }
    source.delete()
    return target
}

/**
 * The libraries nested in [jar], unpacked once into `build/<dirName>`: every nested jar except Fabric API modules (which
 * the run already has). Loom's `include` gives a plain library a generated `fabric.mod.json`, so that cannot tell them apart.
 */
fun nestedLibraries(jar: File, dirName: String): List<File> {
    val dir = layout.buildDirectory.dir(dirName).get().asFile
    if (!dir.isDirectory) {
        dir.mkdirs()
        ZipFile(jar).use { zip ->
            for (entry in zip.entries()) {
                if (!entry.name.startsWith("META-INF/jars/") || !entry.name.endsWith(".jar")) continue
                val nested = File(dir, entry.name.substringAfterLast('/'))
                zip.getInputStream(entry).use { input -> nested.outputStream().use { input.copyTo(it) } }
            }
        }
    }
    return dir.listFiles()?.filter { it.extension == "jar" && !it.name.startsWith("fabric-") }?.sorted() ?: emptyList()
}

/**
 * A mod jar for the dev runs, fetched straight from [url] into `build/<dirName>` (resolving a configuration this early
 * would disturb Loom's repositories), with every jar it nests, recursively, unpacked beside it: a mod given to a dev
 * run is not searched for nested jars. Returns the jars that are Fabric mods (remapped by `modLocalRuntime`) and the
 * plain libraries, apart.
 */
fun runJarsWithNested(url: String, dirName: String): Pair<List<File>, List<File>> {
    val dir = layout.buildDirectory.dir(dirName).get().asFile
    val outer = File(dir, url.substringAfterLast('/'))
    if (!outer.isFile) {
        dir.mkdirs()
        val download = File(dir, "${outer.name}.download")
        uri(url).toURL().openStream().use { input -> download.outputStream().use { input.copyTo(it) } }
        download.renameTo(outer)
    }
    val nestedDir = File(dir, "nested")
    if (!nestedDir.isDirectory) {
        nestedDir.mkdirs()
        val queue = ArrayDeque(listOf(outer))
        while (queue.isNotEmpty()) {
            ZipFile(queue.removeFirst()).use { zip ->
                for (entry in zip.entries()) {
                    if (!entry.name.startsWith("META-INF/jars/") || !entry.name.endsWith(".jar")) continue
                    val nested = File(nestedDir, entry.name.substringAfterLast('/'))
                    zip.getInputStream(entry).use { input -> nested.outputStream().use { input.copyTo(it) } }
                    queue.add(nested)
                }
            }
        }
    }
    val nested = nestedDir.listFiles()?.filter { it.extension == "jar" }?.sorted() ?: emptyList()
    val (mods, libraries) = nested.partition { jar -> ZipFile(jar).use { it.getEntry("fabric.mod.json") != null } }
    return (listOf(outer) + mods) to libraries
}

/**
 * Sable's Fabric jar for the dev runs (`-Psable.run=true`) and the mods it nests (Veil, Forge Config API Port, Sable
 * Companion, its Rapier natives), from Sable's maven.
 */
fun sableRunJars(mc: String, version: String): Pair<List<File>, List<File>> = runJarsWithNested(
    "https://maven.ryanhcode.dev/releases/dev/ryanhcode/sable/sable-fabric-$mc/$version/sable-fabric-$mc-$version.jar",
    "sable-run/$version")

/** The nested libraries [loomCompatibleComputerCraft] unpacked; call it first. */
fun computerCraftLibraries(): FileCollection =
    files(layout.buildDirectory.dir("loom-compatible/nested").get().asFile.listFiles()?.filter { it.extension == "jar" }
        ?: emptyList<File>())

val mcVersion = stonecutter.current.version
val javaVersion = prop("java_version").toInt()
val modId = prop("mod.id")

group = prop("mod.group")
version = "${prop("mod.version")}+$mcVersion"
base.archivesName = "$modId-fabric-$mcVersion"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

sourceSets["main"].java {
    exclude("com/hbm/platform/neoforge/**", "com/hbm/platform/forge/**")
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

// Sable (moving block sub-levels) is an optional peer on the nodes that declare it. HBM's side of it is the shared
// root src/sable (com.hbm.compat.sable) plus this loader's entry points in src/sable-fabric, added only here and
// appended to fabric.mod.json only here; the shared tree reaches sub-levels only through Golem Core's neutral
// dev.golem.core.world.SubLevels. `-Psable.run=true` puts Sable in the dev runs (Create Aeronautics has no Fabric
// build); off by default.
val sableMc = propOrNull("deps.sable_mc")
val sableVer = propOrNull("deps.sable")
val sableCompanionVer = propOrNull("deps.sable_companion")
val hasSable = sableMc != null && sableVer != null && sableCompanionVer != null
if (hasSable) {
    sourceSets["main"].java.srcDir(rootProject.file("src/sable/java"))
    sourceSets["main"].java.srcDir(rootProject.file("src/sable-fabric/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("src/sable/resources"))
}

// Happy ghasts exist from Minecraft 1.21.6, so only the 26.1 nodes carry the happy-ghast Fat Man: the shared root
// src/happyghast (com.hbm.compat.happyghast) plus this loader's entry points in src/happyghast-fabric, added and
// written into fabric.mod.json only on nodes that set `feature.happy_ghast=true`.
val hasHappyGhast = propOrNull("feature.happy_ghast") == "true"
if (hasHappyGhast) {
    sourceSets["main"].java.srcDir(rootProject.file("src/happyghast/java"))
    sourceSets["main"].java.srcDir(rootProject.file("src/happyghast-fabric/java"))
}

// Whether the target Minecraft is obfuscated decides which Loom configurations exist. 26.1+ ships
// unobfuscated, so Loom runs with remapping off and never creates the `mod*` configurations; plain
// `implementation` is correct and no mappings layer is needed. Older nodes need `modImplementation`
// plus an explicit Mojmap layer. This is an ordinary Kotlin conditional, not a Stonecutter `//?`
// comment: Stonecutter only preprocesses files under src/, never build scripts.
val obfuscated = mcVersion.substringBefore('.').toInt() < 26

// Whether this node has vanilla's special-model-renderer system (26.1+). Tracked separately from
// `obfuscated` even though the two currently coincide: they are unrelated facts that will diverge.
val modernRendering = mcVersion.substringBefore('.').toInt() >= 26

// pack.mcmeta's version field, whose very name changed: 26.1 declares a supported RANGE
// (min_format/max_format) where older versions declare a single pack_format. Getting this wrong is
// not cosmetic — vanilla rejects the pack outright, and every datapack entry in the mod (worldgen
// features, recipes, tags) silently disappears with it.
val packFormatEntry = if (modernRendering)
    "\"min_format\": ${prop("pack_format")}, \"max_format\": ${prop("pack_format")}"
else
    "\"pack_format\": ${prop("pack_format")}"
/**
 * Pins LWJGL to the version Minecraft itself ships, undoing a Loom upgrade this project does not need.
 *
 * Loom's `LWJGL3UpgradeLibraryProcessor` force-upgrades LWJGL when the **Gradle JVM** is Java 19+ and
 * the target Minecraft's own library list predates Java 19 support. On 1.20.1 that is true — vanilla
 * ships LWJGL 3.3.1 — so Loom silently substitutes 3.3.2 (`3.3.1 -> 3.3.2` by conflict resolution).
 * It returns MUST_APPLY, so `fabric.loom.libraryProcessors` cannot switch it off: that property is an
 * opt-IN list consulted only for CAN_APPLY processors.
 *
 * The upgrade exists so a dev client can launch on Java 19+. Ours never does — `run-toolchain.gradle.kts`
 * pins every run to the node's own toolchain, Java 17 here — so Loom is solving a problem this project
 * does not have, at the cost of a dev client running a different native stack than any player. Sodium
 * refuses to start on the mismatch outright (its early driver scanner demands exactly 3.3.1 on 1.20.1),
 * which is how this surfaced; the deeper objection is that testing 3.3.2 tests something we never ship.
 *
 * Leave `deps.lwjgl` blank on any node where Loom's choice should stand.
 */
propOrNull("deps.lwjgl")?.let { pinned ->
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.lwjgl") useVersion(pinned)
        }
    }
}

val modCfg = if (obfuscated) "modImplementation" else "implementation"
// Loom's `localRuntime` is not remapped either way; with obfuscation off the plain config is correct.
val devRuntime = if (obfuscated) "modLocalRuntime" else "localRuntime"

/**
 * Drops Loom's cached remap of Golem Core when an upstream input is newer than it.
 *
 * Loom caches remapped mod jars under `.gradle/loom-cache/remapped_mods/remapped/<group>/<artifact>/
 * <version>/`, keyed by the *coordinates* — content is never hashed. Golem Core arrives through a
 * composite build whose version string (`0.1.0+<mc>`) is constant, so editing it produces a
 * byte-different jar under an identical cache key and Loom keeps serving the old remap. HBM then
 * compiles against a stale Golem Core with no error, no warning, and a green build.
 *
 * This was not hypothetical: the cached 1.21.1 remap was found two days old, missing methods that had
 * since been added, while `chiseledBuild` reported success. Only the obfuscated Fabric nodes are
 * affected — NeoForge does not remap, and 26.1-fabric is unobfuscated, so both see the real jar. That
 * asymmetry is what makes it dangerous: five nodes agree and the two quiet ones are wrong.
 *
 * Deleting the remap is only half of it. Loom resolves the substituted dependency at configuration
 * time, which is before the composite build's `jar` task could possibly run, so it re-remaps
 * whichever jar is already sitting in Golem Core's `build/libs` — and if that jar predates the edit
 * too, the fresh remap is just as stale as the one that was deleted. The build then reports zero
 * `:Golem-Core:` tasks and a missing symbol for a method that plainly exists. `checkGolemJarIsFresh`
 * below catches that case, because nothing in this build can fix it after the fact.
 *
 * Runs at configuration time because Loom resolves remapped mods before any task executes.
 */
fun invalidateStaleGolemRemap() {
    if (!obfuscated) return
    val cacheRoot = rootProject.file(".gradle/loom-cache/remapped_mods/remapped/dev/golem")
    if (!cacheRoot.isDirectory) return

    val golemRoot = rootProject.file("../Golem-Core")
    val upstream = sequence {
        // Directory mtimes make deletions and renames visible; file mtimes cover content changes.
        yieldAll(golemRoot.resolve("src").walkTopDown().filter { it.isFile || it.isDirectory })
        yield(golemRoot.resolve("build.fabric.gradle.kts"))
        yield(golemRoot.resolve("settings.gradle.kts"))
        yield(golemRoot.resolve("stonecutter.gradle.kts"))
        yield(golemRoot.resolve("gradle.properties"))
        yield(golemRoot.resolve("versions/$mcVersion-fabric/gradle.properties"))
        yieldAll(golemRoot.resolve("versions/$mcVersion-fabric/build/libs").listFiles()
            ?.filter { it.name.endsWith(".jar") && !it.name.endsWith("-sources.jar") }
            .orEmpty())
    }.filter { it.isFile || it.isDirectory }.maxOfOrNull { it.lastModified() }

    cacheRoot.listFiles { file -> file.isDirectory && file.name.startsWith("$mcVersion-fabric-") }
        ?.forEach { entry ->
            val cachedAt = entry.walkTopDown().filter { it.isFile }.maxOfOrNull { it.lastModified() }
            if (cachedAt != null && upstream != null && upstream > cachedAt) {
                logger.lifecycle("Golem Core changed since its cached remap; invalidating ${entry.name}.")
                entry.deleteRecursively()
            }
        }
}

invalidateStaleGolemRemap()

/**
 * Fails the build when Golem Core's jar is older than Golem Core's sources.
 *
 * The composite build only runs Golem Core's `jar` task when Gradle has to produce the artifact. An
 * artifact already on disk is taken as-is, at configuration time, and no `:Golem-Core:` task appears
 * in the build at all — so an edit to Golem Core that has not been rebuilt is compiled against its
 * previous self. The symptom is a `cannot find symbol` on a method that is right there in the source,
 * which reads like a broken IDE rather than a stale artifact.
 *
 * There is no way to repair this from inside the same invocation; by the time any task could run, the
 * stale jar has already been resolved and remapped. So this stops rather than warns, and names the
 * command that fixes it.
 *
 * It is wired as a task rather than run at configuration time, even though nothing it inspects
 * changes between the two. Stonecutter configures every node on every invocation, so a throw during
 * configuration aborts the build whichever node was asked for — one stale 1.20.1 jar would block a
 * 1.21.1 build that is perfectly fine. As a task it stops exactly the node that asked for it.
 *
 * It is a task and not a `doFirst` on `compileJava` for a reason that was measured, not guessed. An
 * edit to Golem Core alone changes none of this project's inputs, so `compileJava` is UP-TO-DATE and
 * a `doFirst` never runs — silently skipping the check in precisely the case it exists for, where
 * nothing recompiles and the stale jar goes on to `jar` and the run tasks unnoticed. A task with no
 * declared outputs is never up to date, and a dependency runs even when its dependent does not.
 */
fun checkGolemJarIsFresh() {
    if (!obfuscated) return

    val golemRoot = rootProject.file("../Golem-Core")
    val jar = golemRoot.resolve("versions/$mcVersion-fabric/build/libs")
        .listFiles { file -> file.name.endsWith(".jar") && !file.name.endsWith("-sources.jar") }
        ?.maxByOrNull { it.lastModified() }
        // No jar means Gradle must build one, which is the path that works. Only a present-but-old
        // jar is the trap.
        ?: return

    val newestSource = golemRoot.resolve("src").walkTopDown()
        .filter { it.isFile || it.isDirectory }
        .maxOfOrNull { it.lastModified() } ?: return

    if (newestSource > jar.lastModified()) {
        throw GradleException(
            "Golem Core's sources are newer than ${jar.name}, and Loom has already remapped that jar. " +
                "This node would compile against a stale Golem Core and fail on symbols that exist. " +
                "Build Golem Core first:  (cd ../Golem-Core && sh gradlew :$mcVersion-fabric:build)\n" +
                "To launch the client, ./client does both in the right order."
        )
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:${prop("deps.minecraft")}")
    if (obfuscated) "mappings"(loom.officialMojangMappings())

    modCfg("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modCfg("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    // Golem Core supplies the cross-version API facades, resolved from the composite build.
    modCfg("dev.golem:golemcore-fabric-$mcVersion:${prop("deps.golemcore")}+$mcVersion")

    // JEI is per-node optional; see the neoforge buildscript for the rationale.
    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        "compileOnly"("mezz.jei:jei-$jei-common-api:$jeiVer")
        "compileOnly"("mezz.jei:jei-$jei-fabric-api:$jeiVer")
        // JEI's transitive Fabric Loader is excluded: Loom sets Mixin up from the node's declared loader, and a second
        // loader on the dev classpath fails at launch (`IAdviceProvider` not found). The node's loader (0.19.5) already
        // meets JEI 29.43's 0.19.4 floor.
        devRuntime("mezz.jei:jei-$jei-fabric:$jeiVer") { exclude(group = "net.fabricmc", module = "fabric-loader") }
    }

    // REI (Roughly Enough Items) is an optional peer like JEI: HBM's recipe-viewer declarations
    // (com.hbm.handler.jei.HbmRecipeViewer) feed it through Golem Core's dev.golem.core.rei, from the thin plugins in
    // com.hbm.handler.rei (the `rei_client` / `rei_common` entrypoints; guarded by the `rei` constant). Compiled
    // against on every node (intermediary jars below 26.1), never shipped or required. `-Prei.run=true` puts REI
    // (with Cloth Config and Architectury) in the client run, beside JEI; off by default so the recipe baselines stay
    // REI-free. Its POM's Fabric Loader / Fabric API are left out, as JEI's are: the node declares its own.
    propOrNull("deps.rei")?.let { rei ->
        (if (obfuscated) "modCompileOnly" else "compileOnly")("me.shedaniel:RoughlyEnoughItems-api-fabric:$rei") {
            exclude(group = "net.fabricmc")
            exclude(group = "net.fabricmc.fabric-api")
        }
        // REI 26's Fabric POM lists Cloth Config and Architectury at runtime scope; REI's API types name theirs
        // (Point, Rectangle, FluidStack), so the compile classpath needs them as well.
        for (dep in listOfNotNull(
            (findProperty("deps.rei_cloth") as String?)?.takeIf { it.isNotBlank() }?.let { "me.shedaniel.cloth:cloth-config-fabric:$it" },
            (findProperty("deps.rei_architectury") as String?)?.takeIf { it.isNotBlank() }?.let { "dev.architectury:architectury-fabric:$it" },
        )) (if (obfuscated) "modCompileOnly" else "compileOnly")(dep) {
            exclude(group = "net.fabricmc")
            exclude(group = "net.fabricmc.fabric-api")
        }
        if (propOrNull("rei.run") == "true") devRuntime("me.shedaniel:RoughlyEnoughItems-fabric:$rei") {
            exclude(group = "net.fabricmc")
            exclude(group = "net.fabricmc.fabric-api")
        }
    }

    // Published Flywheel supplies the older nodes. 26.1 Fabric has no published build and starts Golem
    // Core's vendored engine instead (switched off with config/golemcore-flywheel.json or
    // `/flywheel backend off`); every machine keeps its vanilla BER on every node either way.
    // `-Pflywheel.run=false` leaves the engine jar out of the dev run (compile stays), so the dev client
    // can be launched as a player without Flywheel would see it: every machine through its vanilla
    // BER. The instanced visuals are optional; this is how that claim is tested on these nodes.
    propOrNull("deps.flywheel")?.let { fw ->
        val cfg = if (obfuscated) "modCompileOnly" else "compileOnly"
        cfg("dev.engine-room.flywheel:flywheel-fabric-api-$mcVersion:$fw")
        if (propOrNull("flywheel.run") != "false") devRuntime("dev.engine-room.flywheel:flywheel-fabric-$mcVersion:$fw")
    }

    // Sodium declares `environment: client`, so Fabric loads it only during the client run.
    // Jade, dev runs only (`-Pjade.run=true`): HBM's overlay declarations (com.hbm.compat.HbmBlockInfo) reach it
    // through Golem Core's dev.golem.core.jade, so HBM has no Jade dependency.
    if (propOrNull("jade.run") == "true") propOrNull("deps.jade")?.let { devRuntime("maven.modrinth:jade:$it") }
    propOrNull("deps.sodium_fabric")?.let { devRuntime("maven.modrinth:sodium:$it") }
    // Iris (Oculus on Forge 1.20.1) for testing under shader packs: `-Pshaders.run=true` puts it in the client run.
    // Client-side, dev runtime only, never compiled against or published; off by default so the standard runs
    // and baselines keep vanilla's pipeline. Place a pack in the run's shaderpacks folder and select it in game.
    if (propOrNull("shaders.run") == "true") propOrNull("deps.iris")?.let { iris ->
        devRuntime("maven.modrinth:iris:$iris")
        // A dev run is not searched for nested jars (see runJarsWithNested), so Iris's GLSL libraries (antlr4-runtime,
        // glsl-transformer, jcpp) are unpacked from its jar onto the runtime classpath; its nested Fabric API modules
        // are already in the run. Resolved lazily, when the run classpath is.
        val irisJar = configurations.detachedConfiguration(dependencies.create("maven.modrinth:iris:$iris")).apply {
            isTransitive = false
        }
        "runtimeOnly"(files(provider { nestedLibraries(irisJar.singleFile, "iris-nested/$iris") }))
    }
    if (hasSable && obfuscated) {
        "modCompileOnly"("dev.ryanhcode.sable:sable-fabric-$sableMc:$sableVer") { isTransitive = false }
        "modCompileOnly"("dev.ryanhcode.sable-companion:sable-companion-fabric-$sableMc:$sableCompanionVer") { isTransitive = false }
        if (propOrNull("sable.run") == "true") {
            val (mods, libraries) = sableRunJars(sableMc!!, sableVer!!)
            devRuntime(files(mods))
            "runtimeOnly"(files(libraries))
        }
    }
    // 1.20.1: Sodium 0.5 skips FabricBakedModel.emitBlockQuads (connected textures) unless Indium is loaded.
    propOrNull("deps.indium")?.let { devRuntime("maven.modrinth:indium:$it") }
    // Team Reborn Energy is an optional peer of Core's energy facades; the dev game loads it so the RF
    // converters' Fabric exposure can be exercised (HBM never compiles against it). `-Ptr_energy.run=false` leaves it
    // out, as most players run: a game without it once crashed at init (Core's RebornEnergy, History §19eq).
    if (propOrNull("tr_energy.run") != "false") propOrNull("deps.tr_energy")?.let { devRuntime("teamreborn:energy:$it") }

    // KubeJS is an optional peer served entirely by Golem Core's dev.golem.core.kubejs adapter (HBM has no KubeJS code).
    // Never compiled against, shipped or required. `-Pkubejs.run=true` puts KubeJS 6, Rhino and Architectury in the dev
    // runs (intermediary jars below 26.1) and copies harness/kubejs into each run's kubejs folder; off by default so
    // the baselines stay KubeJS-free. The node declares its own Fabric Loader / Fabric API, as for JEI.
    propOrNull("deps.kubejs")?.let { kubejs ->
        if (propOrNull("kubejs.run") == "true") {
            for (mod in listOfNotNull(
                "dev.latvian.mods:kubejs-fabric:$kubejs",
                "dev.latvian.mods:rhino-fabric:${prop("deps.kubejs_rhino")}",
                propOrNull("deps.kubejs_architectury")?.let { "dev.architectury:architectury-fabric:$it" },
            )) devRuntime(mod) { isTransitive = false }
        }
    }

    // CC: Tweaked is an optional peer: HBM's 1.7 OpenComputers components are served to it as peripherals
    // (com.hbm.compat.computercraft, guarded by the `computercraft` constant). Compiled against on every node,
    // never shipped or required. `-Pcomputercraft.run=true` puts the mod in the dev runs as well; it is off by
    // default so the recipe baselines stay CC-free.
    val ccMc = propOrNull("deps.cc_mc")
    val ccVer = propOrNull("deps.cc")
    if (ccMc != null && ccVer != null) {
        // HBM's peripherals need the peripheral API alone (core-api, no Minecraft types, so not remapped);
        // registration is Core's.
        "compileOnly"("cc.tweaked:cc-tweaked-$ccMc-core-api:$ccVer") { isTransitive = false }
        if (propOrNull("computercraft.run") == "true") {
            if (obfuscated) {
                devRuntime(files(loomCompatibleComputerCraft(ccMc, ccVer)))
                "runtimeOnly"(computerCraftLibraries())
            } else devRuntime("cc.tweaked:cc-tweaked-$ccMc-fabric:$ccVer") { isTransitive = false }
        }
    }
}

loom {
    // 1.20.1 vanilla hides AbstractMinecart.getDropItem from any subclass outside its package; every other
    // node either patches it (Forge) or moved it (1.21+), so the widening is stated per era, not globally.
    val awName = if (obfuscated && mcVersion.startsWith("1.20")) "$modId.legacy.accesswidener"
    else "$modId.accesswidener"
    val aw = rootProject.file("src/main/resources/$awName")
    if (aw.exists()) accessWidenerPath = aw

    runs {
        named("client") {
            client()
            configName = "Fabric Client ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/client"
            // See the Forge buildscript for why: JEI only builds its categories on world join, so
            // `-PquickPlayWorld=<save name>` is what turns runClient into a real check of the plugin.
            propOrNull("quickPlayWorld")?.let { programArgs("--quickPlaySingleplayer", it) }
        }
        named("server") {
            server()
            configName = "Fabric Server ($mcVersion)"
            ideConfigGenerated(true)
            runDir = "runs/server"
        }
        // Headless GameTest runner: `gradlew :<node>-fabric:runGametest` executes every hbm:* test through
        // Fabric API's runner and the fabric-gametest entrypoint (see FabricGameTests).
        register("gametest") {
            server()
            configName = "Fabric GameTest ($mcVersion)"
            ideConfigGenerated(false)
            runDir = "runs/gametest"
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${project.file("runs/gametest/report.xml").absolutePath}")
            // -Pgametest.filter=<regex> runs only the matching test ids (see Golem's GameTests.addLegacy).
            vmArg("-Dgolem.gametest.filter=${providers.gradleProperty("gametest.filter").orElse("").get()}")
        }
    }
}

// See the Forge buildscript: the legacy GameTest server reuses old region files, so the world is wiped.
tasks.matching { it.name == "runGametest" }.configureEach {
    doFirst { delete(layout.projectDirectory.dir("runs/gametest/world")) }
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
        "mixin_compatibility" to if (javaVersion <= 17) "JAVA_17" else "JAVA_21",
        "pack_format" to prop("pack_format"),
        "pack_format_entry" to packFormatEntry,
        // MixinSpecialModelRenderers targets a class that only exists on 26.1, and is compiled out
        // below it. A mixin config may not name a class that is absent from the jar — Mixin fails the
        // whole config at PREPARE — so the entry is filtered here in step with the Stonecutter guard.
        "fabric_client_mixins" to if (modernRendering) "\"MixinSpecialModelRenderers\", \"MixinItemModels\"" else "",
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "pack.mcmeta", "*.mixins.json")) { expand(props) }
    // Only Forge generates a refmap; naming an absent one leaves the jar pointing at a file it lacks.
    filesMatching("*.mixins.json") { filter { if (it.trimStart().startsWith("\"refmap\"")) null else it } }
    // The Sable integration's entry points, only on nodes that declare Sable. Appended by line rather than templated,
    // so the source fabric.mod.json stays valid JSON (Loom parses it at configuration).
    if (hasSable) {
        val sableLines = mapOf(
            "\"com.hbm.platform.fabric.FabricInit\"" to "\"com.hbm.compat.sable.HbmSableFabric\"",
            "\"com.hbm.platform.fabric.FabricGameTests\"" to "\"com.hbm.compat.sable.HbmSableFabricGameTests\"",
        )
        filesMatching("fabric.mod.json") {
            filter { line -> sableLines[line.trim()]?.let { "$line, $it" } ?: line }
        }
    }
    // The happy-ghast Fat Man's entry points, only on nodes that set feature.happy_ghast. Both go BEFORE HBM's: Fabric
    // registers everything in FabricInit's one flush, so the entity type and GameTest bodies must exist by then, and
    // FabricClientInit binds and audits every hbm entity renderer at once, so the renderer must be handed over first.
    if (hasHappyGhast) {
        val happyGhastLines = mapOf(
            "\"com.hbm.platform.fabric.FabricInit\"" to { it: String -> "\"com.hbm.compat.happyghast.HbmHappyGhastFabric\", \"com.hbm.compat.happyghast.HappyGhastGameTestsFabric\", $it" },
            "\"com.hbm.platform.fabric.FabricClientInit\"" to { it: String -> "\"com.hbm.compat.happyghast.HbmHappyGhastFabricClient\", $it" },
        )
        filesMatching("fabric.mod.json") {
            filter { line ->
                val trimmed = line.trim()
                happyGhastLines[trimmed]?.let { line.replace(trimmed, it(trimmed)) } ?: line
            }
        }
    }
    // Forge-like biome modifiers are loader-specific; Fabric injects the same feature list in code.
    // `data/neoforge/**` is the NeoForge-namespaced data maps (furnace fuels), which Fabric supplies
    // through FuelRegistry/FuelValueEvents instead — excluded so the jar carries no inert files.
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg",
        "data/$modId/neoforge/**", "data/$modId/forge/**", "data/neoforge/**")
}

tasks.jar {
    from(files(rootProject.file("LICENSE"), rootProject.file("LICENSE.LESSER"), rootProject.file("CREDITS.txt"))) { rename { "${it}_${prop("mod.id")}" } }
}

// Stonecutter rewrites the shared src/ tree in place for the active node. Both compilation and
// resource processing must wait for that rewrite, or they consume the previous node's source state.
val checkGolemJarFreshness = tasks.register("checkGolemJarFreshness") {
    doLast { checkGolemJarIsFresh() }
}

tasks.compileJava {
    dependsOn(tasks.named("stonecutterGenerate"), checkGolemJarFreshness)
    options.release = javaVersion
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "2000"))
}
tasks.processResources { dependsOn(tasks.named("stonecutterGenerate")) }

// Recipe ingredients are written in the 26.1 shape and converted downward for older nodes.
extra["mc_version"] = mcVersion
extra["java_version"] = javaVersion.toString()
// Golem Core's shared build layer; HBM's parameters for it (golem.build.*) are in gradle.properties.
apply(from = rootProject.file("../Golem-Core/gradle/mod/run-toolchain.gradle.kts"))
// Visual test harness launch capture (shared by every Golem Core mod); see harness.json and ../Golem-Core/harness/README.md.
apply(from = rootProject.file("../Golem-Core/gradle/harness-spec.gradle.kts"))
apply(from = rootProject.file("../Golem-Core/gradle/mod/legacy-recipes.gradle.kts"))
extra["loader"] = "fabric"
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
apply(from = rootProject.file("gradle/release-jar.gradle.kts"))
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

// The id constants recipe declarations are written against. Generated from the live registry by
// `runGenerateIds` and committed, because the plain-JVM build step the declarations run in cannot
// load half the registry classes; see com.hbm.lib.IdConstants.
sourceSets["main"].java.srcDir(rootProject.file("src/generated/java"))

// KubeJS dev scripts (`-Pkubejs.run=true`): harness/kubejs (startup_scripts, server_scripts, client_scripts) is copied
// into each run's kubejs folder (runs/harness too: the harness clients' game folder). A copy, not a sync: KubeJS writes its own config and exports beside the scripts.
if (propOrNull("kubejs.run") == "true" && propOrNull("deps.kubejs") != null) {
    for ((run, tasksOfRun) in listOf("client" to listOf("runClient"), "harness" to listOf("runClient"), "server" to listOf("runServer"), "gametest" to listOf("runGametest"))) {
        val copy = tasks.register<Copy>("copyKubeJsScripts${run.replaceFirstChar { it.uppercase() }}") {
            description = "Copies harness/kubejs into the $run run's kubejs folder (-Pkubejs.run)."
            from(rootProject.file("harness/kubejs")) { exclude("README.md") }
            into(layout.projectDirectory.dir("runs/$run/kubejs"))
        }
        tasks.matching { it.name in tasksOfRun }.configureEach { dependsOn(copy) }
    }
}
