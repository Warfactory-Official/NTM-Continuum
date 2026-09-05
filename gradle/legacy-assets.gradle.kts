import groovy.json.JsonOutput
import groovy.json.JsonSlurper

data class MachineObjTransformExpectation(
    val scale: Double,
    val yaw: Double,
    val ty: Double = 0.0,
    val guiTranslate: List<Double> = listOf(0.0, 0.0, 0.0),
    val guiScale: Double = 1.0,
    val guiYaw: Double = 0.0,
    val animation: String = "",
    val obj: String? = null,
    val texture: String? = null,
    val noCull: Boolean? = null,
    val exclude: List<String>? = null,
    val overlayTexture: String? = null,
    val overlayParts: List<String>? = null,
    val blackParts: List<String>? = null,
    val specialBase: String = MACHINE_OBJ_ITEM_BASE,
    val directModel: Boolean = false,
    val rawLegacyTransform: Boolean = false,
    val itemTranslate: List<Double> = listOf(0.0, 0.0, 0.0),
    val itemPreTranslate: List<Double> = listOf(0.0, 0.0, 0.0),
)

data class GunObjTransformExpectation(
    val rotation: List<Double>,
    val translation: List<Double>,
    val scale: List<Double>,
)

data class GunObjExpectation(
    val base: String,
    val display: Map<String, GunObjTransformExpectation>,
)

fun gunTransform(rotation: List<Double>, translation: List<Double>, scale: Double) =
    GunObjTransformExpectation(rotation, translation, listOf(scale, scale, scale))

val GUN_OBJ_EXPECTATIONS = mapOf(
    "gun_pepperbox" to GunObjExpectation("hbm:item/gun_obj_pepperbox_base", mapOf(
        "gui" to gunTransform(listOf(-18.881721, -41.641143, -27.236313), listOf(1.011377, 1.228155, -6.40901), 0.09375),
        "ground" to gunTransform(listOf(0.0, -90.0, 0.0), listOf(-0.5, -3.5, 0.5), 0.0625),
        "fixed" to gunTransform(listOf(0.0, 90.0, 0.0), listOf(1.0, -1.56, -0.5), 0.125),
        "thirdperson_righthand" to gunTransform(listOf(-179.493995, -1.125273, 179.74399), listOf(-0.757072, 1.14817, -2.356844), 0.0703125),
        "thirdperson_lefthand" to gunTransform(listOf(-172.402674, 29.330267, 154.481445), listOf(8.662276, 3.115906, 0.150338), 0.0703125),
        "head" to gunTransform(listOf(-31.091374, -55.675371, 143.318463), listOf(-3.357926, -10.391071, 17.010688), 0.3),
    )),
    "gun_g3" to GunObjExpectation("hbm:item/gun_obj_g3_base", mapOf(
        "gui" to gunTransform(listOf(-18.881721, -41.641143, -27.236313), listOf(0.008565, 0.928038, -7.690641), 0.0546875),
        "ground" to gunTransform(listOf(0.0, -90.0, 0.0), listOf(-0.5, -3.5, 0.5), 0.0625),
        "fixed" to gunTransform(listOf(0.0, 90.0, 0.0), listOf(1.0, -1.56, -0.5), 0.125),
        "thirdperson_righthand" to gunTransform(listOf(-179.493995, -1.125273, 179.74399), listOf(-0.784191, 2.283047, -3.47155), 0.0703125),
        "thirdperson_lefthand" to gunTransform(listOf(-172.402674, 29.330267, 154.481445), listOf(8.790823, 4.283303, -0.922934), 0.0703125),
        "head" to gunTransform(listOf(-31.091374, -55.675371, 143.318463), listOf(-8.938885, -13.512669, 19.28852), 0.3),
    )),
    "gun_light_revolver_dani" to GunObjExpectation("hbm:item/gun_obj_dani_base", mapOf(
        "gui" to gunTransform(listOf(-180.0, 0.0, 0.0), listOf(0.5625, 1.4375, -8.5625), 0.0703125),
        "ground" to gunTransform(listOf(0.0, -90.0, 0.0), listOf(-0.5, -3.5, 0.5), 0.0625),
        "fixed" to gunTransform(listOf(0.0, 90.0, 0.0), listOf(1.0, -1.56, -0.5), 0.125),
        "thirdperson_righthand" to gunTransform(listOf(-179.493995, -1.125273, 179.74399), listOf(-0.757072, 1.14817, -2.356844), 0.0703125),
        "thirdperson_lefthand" to gunTransform(listOf(-172.402674, 29.330267, 154.481445), listOf(8.662276, 3.115906, 0.150338), 0.0703125),
        "head" to gunTransform(listOf(-31.091374, -55.675371, 143.318463), listOf(-3.357926, -10.391071, 17.010688), 0.3),
    )),
)

