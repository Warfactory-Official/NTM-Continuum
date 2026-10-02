import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/** Converts the 26.1 biome JSON shape to the pre-26.1 codec shape. */
fun Project.applyLegacyBiomeCompatibility(mcVersion: String) {
    if (mcVersion.substringBefore('.').toIntOrNull() ?: 0 >= 26) return

    tasks.named<ProcessResources>("processResources").configure {
        doLast {
            var converted = 0
            destinationDir.walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.path.contains("${File.separator}worldgen${File.separator}biome${File.separator}") }
                .forEach { file ->
                    @Suppress("UNCHECKED_CAST")
                    val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
                    @Suppress("UNCHECKED_CAST")
                    val effects = json["effects"] as? MutableMap<String, Any?> ?: return@forEach
                    @Suppress("UNCHECKED_CAST")
                    val attributes = json.remove("attributes") as? Map<String, Any?>

                    attributes?.get("minecraft:visual/fog_color")?.let { effects["fog_color"] = biomeColor(it) }
                    attributes?.get("minecraft:visual/sky_color")?.let { effects["sky_color"] = biomeColor(it) }
                    attributes?.get("minecraft:visual/water_fog_color")?.let { effects["water_fog_color"] = biomeColor(it) }
                    effects.keys.toList().forEach { key ->
                        if (key == "water_color" || key == "water_fog_color" || key == "grass_color" || key == "foliage_color") {
                            effects[key] = biomeColor(effects[key])
                        }
                    }
                    if (json["carvers"] is List<*>) json["carvers"] = emptyMap<String, Any?>()
                    file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
                    converted++
                }
            logger.lifecycle("legacyBiomeCompatibility: converted $converted biome(s)")
        }
    }
}

fun biomeColor(value: Any?): Any? = if (value is String && value.startsWith("#")) {
    Integer.parseInt(value.substring(1), 16)
} else value

/**
 * The 1.20.1 spellings of the 26.1 convention biome tags the mod's biome tags reference (`hbm:biome_dictionary/<type>`).
 * (Never write slash-star inside a Kotlin comment: block comments nest, so it silently comments out the rest of
 * the file — this one did, and the biome conversion below stopped running on every pre-26.1 node.)
 *
 * `c:is_*` is the convention on NeoForge and on Fabric's second convention-tag module (1.21+). Forge 1.20.1 spells
 * its own `forge:is_*` (and leaves ocean, river, beach, badlands, savanna and overworld to vanilla's tags, which the
 * files already list); Fabric 1.20.1's first module spells them without `is_` and a few by other names. A name with no
 * counterpart maps to nothing. The `c:is_*` reference itself stays (optional): cross-loader mods ship it on 1.20.1 too.
 */
val FORGE_1_20_BIOME_TAGS = mapOf(
    "c:is_sandy" to "forge:is_sandy", "c:is_desert" to "forge:is_desert", "c:is_plains" to "forge:is_plains",
    "c:is_swamp" to "forge:is_swamp", "c:is_sparse_vegetation" to "forge:is_sparse",
)
val FABRIC_1_20_BIOME_TAGS = mapOf(
    "c:is_ocean" to "c:ocean", "c:is_river" to "c:river", "c:is_beach" to "c:beach", "c:is_stony_shores" to "c:stony_shores",
    "c:is_plains" to "c:plains", "c:is_savanna" to "c:savanna", "c:is_desert" to "c:desert", "c:is_badlands" to "c:badlands",
    "c:is_swamp" to "c:swamp", "c:is_sparse_vegetation" to "c:vegetation_sparse", "c:is_overworld" to "c:in_overworld",
)

/** Adds the 1.20.1 spelling next to every 26.1 convention reference in the mod's biome tags; returns the files changed. */
fun addLegacyConventionBiomeTags(root: File, spellings: Map<String, String>): Int {
    var changed = 0
    root.walkTopDown()
        .filter { it.isFile && it.extension == "json" && it.invariantSeparatorsPath.contains("/tags/worldgen/biome/") }
        .forEach { file ->
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
            val values = (json["values"] as? List<*>)?.toMutableList() ?: return@forEach
            val present = values.map { if (it is Map<*, *>) it["id"] else it }.toMutableSet()
            val added = values.mapNotNull { entry ->
                val id = (if (entry is Map<*, *>) entry["id"] else entry) as? String ?: return@mapNotNull null
                if (!id.startsWith("#")) return@mapNotNull null
                val legacy = spellings[id.substring(1)] ?: return@mapNotNull null
                if (!present.add("#$legacy")) return@mapNotNull null
                mapOf("id" to "#$legacy", "required" to false)
            }
            if (added.isEmpty()) return@forEach
            values.addAll(added)
            json["values"] = values
            file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
            changed++
        }
    return changed
}

fun Project.applyLegacyConventionBiomeTags(mcVersion: String) {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    if (parts.getOrElse(0) { 0 } >= 26 || parts.getOrElse(1) { 0 } >= 21) return
    val spellings = if (extra.properties["convention_namespace"] == "forge") FORGE_1_20_BIOME_TAGS else FABRIC_1_20_BIOME_TAGS
    tasks.named<ProcessResources>("processResources").configure {
        doLast {
            val changed = addLegacyConventionBiomeTags(destinationDir, spellings)
            logger.lifecycle("legacyConventionBiomeTags: added 1.20.1 spellings to $changed biome tag(s)")
        }
    }
}

applyLegacyBiomeCompatibility(extra.properties["mc_version"] as String)
applyLegacyConventionBiomeTags(extra.properties["mc_version"] as String)
