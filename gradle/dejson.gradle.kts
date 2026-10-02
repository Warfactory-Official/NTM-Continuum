// Block models, blockstates, item models, block loot tables and material tags declared in code are
// emitted as ordinary JSON here, at build time, and packed alongside the hand-written resources.
//
// Emitting rather than intercepting is deliberate. What ships is the same files the mod used to
// carry by hand, so every consumer of them — the model loader, resource packs, connected-texture
// engines, JEI, the model debugger — behaves exactly as before, and the only thing that changed is
// that nobody maintains them. The declaration vocabulary touches no Minecraft class, so this runs
// in a plain JVM with no game bootstrap.
//
// An authored file always wins: a path that exists under src/main/resources is left alone, and the
// generator defers to it. That is what makes adoption and retreat both one-step operations —
// deleting a JSON file hands it to the declaration, restoring it takes it back.

// Every class holding declarations. Each is initialised in a plain JVM and then asked for its static
// declare() if it has one, so nothing any of them touches may need a running game. A Reg-migrated
// registration class (com.hbm.registration.Reg) has no declare(): its registration chains record the
// declarations while its statics initialise, and Minecraft is on this classpath but never bootstrapped.
val DECLARATION_CLASSES = arrayOf(
    "com.hbm.items.ModItemModels",
    "com.hbm.inventory.material.ModMaterialTags",
    "com.hbm.lib.ModDamageTypes",
    "com.hbm.world.gen.ModBiomeModifiers",
    "com.hbm.inventory.recipes.ModCraftingRecipes",
    "com.hbm.inventory.recipes.ModShapedRecipes",
    "com.hbm.inventory.recipes.ModShapelessRecipes",
    "com.hbm.inventory.recipes.ModComponentRecipes",
    "com.hbm.inventory.recipes.ModContainerRecipes",
    "com.hbm.inventory.recipes.gen.CraftingRecipesGenerated",
    "com.hbm.blocks.ModSecurityBlocks",
    "com.hbm.items.ModSecurityItems",
    "com.hbm.blocks.ModDoorBlocks",
    // Reg-migrated registration classes (2026-10-01): initialising one records the declarations chained
    // onto its registrations. List the class itself; it needs no declare().
    "com.hbm.blocks.ModPowerGenBlocks",
    "com.hbm.items.ModPowerGenItems",
    "com.hbm.items.ModAchievementItems",
    "com.hbm.items.ModBedrockOreItems",
    "com.hbm.items.ModDungeonItems",
    "com.hbm.items.ModCustomMissileItems",
    "com.hbm.items.ModUtilityToolItems",
    "com.hbm.items.ModMeleeKitItems",
    "com.hbm.items.ModAbilityToolItems",
    "com.hbm.items.ModGunItems",
    "com.hbm.blocks.generic.ModStructureWandBlocks",
    "com.hbm.blocks.ModUtilityToolContent",
    "com.hbm.items.ModWandItems",
    "com.hbm.blocks.ModSatelliteBlocks",
    "com.hbm.items.ModSatelliteItems",
    "com.hbm.blocks.ModProductionBlocks",
    "com.hbm.blocks.ModProduction2Blocks",
    "com.hbm.blocks.ModRailBlocks",
    "com.hbm.blocks.ModRadarUtilityBlocks",
    "com.hbm.items.ModRadarUtilityItems",
    "com.hbm.blocks.machine.rbmk.ModRBMKBlocks",
    "com.hbm.items.ModRBMKItems",
    "com.hbm.blocks.machine.dfc.ModDFCBlocks",
    "com.hbm.blocks.machine.pile.ModPileBlocks",
    "com.hbm.blocks.machine.soyuz.ModSoyuzBlocks",
    "com.hbm.blocks.turret.ModTurretBlocks",
    "com.hbm.blocks.ModPwrBlocks",
    "com.hbm.items.ModPwrItems",
    "com.hbm.items.ModZirnoxItems",
    "com.hbm.items.ModWatzItems",
    "com.hbm.blocks.ModNuclearBlocks",
    "com.hbm.blocks.ModNuclearMiscBlocks",
    "com.hbm.items.ModNuclearMiscItems",
    "com.hbm.blocks.ModIcfBlocks",
    "com.hbm.items.ModIcfItems",
    "com.hbm.items.ModRtgContent",
    "com.hbm.blocks.ModEnergyNetBlocks",
    "com.hbm.blocks.ModEnergyBedrockBlocks",
    "com.hbm.radio.ModRadioBlocks",
    "com.hbm.blocks.ModOilBlocks",
    "com.hbm.items.ModOilItems",
    "com.hbm.blocks.ModDroneBlocks",
    "com.hbm.blocks.ModDroneContent",
    "com.hbm.blocks.ModPneumaticBlocks",
    "com.hbm.blocks.ModConveyorBlocks",
    "com.hbm.items.ModConveyorItems",
    "com.hbm.blocks.machine.ModMachineBlocks",
    "com.hbm.blocks.ModFluidNetBlocks",
    "com.hbm.items.ModFluidNetItems",
    "com.hbm.items.ModFluidItems",
    "com.hbm.blocks.ModExplosivesBlocks",
    "com.hbm.items.ModExplosivesItems",
    "com.hbm.blocks.ModBlocks",
    "com.hbm.items.ModItems",
    "com.hbm.blocks.ModHazardBlocks",
    "com.hbm.blocks.ModOutgasBlocks",
    "com.hbm.blocks.ModFalloutBlocks",
    "com.hbm.blocks.ModFallingBlocks",
    "com.hbm.blocks.ModGlassBlocks",
    "com.hbm.blocks.ModBeaconBlocks",
    "com.hbm.blocks.ModLadderBlocks",
    "com.hbm.blocks.ModAnvilBlocks",
    "com.hbm.blocks.ModSteelBlocks",
    "com.hbm.blocks.ModOreBlocks",
    "com.hbm.items.ModCatalystItems",
    "com.hbm.items.machine.ModStampItems",
    "com.hbm.items.ModFoodItems",
    "com.hbm.items.ModMedicineItems",
    "com.hbm.items.ModFuelItems",
    "com.hbm.items.machine.ModMoldItems",
    "com.hbm.items.ModIngredientItems",
    "com.hbm.items.ModIntermediateItems",
    "com.hbm.items.ModCasingItems",
    "com.hbm.items.ModAutogenItems",
    "com.hbm.items.material.FuelAdditiveItems",
    "com.hbm.items.ModConsumableTailItems",
    "com.hbm.items.ModChemicalDyeItems",
    "com.hbm.items.ModSingularityItems",
    "com.hbm.blocks.material.MaterialCompletionRegistrations",
    "com.hbm.blocks.ModDecoBlocks",
    "com.hbm.blocks.ModDecoContent",
    "com.hbm.blocks.ModTerrainBlocks",
    "com.hbm.items.ModTerrainItems",
    "com.hbm.blocks.TerrainDeclarations",
    "com.hbm.blocks.ModBombs2Blocks",
    "com.hbm.items.ModBombs2Items",
    "com.hbm.blocks.ModCustomMissileBlocks",
    "com.hbm.blocks.bomb.ModBombBlocks",
    "com.hbm.items.ModBombItems",
    "com.hbm.items.ModAmmoItems",
    "com.hbm.items.ModGunSupportItems",
    "com.hbm.items.ModWeaponModItems",
    "com.hbm.items.ModMissileItems",
    "com.hbm.items.ModToolItems",
    "com.hbm.items.ModJetpackDependencyItems",
    "com.hbm.items.ModEntityItems",
    "com.hbm.items.ModArmorModules",
    "com.hbm.items.ModArmorItems",
    "com.hbm.items.ModGearItems",
    "com.hbm.items.ModArmorSetItems",
    "com.hbm.items.ModArmorSetBItems",
    "com.hbm.blocks.ModArmorSetBBlocks",
    "com.hbm.items.ModArmorModItems",
    "com.hbm.smoke.SmokeChainRegistration",
    "com.hbm.blocks.CustomMissileDeclarations",
    "com.hbm.blocks.RadarUtilityDeclarations",
    "com.hbm.blocks.ModDecoCrateBlocks",
    "com.hbm.blocks.DecoCrateDeclarations",
    "com.hbm.blocks.EnergyBedrockDeclarations",
    "com.hbm.blocks.ModDungeonBlocks",
    "com.hbm.blocks.DungeonDeclarations",
    "com.hbm.items.AbilityToolDeclarations",
    "com.hbm.items.MeleeKitDeclarations",
    "com.hbm.items.ModFinalConsumableItems",
    "com.hbm.items.FinalConsumableDeclarations",
    "com.hbm.items.ArmorModDeclarations",
    "com.hbm.items.ArmorSetDeclarations",
    "com.hbm.items.ArmorSetBDeclarations",
    "com.hbm.items.UtilityToolDeclarations",
    "com.hbm.blocks.ModCustomMachineBlocks",
    "com.hbm.items.ModCustomMachineItems",
    "com.hbm.items.ModMachineItems",
    "com.hbm.blocks.machine.storage.ModBatteryBlocks",
    "com.hbm.blocks.machine.storage.ModStorageBlocks",
    "com.hbm.items.ModStorageDrumItems",
    "com.hbm.items.ModSelfchargerItems",
    "com.hbm.items.ModBatterySCItems",
    "com.hbm.items.ModUpgradeItems",
    "com.hbm.blocks.machine.DiFurnaceContent",
    "com.hbm.blocks.machine.AutosawFeature",
    "com.hbm.blocks.machine.ModArmorTableBlocks",
    "com.hbm.blocks.machine.ModWeaponTableBlocks",
    "com.hbm.blocks.machine.solar.ModSolarBlocks",
    "com.hbm.items.solar.ModSolarItems",
    "com.hbm.foundry.FoundryExpansion",
    "com.hbm.chemicalfactory.ChemicalFactoryRegistrations",
    "com.hbm.assemblyfactory.AssemblyFactoryRegistrations",
    "com.hbm.blocks.RBMKPeripheralDeclarations",
    "com.hbm.items.WandDeclarations",
    "com.hbm.blocks.SatelliteDeclarations",
    "com.hbm.items.GapsADeclarations",
    "com.hbm.world.gen.NTMStructureDeclarations",
    "com.hbm.world.gen.LegacyDungeonDeclarations",
    "com.hbm.advancement.HbmAdvancementDeclarations",
    // 2026-09-28 JSON migration (§19ed-dejson): the files that were still authored.
    "com.hbm.items.ObjItemDeclarations",
    "com.hbm.items.GunItemDeclarations",
    "com.hbm.items.ItemModelDeclarations",
    // BlockModelDeclarations (hand-built element geometry transliterated from JSON) was removed 2026-09-30;
    // those models are authored files again, listed in gradle/dejson-authored.txt.
    "com.hbm.items.ArmorEquipmentDeclarations",
    "com.hbm.items.FuelDataMapDeclarations",
    "com.hbm.lib.ModTagDeclarations",
    "com.hbm.world.biome.CraterBiomeDeclarations",
)

