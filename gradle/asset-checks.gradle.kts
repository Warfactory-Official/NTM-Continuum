import groovy.json.JsonSlurper

// HBM's own resource-level regression checks for its item-model vocabulary (machine OBJ transforms, Sedna
// gun bases, missile icons, block-space OBJ frames). They used to live in legacy-assets.gradle.kts; the
// conversion itself is shared now (../Golem-Core/gradle/mod/legacy-assets.gradle.kts) and this file holds
// only what is specific to HBM. Apply it after that script: the gun and missile checks read the conversion's
// `golem.legacyAssets.parentModelOf` export, so they check the routing the build actually applies.

/** The model an item definition resolves to below 1.21.4, as the shared legacy conversion computes it. */
fun Project.parentModelOf(definition: File): String? {
    @Suppress("UNCHECKED_CAST")
    val function = extra["golem.legacyAssets.parentModelOf"] as (File) -> String?
    return function(definition)
}

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
    // icon-P2 (2026-10-01): 1.7 ItemRenderLibrary inv translate(0, -3, 0) scale(2), raw
    "machine_bat9000" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/bat9000.obj",
        texture = "hbm:textures/models/machines/bat9000.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -3.0, 0.0),
        guiScale = 2.0,
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
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_radgen" to MachineObjTransformExpectation(
        0.5, 0.0,
        obj = "hbm:models/machines/radgen.obj",
        texture = "hbm:textures/models/machines/rad_gen_body.png",
        noCull = true,
        exclude = listOf("Glass"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -1.0, 0.0),
        guiScale = 4.5,
        itemTranslate = listOf(0.25, 0.0, 0.0),
    ),
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
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_solar_boiler" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/solar_boiler.obj",
        texture = "hbm:textures/models/machines/solar_boiler.png",
        noCull = true,
        exclude = listOf("Overglow_1", "Overglow_2", "Overglow_3"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.5, 0.0),
        guiScale = 3.25,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
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
    // icon-P2 (2026-10-01): 1.7 RenderDiFurnaceExtension.renderInventoryBlock translate(0, -0.5, 0) on the block frame
    "machine_difurnace_ext" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/block/difurnace_extension.obj",
        texture = "hbm:textures/blocks/difurnace_extension.png",
        directModel = true,
        rawLegacyTransform = true,
        itemTranslate = listOf(0.0, -0.5, 0.0),
    ),
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "furnace_iron" to MachineObjTransformExpectation(
        1.0, 90.0,
        obj = "hbm:models/machines/furnace_iron.obj",
        texture = "hbm:textures/models/machines/furnace_iron.png",
        exclude = listOf("On"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -2.0, 0.0),
        guiScale = 5.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
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
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_gascent" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/centrifuge_gas.obj",
        texture = "hbm:textures/models/machines/centrifuge_gas.png",
        exclude = listOf("Flag"),
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 3.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    "machine_electrolyser" to MachineObjTransformExpectation(
        0.5, 0.0,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(-1.0, -1.0, 0.0),
        guiScale = 2.5,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_deuterium_tower" to MachineObjTransformExpectation(
        0.5, 180.0,
        obj = "hbm:models/machines/machine_deuterium_tower.obj",
        texture = "hbm:textures/models/machines/machine_deuterium_tower.png",
        noCull = true,
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -5.0, 0.0),
        guiScale = 3.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_uf6_tank" to MachineObjTransformExpectation(
        1.0, -90.0,
        obj = "hbm:models/tank.obj",
        texture = "hbm:textures/models/machines/uf6tank.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 6.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
    // icon-P2 (2026-10-01) raw 1.7 ItemRenderLibrary chain, see ObjItemDeclarations
    "machine_puf6_tank" to MachineObjTransformExpectation(
        1.0, -90.0,
        obj = "hbm:models/tank.obj",
        texture = "hbm:textures/models/machines/puf6tank.png",
        directModel = true,
        rawLegacyTransform = true,
        guiTranslate = listOf(0.0, -4.0, 0.0),
        guiScale = 6.0,
        itemTranslate = listOf(0.0, 0.0, 0.0),
    ),
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
    // icon-P2 (2026-10-01): 1.7 RenderRTGBlock.renderInventoryBlock translate(0, -0.5, 0) on the block frame
    "machine_rtg_grey" to MachineObjTransformExpectation(
        1.0, 0.0,
        obj = "hbm:models/machines/rtg.obj",
        texture = "hbm:textures/models/machines/rtg.png",
        noCull = true,
        exclude = emptyList(),
        directModel = true,
        rawLegacyTransform = true,
        itemTranslate = listOf(0.0, -0.5, 0.0),
    ),
)


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

