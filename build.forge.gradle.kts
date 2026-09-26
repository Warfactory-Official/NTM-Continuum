plugins {
    id("dev.golem.mod")
}

fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val modId = property("mod.id") as String

sourceSets["main"].java {
    exclude("com/hbm/platform/fabric/**", "com/hbm/platform/neoforge/**")
    srcDir(rootProject.file("src/generated/java"))
}
sourceSets["main"].resources.srcDir("src/generated/resources")

golem.loaderEntry = "com.hbm.platform.forge.ForgeInit"

legacyForge {
    runs.register("data") {
        data()
        gameDirectory = file("runs/data")
        programArguments.addAll(
            "--mod", modId, "--all",
            "--output", file("src/generated/resources").absolutePath,
            "--existing", rootProject.file("src/main/resources").absolutePath,
        )
    }
}

repositories {
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
    // Sodium has no Forge build; Embeddium is its Forge port. Dev client only.
    propOrNull("deps.embeddium")?.let { modRuntimeOnly("maven.modrinth:embeddium:$it") }
    // Real third-party energy peer for the foreign-bridge GameTests; reached reflectively.
    propOrNull("deps.pipez")?.let { modRuntimeOnly("maven.modrinth:pipez:$it") }
}

apply(from = rootProject.file("gradle/gun-smoke-test.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))
apply(from = rootProject.file("gradle/dejson.gradle.kts"))
