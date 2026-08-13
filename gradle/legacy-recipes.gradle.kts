import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Rewrites crafting-recipe ingredients into the pre-1.21.2 JSON shape.
 *
 * Vanilla's `Ingredient` codec changed shape, not meaning. From 1.21.2 an ingredient is written as a
 * bare string — `"hbm:ingot_lead"` for an item, `"#c:ingots/lead"` for a tag — while before that it
 * had to be an object: `{"item": "hbm:ingot_lead"}` or `{"tag": "c:ingots/lead"}`. Nothing else about
 * these recipes differs, so the port keeps ONE set of recipe files, written in the 26.1 shape that
 * `gen_recipes` emits, and converts them on the way into older nodes' resources.
 *
 * Applied only where it is needed; see [legacyRecipeIngredients] below for the gate. The conversion
 * runs over `processResources`' OUTPUT rather than the source tree, so `src/` is never touched and
 * the transform cannot leak into another node.
 *
 * Only three positions can hold an ingredient in the recipe types this mod generates
 * (crafting_shaped, crafting_shapeless, smelting):
 *   - `key` — one ingredient per pattern symbol
 *   - `ingredients` — a list, for shapeless
 *   - `ingredient` — a single one, for smelting
 * An unrecognised shape (already an object, or a list of alternatives) is passed through untouched,
 * so hand-written recipes that already use the old form still work.
 */
fun Project.applyLegacyRecipeIngredients() {
    tasks.named<ProcessResources>("processResources").configure {
        doLast {
            val root = destinationDir
            var converted = 0
            root.walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.parentFile.name == "recipe" }
                .forEach { file ->
                    @Suppress("UNCHECKED_CAST")
                    val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
                    var changed = false

                    (json["key"] as? MutableMap<String, Any?>)?.let { key ->
                        key.keys.toList().forEach { symbol ->
                            val old = key[symbol]
                            val new = toLegacyIngredient(old)
                            if (new !== old) { key[symbol] = new; changed = true }
                        }
                    }
                    (json["ingredients"] as? List<Any?>)?.let { list ->
                        val mapped = list.map { toLegacyIngredient(it) }
                        if (mapped != list) { json["ingredients"] = mapped; changed = true }
                    }
                    json["ingredient"]?.let { old ->
                        val new = toLegacyIngredient(old)
                        if (new !== old) { json["ingredient"] = new; changed = true }
                    }

                    if (changed) {
                        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
                        converted++
                    }
                }
            logger.lifecycle("legacyRecipeIngredients: converted $converted recipe(s) to the pre-1.21.2 ingredient shape.")
        }
    }
}

/**
 * One ingredient, 26.1 shape to legacy shape. Strings become objects; a leading `#` marks a tag.
 * Anything that is not a string is already in a shape the old codec understands, and is returned as-is
 * (identity preserved, so callers can detect "nothing to do").
 */
fun toLegacyIngredient(value: Any?): Any? = when {
    value is String && value.startsWith("#") -> mapOf("tag" to value.substring(1))
    value is String -> mapOf("item" to value)
    value is List<*> -> value.map { toLegacyIngredient(it) }
    else -> value
}

/**
 * Whether this Minecraft version predates the bare-string ingredient form, i.e. anything below
 * 1.21.2. Kept here rather than in the buildscripts so both loaders share one answer.
 */
fun needsLegacyIngredients(mcVersion: String): Boolean {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return when {
        major >= 26 -> false          // 26.1 and up: the new shape is native
        minor > 21 -> false
        minor < 21 -> true            // 1.20.x
        else -> patch < 2             // 1.21 and 1.21.1, but not 1.21.2+
    }
}

// The applying buildscript passes its node's Minecraft version; it is the only place that knows it.
if (needsLegacyIngredients(extra.properties["mc_version"] as String)) {
    applyLegacyRecipeIngredients()
}