val RTG_BLOCKSTATE_IDS = listOf("machine_minirtg", "rtg_polonium", "machine_rtg_grey")
val MACHINE_OBJ_ITEM_BASE = "hbm:item/machine_obj_base"

/** Centered item scales derived from submitted OBJ bounds, using the assembly machine's 1.05-unit frame. */
val MACHINE_OBJ_TRANSFORM_EXPECTATIONS = mapOf(
    "machine_fensu" to MachineObjTransformExpectation(
        0.21, 90.0,
        obj = "hbm:models/machines/fensu.obj",
        texture = "hbm:textures/models/machines/fensus/fensu_light_blue.png",
    ),
    "machine_bigasstank" to MachineObjTransformExpectation(
        0.0807692308, 0.0,
        obj = "hbm:models/machines/bigasstank.obj",
        texture = "hbm:textures/models/machines/bigasstank.png",
    ),
    "machine_bat9000" to MachineObjTransformExpectation(
        0.2, 0.0,
        obj = "hbm:models/machines/bat9000.obj",
        texture = "hbm:textures/models/machines/bat9000.png",
    ),
    "machine_battery_redd" to MachineObjTransformExpectation(
        0.1024390244, 270.0,
        obj = "hbm:models/machines/fensu2.obj",
        texture = "hbm:textures/models/machines/fensu2.png",
        exclude = listOf("Plasma"),
    ),
    "fusion_breeder" to MachineObjTransformExpectation(0.21, 90.0, exclude = listOf("BreederAlt")),
    "fusion_plasma_forge" to MachineObjTransformExpectation(
        0.095454545, 90.0,
        obj = "hbm:models/fusion/plasma_forge.obj",
        texture = "hbm:textures/models/fusion/plasma_forge.png",
        exclude = listOf("Plasma"),
        blackParts = listOf("Plasma"),
    ),
    "machine_turbine_gas" to MachineObjTransformExpectation(0.105, 90.0),
    "machine_turbofan" to MachineObjTransformExpectation(
        0.15, 0.0,
        overlayTexture = "hbm:textures/models/machines/turbofan_back.png",
        overlayParts = listOf("Afterburner"),
    ),
    "machine_radgen" to MachineObjTransformExpectation(0.171428571, 0.0, exclude = listOf("Glass")),
    "machine_industrial_boiler" to MachineObjTransformExpectation(0.21, 90.0),
    "heat_boiler" to MachineObjTransformExpectation(0.2625, 0.0),
    "heater_oven" to MachineObjTransformExpectation(0.329411765, 180.0),
    "heater_heatex" to MachineObjTransformExpectation(0.35, 90.0),
    "machine_arc_welder" to MachineObjTransformExpectation(0.28, 0.0),
    "machine_assembly_factory" to MachineObjTransformExpectation(0.35, 180.0),
    "machine_chemical_factory" to MachineObjTransformExpectation(
        0.35, 180.0, exclude = listOf("Plane.001"),
    ),
    "machine_autosaw" to MachineObjTransformExpectation(0.15, -90.0),
    "machine_solar_boiler" to MachineObjTransformExpectation(0.2625, 0.0),
    "machine_intake" to MachineObjTransformExpectation(0.525, 0.0),
    "fan" to MachineObjTransformExpectation(1.05, 0.0),
    "chimney_brick" to MachineObjTransformExpectation(0.5, 0.0),
    "chimney_industrial" to MachineObjTransformExpectation(0.25, 0.0),
    "machine_ashpit" to MachineObjTransformExpectation(0.329411765, 180.0),
    "machine_strand_caster" to MachineObjTransformExpectation(0.15, 0.0),
    "machine_arc_furnace" to MachineObjTransformExpectation(0.161538461538462, 0.0),
    "machine_difurnace_ext" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/block/difurnace_extension.obj",
        texture = "hbm:textures/blocks/difurnace_extension.png",
    ),
    "furnace_iron" to MachineObjTransformExpectation(0.525, 90.0, exclude = listOf("On")),
    "furnace_steel" to MachineObjTransformExpectation(0.35, 0.0),
    "machine_wood_burner" to MachineObjTransformExpectation(0.266666667, 90.0),
    "machine_gascent" to MachineObjTransformExpectation(0.2625, 0.0),
    "machine_electrolyser" to MachineObjTransformExpectation(0.091290059, 0.0),
    "machine_deuterium_tower" to MachineObjTransformExpectation(0.099275384, 180.0),
    "machine_uf6_tank" to MachineObjTransformExpectation(0.525, 270.0),
    "machine_puf6_tank" to MachineObjTransformExpectation(0.525, 270.0),
    "machine_mixer" to MachineObjTransformExpectation(0.35, 180.0, exclude = listOf("Fluid")),
    "machine_exposure_chamber" to MachineObjTransformExpectation(0.095454545, 90.0),
    "radio_telex" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/telex.obj",
        texture = "hbm:textures/models/machines/telex.png",
        noCull = true,
        guiTranslate = listOf(0.0, -2.0, 0.0),
        guiScale = 6.0,
        rawLegacyTransform = true,
        itemTranslate = listOf(0.0, 0.0, 0.0),
        itemPreTranslate = listOf(0.0, 0.0, -0.5),
    ),
    "radio_autocal" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/autocal.obj",
        texture = "hbm:textures/models/machines/autocal.png",
        noCull = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 6.25,
        rawLegacyTransform = true,
        itemTranslate = listOf(0.0, 0.0, 0.0),
        itemPreTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_pump_electric" to MachineObjTransformExpectation(0.21, 0.0, animation = "pump"),
    "machine_pump_steam" to MachineObjTransformExpectation(0.21, 0.0, animation = "pump"),
    "machine_minirtg" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/rtg.obj",
        texture = "hbm:textures/models/machines/rtg_cell.png",
        noCull = true,
        exclude = emptyList(),
    ),
    "rtg_polonium" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/rtg.obj",
        texture = "hbm:textures/models/machines/rtg_polonium.png",
        noCull = true,
        exclude = emptyList(),
    ),
    "machine_rtg_grey" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/rtg.obj",
        texture = "hbm:textures/models/machines/rtg.png",
        noCull = true,
        exclude = emptyList(),
    ),
)

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

