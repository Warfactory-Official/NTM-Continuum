import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Converts client assets written in the 26.1 layout into the pre-1.21.4 layout.
 *
 * Two unrelated things changed between the eras, and both fail *silently* — the game logs a warning and
 * draws a missing-texture cube rather than erroring, so a build and a server boot both stay green:
 *
 *  1. **Item models moved a level of indirection.** From 1.21.4 an item is described by
 *     `assets/<ns>/items/<name>.json` (an "item definition": which model, plus conditions/overrides),
 *     and `models/item/<name>.json` is only needed when that definition points at one. Before that, the
 *     item model *is* `models/item/<name>.json` and the definition layer does not exist — so every item
 *     whose definition points straight at a block model (all 238 block items here) has no item model at
 *     all on the older line. This generates the missing ones from the definitions themselves.
 *
 *  2. **There was no `items` atlas.** Item textures are stitched into the *blocks* atlas until 1.21.4.
 *     The mod ships `assets/minecraft/atlases/items.json` to register its non-standard `textures/items/`
 *     directory; on the older line that file names an atlas that does not exist and is ignored, so ~600
 *     item textures never get stitched. Its sources are folded into `blocks.json` instead. (Atlas files
 *     from every pack are merged, not overridden, which is why the mod can ship a sources-only file.)
 *
 * Like the recipe conversion, this rewrites `processResources`' OUTPUT: `src/` keeps exactly one copy of
 * every asset, in the 26.1 shape, and nothing can leak between nodes.
 */
/**
 * The item model every OBJ machine gets below 1.21.4. Its `builtin/entity` parent is what makes vanilla
 * report `isCustomRenderer()` and route the item to the loader's BEWLR / DynamicItemRenderer.
 */
val MACHINE_OBJ_LEGACY_MODEL = "hbm:item/machine_obj_legacy"

fun Project.applyLegacyItemAssets() {
    tasks.named<ProcessResources>("processResources").configure {
        doLast {
            val root = destinationDir
            val assets = File(root, "assets")
            if (!assets.isDirectory) return@doLast

            var generated = 0
            assets.listFiles()?.filter { it.isDirectory }?.forEach { namespace ->
                val definitions = File(namespace, "items")
                if (!definitions.isDirectory) return@forEach
                val itemModels = File(namespace, "models/item")

                definitions.listFiles()?.filter { it.extension == "json" }?.forEach { definition ->
                    val target = File(itemModels, definition.name)
                    val parent = parentModelOf(definition) ?: return@forEach
                    // An OBJ machine's item model MUST be the builtin/entity marker, so it overwrites any
                    // existing file: a couple of ducts still carry a flat sprite model that 26.1 no longer
                    // references (their definitions point at the OBJ renderer), and leaving it in place
                    // would render them flat here and 3D there. Every other item defers to what ships.
                    if (target.exists() && parent != MACHINE_OBJ_LEGACY_MODEL) return@forEach
                    itemModels.mkdirs()
                    target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("parent" to parent))))
                    generated++
                }
            }

            // Model switching on a custom_model_data condition. 26.1 picks between two models in the item
            // definition; below 1.21.4 the same choice is a model `overrides` predicate on the losing
            // model. The on_false model IS an existing file (that is what the definition points at), so
            // this adds the override to it rather than generating a replacement.
            var conditioned = 0
            assets.listFiles()?.filter { it.isDirectory }?.forEach { namespace ->
                val definitions = File(namespace, "items")
                if (!definitions.isDirectory) return@forEach
                definitions.listFiles()?.filter { it.extension == "json" }?.forEach { definition ->
                    val branches = customModelDataConditionOf(definition) ?: return@forEach
                    val (whenFalse, whenTrue) = branches
                    val target = File(namespace, "models/item/" + whenFalse.substringAfter("item/") + ".json")
                    if (!target.isFile) return@forEach
                    @Suppress("UNCHECKED_CAST")
                    val model = JsonSlurper().parse(target) as MutableMap<String, Any?>
                    model["overrides"] = listOf(mapOf("predicate" to mapOf("custom_model_data" to 1), "model" to whenTrue))
                    target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(model)))
                    conditioned++
                }
            }

            val atlases = File(assets, "minecraft/atlases")
            val items = File(atlases, "items.json")
            val blocks = File(atlases, "blocks.json")
            var folded = 0
            if (items.isFile && blocks.isFile) {
                @Suppress("UNCHECKED_CAST")
                val itemSources = (JsonSlurper().parse(items) as Map<String, Any?>)["sources"] as? List<Any?>
                @Suppress("UNCHECKED_CAST")
                val blockJson = JsonSlurper().parse(blocks) as MutableMap<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val blockSources = (blockJson["sources"] as? List<Any?>).orEmpty()
                if (itemSources != null) {
                    blockJson["sources"] = blockSources + itemSources.filter { it !in blockSources }
                    blocks.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(blockJson)))
                    items.delete()
                    folded = itemSources.size
                }
            }

            logger.lifecycle(
                "legacyItemAssets: generated $generated item model(s); rewrote $conditioned conditional model(s); " +
                    "folded $folded item atlas source(s) into blocks.json.")
        }
    }
}