val authoredResources = rootProject.file("src/main/resources")
val generatedResources = layout.buildDirectory.dir("generated/dejson")

// Compiled classes, not the source set's output: the output includes processResources, and this
// generator feeds processResources. Naming compileJava's directory keeps the two from depending on
// each other. The runtimeClasspath configuration is dependencies only, which is where Golem Core's
// dejson classes come from.
val compiledClasses = tasks.named<JavaCompile>("compileJava").flatMap { it.destinationDirectory }
val dejsonClasspath: FileCollection = files(compiledClasses, configurations["runtimeClasspath"])

// Verification is the whole point and runs first: a declaration is only safe to delete a file for
// once it has been held against that file and matched. Once the file is gone there is nothing left
// to check, so the check has to happen while both still exist.
val dejsonVerify = tasks.register<JavaExec>("dejsonVerify") {
    group = "verification"
    description = "Checks every code-declared model against the JSON file it stands in for."
    dependsOn(tasks.named("compileJava"))
    classpath = dejsonClasspath
    mainClass = "dev.golem.core.dejson.DejsonMain"
    args("verify", generatedResources.get().asFile.absolutePath, authoredResources.absolutePath,
        *DECLARATION_CLASSES)
}

val dejsonGenerate = tasks.register<JavaExec>("dejsonGenerate") {
    group = "build"
    description = "Emits the code-declared models, blockstates and item definitions as JSON."
    dependsOn(tasks.named("compileJava"))
    inputs.dir(authoredResources)
    inputs.files(compiledClasses)
    outputs.dir(generatedResources)
    classpath = dejsonClasspath
    mainClass = "dev.golem.core.dejson.DejsonMain"
    args("write", generatedResources.get().asFile.absolutePath, authoredResources.absolutePath,
        *DECLARATION_CLASSES)
}

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    dependsOn(dejsonGenerate)
    from(generatedResources)
}

