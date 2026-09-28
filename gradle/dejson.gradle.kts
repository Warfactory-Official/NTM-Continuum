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

// Every class holding declarations. Each is asked for its static declare() in a plain JVM, so
// nothing any of them touches may reach a Minecraft class.
val DECLARATION_CLASSES = arrayOf(
    "com.hbm.blocks.ModBlockModels",
    "com.hbm.items.ModItemModels",
    "com.hbm.blocks.ModBlockDrops",
    "com.hbm.inventory.material.ModMaterialTags",
    "com.hbm.lib.ModDamageTypes",
    "com.hbm.world.gen.ModBiomeModifiers",
    "com.hbm.inventory.recipes.ModCraftingRecipes",
    "com.hbm.inventory.recipes.ModShapedRecipes",
    "com.hbm.inventory.recipes.ModShapelessRecipes",
    "com.hbm.inventory.recipes.ModComponentRecipes",
    "com.hbm.inventory.recipes.ModContainerRecipes",
    "com.hbm.inventory.recipes.gen.CraftingRecipesGenerated",
    "com.hbm.blocks.FluidNetDeclarations",
    "com.hbm.blocks.ConveyorDeclarations",
    "com.hbm.blocks.ProductionDeclarations",
    "com.hbm.blocks.Production2Declarations",
    "com.hbm.blocks.SecurityDeclarations",
    "com.hbm.blocks.DoorDeclarations",
    "com.hbm.blocks.PneumaticDeclarations",
    "com.hbm.blocks.PwrDeclarations",
    "com.hbm.blocks.DroneDeclarations",
    "com.hbm.blocks.EnergyNetDeclarations",
    "com.hbm.blocks.PowerGenDeclarations",
    "com.hbm.blocks.NuclearMiscDeclarations",
    "com.hbm.blocks.ExplosivesDeclarations",
    "com.hbm.blocks.DecoDeclarations",
    "com.hbm.blocks.TerrainDeclarations",
    "com.hbm.blocks.Bombs2Declarations",
    "com.hbm.blocks.CustomMissileDeclarations",
    "com.hbm.blocks.RadarUtilityDeclarations",
    "com.hbm.blocks.DecoCrateDeclarations",
    "com.hbm.blocks.IcfDeclarations",
    "com.hbm.blocks.EnergyBedrockDeclarations",
    "com.hbm.blocks.DungeonDeclarations",
    "com.hbm.blocks.NorwoodMachineDeclarations",
    "com.hbm.blocks.RailDeclarations",
    "com.hbm.items.AbilityToolDeclarations",
    "com.hbm.items.MeleeKitDeclarations",
    "com.hbm.items.FinalConsumableDeclarations",
    "com.hbm.items.ArmorModDeclarations",
    "com.hbm.items.ArmorSetDeclarations",
    "com.hbm.items.ArmorSetBDeclarations",
    "com.hbm.items.UtilityToolDeclarations",
    "com.hbm.blocks.CustomMachineDeclarations",
    "com.hbm.blocks.RBMKPeripheralDeclarations",
    "com.hbm.items.WandDeclarations",
    "com.hbm.blocks.SatelliteDeclarations",
    "com.hbm.items.GapsADeclarations",
    "com.hbm.items.RenderInputDeclarations",
    "com.hbm.blocks.StructureWandDeclarations",
    "com.hbm.world.gen.NTMStructureDeclarations",
    "com.hbm.world.gen.LegacyDungeonDeclarations",
    "com.hbm.advancement.HbmAdvancementDeclarations",
    // 2026-09-28 JSON migration (§19ed-dejson): the files that were still authored.
    "com.hbm.items.SpriteItemDeclarations",
    "com.hbm.items.ItemDefinitionDeclarations",
    "com.hbm.items.ObjItemDeclarations",
    "com.hbm.items.GunItemDeclarations",
    "com.hbm.items.ItemModelDeclarations",
    "com.hbm.blocks.BlockModelDeclarations",
    "com.hbm.blocks.BlockStateDeclarations",
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
val DEJSON_AUTHORED_EXCEPTIONS = setOf<String>()

val dejsonOwnership = tasks.register("dejsonOwnership") {
    group = "verification"
    description = "Fails when a JSON file is hand-written where Dejson declarations own the folder."
    inputs.dir(authoredResources)
    doLast {
        val root = authoredResources
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .filter { path ->
                path !in DEJSON_AUTHORED_EXCEPTIONS && DEJSON_UNOWNED_MODELS.none { path.startsWith(it) } &&
                    DEJSON_OWNED_FOLDERS.any { folder ->
                        val pattern = folder.split('/')
                        val parts = path.split('/')
                        parts.size > pattern.size && pattern.indices.all { pattern[it] == "*" || pattern[it] == parts[it] }
                    }
            }
            .sorted().toList()
        check(offenders.isEmpty()) {
            "${offenders.size} JSON file(s) are hand-written in folders Dejson owns; declare them instead " +
                "(Dejson.block/item/model, States, ItemModels, Tags, Drops, Equipment, ...) or list a deliberate " +
                "exception in DEJSON_AUTHORED_EXCEPTIONS:\n  " + offenders.take(30).joinToString("\n  ") +
                (if (offenders.size > 30) "\n  ... and ${offenders.size - 30} more" else "")
        }
    }
}
dejsonGenerate.configure { dependsOn(dejsonOwnership) }
dejsonVerify.configure { dependsOn(dejsonOwnership) }