/**
 * Marker for context-aware machine OBJ definitions. Its display block is the exact perspective matrix
 * used by the old StaticWavefrontItemBakedModel/ItemRenderBase path.
 */
val MACHINE_OBJ_CONTEXT_LEGACY_MODEL = "hbm:item/machine_obj_context_legacy"

/**
 * The same marker for a Sedna gun. `hbm:gun` is a whole item MODEL on 26.1 (only that layer is handed the
 * display context a weapon needs); below it the equivalent is the same per-loader BEWLR the machines use.
 * Every gun base inherits this `builtin/entity` marker, so modern properties and legacy routing consume
 * that base's one display block.
 */
val GUN_OBJ_LEGACY_MODEL = "hbm:item/gun_obj_legacy"

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
                    // A self-rendered item's model MUST be the builtin/entity marker, so it overwrites
                    // any existing file: a couple of ducts still carry a flat sprite model that 26.1 no
                    // longer references (their definitions point at the OBJ renderer), and leaving it in
                    // place would render them flat here and 3D there. The same applies to a gun, whose
                    // placeholder sprite predates its OBJ renderer. Every other item defers to what ships.
                    val selfRendered = parent == MACHINE_OBJ_LEGACY_MODEL || parent == MACHINE_OBJ_CONTEXT_LEGACY_MODEL
                            || isGunDefinition(definition)
                    if (target.exists() && !selfRendered) return@forEach
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

            // RBMK pellets use the registered hbm:item_variant property. The modern item definition selects
            // its ten visual-state models; older clients express the same selection as ordered overrides.
            var selected = 0
            assets.listFiles()?.filter { it.isDirectory }?.forEach { namespace ->
                val definitions = File(namespace, "items")
                if (!definitions.isDirectory) return@forEach
                definitions.listFiles()?.filter { it.extension == "json" }?.forEach { definition ->
                    val branches = itemVariantSelectOf(definition) ?: return@forEach
                    val (fallback, cases) = branches
                    val target = File(namespace, "models/item/" + fallback.substringAfter("item/") + ".json")
                    if (!target.isFile) return@forEach
                    @Suppress("UNCHECKED_CAST")
                    val model = JsonSlurper().parse(target) as MutableMap<String, Any?>
                    model["overrides"] = cases.map { (value, selectedModel) ->
                        mapOf("predicate" to mapOf("hbm:item_variant" to value), "model" to selectedModel)
                    }
                    target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(model)))
                    selected++
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
                "legacyItemAssets: generated $generated item model(s); rewrote $conditioned conditional model(s); selected $selected item-variant model(s); " +
                    "folded $folded item atlas source(s) into blocks.json.")
        }
    }
}

