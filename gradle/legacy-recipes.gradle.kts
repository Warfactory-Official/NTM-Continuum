import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Rewrites crafting recipes into the pre-26.1 JSON shape, and restores the 1.20.x data-pack directory
 * layout where necessary.
 *
 * Vanilla's `Ingredient` codec changed shape, not meaning. In 26.1 an ingredient is written as a bare
 * string — `"hbm:ingot_lead"` for an item, `"#c:ingots/lead"` for a tag — while before that it
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
fun Project.applyLegacyRecipeCompatibility(mcVersion: String, conventionNamespace: String) {
    tasks.named<ProcessResources>("processResources").configure {
        doLast {
            val root = destinationDir
            val renamed = if (needsLegacyDataDirectories(mcVersion)) renameLegacyDataDirectories(root) else 0
            val lootTables = if (needsLegacyItemStacks(mcVersion)) convertLegacyLootTables(root) else 0
            val advancements = if (needsLegacyItemStacks(mcVersion)) convertLegacyAdvancements(root) else 0
            // Before the ingredient rewrite, while tag references are still bare "#c:..." strings.
            val tags = if (conventionNamespace != "c") duplicateConventionTags(root, conventionNamespace) else 0
            val repointed = if (conventionNamespace != "c") repointConventionTagReferences(root, conventionNamespace) else 0
            var converted = 0
            root.walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.parentFile.name in setOf("recipe", "recipes") }
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
                    json["copy_from"]?.let { old ->
                        val new = toLegacyIngredient(old)
                        if (new !== old) { json["copy_from"] = new; changed = true }
                    }
                    (json["result"] as? MutableMap<String, Any?>)?.let { result ->
                        if (needsLegacyItemStacks(mcVersion)) {
                            @Suppress("UNCHECKED_CAST")
                            val components = result.remove("components") as? Map<String, Any?>
                            if (components != null) {
                                @Suppress("UNCHECKED_CAST")
                                val tag = (result["tag"] as? MutableMap<String, Any?>) ?: mutableMapOf()
                                components.forEach { (key, value) -> tag[key] = value }
                                result["tag"] = tag
                                changed = true
                            }
                        }
                        val id = result["id"] as? String
                        if (needsLegacyRecipeResults(mcVersion) && json["type"] in legacyCookingRecipeTypes && id != null) {
                            // Cooking recipes use a bare result id on 1.20.x.
                            json["result"] = id
                            changed = true
                        } else if (needsLegacyRecipeResults(mcVersion) && id != null && "item" !in result) {
                            // Crafting recipe results use `item` on 1.20.x.
                            result.remove("id")
                            result["item"] = id
                            changed = true
                        }
                    }

                    if (changed) {
                        file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
                        converted++
                    }
                }
            logger.lifecycle("legacyRecipeCompatibility: renamed $renamed data directory(s); converted " +
                    "$converted recipe(s), $lootTables loot table(s), and $advancements advancement(s) to the legacy shape; " +
                    "copied $tags convention tag(s) to '$conventionNamespace' and repointed $repointed recipe(s) at them.")
        }
    }
}

/**
 * Converts data-component-era loot tables to their pre-1.20.5 shape: crate drops to the equivalent
 * legacy BlockEntityTag payload, and `match_tool` predicates back to the flat `ItemPredicate`.
 *
 * The predicate half matters more than it looks. A `match_tool` predicate is an `ItemPredicate`, and
 * `ItemPredicate` became component-based in the same release that gave items components — so a
 * silk-touch check is `{"predicates": {"minecraft:enchantments": [{"enchantments": …}]}}` from
 * 1.20.5 on and `{"enchantments": [{"enchantment": …}]}` below it. The old parser reads its own keys
 * and ignores the rest, so the modern spelling does not fail on 1.20.1: it parses as an
 * `ItemPredicate` with no constraints, which matches every tool and every empty hand. Every
 * silk-touch ore then always takes the silk branch and drops the ore block instead of its mineral,
 * silently, on that node alone.
 */
