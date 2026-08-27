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

applyLegacyBiomeCompatibility(extra.properties["mc_version"] as String)