/**
 * Resource-level regression test for the nested machine OBJ definitions. This is deliberately
 * independent of a client launch: it catches a wrapper/type regression and exact-value drift in every node.
 */
fun Project.registerMachineObjTransformChecks() {
    val validation = tasks.register("validateMachineObjTransforms") {
        group = "verification"
        description = "Checks the legacy machine OBJ item transform contract."
        doLast {
            val directory = rootProject.file("src/main/resources/assets/hbm/items")
            fun close(actual: Any?, expected: Double, label: String) {
                check(actual is Number && kotlin.math.abs(actual.toDouble() - expected) < 0.000001) {
                    "$label expected $expected but was $actual"
                }
            }
            fun vector(actual: Any?, expected: List<Double>, label: String) {
                check(actual is List<*> && actual.size == expected.size) {
                    "$label must contain ${expected.size} values"
                }
                expected.forEachIndexed { index, value -> close(actual[index], value, "$label[$index]") }
            }
            fun strings(actual: Any?, label: String): List<String> {
                if (actual == null) return emptyList()
                check(actual is List<*>) { "$label must be an array" }
                return actual.mapIndexed { index, value ->
                    check(value is String) { "$label[$index] must be a string" }
                    value
                }
            }
            MACHINE_OBJ_TRANSFORM_EXPECTATIONS.forEach { (name, expected) ->
                val file = File(directory, "$name.json")
                check(file.isFile) { "Missing machine OBJ item definition: $file" }
                @Suppress("UNCHECKED_CAST")
                val root = JsonSlurper().parse(file) as? Map<String, Any?>
                    ?: error("Invalid JSON object: $file")
                @Suppress("UNCHECKED_CAST")
                val rootModel = root["model"] as? Map<String, Any?>
                    ?: error("Missing model object: $file")
                check(rootModel["type"] == "minecraft:special") {
                    "$file must use a minecraft:special wrapper"
                }
                check(rootModel["base"] == expected.specialBase) {
                    "$file must use ${expected.specialBase} as its special-model base"
                }
                check((rootModel["type"] == "hbm:machine_obj") == expected.directModel) {
                    "$file has the wrong item-model form"
                }
                @Suppress("UNCHECKED_CAST")
                val model = when (rootModel["type"]) {
                    "hbm:machine_obj" -> rootModel
                    "minecraft:special" -> {
                        check(rootModel["base"] == MACHINE_OBJ_ITEM_BASE) {
                            "$file must use $MACHINE_OBJ_ITEM_BASE as its special-model base"
                        }
                        rootModel["model"] as? Map<String, Any?>
                            ?: error("Missing nested hbm:machine_obj model: $file")
                    }
                    else -> error("$file must use hbm:machine_obj directly or inside minecraft:special")
                }
                check(model["type"] == "hbm:machine_obj") {
                    "$file must use the hbm:machine_obj item-model type"
                }
                expected.obj?.let { check(model["obj"] == it) { "$name.obj expected $it but was ${model["obj"]}" } }
                expected.texture?.let {
                    check(model["texture"] == it) { "$name.texture expected $it but was ${model["texture"]}" }
                }
                expected.noCull?.let {
                    check(model["noCull"] == it) { "$name.noCull expected $it but was ${model["noCull"]}" }
                }
                expected.exclude?.let {
                    check(strings(model["exclude"], "$name.exclude") == it) {
                        "$name.exclude expected $it but was ${model["exclude"]}"
                    }
                }
                expected.overlayTexture?.let {
                    check(model["overlayTexture"] == it) {
                        "$name.overlayTexture expected $it but was ${model["overlayTexture"]}"
                    }
                }
                expected.overlayParts?.let {
                    check(strings(model["overlayParts"], "$name.overlayParts") == it) {
                        "$name.overlayParts expected $it but was ${model["overlayParts"]}"
                    }
                }
                expected.blackParts?.let {
                    check(strings(model["blackParts"], "$name.blackParts") == it) {
                        "$name.blackParts expected $it but was ${model["blackParts"]}"
                    }
                }
                if (!expected.rawLegacyTransform) {
                    listOf("rawLegacyTransform", "guiTranslate", "guiScale", "guiYaw",
                        "contextTransforms", "itemTranslate", "itemPreTranslate").forEach { field ->
                        check(!model.containsKey(field)) {
                            "$name must not declare legacy $field"
                        }
                    }
                    vector(expected.guiTranslate, listOf(0.0, 0.0, 0.0), "$name.guiTranslate")
                    close(expected.guiScale, 1.0, "$name.guiScale")
                    close(expected.guiYaw, 0.0, "$name.guiYaw")
                } else {
                    check(model["rawLegacyTransform"] == true) {
                        "$name must opt into rawLegacyTransform"
                    }
                    vector(model["itemTranslate"], expected.itemTranslate, "$name.itemTranslate")
                    vector(model["itemPreTranslate"], expected.itemPreTranslate, "$name.itemPreTranslate")
                    vector(model["guiTranslate"], expected.guiTranslate, "$name.guiTranslate")
                    close(model["guiScale"], expected.guiScale, "$name.guiScale")
                    close(model["guiYaw"] ?: 0.0, expected.guiYaw, "$name.guiYaw")
                }
                check(model.containsKey("ty")) { "$name must explicitly declare ty" }
                close(model["ty"], expected.ty, "$name.ty")
                close(model["scale"], expected.scale, "$name.scale")
                close(model["yaw"], expected.yaw, "$name.yaw")
                check((model["animation"] ?: "") == expected.animation) {
                    "$name.animation expected ${expected.animation} but was ${model["animation"]}"
                }
            }

            val blockstateDirectory = rootProject.file("src/main/resources/assets/hbm/blockstates")
            RTG_BLOCKSTATE_IDS.forEach { name ->
                val file = File(blockstateDirectory, "$name.json")
                check(file.isFile) { "Missing RTG blockstate: $file" }
                @Suppress("UNCHECKED_CAST")
                val root = JsonSlurper().parse(file) as? Map<String, Any?>
                    ?: error("Invalid JSON object: $file")
                @Suppress("UNCHECKED_CAST")
                val variants = root["variants"] as? Map<Any?, Any?>
                    ?: error("$file must contain variants")
                check(variants.isNotEmpty()) { "$file must contain at least one variant" }
                variants.forEach { (variant, value) ->
                    @Suppress("UNCHECKED_CAST")
                    val variantModel = value as? Map<String, Any?>
                        ?: error("$file variant $variant must be an object")
                    check(variantModel["model"] == "hbm:block/machine_dummy_empty") {
                        "$file variant $variant must map to hbm:block/machine_dummy_empty"
                    }
                }
            }
            logger.lifecycle("machineObjTransforms: validated ${MACHINE_OBJ_TRANSFORM_EXPECTATIONS.size} definitions and ${RTG_BLOCKSTATE_IDS.size} RTG blockstates")
        }
    }
    tasks.named("check") { dependsOn(validation) }
}