fun convertLegacyLootTables(root: File): Int {
    var converted = 0
    root.walkTopDown()
        .filter { it.isFile && it.extension == "json" && it.path.contains("${File.separator}loot_tables${File.separator}") }
        .forEach { file ->
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
            var changed = convertLegacyToolPredicates(json)
            (json["pools"] as? List<*>)?.forEach { poolValue ->
                val pool = poolValue as? MutableMap<*, *> ?: return@forEach
                (pool["entries"] as? List<*>)?.forEach { entryValue ->
                    @Suppress("UNCHECKED_CAST")
                    val entry = entryValue as? MutableMap<String, Any?> ?: return@forEach
                    val functions = entry["functions"] as? List<*> ?: return@forEach
                    val legacy = mutableListOf<Any?>()
                    var entryChanged = false
                    functions.forEach { functionValue ->
                        @Suppress("UNCHECKED_CAST")
                        val function = functionValue as? Map<String, Any?>
                        if (function?.get("function") == "minecraft:copy_components"
                                && function["source"] == "block_entity") {
                            val include = function["include"] as? List<*>
                            if (include?.contains("minecraft:custom_name") == true) {
                                legacy += mapOf("function" to "minecraft:copy_name", "source" to "block_entity")
                            }
                            if (include?.contains("minecraft:container") == true) {
                                legacy += mapOf(
                                    "function" to "minecraft:copy_nbt",
                                    "source" to "block_entity",
                                    "ops" to listOf(mapOf(
                                        "source" to "inventory.Items",
                                        "target" to "BlockEntityTag.inventory.Items",
                                        "op" to "replace",
                                    )),
                                )
                            }
                            entryChanged = true
                        } else {
                            legacy += functionValue
                        }
                    }
                    if (entryChanged) {
                        entry["functions"] = legacy
                        changed = true
                    }
                }
            }
            if (changed) {
                file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
                converted++
            }
        }
    return converted
}

/**
 * Rewrites every `minecraft:match_tool` predicate in a loot table, wherever it sits. Conditions
 * appear at pool level, at entry level, and inside an `alternatives` entry's children, so this walks
 * the whole tree rather than the three places these tables happen to use today.
 */
fun convertLegacyToolPredicates(node: Any?): Boolean {
    var changed = false
    when (node) {
        is MutableList<*> -> node.forEach { if (convertLegacyToolPredicates(it)) changed = true }
        is MutableMap<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = node as MutableMap<String, Any?>
            map.values.toList().forEach { if (convertLegacyToolPredicates(it)) changed = true }
            if (map["condition"] == "minecraft:match_tool") {
                @Suppress("UNCHECKED_CAST")
                val predicate = map["predicate"] as? MutableMap<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val predicates = predicate?.remove("predicates") as? Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val enchantments = predicates?.get("minecraft:enchantments") as? List<Map<String, Any?>>
                if (enchantments != null) {
                    predicate["enchantments"] = enchantments.map { entry ->
                        entry.entries.associate { (key, value) ->
                            (if (key == "enchantments") "enchantment" else key) to value
                        }
                    }
                    changed = true
                }
            }
        }
    }
    return changed
}

/** Advancement icons used an `item` key before item-stack codecs adopted `id`. */
fun convertLegacyAdvancements(root: File): Int {
    var converted = 0
    root.walkTopDown()
        .filter { it.isFile && it.extension == "json" && it.path.contains("${File.separator}advancements${File.separator}") }
        .forEach { file ->
            @Suppress("UNCHECKED_CAST")
            val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
            @Suppress("UNCHECKED_CAST")
            val display = json["display"] as? MutableMap<String, Any?> ?: return@forEach
            @Suppress("UNCHECKED_CAST")
            val icon = display["icon"] as? MutableMap<String, Any?> ?: return@forEach
            val id = icon.remove("id") as? String ?: return@forEach
            icon["item"] = id
            file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
            converted++
        }
    return converted
}

val legacyCookingRecipeTypes = setOf(
    "minecraft:smelting",
    "minecraft:blasting",
    "minecraft:smoking",
    "minecraft:campfire_cooking",
)

/**
 * The supported 1.20.x node expects plural recipe, loot-table, advancement, structure, and tag registry
 * directories. 1.21.1 and newer use the singular registry paths.
 * This only moves files after Gradle has copied them into [ProcessResources.destinationDir].
 */
fun renameLegacyDataDirectories(root: File): Int {
    val data = File(root, "data")
    if (!data.isDirectory) return 0

    var renamed = 0
    data.listFiles()?.filter { it.isDirectory }?.forEach { namespace ->
        listOf(
            "recipe" to "recipes",
            "loot_table" to "loot_tables",
            "advancement" to "advancements",
            "structure" to "structures",
        ).forEach { (modern, legacy) ->
            if (moveOutputDirectory(File(namespace, modern), File(namespace, legacy))) renamed++
        }

        val tags = File(namespace, "tags")
        if (tags.isDirectory) {
            listOf("block" to "blocks", "item" to "items", "fluid" to "fluids", "entity_type" to "entity_types", "game_event" to "game_events")
                .forEach { (modern, legacy) ->
                    if (moveOutputDirectory(File(tags, modern), File(tags, legacy))) renamed++
                }
        }
    }
    return renamed
}