tasks.named("check") { dependsOn(dejsonVerify) }

// Emits every declaration regardless of what is authored, into a scratch directory that nothing
// packs. This is how you find out what a declaration actually produces without deleting its file
// first, and it is how the 5084 files the declarations took over were identified: generate
// everything, intersect with src/main/resources, compare, then delete the intersection.
tasks.register<JavaExec>("dejsonGenerateAll") {
    group = "verification"
    description = "Emits every code-declared file into build/generated/dejson-all, ignoring the authored resources."
    dependsOn(tasks.named("compileJava"))
    classpath = dejsonClasspath
    mainClass = "dev.golem.core.dejson.DejsonMain"
    args("write", layout.buildDirectory.dir("generated/dejson-all").get().asFile.absolutePath,
        layout.buildDirectory.dir("generated/dejson-nothing").get().asFile.absolutePath,
        *DECLARATION_CLASSES)
}

// Every JSON file in these resource folders is a declaration's output. A hand-written file here is not an
// addition — it silently replaces whatever a declaration emits at that path ("file wins", above), which is
// how 1,351 files once drifted out of reach of dejsonVerify. So an authored one fails the build instead,
// naming the declaration vocabulary to use. The folders are the ones port-tools/generator_safety.py
// refuses to write into; keep the two lists in step. Exempt: the Blender-exported animation and scene data
// under models/ (not models at all), and anything listed in DEJSON_AUTHORED_EXCEPTIONS — which is the
// deliberate, reviewed way to take one file back from a declaration.
val DEJSON_OWNED_FOLDERS = listOf(
    "assets/*/items", "assets/*/models", "assets/*/blockstates", "assets/*/equipment", "assets/*/atlases",
    "data/*/tags", "data/*/loot_table", "data/*/recipe", "data/*/advancement", "data/*/damage_type",
    "data/*/worldgen", "data/*/data_maps", "data/*/jukebox_song", "data/*/neoforge/biome_modifier",
)
val DEJSON_UNOWNED_MODELS = listOf("assets/hbm/models/anim/", "assets/hbm/models/doors/", "assets/hbm/models/weapons/")
// Dejson declares uniform boilerplate only (sprite items, cube_all-style parent+texture blocks, simple
// blockstates, loot, tags, recipes). Hand-tuned JSON — custom `elements` geometry, display transforms, one-off
// OBJ item definitions — stays an authored file (rule of 2026-09-30, History §19eg-dejson-authored), listed in
// gradle/dejson-authored.txt: one resource-relative path per line, a trailing `/` exempts a whole folder.
// port-tools/generator_safety.py reads the same file and keeps generators out of those paths. A listed path
// that no longer exists fails the check, so the list cannot rot.
val dejsonAuthoredManifest = rootProject.file("gradle/dejson-authored.txt")
val DEJSON_AUTHORED_EXCEPTIONS: Set<String> = if (!dejsonAuthoredManifest.isFile) emptySet() else
    dejsonAuthoredManifest.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()