/** Checks the dedicated old-renderer matrices and the shared modern/legacy base reference. */
fun Project.registerGunObjTransformChecks() {
    val validation = tasks.register("validateGunObjTransforms") {
        group = "verification"
        description = "Checks Sedna gun base references and exact static display transforms."
        doLast {
            val assets = rootProject.file("src/main/resources/assets/hbm")
            fun vector(actual: Any?, expected: List<Double>, label: String) {
                check(actual is List<*> && actual.size == expected.size) {
                    "$label must contain ${expected.size} values"
                }
                expected.forEachIndexed { index, value ->
                    val number = actual[index]
                    check(number is Number && kotlin.math.abs(number.toDouble() - value) < 0.0000001) {
                        "$label[$index] expected $value but was $number"
                    }
                }
            }

            GUN_OBJ_EXPECTATIONS.forEach { (item, expected) ->
                val definition = File(assets, "items/$item.json")
                check(definition.isFile) { "Missing gun item definition: $definition" }
                @Suppress("UNCHECKED_CAST")
                val root = JsonSlurper().parse(definition) as Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val model = root["model"] as? Map<String, Any?> ?: error("Missing model object: $definition")
                check(model["type"] == "hbm:gun") { "$definition must use hbm:gun" }
                check(model["base"] == expected.base) {
                    "$definition must reference ${expected.base}, not ${model["base"]}"
                }
                check(parentModelOf(definition) == expected.base) {
                    "Legacy conversion must retain ${expected.base} for $item"
                }

                val baseName = expected.base.substringAfter("hbm:item/")
                val baseFile = File(assets, "models/item/$baseName.json")
                check(baseFile.isFile) { "Missing dedicated gun base: $baseFile" }
                @Suppress("UNCHECKED_CAST")
                val base = JsonSlurper().parse(baseFile) as Map<String, Any?>
                check(base["parent"] == GUN_OBJ_LEGACY_MODEL) {
                    "$baseFile must inherit $GUN_OBJ_LEGACY_MODEL for legacy custom rendering"
                }
                @Suppress("UNCHECKED_CAST")
                val display = base["display"] as? Map<String, Any?> ?: error("Missing display object: $baseFile")
                check(display.keys == expected.display.keys) {
                    "$baseFile contexts expected ${expected.display.keys} but were ${display.keys}"
                }
                expected.display.forEach { (context, transform) ->
                    @Suppress("UNCHECKED_CAST")
                    val actual = display[context] as? Map<String, Any?>
                        ?: error("Missing $context transform: $baseFile")
                    vector(actual["rotation"], transform.rotation, "$item.$context.rotation")
                    vector(actual["translation"], transform.translation, "$item.$context.translation")
                    vector(actual["scale"], transform.scale, "$item.$context.scale")
                }
            }

            val marker = JsonSlurper().parse(File(assets, "models/item/gun_obj_legacy.json")) as Map<*, *>
            check(marker["parent"] == "minecraft:builtin/entity" && !marker.containsKey("display")) {
                "gun_obj_legacy must remain a transform-free builtin/entity marker"
            }
            logger.lifecycle("gunObjTransforms: validated ${GUN_OBJ_EXPECTATIONS.size} dedicated weapon bases")
        }
    }
    tasks.named("check") { dependsOn(validation) }
}

