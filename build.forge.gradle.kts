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
    }
    mods {
        register(modId) { sourceSet(sourceSets["main"]) }
    }
}

sourceSets["main"].resources.srcDir("src/generated/resources")

// A one-off smoke launch must not leave Quick Play in ModDev's cached argument file.
tasks.matching { it.name == "prepareClientRun" }.configureEach {
    inputs.property("hbmQuickPlayWorld", providers.gradleProperty("quickPlayWorld").orElse(""))
}

repositories {
    mavenCentral()
    maven("https://maven.blamejared.com") { name = "BlameJared" }
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
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        modCompileOnly("mezz.jei:jei-$jei-common-api:$jeiVer")
        modCompileOnly("mezz.jei:jei-$jei-forge-api:$jeiVer")
        modRuntimeOnly("mezz.jei:jei-$jei-forge:$jeiVer")
    }

    propOrNull("deps.flywheel")?.let { fw ->
        modCompileOnly("dev.engine-room.flywheel:flywheel-forge-api-$mcVersion:$fw")
        modRuntimeOnly("dev.engine-room.flywheel:flywheel-forge-$mcVersion:$fw")
    }

    // Embeddium is the Forge port of Sodium — Sodium itself ships no Forge build, so this is the
    // equivalent dev renderer for this node. Client-side only, dev runtime only, never published.
    propOrNull("deps.embeddium")?.let { modRuntimeOnly("maven.modrinth:embeddium:$it") }
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
    from(rootProject.file("LICENSE")) { rename { "${it}_${prop("mod.name")}" } }
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
apply(from = rootProject.file("gradle/legacy-recipes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-biomes.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))
apply(from = rootProject.file("gradle/dejson.gradle.kts"))
apply(from = rootProject.file("gradle/mixin-validation.gradle.kts"))

dependencies { compileOnly("org.jspecify:jspecify:1.0.0") }