/**
 * One hbm asset as the mod ships it: the Dejson-declared copy the build generated, or an authored file
 * where one still exists (an authored file wins in the jar, so it wins here too). Item definitions and
 * models are declarations now, so reading `src/main/resources` alone would find nothing.
 */
fun Project.hbmAsset(relative: String): File {
    val authored = rootProject.file("src/main/resources/assets/hbm/$relative")
    if (authored.isFile) return authored
    return layout.buildDirectory.file("generated/dejson/assets/hbm/$relative").get().asFile
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
                val file = hbmAsset("items/$name.json")
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
                    // Absent = the codec defaults (MachineItemModelRenderer: empty guiTranslate, guiScale 1), which
                    // the block-framed 1.7 ISBRH icons (difurnace extension, RTG) rely on.
                    vector(model["guiTranslate"] ?: listOf(0.0, 0.0, 0.0), expected.guiTranslate, "$name.guiTranslate")
                    close(model["guiScale"] ?: 1.0, expected.guiScale, "$name.guiScale")
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
            // Every item definition the jar ships: the declared ones, with an authored file winning.
            val definitions = sortedMapOf<String, File>()
            listOf(layout.buildDirectory.dir("generated/dejson/assets/hbm/items").get().asFile,
                rootProject.file("src/main/resources/assets/hbm/items")).forEach { directory ->
                directory.listFiles()?.filter { it.name.endsWith(".json") }?.forEach { definitions[it.name] = it }
            }
            // A worn-armour icon is an item whose own `model` is an hbm:machine_obj with an `armor_*` animation.
            // MachineItemModel.applyTransform sends exactly those to applyArmorItemTransform, which reproduces
            // ArmorFSB.setupRenderInv/NonInv and renderStandard with their hard-coded numbers, so `scale` is never
            // read and the codec's 0.28 default is irrelevant. (Wrapped models such as the BJ/DNS armour, nested
            // inside minecraft:special, are block-framed and do read `scale`, so they stay checked.)
            val armourRendered = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any?, Boolean>())
            definitions.values.forEach { file ->
                val root = JsonSlurper().parse(file)
                val model = (root as? Map<*, *>)?.get("model") as? Map<*, *>
                if (model != null && model["type"] == "hbm:machine_obj"
                    && (model["animation"] as? String)?.startsWith("armor_") == true) armourRendered.add(model)
                pending.add(file.name to root)
            }
            while (pending.isNotEmpty()) {
                val (name, node) = pending.removeFirst()
                when (node) {
                    is Map<*, *> -> {
                        if (node["type"] == "hbm:machine_obj" && node !in armourRendered) {
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

            // These three are declared on their registration chains (Reg) rather than written, so the
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
        dependsOn("dejsonGenerate")
        doLast {
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
                val definition = hbmAsset("items/$item.json")
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
                // a sprite slot behind a select is drawn by GunLegacyModels, so the item keeps the gun base too
                val legacy = parentModelOf(definition)
                check(legacy == base) { "Legacy conversion must retain $base for $item, not $legacy" }
            }

            gunBaseChain.forEach { name ->
                val baseFile = hbmAsset("models/item/$name.json")
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

            val marker = JsonSlurper().parse(hbmAsset("models/item/gun_obj_legacy.json")) as Map<*, *>
            check(marker["parent"] == "minecraft:builtin/entity" && !marker.containsKey("display")) {
                "gun_obj_legacy must remain a transform-free builtin/entity marker"
            }
            logger.lifecycle("gunObjTransforms: validated ${GUN_OBJ_EXPECTATIONS.size} gun base references")
        }
    }
    tasks.named("check") { dependsOn(validation) }
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
        dependsOn("dejsonGenerate")
        doLast {
            val assets = rootProject.file("src/main/resources/assets/hbm")
            MISSILE_OBJ_EXPECTATIONS.forEach { (item, expected) ->
                val definition = hbmAsset("items/$item.json")
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