val dejsonOwnership = tasks.register("dejsonOwnership") {
    group = "verification"
    description = "Fails when a JSON file is hand-written where Dejson declarations own the folder."
    inputs.dir(authoredResources)
    if (dejsonAuthoredManifest.isFile) inputs.file(dejsonAuthoredManifest)
    doLast {
        val root = authoredResources
        val stale = DEJSON_AUTHORED_EXCEPTIONS.filter { !File(root, it).exists() }.sorted()
        check(stale.isEmpty()) {
            "${stale.size} path(s) in gradle/dejson-authored.txt do not exist under src/main/resources:\n  " +
                stale.joinToString("\n  ")
        }
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filter { path ->
                DEJSON_AUTHORED_EXCEPTIONS.none { path == it || (it.endsWith("/") && path.startsWith(it)) } &&
                    DEJSON_UNOWNED_MODELS.none { path.startsWith(it) } &&
                    DEJSON_OWNED_FOLDERS.any { folder ->
                        val pattern = folder.split('/')
                        val parts = path.split('/')
                        parts.size > pattern.size && pattern.indices.all { pattern[it] == "*" || pattern[it] == parts[it] }
                    }
            }
            .sorted().toList()
        check(offenders.isEmpty()) {
            "${offenders.size} JSON file(s) are hand-written in folders Dejson owns; declare uniform boilerplate " +
                "(Dejson.block/item/model, States, ItemModels, Tags, Drops, Equipment, ...), or list hand-tuned " +
                "geometry/display transforms in gradle/dejson-authored.txt:\n  " + offenders.take(30).joinToString("\n  ") +
                (if (offenders.size > 30) "\n  ... and ${offenders.size - 30} more" else "")
        }
    }
}
dejsonGenerate.configure { dependsOn(dejsonOwnership) }
dejsonVerify.configure { dependsOn(dejsonOwnership) }