/**
 * The model an item definition resolves to, as a plain parent reference, or null if this definition has
 * no single static model to fall back on.
 *
 * `minecraft:model` is the ordinary case and names its model directly.
 *
         * `hbm:machine_obj` item-model type is nested under the shared special-model base. For
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

fun itemVariantSelectOf(definition: File): Pair<String, List<Pair<Float, String>>>? {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return null
    @Suppress("UNCHECKED_CAST")
    val model = json["model"] as? Map<String, Any?> ?: return null
    if (model["type"] != "minecraft:select" || model["property"] != "hbm:item_variant") return null
    @Suppress("UNCHECKED_CAST")
    val fallback = (model["fallback"] as? Map<String, Any?>)?.get("model") as? String ?: return null
    @Suppress("UNCHECKED_CAST")
    val cases = model["cases"] as? List<Map<String, Any?>> ?: return null
    val parsed = cases.mapNotNull { entry ->
        val value = (entry["when"] as? String)?.toFloatOrNull() ?: return@mapNotNull null
        @Suppress("UNCHECKED_CAST")
        val selected = (entry["model"] as? Map<String, Any?>)?.get("model") as? String ?: return@mapNotNull null
        value to selected
    }
    return fallback to (parsed.takeIf { it.isNotEmpty() } ?: return null)
}

fun isMachineObjSpecial(model: Any?): Boolean {
    @Suppress("UNCHECKED_CAST")
    val json = model as? Map<String, Any?> ?: return false
    if (json["type"] != "minecraft:special") return false
    @Suppress("UNCHECKED_CAST")
    val inner = json["model"] as? Map<String, Any?> ?: return false
    return inner["type"] == "hbm:machine_obj"
}

fun isGunDefinition(definition: File): Boolean {
    @Suppress("UNCHECKED_CAST")
    val root = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return false
    @Suppress("UNCHECKED_CAST")
    val model = root["model"] as? Map<String, Any?> ?: return false
    return model["type"] == "hbm:gun"
}

fun parentModelOf(definition: File): String? {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return null
    @Suppress("UNCHECKED_CAST")
    val model = json["model"] as? Map<String, Any?> ?: return null
    return when (model["type"]) {
        "minecraft:model" -> model["model"] as? String
        "hbm:machine_obj" -> MACHINE_OBJ_CONTEXT_LEGACY_MODEL
        // Gun bases inherit builtin/entity themselves, preserving their exact modern display matrices.
        "hbm:gun" -> model["base"] as? String
        "minecraft:special" -> {
            @Suppress("UNCHECKED_CAST")
            val inner = model["model"] as? Map<String, Any?>
             if (inner?.get("type") == "hbm:machine_obj" || inner?.get("type") == "hbm:rbmk_fuel_column"
                 || inner?.get("type") == "hbm:rbmk_block_column") MACHINE_OBJ_LEGACY_MODEL
             else model["base"] as? String
         }
        "minecraft:select" -> {
            @Suppress("UNCHECKED_CAST")
            val fallback = model["fallback"]
            @Suppress("UNCHECKED_CAST")
            val cases = model["cases"] as? List<Map<String, Any?>> ?: emptyList()
            if (isMachineObjSpecial(fallback) || cases.any { isMachineObjSpecial(it["model"]) }) {
                MACHINE_OBJ_LEGACY_MODEL
            } else null
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

registerMachineObjTransformChecks()
registerGunObjTransformChecks()
