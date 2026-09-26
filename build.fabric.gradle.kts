plugins {
    id("dev.golem.mod")
}

fun propOrNull(name: String): String? = (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val mcVersion = stonecutter.current.version
val obfuscated = mcVersion.substringBefore('.').toInt() < 26
val devRuntime = if (obfuscated) "modLocalRuntime" else "localRuntime"

sourceSets["main"].java {
    exclude("com/hbm/platform/neoforge/**", "com/hbm/platform/forge/**")
    srcDir(rootProject.file("src/generated/java"))
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

golem {
    fabricEntrypoint("main", "com.hbm.platform.fabric.FabricInit")
    fabricEntrypoint("client", "com.hbm.platform.fabric.FabricClientInit")
    fabricEntrypoint("jei_mod_plugin", "com.hbm.handler.jei.HbmJeiPluginFabric")
    // MixinSpecialModelRenderers/MixinItemModels target 26.1-only classes; a config naming an absent class fails at PREPARE.
    resourceProperties.put("fabric_client_mixins", if (obfuscated) "" else "\"MixinSpecialModelRenderers\", \"MixinItemModels\"")
}

dependencies {
    val jei = propOrNull("deps.jei_mc")
    val jeiVer = propOrNull("deps.jei")
    if (jei != null && jeiVer != null) {
        "compileOnly"("mezz.jei:jei-$jei-common-api:$jeiVer")
        "compileOnly"("mezz.jei:jei-$jei-fabric-api:$jeiVer")
        devRuntime("mezz.jei:jei-$jei-fabric:$jeiVer")
    }
    propOrNull("deps.flywheel")?.let { fw ->
        (if (obfuscated) "modCompileOnly" else "compileOnly")("dev.engine-room.flywheel:flywheel-fabric-api-$mcVersion:$fw")
        devRuntime("dev.engine-room.flywheel:flywheel-fabric-$mcVersion:$fw")
    }
    // Sodium declares `environment: client`: loaded only by the client run.
    propOrNull("deps.sodium_fabric")?.let { devRuntime("maven.modrinth:sodium:$it") }
}

apply(from = rootProject.file("gradle/gun-smoke-test.gradle.kts"))
apply(from = rootProject.file("gradle/legacy-assets.gradle.kts"))
apply(from = rootProject.file("gradle/dejson.gradle.kts"))