/**
 * The model an item definition resolves to, as a plain parent reference, or null if this definition has
 * no single static model to fall back on.
 *
 * `minecraft:model` is the ordinary case and names its model directly.
 *
 * `minecraft:special` pairs a base model with a special renderer. For HBM's OBJ machine renderer the
 * older-version equivalent is a per-loader "render this item yourself" hook (NeoForge's BEWLR, Fabric's
 * DynamicItemRenderer) — and vanilla only consults that hook when the baked model reports
 * `isCustomRenderer()`, which is true exactly when the model's parent chain ends at
 * `minecraft:builtin/entity`. So these get `hbm:item/machine_obj_legacy`, which does that and carries the
 * block display transforms. Any other special renderer falls back to its own `base`, which is precisely
 * what vanilla draws underneath it.
 */
/**
 * For an item definition that selects a model on a `minecraft:custom_model_data` condition, the pair
 * (model when false, model when true); null for anything else. Only the boolean-flag form is handled,
 * which is the one `ModelData.setColorAndFlag` produces.
 */
fun customModelDataConditionOf(definition: File): Pair<String, String>? {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return null
    @Suppress("UNCHECKED_CAST")
    val model = json["model"] as? Map<String, Any?> ?: return null
    if (model["type"] != "minecraft:condition" || model["property"] != "minecraft:custom_model_data") return null
    @Suppress("UNCHECKED_CAST")
    val onTrue = (model["on_true"] as? Map<String, Any?>)?.get("model") as? String ?: return null
    @Suppress("UNCHECKED_CAST")
    val onFalse = (model["on_false"] as? Map<String, Any?>)?.get("model") as? String ?: return null
    return onFalse to onTrue
}

fun parentModelOf(definition: File): String? {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return null
    @Suppress("UNCHECKED_CAST")
    val model = json["model"] as? Map<String, Any?> ?: return null
    return when (model["type"]) {
        "minecraft:model" -> model["model"] as? String
        "minecraft:special" -> {
            @Suppress("UNCHECKED_CAST")
            val inner = model["model"] as? Map<String, Any?>
            if (inner?.get("type") == "hbm:machine_obj") MACHINE_OBJ_LEGACY_MODEL
            else model["base"] as? String
        }
        else -> null
    }
}

// Same gate as the recipe conversion: the item-definition layer and the items atlas both arrived in
// 1.21.4, one release after the ingredient change, but no node sits between the two — 1.21.1 and 1.20.1
// need all of it, 26.1 needs none of it. Kept as its own predicate so that stays visible if one ever does.
fun needsLegacyItemAssets(mcVersion: String): Boolean {
    val parts = mcVersion.split('.').map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return when {
        major >= 26 -> false
        minor > 21 -> false
        minor < 21 -> true
        else -> patch < 4
    }
}

if (needsLegacyItemAssets(extra.properties["mc_version"] as String)) {
    applyLegacyItemAssets()
}