/** Moves one copied resource directory, preserving any already-copied legacy siblings. */
fun moveOutputDirectory(source: File, target: File): Boolean {
    if (!source.isDirectory) return false
    source.walkTopDown().filter { it.isFile }.forEach { file ->
        val relative = file.relativeTo(source)
        val destination = File(target, relative.path)
        destination.parentFile.mkdirs()
        file.copyTo(destination, overwrite = true)
    }
    source.deleteRecursively()
    return true
}

/**
 * Publishes the mod's `c:` convention tags under a second namespace, and repoints recipes at it.
 *
 * The community convention namespace for item/block tags is `c:` on NeoForge and modern Fabric, but
 * `forge:` on Forge 1.20.1 — `c:` only became universal with NeoForge 1.21. A mod that ships only
 * `c:` tags on 1.20.1 Forge is silently cut off from the ecosystem in *both* directions: no other
 * mod's copper reaches `#c:ingots/copper`, and this mod's copper never appears in the `forge:ingots`
 * tag that everything else reads. Nothing fails to build, and nothing fails to run standalone — it
 * only shows up in a modpack, as recipes that inexplicably reject a valid ingredient.
 *
 * The tags are **copied, not moved**, so the mod's items are members of both namespaces; only the
 * recipes are repointed, because an ingredient has to choose one and `forge:` is the one populated
 * by other 1.20.1 mods. Every convention path this mod uses (`ingots`, `nuggets`, `ores`, `plates`,
 * `dusts`, `gems`, `storage_blocks`, `raw_materials`) is spelled identically under both namespaces,
 * so this is a straight namespace substitution. The mod's own non-convention paths (`billets`,
 * `cokes`, `crystals`, `dusts_tiny`, `fragments`) have no `forge:` counterpart to collide with and
 * are copied along harmlessly.
 *
 * Runs after [renameLegacyDataDirectories], so the tag directories are already plural.
 */
fun duplicateConventionTags(root: File, namespace: String): Int {
    val source = File(root, "data/c/tags")
    if (!source.isDirectory) return 0
    val target = File(root, "data/$namespace/tags")

    var copied = 0
    source.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
        val destination = File(target, file.relativeTo(source).path)
        destination.parentFile.mkdirs()
        // Tag files reference sibling tags as "#c:..."; inside the copies those must resolve to the
        // copies, or a `forge:` tag would pull in members through a `c:` tag no other mod feeds.
        destination.writeText(file.readText().replace("\"#c:", "\"#$namespace:"))
        copied++
    }
    return copied
}

/** Repoints `#c:` ingredient references at the loader's convention namespace. */
fun repointConventionTagReferences(root: File, namespace: String): Int {
    var repointed = 0
    root.walkTopDown()
        .filter { it.isFile && it.extension == "json" && it.parentFile.name == "recipes" }
        .forEach { file ->
            val text = file.readText()
            val updated = text.replace("\"#c:", "\"#$namespace:").replace("\"tag\": \"c:", "\"tag\": \"$namespace:")
            if (updated != text) { file.writeText(updated); repointed++ }
        }
    return repointed
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
 * Whether this Minecraft version uses object-form ingredients. 26.1 is the first supported node with
 * bare-string ingredients; 1.21.1 still uses the object form but already uses result ids.
 */
fun needsLegacyIngredients(mcVersion: String): Boolean {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    return major < 26
}

/** 1.20.x uses the pre-data-component recipe result shape; 1.21.1 already uses result ids. */
fun needsLegacyRecipeResults(mcVersion: String): Boolean = needsLegacyDataDirectories(mcVersion)

/** 1.20.x uses plural data directories; 1.21.x and newer use the singular registry paths. */
fun needsLegacyDataDirectories(mcVersion: String): Boolean {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } == 1 && parts.getOrElse(1) { 0 } < 21
}

/** Item stacks switched from NBT to data components in 1.20.5. */
fun needsLegacyItemStacks(mcVersion: String): Boolean {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major < 26 && (minor < 20 || minor == 20 && patch < 5)
}

// The applying buildscript passes its node's Minecraft version; it is the only place that knows it.
// `convention_namespace` is likewise the buildscript's to declare: it is a loader fact, not a version
// fact, and only the Forge buildscript sets it to anything other than the modern default.
if (needsLegacyIngredients(extra.properties["mc_version"] as String)) {
    applyLegacyRecipeCompatibility(
        extra.properties["mc_version"] as String,
        extra.properties["convention_namespace"] as String? ?: "c",
    )
}
