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

/**
 * The base model each Sedna gun names. No matrices here any more: the frame is
 * `Pose17.ITEM_RENDER_WEAPON`, composed at datagen, and every weapon's own inventory and third-person
 * pose is the 1.7 `setupInv`/`setupThirdPerson` chain replayed by its `WeaponRenderer`. What this table
 * used to hold was one hand-fitted display block per base, pinned here as expected, which locked in a
 * pose no oracle produced and made the drift permanent.
 */
val GUN_OBJ_EXPECTATIONS = mapOf(
    "gun_pepperbox" to "hbm:item/gun_obj_pepperbox_base",
    "gun_g3" to "hbm:item/gun_obj_g3_base",
    "gun_light_revolver_dani" to "hbm:item/gun_obj_dani_base",
    "gun_b92" to "hbm:item/gun_b92_base",
)


val RTG_BLOCKSTATE_IDS = listOf("machine_minirtg", "rtg_polonium", "machine_rtg_grey")
val MACHINE_OBJ_ITEM_BASE = "hbm:item/machine_obj_base"

/** Centered item scales derived from submitted OBJ bounds, using the assembly machine's 1.05-unit frame. */
val MACHINE_OBJ_TRANSFORM_EXPECTATIONS = mapOf(
    "pa_source" to MachineObjTransformExpectation(
        0.5, 90.0,
        obj = "hbm:models/particleaccelerator/source.obj",
        texture = "hbm:textures/models/particleaccelerator/source.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 4.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "pa_rfc" to MachineObjTransformExpectation(
        0.5, 90.0,
        obj = "hbm:models/particleaccelerator/rfc.obj",
        texture = "hbm:textures/models/particleaccelerator/rfc.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 4.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "pa_detector" to MachineObjTransformExpectation(
        0.5, 90.0,
        obj = "hbm:models/particleaccelerator/detector.obj",
        texture = "hbm:textures/models/particleaccelerator/detector.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "pa_beamline" to MachineObjTransformExpectation(
        1.0, 90.0,
        obj = "hbm:models/particleaccelerator/beamline.obj",
        texture = "hbm:textures/models/particleaccelerator/beamline.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, 0.0, 0.0),
        guiScale = 4.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "pa_dipole" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/particleaccelerator/dipole.obj",
        texture = "hbm:textures/models/particleaccelerator/dipole.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 3.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "pa_quadrupole" to MachineObjTransformExpectation(
        1.0, 90.0,
        obj = "hbm:models/particleaccelerator/quadrupole.obj",
        texture = "hbm:textures/models/particleaccelerator/quadrupole.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.5, 0.0),
        guiScale = 4.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "dfc_emitter" to MachineObjTransformExpectation(
        2.0, 90.0,
        obj = "hbm:models/core_emitter.obj",
        texture = "hbm:textures/models/machines/core_emitter.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "dfc_receiver" to MachineObjTransformExpectation(
        2.0, 90.0,
        obj = "hbm:models/core_receiver.obj",
        texture = "hbm:textures/models/machines/core_receiver.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "dfc_injector" to MachineObjTransformExpectation(
        2.0, 90.0,
        obj = "hbm:models/core_injector.obj",
        texture = "hbm:textures/models/machines/core_injector.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "dfc_stabilizer" to MachineObjTransformExpectation(
        2.0, 90.0,
        obj = "hbm:models/core_injector.obj",
        texture = "hbm:textures/models/machines/core_stabilizer.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "cargo_elevator" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/elevator.obj",
        texture = "hbm:textures/models/machines/elevator.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.75, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_satlink" to MachineObjTransformExpectation(
        0.5, 0.0,
        obj = "hbm:models/machines/satlink.obj",
        texture = "hbm:textures/models/machines/satlink.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -5.0, 0.0),
        guiScale = 3.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_fensu" to MachineObjTransformExpectation(
        0.21, 90.0,
        obj = "hbm:models/machines/fensu.obj",
        texture = "hbm:textures/models/machines/fensus/fensu_light_blue.png",
    ),
    "machine_bigasstank" to MachineObjTransformExpectation(
        0.5, 0.0,
        obj = "hbm:models/machines/bigasstank.obj",
        texture = "hbm:textures/models/machines/bigasstank.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_bat9000" to MachineObjTransformExpectation(
        0.2, 0.0,
        obj = "hbm:models/machines/bat9000.obj",
        texture = "hbm:textures/models/machines/bat9000.png",
    ),
    "machine_battery_redd" to MachineObjTransformExpectation(
        0.5, -90.0,
        obj = "hbm:models/machines/fensu2.obj",
        texture = "hbm:textures/models/machines/fensu2.png",
        exclude = listOf("Plasma"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "fusion_breeder" to MachineObjTransformExpectation(
        0.5, 90.0,
        exclude = listOf("BreederAlt"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 5.0,
        guiYaw = 90.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "fusion_plasma_forge" to MachineObjTransformExpectation(
        0.5, 90.0,
        obj = "hbm:models/fusion/plasma_forge.obj",
        texture = "hbm:textures/models/fusion/plasma_forge.png",
        exclude = listOf("Plasma"),
        blackParts = listOf("Plasma"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 2.75,
        guiYaw = 90.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_turbine_gas" to MachineObjTransformExpectation(
        0.75, 90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 1.5),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_turbofan" to MachineObjTransformExpectation(
        1.0, 0.0,
        overlayTexture = "hbm:textures/models/machines/turbofan_back.png",
        overlayParts = listOf("Afterburner"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, 0.0, 0.0),
        guiScale = 2.25,
        guiYaw = 90.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_radgen" to MachineObjTransformExpectation(0.171428571, 0.0, exclude = listOf("Glass")),
    "machine_industrial_boiler" to MachineObjTransformExpectation(
        1.0, 90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "heat_boiler" to MachineObjTransformExpectation(
        1.0, 90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "heater_oven" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "heater_heatex" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_arc_welder" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.0, 0.0),
        guiScale = 4.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_assembly_factory" to MachineObjTransformExpectation(
        0.75, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.5, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_chemical_factory" to MachineObjTransformExpectation(
        0.75, 0.0,
        exclude = listOf("Plane.001"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.5, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_autosaw" to MachineObjTransformExpectation(
        0.5, -90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.5, -3.0),
        guiScale = 5.0,
        animation = "autosaw",
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_solar_boiler" to MachineObjTransformExpectation(0.2625, 0.0),
    "machine_intake" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, 0.0, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "fan" to MachineObjTransformExpectation(
        2.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "chimney_brick" to MachineObjTransformExpectation(
        0.5, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -5.0, 0.0),
        guiScale = 2.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "chimney_industrial" to MachineObjTransformExpectation(
        0.25, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -5.0, 0.0),
        guiScale = 2.75,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_ashpit" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_strand_caster" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(2.0, 0.0, 2.0),
        guiScale = 2.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_arc_furnace" to MachineObjTransformExpectation(
        0.5, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 3.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_difurnace_ext" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/block/difurnace_extension.obj",
        texture = "hbm:textures/blocks/difurnace_extension.png",
    ),
    "furnace_iron" to MachineObjTransformExpectation(0.525, 90.0, exclude = listOf("On")),
    "furnace_steel" to MachineObjTransformExpectation(
        1.0, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.5, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_wood_burner" to MachineObjTransformExpectation(
        1.0, 90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 3.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_gascent" to MachineObjTransformExpectation(0.2625, 0.0),
    "machine_electrolyser" to MachineObjTransformExpectation(
        0.5, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(-1.0, -1.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_deuterium_tower" to MachineObjTransformExpectation(0.099275384, 180.0),
    "machine_uf6_tank" to MachineObjTransformExpectation(0.525, 270.0),
    "machine_puf6_tank" to MachineObjTransformExpectation(0.525, 270.0),
    "machine_mixer" to MachineObjTransformExpectation(
        1.0, 180.0,
        exclude = listOf("Fluid"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -5.0, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_exposure_chamber" to MachineObjTransformExpectation(
        0.5, 90.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.5, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(1.5, 0.0, 0.0),
    ),
    "radio_telex" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/telex.obj",
        texture = "hbm:textures/models/machines/telex.png",
        noCull = true,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.0, 0.0),
        guiScale = 6.0,
        itemTranslate = listOf(0.0, 0.0, -0.5),
    ),
    "radio_autocal" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/autocal.obj",
        texture = "hbm:textures/models/machines/autocal.png",
        noCull = true,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 6.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_pump_electric" to MachineObjTransformExpectation(
        1.0, 0.0,
        animation = "pump",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_pump_steam" to MachineObjTransformExpectation(
        1.0, 0.0,
        animation = "pump",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
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
val GUN_OBJ_BASE_MODEL = "hbm:item/gun_obj_base"

/** The authored gun bases. `gun_obj_base` itself is generated, so it is not among them. */
val gunBaseChain = listOf("gun_obj_pepperbox_base", "gun_obj_g3_base", "gun_obj_dani_base", "gun_b92_base")

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
                    // A model that is its own definition's target. The two machine_obj frame markers are
                    // declared through the item vocabulary, so each gets an item definition naming itself;
                    // writing that parent out would replace the model with one inheriting from itself and
                    // drop the display block it exists to carry.
                    if (parent == "${namespace.name}:item/${definition.nameWithoutExtension}") return@forEach
                    // A self-rendered item's model MUST be the builtin/entity marker, so it overwrites
                    // any existing file: a couple of ducts still carry a flat sprite model that 26.1 no
                    // longer references (their definitions point at the OBJ renderer), and leaving it in
                    // place would render them flat here and 3D there. The same applies to a gun, whose
                    // placeholder sprite predates its OBJ renderer. Every other item defers to what ships.
                    val selfRendered = parent == MACHINE_OBJ_LEGACY_MODEL || parent.endsWith("_obj_context_legacy")
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
                    val (_, cases) = branches
                    // The item's OWN model, not the fallback's: overrides are not inherited through
                    // `parent`, so they have to sit on the file vanilla resolves for the item itself.
                    val target = File(namespace, "models/item/" + definition.nameWithoutExtension + ".json")
                    if (!target.isFile) return@forEach
                    @Suppress("UNCHECKED_CAST")
                    val model = JsonSlurper().parse(target) as MutableMap<String, Any?>
                    // Ascending: vanilla scans overrides from the end and takes the first whose
                    // threshold the value clears, so a case out of order would shadow the ones after it.
                    model["overrides"] = cases.sortedBy { it.first }.map { (value, selectedModel) ->
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
        // By name: dejson.gradle.kts is applied after this script, so the task does not exist yet.
        dependsOn("dejsonGenerate")
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
                check((rootModel["type"] == "hbm:machine_obj") == expected.directModel) {
                    "$file has the wrong item-model form"
                }
                if (!expected.directModel) {
                    check(rootModel["type"] == "minecraft:special") {
                        "$file must use a minecraft:special wrapper"
                    }
                    check(rootModel["base"] == expected.specialBase) {
                        "$file must use ${expected.specialBase} as its special-model base"
                    }
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

            // Every legacy OBJ body states its own scale. One that leaves the field out takes the
            // codec's 0.28 default, which is the block-framed convention and not a 1.7 renderInventory
            // number at all, so the icon draws at roughly a third of its size behind a green build.
            var scaled = 0
            val pending = ArrayDeque<Pair<String, Any?>>()
            directory.listFiles()?.filter { it.name.endsWith(".json") }?.sortedBy { it.name }
                ?.forEach { pending.add(it.name to JsonSlurper().parse(it)) }
            while (pending.isNotEmpty()) {
                val (name, node) = pending.removeFirst()
                when (node) {
                    is Map<*, *> -> {
                        if (node["type"] == "hbm:machine_obj") {
                            check(node.containsKey("scale")) {
                                "$name declares an hbm:machine_obj model with no scale"
                            }
                            scaled++
                        }
                        node.values.forEach { pending.add(name to it) }
                    }
                    is List<*> -> node.forEach { pending.add(name to it) }
                }
            }

            // These three are declared in ModBlockModels.RENDERED_2 rather than written, so the
            // generated copy is the one to check and the authored path is only a fallback for a
            // blockstate somebody has taken back into a file.
            val authoredBlockstates = rootProject.file("src/main/resources/assets/hbm/blockstates")
            val declaredBlockstates = layout.buildDirectory.dir("generated/dejson/assets/hbm/blockstates").get().asFile
            RTG_BLOCKSTATE_IDS.forEach { name ->
                val declared = File(declaredBlockstates, "$name.json")
                val file = if (declared.isFile) declared else File(authoredBlockstates, "$name.json")
                check(file.isFile) { "Missing RTG blockstate: $name (neither declared nor authored)" }
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
            logger.lifecycle("machineObjTransforms: validated ${MACHINE_OBJ_TRANSFORM_EXPECTATIONS.size} definitions, $scaled declared scales and ${RTG_BLOCKSTATE_IDS.size} RTG blockstates")
        }
    }
    tasks.named("check") { dependsOn(validation) }
}

/**
 * 1.7's `ItemRenderMissileGeneric`: the per-type GUI scale and offset, the body each missile wears, and the
 * extra scale `generateLarge` applied. The renderer is one class configured eight ways, so a missile put on
 * the wrong row is a wrong-size icon and nothing else — which is invisible without the table to check it
 * against. Carrier, endo and exo are absent here because upstream registers no body for them either.
 */
data class MissileObjExpectation(val obj: String, val texture: String, val modelScale: Double,
                                 val guiScale: Double, val guiOffset: Double)

fun missile(obj: String, texture: String, modelScale: Double, tier: Pair<Double, Double>) =
    MissileObjExpectation(obj, texture, modelScale, tier.first, tier.second)

val MISSILE_TIER0 = 3.75 to 10.75
val MISSILE_TIER1 = 2.5 to 8.5
val MISSILE_TIER2 = 2.0 to 6.5
val MISSILE_TIER3 = 1.25 to 1.0
val MISSILE_STEALTH = 1.75 to 4.75
val MISSILE_ABM = 2.25 to 7.0
val MISSILE_NUCLEAR = 1.375 to 1.5
val MISSILE_ROBIN = 1.25 to 2.0

/** 1.7 spins these at `currentTimeMillis() / 15 % 360`, a degree every fifteen milliseconds. */
val MISSILE_SPIN_SPEED = 1.0 / 15.0

val MISSILE_OBJ_EXPECTATIONS = mapOf(
    "missile_micro" to missile("missile_micro.obj", "missile_micro.png", 1.0, MISSILE_TIER0),
    "missile_taint" to missile("missile_micro.obj", "missile_micro_taint.png", 1.0, MISSILE_TIER0),
    "missile_bhole" to missile("missile_micro.obj", "missile_micro_bhole.png", 1.0, MISSILE_TIER0),
    "missile_schrabidium" to missile("missile_micro.obj", "missile_micro_schrab.png", 1.0, MISSILE_TIER0),
    "missile_emp" to missile("missile_micro.obj", "missile_micro_emp.png", 1.0, MISSILE_TIER0),
    "missile_generic" to missile("missile_v2.obj", "missile_v2.png", 1.0, MISSILE_TIER1),
    "missile_incendiary" to missile("missile_v2.obj", "missile_v2_inc.png", 1.0, MISSILE_TIER1),
    "missile_cluster" to missile("missile_v2.obj", "missile_v2_cl.png", 1.0, MISSILE_TIER1),
    "missile_buster" to missile("missile_v2.obj", "missile_v2_bu.png", 1.0, MISSILE_TIER1),
    "missile_decoy" to missile("missile_v2.obj", "missile_v2_decoy.png", 1.0, MISSILE_TIER1),
    "missile_anti_ballistic" to missile("missile_abm.obj", "missile_abm.png", 1.0, MISSILE_ABM),
    "missile_stealth" to missile("missile_stealth.obj", "missile_stealth.png", 1.0, MISSILE_STEALTH),
    "missile_strong" to missile("missile_strong.obj", "missile_strong.png", 1.5, MISSILE_TIER2),
    "missile_incendiary_strong" to missile("missile_strong.obj", "missile_strong_inc.png", 1.5, MISSILE_TIER2),
    "missile_cluster_strong" to missile("missile_strong.obj", "missile_strong_cl.png", 1.5, MISSILE_TIER2),
    "missile_buster_strong" to missile("missile_strong.obj", "missile_strong_bu.png", 1.5, MISSILE_TIER2),
    "missile_emp_strong" to missile("missile_strong.obj", "missile_strong_emp.png", 1.5, MISSILE_TIER2),
    "missile_burst" to missile("missile_huge.obj", "missile_huge.png", 1.0, MISSILE_TIER3),
    "missile_inferno" to missile("missile_huge.obj", "missile_huge_inc.png", 1.0, MISSILE_TIER3),
    "missile_rain" to missile("missile_huge.obj", "missile_huge_cl.png", 1.0, MISSILE_TIER3),
    "missile_drill" to missile("missile_huge.obj", "missile_huge_bu.png", 1.0, MISSILE_TIER3),
    "missile_nuclear" to missile("missile_atlas.obj", "missile_atlas_nuclear.png", 1.0, MISSILE_NUCLEAR),
    "missile_nuclear_cluster" to missile("missile_atlas.obj", "missile_atlas_thermo.png", 1.0, MISSILE_NUCLEAR),
    "missile_volcano" to missile("missile_atlas.obj", "missile_atlas_tectonic.png", 1.0, MISSILE_NUCLEAR),
    "missile_doomsday" to missile("missile_atlas.obj", "missile_atlas_doomsday.png", 1.0, MISSILE_NUCLEAR),
    "missile_doomsday_rusted" to missile("missile_atlas.obj", "missile_atlas_doomsday_weathered.png", 1.0, MISSILE_NUCLEAR),
    "missile_n2" to missile("missile_n2_body.obj", "missile_n2.png", 1.5, MISSILE_NUCLEAR),
    "missile_shuttle" to missile("missile_shuttle.obj", "missile_shuttle.png", 1.0, MISSILE_ROBIN),
)

val MISSILE_OBJ_CONTEXT_LEGACY_MODEL = "hbm:item/missile_obj_context_legacy"

/** Checks the dedicated old-renderer matrices and the shared modern/legacy base reference. */
fun Project.registerGunObjTransformChecks() {
    val validation = tasks.register("validateGunObjTransforms") {
        group = "verification"
        description = "Checks Sedna gun base references and that no base authors its own frame."
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

            GUN_OBJ_EXPECTATIONS.forEach { (item, base) ->
                val definition = File(assets, "items/$item.json")
                check(definition.isFile) { "Missing gun item definition: $definition" }
                @Suppress("UNCHECKED_CAST")
                val root = JsonSlurper().parse(definition) as Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                var model = root["model"] as? Map<String, Any?> ?: error("Missing model object: $definition")
                if (model["type"] == "minecraft:select") {
                    @Suppress("UNCHECKED_CAST")
                    model = model["fallback"] as? Map<String, Any?>
                        ?: error("Missing select fallback: $definition")
                }
                check(model["type"] == "hbm:gun") { "$definition must use hbm:gun" }
                check(model["base"] == base) {
                    "$definition must reference $base, not ${model["base"]}"
                }
                @Suppress("UNCHECKED_CAST")
                val slotModel = ((root["model"] as? Map<String, Any?>)
                    ?.takeIf { it["type"] == "minecraft:select" }
                    ?.get("cases") as? List<Map<String, Any?>>)
                    ?.firstOrNull()
                    ?.let { (it["model"] as? Map<String, Any?>)?.get("model") as? String }
                val legacy = parentModelOf(definition)
                check(legacy == base || legacy == slotModel) {
                    "Legacy conversion must retain ${slotModel ?: base} for $item, not $legacy"
                }
            }

            gunBaseChain.forEach { name ->
                val baseFile = File(assets, "models/item/$name.json")
                check(baseFile.isFile) { "Missing gun base: $baseFile" }
                @Suppress("UNCHECKED_CAST")
                val base = JsonSlurper().parse(baseFile) as Map<String, Any?>
                check(!base.containsKey("display")) {
                    "$baseFile must not author a display block: the weapon frame is Pose17.ITEM_RENDER_WEAPON, " +
                            "composed at datagen, and the per-weapon pose is the renderer's setupInv/setupThirdPerson"
                }
                val parent = base["parent"]
                check(parent == GUN_OBJ_BASE_MODEL || parent == GUN_OBJ_LEGACY_MODEL) {
                    "$baseFile must inherit $GUN_OBJ_BASE_MODEL or $GUN_OBJ_LEGACY_MODEL for legacy custom rendering"
                }
            }

            val marker = JsonSlurper().parse(File(assets, "models/item/gun_obj_legacy.json")) as Map<*, *>
            check(marker["parent"] == "minecraft:builtin/entity" && !marker.containsKey("display")) {
                "gun_obj_legacy must remain a transform-free builtin/entity marker"
            }
            logger.lifecycle("gunObjTransforms: validated ${GUN_OBJ_EXPECTATIONS.size} gun base references")
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

fun isMachineObjDirect(model: Any?): Boolean {
    @Suppress("UNCHECKED_CAST")
    val json = model as? Map<String, Any?> ?: return false
    return json["type"] == "hbm:machine_obj"
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

/**
 * Which shared frame a `hbm:machine_obj` model wears below 1.21.4. The `frame` field names the 1.7 item
 * render class; absent, it is `ItemRenderBase` like every machine.
 */
fun contextLegacyModelOf(model: Map<String, Any?>): String =
    if (model["frame"] == "missile") MISSILE_OBJ_CONTEXT_LEGACY_MODEL else MACHINE_OBJ_CONTEXT_LEGACY_MODEL

fun parentModelOf(definition: File): String? {
    @Suppress("UNCHECKED_CAST")
    val json = JsonSlurper().parse(definition) as? Map<String, Any?> ?: return null
    @Suppress("UNCHECKED_CAST")
    val model = json["model"] as? Map<String, Any?> ?: return null
    return when (model["type"]) {
        "minecraft:model" -> model["model"] as? String
        "hbm:machine_obj" -> contextLegacyModelOf(model)
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
            if (isMachineObjDirect(fallback) || cases.any { isMachineObjDirect(it["model"]) }) {
                // A variant select whose branches are context-aware machine_obj models: same frame the
                // unwrapped form gets, or the item resolves no model at all below 1.21.4.
                MACHINE_OBJ_CONTEXT_LEGACY_MODEL
            } else if (isMachineObjSpecial(fallback) || cases.any { isMachineObjSpecial(it["model"]) }) {
                MACHINE_OBJ_LEGACY_MODEL
            } else if ((fallback as? Map<String, Any?>)?.get("type") == "hbm:gun") {
                // A gun whose slot icon is a sprite and whose model is only drawn in hand: below 1.21.4
                // nothing reads the definition, so the item resolves models/item/<id>.json. Point it at
                // the sprite branch, which is what the slot shows in 1.7 too.
                @Suppress("UNCHECKED_CAST")
                val cases2 = model["cases"] as? List<Map<String, Any?>> ?: emptyList()
                ((cases2.firstOrNull()?.get("model") as? Map<String, Any?>)?.get("model") as? String)
            } else {
                // A select over ordinary sprite models still needs a model at the ITEM's own path.
                // Nothing below 1.21.4 reads the definition, so the item resolves
                // models/item/<id>.json and finds nothing at all — which is a missing texture, not a
                // wrong variant. Parent it to the branch the definition falls back to; the overrides
                // that pick the other branches are added below.
                @Suppress("UNCHECKED_CAST")
                ((fallback as? Map<String, Any?>)?.get("model") as? String)
            }
        }
        else -> null
    }
}

/**
 * Checks every launchable missile's icon against 1.7's `ItemRenderMissileGeneric` table. The frame these
 * bodies hang on is `Pose17.Klass.ITEM_RENDER_MISSILE` and is declared, not written, so `DisplayFrame`
 * already refuses a pose vanilla would corrupt; what a file can still get wrong is which row of the tier
 * table it took, which this reads back.
 */
fun Project.registerMissileObjTransformChecks() {
    val validation = tasks.register("validateMissileObjTransforms") {
        group = "verification"
        description = "Checks the launchable missiles' item icons against 1.7's per-tier GUI table."
        doLast {
            val assets = rootProject.file("src/main/resources/assets/hbm")
            MISSILE_OBJ_EXPECTATIONS.forEach { (item, expected) ->
                val definition = File(assets, "items/$item.json")
                check(definition.isFile) { "Missing missile item definition: $definition" }
                @Suppress("UNCHECKED_CAST")
                val root = JsonSlurper().parse(definition) as Map<String, Any?>
                check(root["oversized_in_gui"] == true) { "$definition must set oversized_in_gui" }
                @Suppress("UNCHECKED_CAST")
                val model = root["model"] as? Map<String, Any?> ?: error("Missing model object: $definition")
                check(model["type"] == "hbm:machine_obj") {
                    "$definition must use hbm:machine_obj directly, or it loses the display context"
                }
                check(model["frame"] == "missile") {
                    "$definition must name the missile frame, not ${model["frame"]}"
                }
                check(parentModelOf(definition) == MISSILE_OBJ_CONTEXT_LEGACY_MODEL) {
                    "Legacy conversion must route $item to $MISSILE_OBJ_CONTEXT_LEGACY_MODEL"
                }
                check(model["rawLegacyTransform"] == true) { "$definition must set rawLegacyTransform" }
                check(model["noCull"] == true) { "$definition must disable culling, as 1.7 does" }
                check(model["obj"] == "hbm:models/missiles/${expected.obj}") {
                    "$definition draws ${model["obj"]}, expected ${expected.obj}"
                }
                check(model["texture"] == "hbm:textures/models/missiles/${expected.texture}") {
                    "$definition wears ${model["texture"]}, expected ${expected.texture}"
                }
                check(File(assets, "models/missiles/${expected.obj}").isFile) {
                    "Missing missile body: ${expected.obj}"
                }
                check(File(assets, "textures/models/missiles/${expected.texture}").isFile) {
                    "Missing missile skin: ${expected.texture}"
                }
                number(model["scale"], expected.modelScale, "$item.scale")
                number(model["yaw"], 0.0, "$item.yaw")
                number(model["guiSpinSpeed"], MISSILE_SPIN_SPEED, "$item.guiSpinSpeed")

                @Suppress("UNCHECKED_CAST")
                val gui = (model["contextTransforms"] as? Map<String, Any?>)?.get("gui") as? Map<String, Any?>
                    ?: error("Missing gui contextTransform: $definition")
                val rotation = gui["rotation"] as? List<*> ?: error("Missing gui rotation: $definition")
                val translation = gui["translation"] as? List<*> ?: error("Missing gui translation: $definition")
                number(rotation.getOrNull(2), 135.0, "$item.gui.rotation.z")
                number(gui["scale"], expected.guiScale, "$item.gui.scale")
                number(translation.getOrNull(1), (-16.0 + expected.guiOffset) * expected.guiScale,
                    "$item.gui.translation.y")
            }
            logger.lifecycle("missileObjTransforms: validated ${MISSILE_OBJ_EXPECTATIONS.size} missile icons")
        }
    }
    tasks.named("check") { dependsOn(validation) }
}

fun number(actual: Any?, expected: Double, label: String) {
    check(actual is Number && kotlin.math.abs(actual.toDouble() - expected) < 0.000001) {
        "$label expected $expected but was $actual"
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
registerMissileObjTransformChecks()

/**
 * A mesh drawn by a block renderer states its own frame, and the two frames in this tree disagree:
 * the meshes ported from 1.7 are authored about the origin, so the renderer moves them to the block
 * centre, while those ported from 1.12 were baked with that move already applied and are authored in
 * 0..1 block space. Adding the centring translate to a block-space mesh puts the block half a block
 * out along x and z — the defect the drums shipped with. This grades the pair, because neither half
 * is wrong on its own.
 */
fun Project.registerBlockObjFrameChecks() {
    val validation = tasks.register("validateBlockObjFrames") {
        group = "verification"
        description = "Checks that a block-space OBJ is not also given the block-centring translate."
        doLast {
            val assets = rootProject.file("src/main/resources/assets/hbm")
            val renderers = rootProject.file("src/main/java/com/hbm/client/render")

            val blockSpace = mutableListOf<String>()
            assets.resolve("models").walkTopDown().filter { it.extension == "obj" }.forEach { obj ->
                var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
                var minZ = Double.MAX_VALUE; var maxZ = -Double.MAX_VALUE
                obj.forEachLine { line ->
                    if (line.startsWith("v ")) {
                        val parts = line.trim().split(Regex("\\s+"))
                        val x = parts[1].toDouble(); val z = parts[3].toDouble()
                        if (x < minX) minX = x; if (x > maxX) maxX = x
                        if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                    }
                }
                if (minX > maxX) return@forEach
                val single = (maxX - minX) <= 1.02 && (maxZ - minZ) <= 1.02
                val inside = minX > -0.06 && maxX < 1.06 && minZ > -0.06 && maxZ < 1.06
                val offOrigin = kotlin.math.abs((minX + maxX) / 2) > 0.2 || kotlin.math.abs((minZ + maxZ) / 2) > 0.2
                if (single && inside && offOrigin) blockSpace += obj.nameWithoutExtension
            }

            val centring = Regex("""translate\(\s*0\.5F?\s*,[^,]*,\s*0\.5F?\s*\)""")
            val pivotBack = Regex("""translate\(\s*-0\.5F?\s*,[^,]*,\s*-0\.5F?\s*\)""")
            val comments = Regex("""/\*.*?\*/|//[^\n]*""", RegexOption.DOT_MATCHES_ALL)
            var graded = 0
            renderers.walkTopDown().filter { it.extension == "java" }.forEach { source ->
                val text = comments.replace(source.readText(), " ")
                val meshes = blockSpace.filter { text.contains(it) }
                if (meshes.isEmpty()) return@forEach
                graded++
                text.split(';').filter { it.contains(".part(") || it.contains(".child(") }.forEach { statement ->
                    val flat = statement.replace(Regex("\\s+"), " ")
                    check(!(centring.containsMatchIn(flat) && !pivotBack.containsMatchIn(flat))) {
                        "${source.name} centres a block-space mesh (${meshes.joinToString()}) that is already " +
                                "authored about the block centre: drop the translate(0.5, y, 0.5), or pair it with " +
                                "translate(-0.5, y, -0.5) if it is the pivot of a rotation about the block centre.\n" +
                                "  ${flat.trim()}"
                    }
                }
            }
            logger.lifecycle("blockObjFrames: ${blockSpace.size} block-space mesh(es) across $graded renderer(s)")
        }
    }
    tasks.named("check") { dependsOn(validation) }
}

registerBlockObjFrameChecks()
