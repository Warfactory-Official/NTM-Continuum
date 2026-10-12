package com.hbm.gametest;

import com.hbm.blocks.material.BlockConcreteColored;
import com.hbm.blocks.material.BlockConcreteColoredExt;
import com.hbm.blocks.material.BlockLightstone;
import com.hbm.blocks.material.MaterialCompletionRegistrations;
import com.hbm.blocks.generic.HbmStairBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFuelItems;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.special.ItemAutogen;
import com.hbm.items.material.FuelAdditiveItems;
import com.hbm.inventory.recipes.material.FuelAdditiveRecipes;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.machine.ModStampItems;
import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.hbm.inventory.fluid.Fluids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Focused contracts for the isolated material, colored-block, and fuel-additive completion package. */
public final class MaterialCompletionGameTests {

    private MaterialCompletionGameTests() {}

    /** Forward this feature-local suite from each loader; HbmGameTestFunctions remains untouched. */
    public static final GameTests.Suite TESTS = GameTests.suite("hbm");
    public static final GameTests.Function ENUM_ORDERS_AND_REGISTRY =
            TESTS.function("material_enum_orders_and_registry", 40,
                    () -> MaterialCompletionGameTests::enumOrdersCreativeAndPlacement);
    public static final GameTests.Function COLORED_STAIR_IDS_AND_TEXTURES =
            TESTS.function("material_colored_stair_ids_and_textures", 40,
                    () -> MaterialCompletionGameTests::coloredStairIdsAndTextures);
    public static final GameTests.Function LIGHTSTONE_ROUND_TRIP_AND_LIGHT =
            TESTS.function("material_lightstone_round_trip_and_light", 40,
                    () -> MaterialCompletionGameTests::lightstoneRoundTripAndLight);
    public static final GameTests.Function MATERIAL_IDENTITY_AND_SMELTING =
            TESTS.function("material_identity_and_smelting", 40,
                    () -> MaterialCompletionGameTests::materialIdentityAndSmelting);
    public static final GameTests.Function FUEL_ADDITIVE_VARIANTS =
            TESTS.function("material_fuel_additive_variants", 40,
                    () -> MaterialCompletionGameTests::fuelAdditiveVariants);
    public static void init() {}

    public static void enumOrdersCreativeAndPlacement(GameTestHelper helper) {
        helper.assertTrue(List.of(DyeColor.values()).equals(List.of(
                        DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
                        DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY,
                        DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE,
                        DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK)),
                "Concrete colour order no longer matches legacy metadata");
        helper.assertTrue(BlockConcreteColoredExt.EnumConcreteType.values().length == 8,
                "Extended concrete must retain eight variants");
        helper.assertTrue(BlockLightstone.LightstoneType.values().length == 5,
                "Lightstone must retain five variants");
        helper.assertTrue(MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.size() == 16
                        && MaterialCompletionRegistrations.CONCRETE_COLORED_EXT_STAIRS.size() == 8,
                "Flattened colored stair count changed");
        BlockConcreteColored block = MaterialCompletionRegistrations.CONCRETE_COLORED.get();
        for (int variant = 0; variant < DyeColor.values().length; variant++) {
            ItemStack stack = MaterialCompletionRegistrations.concreteColoredItem(variant);
            helper.assertTrue(com.hbm.items.LegacyMeta.of(stack) == variant,
                    "Concrete item lost variant " + variant);
            helper.assertTrue(BlockConcreteColored.variant(block.stateForItem(stack)) == variant,
                    "Concrete placement changed variant " + variant);
            ItemStack clone = block.pickBlockStack((net.minecraft.world.level.BlockGetter) null,
                    BlockPos.ZERO, block.stateForVariant(variant));
            helper.assertTrue(ItemStack.isSameItem(clone, stack),
                    "Concrete clone lost variant " + variant);
            ItemStack drop = block.getDrops(block.stateForVariant(variant), null).get(0);
            helper.assertTrue(ItemStack.isSameItem(drop, stack),
                    "Concrete drop lost variant " + variant);
        }
        helper.succeed();
    }

    public static void coloredStairIdsAndTextures(GameTestHelper helper) {
        List<String> colours = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black");
        for (int i = 0; i < colours.size(); i++) {
            RegistryObject<Block> stair = MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.get(i);
            helper.assertTrue(stair.getId().equals(Ids.of("hbm", "concrete_colored_stairs_" + colours.get(i))),
                    "Wrong colored stair id at " + i);
            helper.assertTrue(MaterialCompletionRegistrations.concreteColoredStairTexture(i)
                            .equals("hbm:blocks/concrete_" + colours.get(i)),
                    "Wrong colored stair texture at " + i);
            helper.assertTrue(stair.get() instanceof HbmStairBlock, "Colored stair is not a vanilla stair block");
        }
        for (int i = 0; i < BlockConcreteColoredExt.EnumConcreteType.values().length; i++) {
            String name = BlockConcreteColoredExt.EnumConcreteType.values()[i].getSerializedName();
            helper.assertTrue(MaterialCompletionRegistrations.CONCRETE_COLORED_EXT_STAIRS.get(i).getId()
                            .equals(Ids.of("hbm", "concrete_colored_ext_stairs_" + name)),
                    "Wrong extended stair id at " + i);
            helper.assertTrue(MaterialCompletionRegistrations.concreteColoredExtStairTexture(i)
                            .equals("hbm:blocks/concrete_colored_ext." + name),
                    "Wrong extended stair texture at " + i);
        }
        helper.assertTrue(MaterialCompletionRegistrations.lightstoneStairTexture(false)
                        .equals("hbm:blocks/lightstone.tile")
                        && MaterialCompletionRegistrations.lightstoneStairTexture(true)
                        .equals("hbm:blocks/lightstone.bricks"),
                "Wrong lightstone stair texture mapping");
        helper.succeed();
    }

    public static void lightstoneRoundTripAndLight(GameTestHelper helper) {
        BlockLightstone block = MaterialCompletionRegistrations.LIGHTSTONE.get();
        for (int variant = 0; variant < BlockLightstone.LightstoneType.values().length; variant++) {
            ItemStack stack = MaterialCompletionRegistrations.lightstoneItem(variant);
            helper.assertTrue(BlockLightstone.variant(block.stateForItem(stack)) == variant,
                    "Lightstone placement changed variant " + variant);
            helper.assertTrue(block.stateForVariant(variant).getLightEmission() == 0,
                    "Legacy lightstone variant unexpectedly emits light: " + variant);
            ItemStack clone = block.pickBlockStack((net.minecraft.world.level.BlockGetter) null,
                    BlockPos.ZERO, block.stateForVariant(variant));
            helper.assertTrue(ItemStack.isSameItem(clone, stack),
                    "Lightstone clone lost variant " + variant);
            ItemStack drop = block.getDrops(block.stateForVariant(variant), null).get(0);
            helper.assertTrue(ItemStack.isSameItem(drop, stack),
                    "Lightstone drop lost variant " + variant);
        }
        helper.succeed();
    }

    public static void materialIdentityAndSmelting(GameTestHelper helper) {
        helper.assertTrue(Mats.MAT_IRON.id == 2600 && Mats.MAT_IRON.name.equals("iron"),
                "Iron material identity changed");
        List<Mats.MaterialStack> iron = Mats.getSmeltingMaterialsFromItem(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(iron.size() == 1 && iron.get(0).material == Mats.MAT_IRON
                        && iron.get(0).amount == 72,
                "Iron ingot smelting sample changed");
        assertExactMaterial(helper, new ItemStack(ModFuelItems.lignite.get()), Mats.MAT_CARBON, 24, 1);
        assertExactMaterial(helper, new ItemStack(ModFuelItems.powder_lignite.get()), Mats.MAT_CARBON, 24, 1);
        assertExactMaterial(helper, new ItemStack(ModFuelItems.powder_coal.get()), Mats.MAT_CARBON, 36, 1);
        assertExactMaterial(helper, new ItemStack(ModFuelItems.powder_coal_tiny.get()), Mats.MAT_CARBON, 4, 1);
        assertExactMaterial(helper, new ItemStack(ModFuelItems.ingot_graphite.get()), Mats.MAT_CARBON, 72, 1);
        assertExactMaterial(helper, new ItemStack(ModItems.powder_flux.get()), Mats.MAT_FLUX, 72, 2);
        helper.assertTrue(Mats.MAT_COAL.smeltsInto == Mats.MAT_CARBON
                        && Mats.MAT_COAL.convIn == 2 && Mats.MAT_COAL.convOut == 1,
                "Coal-to-carbon conversion sample changed");
        helper.succeed();
    }

    public static void fuelAdditiveVariants(GameTestHelper helper) {
        helper.assertTrue(FuelAdditiveItems.Variant.values().length == 2,
                "Fuel additive must retain exactly two variants");
        for (FuelAdditiveItems.Variant variant : FuelAdditiveItems.Variant.values()) {
            var item = FuelAdditiveItems.get(variant);
            helper.assertTrue(item.getClass().equals(com.hbm.items.material.FuelAdditiveItem.class),
                    "Fuel additive is not a plain feature item");
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(Ids.of("hbm", variant.id)),
                    "Wrong fuel additive id for " + variant);
        }
        helper.assertTrue(FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK)
                        != FuelAdditiveItems.get(FuelAdditiveItems.Variant.DEICER),
                "Flattened fuel additives must be distinct items");
        // the two chemical plant rows are datapack recipes since §19fa-m1 (data/hbm/recipe/chemical_plant/tel, deicer)
        com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE.registerRecipes();
        var tel = com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE.getRecipe("chem.tel");
        var deicer = com.hbm.inventory.recipes.ChemicalPlantRecipes.INSTANCE.getRecipe("chem.deicer");
        helper.assertTrue(tel != null && deicer != null
                        && tel.outputItem[0].getItem() == FuelAdditiveItems.get(FuelAdditiveItems.Variant.ANTIKNOCK)
                        && deicer.outputItem[0].getItem() == FuelAdditiveItems.get(FuelAdditiveItems.Variant.DEICER),
                "Fuel additive chemical recipe IDs changed");
        var mixer = FuelAdditiveRecipes.mixerRecipes();
        helper.assertTrue(mixer.size() == 3
                        && mixer.get(0).output() == com.hbm.inventory.fluid.Fluids.PETROIL_LEADED
                        && mixer.get(1).output() == com.hbm.inventory.fluid.Fluids.GASOLINE_LEADED
                        && mixer.get(2).output() == com.hbm.inventory.fluid.Fluids.COALGAS_LEADED
                        && mixer.get(0).input() == com.hbm.inventory.fluid.Fluids.PETROIL
                        && mixer.get(1).input() == com.hbm.inventory.fluid.Fluids.GASOLINE
                        && mixer.get(2).input() == com.hbm.inventory.fluid.Fluids.COALGAS
                        && mixer.stream().allMatch(recipe ->
                        recipe.additive() == FuelAdditiveItems.Variant.ANTIKNOCK
                                && recipe.outputAmount() == 12_000
                                && recipe.inputAmount() == 10_000
                                && recipe.processTime() == 40),
                "Leaded Mixer recipes changed or use the wrong additive variant");
        helper.succeed();
    }

    public static void materialCatalogueComplete(GameTestHelper helper) {
        helper.assertTrue(Mats.orderedList.size() == 108 && Mats.matById.size() == 108,
                "Material catalogue must contain 108 unique ordered/id entries");
        Set<String> names = new HashSet<>();
        for (NTMMaterial material : Mats.orderedList) {
            helper.assertTrue(names.add(material.name), "Duplicate material name " + material.name);
            helper.assertTrue(Mats.matById.get(material.id) == material
                            && Mats.matByName.get(material.name) == material,
                    "Material lookup lost " + material.name);
        }
        helper.assertTrue(names.size() == 108 && Mats.matByName.size() == 110,
                "Canonical material names or compatibility aliases changed");
        helper.assertTrue(Mats.byName("phosphorus") == Mats.MAT_PHOSPHORUS
                        && Mats.byName("th232") == Mats.MAT_THORIUM,
                "Legacy material-name aliases no longer resolve");

        String[][] displayNames = {
                {"wood", "Wood"}, {"iron", "Iron"}, {"gold", "Gold"}, {"titanium", "Titanium"},
                {"aluminium", "Aluminium"}, {"steel", "Steel"}, {"lead", "Lead"}, {"copper", "Copper"},
                {"gunmetal", "Gunmetal"}, {"weaponsteel", "Weapon Steel"}, {"combine_steel", "Combine Steel"},
                {"dura_steel", "High-Speed Steel"}, {"saturnite", "Saturnite"}, {"schrabidium", "Schrabidium"},
                {"desh", "Desh"}, {"red_copper", "Minecraft Grade Copper"},
                {"magnetized_tungsten", "Magnetized Tungsten"}, {"tungsten", "Tungsten"},
                {"beryllium", "Beryllium"}, {"cobalt", "Cobalt"}, {"niobium", "Niobium"},
                {"zirconium", "Zirconium"}, {"bismuth", "Bismuth"}, {"boron", "Boron"},
                {"arsenic", "Arsenic"}, {"neodymium", "Neodymium"}, {"lanthanium", "Lanthanium"},
                {"cadmium", "Cadmium"}, {"asbestos", "Asbestos"}, {"uranium", "Uranium"},
                {"thorium", "Thorium-232"}, {"plutonium", "Plutonium"}, {"neptunium", "Neptunium-237"},
                {"polonium", "Polonium-210"}, {"actinium", "Actinium-227"}, {"u233", "Uranium-233"},
                {"u235", "Uranium-235"}, {"u238", "Uranium-238"}, {"pu238", "Plutonium-238"},
                {"pu239", "Plutonium-239"}, {"pu240", "Plutonium-240"}, {"pu241", "Plutonium-241"},
                {"am241", "Americium-241"}, {"am242", "Americium-242"},
                {"technetium", "Technetium-99"}, {"ra226", "Radium-226"}, {"co60", "Cobalt-60"},
                {"au198", "Gold-198"}, {"pb209", "Lead-209"}, {"solinium", "Solinium"},
                {"schrabidate", "Ferric Schrabidate"}, {"schraranium", "Schraranium"},
                {"gh336", "Ghiorsium-336"}, {"tantalium", "Tantalum"}, {"silicon", "Silicon"},
                {"osmiridium", "Osmiridium"}, {"calcium", "Calcium"}, {"sodium", "Sodium"},
                {"strontium", "Strontium"}, {"lithium", "Lithium"}, {"sulfur", "Sulfur"},
                {"fluorite", "Fluorite"}, {"red_phosphorus", "Red Phosphorus"},
                {"chlorocalcite", "Chlorocalcite"}, {"molysite", "Molysite"}, {"cinnabar", "Cinnabar"},
                {"borax", "Borax"}, {"sodalite", "Sodalite"}, {"carbon", "Carbon"},
                {"graphite", "Graphite"}, {"starmetal", "Starmetal"}, {"ferrouranium", "Ferrouranium"},
                {"tcalloy", "Technetium Steel"}, {"cdalloy", "Cadmium Steel"},
                {"bismuth_bronze", "Bismuth Bronze"}, {"arsenic_bronze", "Arsenic Bronze"},
                {"bscco", "BSCCO"}, {"dineutronium", "Dineutronium"}, {"slag", "Slag"}, {"flux", "Flux"},
                {"polymer", "Polymer"}, {"bakelite", "Bakelite"}, {"rubber", "Rubber"},
                {"pc", "Hard Plastic"}, {"pvc", "PVC"}, {"stone", "Stone"}, {"redstone", "Redstone"},
                {"obsidian", "Obsidian"}, {"coal", "Coal"}, {"lignite", "Lignite"}, {"bone", "Ivory"},
                {"coal_coke", "Coal Coke"}, {"pet_coke", "Petroleum Coke"}, {"lignite_coke", "Lignite Coke"},
                {"diamond", "Diamond"}, {"hematite", "Hematite"}, {"wrought_iron", "Wrought Iron"},
                {"pig_iron", "Pig Iron"}, {"meteoric_iron", "Meteoric Iron"}, {"malachite", "Malachite"},
                {"bauxite", "Bauxite"}, {"cryolite", "Cryolite"}, {"pu_mix", "Reactor-Grade Plutonium"},
                {"am_mix", "Reactor-Grade Americium"}, {"emerald", "Emerald"}, {"niter", "Niter"},
                {"mud", "Poisonous Mud"}, {"rare", "Rare Earth"}
        };
        helper.assertTrue(displayNames.length == 108, "Legacy material display-name table changed");
        for (String[] entry : displayNames) {
            NTMMaterial material = Mats.byName(entry[0]);
            helper.assertTrue(material != null && material.getDisplayName().equals(entry[1]),
                    "Wrong display name for " + entry[0]);
        }

        List<NTMMaterial> completion = List.of(
                Mats.MAT_IVORY, Mats.MAT_COALCOKE, Mats.MAT_PETCOKE, Mats.MAT_LIGCOKE,
                Mats.MAT_DIAMOND, Mats.MAT_HEMATITE, Mats.MAT_WROUGHTIRON, Mats.MAT_PIGIRON,
                Mats.MAT_METEORICIRON, Mats.MAT_MALACHITE, Mats.MAT_BAUXITE, Mats.MAT_CRYOLITE,
                Mats.MAT_RGP, Mats.MAT_RGA, Mats.MAT_EMERALD, Mats.MAT_KNO, Mats.MAT_MUD,
                Mats.MAT_RAREEARTH);
        int[] ids = {4, 610, 611, 612, 1430, 2601, 2602, 2603, 2604, 2901, 2902, 2903,
                9401, 9501, 401, 700, 44, 20000};
        for (int i = 0; i < completion.size(); i++) {
            NTMMaterial material = completion.get(i);
            helper.assertTrue(material.id == ids[i] && Mats.matById.get(ids[i]) == material,
                    "Wrong completion material identity at index " + i);
        }
        helper.succeed();
    }

    public static void materialAutogenShapesComplete(GameTestHelper helper) {
        helper.assertTrue(ModAutogenItems.registeredCount() == 188,
                "Expected 188 autogen items, found " + ModAutogenItems.registeredCount());
        helper.assertTrue(ModAutogenItems.shapeOrder().equals(List.of(
                        MaterialShapes.SHELL, MaterialShapes.PIPE, MaterialShapes.INGOT,
                        MaterialShapes.CASTPLATE, MaterialShapes.WELDEDPLATE, MaterialShapes.HEAVY_COMPONENT,
                        MaterialShapes.WIRE, MaterialShapes.DENSEWIRE, MaterialShapes.BOLT,
                        MaterialShapes.LIGHTBARREL, MaterialShapes.HEAVYBARREL,
                        MaterialShapes.LIGHTRECEIVER, MaterialShapes.HEAVYRECEIVER,
                        MaterialShapes.MECHANISM, MaterialShapes.STOCK, MaterialShapes.GRIP,
                        MaterialShapes.FRAGMENT)),
                "ItemAutogen shape declaration order changed");
        ItemAutogen lightSteel = ModAutogenItems.get(MaterialShapes.LIGHTBARREL, Mats.MAT_STEEL);
        ItemAutogen ivoryGrip = ModAutogenItems.get(MaterialShapes.GRIP, Mats.MAT_IVORY);
        helper.assertTrue(lightSteel != null && ivoryGrip != null
                        && lightSteel.getName(new ItemStack(lightSteel)).getString().equals("Light Steel Barrel")
                        && ivoryGrip.getName(new ItemStack(ivoryGrip)).getString().equals("Ivory Grip"),
                "ItemAutogen legacy shape templates changed");
        Set<String> ids = new HashSet<>();
        int count = 0;
        for (NTMMaterial material : Mats.orderedList) {
            for (MaterialShapes shape : MaterialShapes.values()) {
                if (!material.autogen.contains(shape)) continue;
                String base = autogenBase(shape);
                if (base == null) continue;
                var item = ModAutogenItems.get(shape, material);
                helper.assertTrue(item != null && item.shape == shape && item.material == material,
                        "Missing autogen registration for " + shape + "/" + material.name);
                if (item == null) continue;
                String id = BuiltInRegistries.ITEM.getKey(item).getPath();
                helper.assertTrue(ids.add(id) && id.equals(base + "_" + material.name),
                        "Wrong or duplicate autogen id " + id);
                count++;
            }
        }
        helper.assertTrue(count == 188 && ids.size() == 188,
                "Autogen catalogue identity count changed: " + count);
        helper.succeed();
    }

    public static void materialDistributionComplete(GameTestHelper helper) {
        assertMaterial(helper, new ItemStack(ModStampItems.stamp_stone_flat.get()), Mats.MAT_STONE, 216);
        assertMaterial(helper, new ItemStack(ModStampItems.stamp_iron_flat.get()), Mats.MAT_IRON, 216);
        assertMaterial(helper, new ItemStack(ModStampItems.stamp_steel_flat.get()), Mats.MAT_STEEL, 216);
        assertMaterial(helper, new ItemStack(ModStampItems.stamp_titanium_flat.get()), Mats.MAT_TITANIUM, 216);
        assertMaterial(helper, new ItemStack(ModStampItems.stamp_obsidian_flat.get()), Mats.MAT_OBSIDIAN, 216);
        assertExactDistribution(helper, new ItemStack(ModIntermediateItems.chunk(EnumChunkType.CRYOLITE)),
                new Mats.MaterialStack(Mats.MAT_ALUMINIUM, 72), new Mats.MaterialStack(Mats.MAT_SODIUM, 72));
        assertMaterial(helper, new ItemStack(ModOreBlocks.stone_resource_limestone.get().asItem()), Mats.MAT_FLUX, 720);
        assertMaterial(helper, new ItemStack(ModIngredientItems.ash(EnumAshType.WOOD)), Mats.MAT_CARBON, 8);
        assertMaterial(helper, new ItemStack(ModIngredientItems.ash(EnumAshType.COAL)), Mats.MAT_CARBON, 16);
        assertMaterial(helper, new ItemStack(ModIngredientItems.ash(EnumAshType.MISC)), Mats.MAT_CARBON, 8);
        assertMaterial(helper, new ItemStack(ModOreBlocks.stone_resource_hematite.get().asItem()), Mats.MAT_HEMATITE, 72);
        assertMaterial(helper, new ItemStack(ModOreBlocks.stone_resource_malachite.get().asItem()), Mats.MAT_MALACHITE, 432);
        helper.succeed();
    }

    public static void materialCruciblePressCrystallizerComplete(GameTestHelper helper) {
        CrucibleRecipes.register();
        helper.assertTrue(CrucibleRecipes.INSTANCE.recipeOrderedList.size() == 13,
                "Crucible catalogue must contain 13 recipes");
        var hematite = (CrucibleRecipes.CrucibleRecipe) CrucibleRecipes.INSTANCE.getRecipe("crucible.hematite");
        var malachite = (CrucibleRecipes.CrucibleRecipe) CrucibleRecipes.INSTANCE.getRecipe("crucible.malachite");
        var cmb = (CrucibleRecipes.CrucibleRecipe) CrucibleRecipes.INSTANCE.getRecipe("crucible.cmb");
        helper.assertTrue(hematite.frequency == 6 && hematite.input[0].material == Mats.MAT_HEMATITE
                        && hematite.input[0].amount == 144 && hematite.output[0].material == Mats.MAT_IRON
                        && hematite.output[0].amount == 72 && hematite.output[1].material == Mats.MAT_SLAG
                        && hematite.output[1].amount == 24,
                "Hematite crucible recipe changed");
        helper.assertTrue(malachite.frequency == 6 && malachite.input[0].material == Mats.MAT_MALACHITE
                        && malachite.input[0].amount == 144 && malachite.output[0].material == Mats.MAT_COPPER
                        && malachite.output[0].amount == 72 && malachite.output[1].amount == 24,
                "Malachite crucible recipe changed");
        helper.assertTrue(cmb.frequency == 3 && cmb.input[0].material == Mats.MAT_MAGTUNG
                        && cmb.input[0].amount == 48 && cmb.input[1].material == Mats.MAT_MUD
                        && cmb.input[1].amount == 24 && cmb.output[0].material == Mats.MAT_CMB
                        && cmb.output[0].amount == 72,
                "Combine-steel crucible recipe changed");

        assertPress(helper, new ItemStack(ModItems.powder_quartz.get()), ModStampItems.stamp_stone_flat.get(), Items.QUARTZ, 1);
        assertPress(helper, new ItemStack(ModItems.powder_lapis.get()), ModStampItems.stamp_stone_flat.get(), Items.LAPIS_LAZULI, 1);
        assertPress(helper, new ItemStack(ModItems.powder_diamond.get()), ModStampItems.stamp_stone_flat.get(), Items.DIAMOND, 1);
        assertPress(helper, new ItemStack(ModItems.powder_emerald.get()), ModStampItems.stamp_stone_flat.get(), Items.EMERALD, 1);
        assertPress(helper, new ItemStack(ModFuelItems.biomass.get()), ModStampItems.stamp_stone_flat.get(), ModFuelItems.biomass_compressed.get(), 1);
        assertPress(helper, new ItemStack(ModOilItems.coke(EnumCokeType.COAL)), ModStampItems.stamp_stone_flat.get(), ModFuelItems.ingot_graphite.get(), 1);
        assertPress(helper, new ItemStack(Blocks.JUNGLE_LOG), ModStampItems.stamp_stone_flat.get(), ModFuelItems.ball_resin.get(), 1);
        assertPress(helper, new ItemStack(ModItems.billet_silicon.get()), ModStampItems.stamp_stone_circuit.get(),
                ModIntermediateItems.circuit(EnumCircuitType.SILICON), 1);

        CrystallizerRecipes.register();
        // 82 dependency-complete defaults (79 + the two 1.7 mercury recipes + mustard willow -> cadmium; the
        // three tag-gated compat recipes stay absent without another mod), + the rebar -> concrete rebar recipe, + the etched meteorite sword,
        // + 1.7's bedrock-ore washing/acid grades: 37 per ore type (2 washed, 2 sulfuric, 3 solvent, 4 rad,
        // 3+3+3 byproduct washes, 8 hydrogen, 8 chlorine, 1 slop crumb recovery) over the six types.
        // + the 23 plastic-scrap -> circuit-piece recipes (1.7 :126, fix phase 3)
        int crystallizerExpected = 82 + 1 + 1 + 23 + 37 * com.hbm.items.special.BedrockOreType.VALUES.length;
        helper.assertTrue(CrystallizerRecipes.recipes.size() == crystallizerExpected,
                "Crystallizer catalogue must contain exactly " + crystallizerExpected
                        + " dependency-complete defaults, found " + CrystallizerRecipes.recipes.size());
        assertCrystallizer(helper, new ItemStack(Blocks.COAL_ORE.asItem()), Fluids.PEROXIDE, 500, 600, 0.05F,
                new ItemStack(ModFuelItems.crystal_coal.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModNuclearBlocks.ore_gneiss_uranium.get().asItem()), Fluids.SULFURIC_ACID,
                500, 600, 0.05F, new ItemStack(ModItems.crystal_uranium.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModItems.powder_calcium.get()), Fluids.REDMUD, 75, 100, 0.10F,
                new ItemStack(ModFuelItems.powder_cement.get(), 8), 1);
        assertCrystallizer(helper, new ItemStack(ModFallingBlocks.gravel_diamond.get().asItem()), Fluids.PEROXIDE,
                500, 600, 0.05F, new ItemStack(ModItems.crystal_diamond.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModFallingBlocks.gravel_obsidian.get().asItem()), Fluids.PEROXIDE,
                500, 100, 0F, new ItemStack(ModBlocks.brick_obsidian.get().asItem()), 1);
        assertCrystallizer(helper, new ItemStack(ModFuelItems.coal_infernal.get()), Fluids.PEROXIDE,
                500, 100, 0F, new ItemStack(ModFuelItems.solid_fuel.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModFuelItems.ingot_schraranium.get()), Fluids.PEROXIDE,
                500, 600, 0.05F, new ItemStack(ModItems.crystal_schraranium.get()), 1);
        assertCrystallizer(helper, new ItemStack(Blocks.COAL_BLOCK.asItem()), Fluids.PEROXIDE,
                500, 600, 0F, new ItemStack(ModRBMKBlocks.block_graphite.get().asItem()), 1);
        assertCrystallizer(helper, new ItemStack(ModItems.powder_semtex_mix.get()), Fluids.PEROXIDE,
                500, 600, 0F, new ItemStack(ModFoodItems.ingot_semtex.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModItems.ingot_biorubber.get()), Fluids.SOURGAS,
                25, 20, 0.15F, new ItemStack(ModItems.ingot_rubber.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModIngredientItems.ash(EnumAshType.FULLERENE), 4), Fluids.XYLENE,
                1_000, 600, 0.10F, new ItemStack(ModItems.ingot_cft.get()), 4);
        assertCrystallizer(helper, new ItemStack(ModOilItems.tar(EnumTarType.CRUDE)), Fluids.CHLORINE,
                250, 20, 0F, new ItemStack(ModOilItems.tar(EnumTarType.WAX)), 1);
        assertCrystallizer(helper, new ItemStack(ModOilItems.tar(EnumTarType.CRACK)), Fluids.CHLORINE,
                100, 20, 0F, new ItemStack(ModOilItems.tar(EnumTarType.WAX)), 1);
        assertCrystallizer(helper, new ItemStack(ModOilItems.tar(EnumTarType.PARAFFIN)), Fluids.CHLORINE,
                100, 20, 0F, new ItemStack(ModOilItems.tar(EnumTarType.WAX)), 1);
        assertCrystallizer(helper, new ItemStack(ModOilItems.tar(EnumTarType.PARAFFIN)), Fluids.ESTRADIOL,
                250, 200, 0F, new ItemStack(ModMedicineItems.pill_red.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModOilItems.tar(EnumTarType.WAX)), Fluids.IONGEL,
                500, 200, 0F, new ItemStack(ModItems.pellet_charged.get()), 1);
        assertCrystallizer(helper, new ItemStack(ModFallingBlocks.moon_turf.get().asItem(), 16), Fluids.PEROXIDE,
                500, 1_200, 0F, new ItemStack(ModIntermediateItems.chunk(EnumChunkType.MOONSTONE)), 16);
        var crystallizer = CrystallizerRecipes.getOutput(
                new ItemStack(ModIntermediateItems.chunk(EnumChunkType.MALACHITE)), Fluids.SULFURIC_ACID);
        var scraps = crystallizer == null ? null : ItemScraps.getMats(crystallizer.output);
        helper.assertTrue(crystallizer != null && crystallizer.acidAmount == 250 && crystallizer.duration == 300
                        && crystallizer.productivity == 0.10F && scraps != null
                        && scraps.material == Mats.MAT_COPPER && scraps.amount == 72,
                "Malachite crystallizer recipe changed");
        helper.succeed();
    }

    public static void materialFragmentRecipesComplete(GameTestHelper helper) {
        String[] materials = {"coal", "lignite", "diamond", "iron", "gold", "redstone", "uranium", "u238",
                "thorium", "polonium", "ra226", "titanium", "copper", "tungsten", "aluminium", "lead",
                "bismuth", "tantalium", "neodymium", "niobium", "beryllium", "emerald", "cobalt", "boron",
                "borax", "lanthanium", "zirconium", "sodium", "strontium", "lithium", "sulfur", "red_phosphorus",
                "niter", "fluorite", "chlorocalcite", "molysite", "asbestos", "bauxite", "cryolite", "technetium",
                "sodalite", "cinnabar", "silicon", "rare"};
        String[] outputs = {"powder_coal", "powder_lignite", "powder_diamond", "powder_iron", "powder_gold",
                "minecraft:redstone", "powder_uranium", "ingot_u238", "powder_thorium", "powder_polonium",
                "powder_ra226", "powder_titanium", "powder_copper", "powder_tungsten", "powder_aluminium",
                "powder_lead", "powder_bismuth", "powder_tantalium", "powder_neodymium", "powder_niobium",
                "powder_beryllium", "powder_emerald", "powder_cobalt", "powder_boron", "powder_borax",
                "powder_lanthanium", "powder_zirconium", "powder_sodium", "powder_strontium", "powder_lithium",
                "sulfur", "powder_fire", "niter", "fluorite", "powder_chlorocalcite", "powder_molysite",
                "powder_asbestos", "stone_resource_bauxite", "chunk_ore_cryolite", "ingot_technetium", "gem_sodalite", "cinnabar",
                "ingot_silicon", "chunk_ore_rare"};
        int[] outputCounts = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        helper.assertTrue(materials.length == 44 && outputs.length == 44 && outputCounts.length == 44,
                "Fragment recipe contract list changed");
        for (int i = 0; i < materials.length; i++) {
            String material = materials[i];
            String id = "bedrock_ore_fragment_" + material + "_compression";
            helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), "hbm", id),
                    "Missing loaded fragment recipe " + id);
            ItemAutogen fragment = ModAutogenItems.get(MaterialShapes.FRAGMENT, Mats.byName(material));
            helper.assertTrue(fragment != null, "Missing fragment input for " + material);
            if (fragment == null) continue;
            List<ItemStack> grid = List.of(
                    new ItemStack(fragment), new ItemStack(fragment), new ItemStack(fragment),
                    new ItemStack(fragment), new ItemStack(fragment), new ItemStack(fragment),
                    new ItemStack(fragment), new ItemStack(fragment), new ItemStack(fragment));
            ItemStack result = Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
            Item expected = outputItem(outputs[i]);
            helper.assertTrue(expected != null && result.is(expected) && result.getCount() == outputCounts[i],
                    "Fragment recipe " + id + " produced " + result + ", expected " + outputs[i]);
        }
        helper.succeed();
    }

    public static void materialTagAliasesComplete(GameTestHelper helper) {
        assertTagged(helper, new ItemStack(ModItems.ingot_th232.get()), "ingots/thorium");
        assertTagged(helper, new ItemStack(ModItems.ingot_th232.get()), "ingots/th232");
        assertTagged(helper, new ItemStack(ModItems.nugget_th232.get()), "nuggets/thorium");
        assertTagged(helper, new ItemStack(ModItems.billet_th232.get()), "billets/thorium");
        assertTagged(helper, new ItemStack(ModItems.ingot_pu_mix.get()), "ingots/pu_mix");
        assertTagged(helper, new ItemStack(ModItems.nugget_am_mix.get()), "nuggets/am_mix");
        assertTagged(helper, new ItemStack(ModOilItems.coke(EnumCokeType.COAL)), "gems/coal_coke");
        assertTagged(helper, new ItemStack(ModOilItems.coke(EnumCokeType.LIGNITE)), "gems/lignite_coke");
        assertTagged(helper, new ItemStack(ModOilItems.coke(EnumCokeType.PETROLEUM)), "gems/pet_coke");
        assertTagged(helper, new ItemStack(ModOreBlocks.stone_resource_hematite.get().asItem()), "ores/hematite");
        assertTagged(helper, new ItemStack(ModOreBlocks.stone_resource_malachite.get().asItem()), "ores/malachite");
        assertTagged(helper, new ItemStack(ModIntermediateItems.chunk(EnumChunkType.MALACHITE)), "ingots/malachite");
        assertTagged(helper, new ItemStack(ModOreBlocks.stone_resource_bauxite.get().asItem()), "gems/bauxite");
        assertTagged(helper, new ItemStack(ModIntermediateItems.chunk(EnumChunkType.CRYOLITE)), "crystals/cryolite");
        assertTagged(helper, new ItemStack(ModIntermediateItems.chunk(EnumChunkType.RARE)), "ingots/rare");
        assertTagged(helper, new ItemStack(ModFuelItems.powder_fire.get()), "dusts/red_phosphorus");
        assertTagged(helper, new ItemStack(ModItems.crystal_phosphorus.get()), "crystals/red_phosphorus");
        assertTagged(helper, new ItemStack(ModItems.powder_diamond.get()), "dusts/diamond");
        assertTagged(helper, new ItemStack(ModItems.powder_emerald.get()), "dusts/emerald");
        assertTagged(helper, new ItemStack(ModItems.niter.get()), "dusts/niter");
        assertTagged(helper, new ItemStack(ModItems.ingot_mud.get()), "ingots/mud");
        helper.succeed();
    }

    public static void machineRecipeCompletion(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();

        assertWarheadRecipe(helper, "ass.warheadinc1", ModItems.warhead_generic_small.get(), 2,
                ModItems.warhead_incendiary_small.get(), 100);
        assertWarheadRecipe(helper, "ass.warheadinc2", ModItems.warhead_generic_medium.get(), 4,
                ModItems.warhead_incendiary_medium.get(), 200);
        assertWarheadRecipe(helper, "ass.warheadinc3", ModItems.warhead_generic_large.get(), 8,
                ModItems.warhead_incendiary_large.get(), 400);

        GenericRecipe epearl = ChemicalPlantRecipes.INSTANCE.getRecipe("chem.epearl");
        helper.assertTrue(epearl != null && epearl.inputItem != null && epearl.inputItem.length == 1
                        && epearl.inputItem[0].matchesRecipe(new ItemStack(ModItems.powder_diamond.get()), false)
                        && epearl.inputFluid != null && epearl.inputFluid.length == 1
                        && epearl.inputFluid[0].type == Fluids.XPJUICE && epearl.inputFluid[0].fill == 500
                        && epearl.outputFluid != null && epearl.outputFluid.length == 1
                        && epearl.outputFluid[0].type == Fluids.ENDERJUICE && epearl.outputFluid[0].fill == 100,
                "Diamond dust to Ender Juice recipe changed");

        GenericRecipe laminate = ChemicalPlantRecipes.INSTANCE.getRecipe("chem.laminate");
        Item steelBolt = ModAutogenItems.get(MaterialShapes.BOLT, Mats.MAT_STEEL);
        helper.assertTrue(laminate != null && steelBolt != null && laminate.inputItem != null
                        && laminate.inputItem.length == 2
                        && laminate.inputItem[0].matchesRecipe(new ItemStack(Items.GLASS), false)
                        && laminate.inputItem[1].matchesRecipe(new ItemStack(steelBolt, 4), false)
                        && laminate.inputItem[1].stacksize == 4
                        && laminate.inputFluid != null && laminate.inputFluid.length == 2
                        && laminate.inputFluid[0].type == Fluids.XYLENE && laminate.inputFluid[0].fill == 50
                        && laminate.inputFluid[1].type == Fluids.PHOSGENE && laminate.inputFluid[1].fill == 50
                        && laminate.outputItem != null && laminate.outputItem.length == 1
                        && laminate.outputItem[0].is(ModGlassBlocks.reinforced_laminate.get().asItem()),
                "Reinforced laminate chemical recipe changed");

        GenericRecipe rbmk = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.rbmk");
        helper.assertTrue(rbmk != null && !rbmk.isPooled() && rbmk.duration == 100 && rbmk.power == 100
                        && rbmk.inputItem != null && rbmk.inputItem.length == 4
                        && rbmk.inputItem[0].matchesRecipe(new ItemStack(ModBlocks.concrete_asbestos.get().asItem(), 4), false)
                        && rbmk.inputItem[1].matchesRecipe(new ItemStack(
                        ModAutogenItems.get(MaterialShapes.CASTPLATE, Mats.MAT_STEEL), 2), false)
                        && rbmk.inputItem[2].matchesRecipe(new ItemStack(ModItems.plate_copper.get(), 4), false)
                        && rbmk.inputItem[3].matchesRecipe(new ItemStack(ModItems.ingot_rubber.get(), 2), false)
                        && rbmk.outputItem != null && rbmk.outputItem.length == 1
                        && rbmk.outputItem[0].is(ModRBMKBlocks.rbmk_blank.get().asItem()),
                "Default RBMK assembler recipe is missing or not the normal branch");
        helper.succeed();
    }

    private static void assertWarheadRecipe(GameTestHelper helper, String name, Item input, int powderCount,
                                            Item output, int duration) {
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(name);
        helper.assertTrue(recipe != null && recipe.duration == duration && recipe.power == 100
                        && recipe.inputItem != null && recipe.inputItem.length == 2
                        && recipe.inputItem[0].matchesRecipe(new ItemStack(input), false)
                        && recipe.inputItem[1].matchesRecipe(new ItemStack(ModFuelItems.powder_fire.get(), powderCount), false)
                        && recipe.inputItem[1].stacksize == powderCount
                        && recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(output),
                "Changed assembler recipe " + name);
    }

    private static String autogenBase(MaterialShapes shape) {
        switch (shape) {
            case INGOT: return "ingot_raw";
            case FRAGMENT: return "bedrock_ore_fragment";
            case CASTPLATE: return "plate_cast";
            case WELDEDPLATE: return "plate_welded";
            case SHELL: return "shell";
            case PIPE: return "pipe";
            case WIRE: return "wire_fine";
            case DENSEWIRE: return "wire_dense";
            case BOLT: return "bolt";
            case HEAVY_COMPONENT: return "heavy_component";
            case LIGHTBARREL: return "part_barrel_light";
            case HEAVYBARREL: return "part_barrel_heavy";
            case LIGHTRECEIVER: return "part_receiver_light";
            case HEAVYRECEIVER: return "part_receiver_heavy";
            case MECHANISM: return "part_mechanism";
            case STOCK: return "part_stock";
            case GRIP: return "part_grip";
            default: return null;
        }
    }

    private static void assertCrystallizer(GameTestHelper helper, ItemStack input, FluidType fluid, int acidAmount,
                                           int duration, float productivity, ItemStack output, int inputCount) {
        CrystallizerRecipes.CrystallizerRecipe recipe = CrystallizerRecipes.getOutput(input, fluid);
        helper.assertTrue(recipe != null && recipe.input.stacksize == inputCount
                        && recipe.input.matchesRecipe(input, false)
                        && recipe.fluid == fluid && recipe.acidAmount == acidAmount
                        && recipe.duration == duration && recipe.productivity == productivity
                        && recipe.output.is(output.getItem()) && recipe.output.getCount() == output.getCount(),
                "Changed crystallizer recipe for " + input + " with " + fluid.getName());
    }

    private static Item outputItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id.contains(":") ? id : "hbm:" + id));
    }

    private static void assertExactMaterial(GameTestHelper helper, ItemStack input, NTMMaterial material,
                                            int amount, int cardinality) {
        List<Mats.MaterialStack> actual = Mats.getSmeltingMaterialsFromItem(input);
        helper.assertTrue(actual.size() == cardinality
                        && actual.stream().allMatch(stack -> stack.material == material && stack.amount == amount),
                "Material distribution drift for " + input + ": " + actual);
    }

    private static void assertMaterial(GameTestHelper helper, ItemStack input, NTMMaterial material, int amount) {
        List<Mats.MaterialStack> actual = Mats.getSmeltingMaterialsFromItem(input);
        helper.assertTrue(actual.size() == 1 && actual.get(0).material == material && actual.get(0).amount == amount,
                "Missing material distribution " + material.name + "=" + amount + " for " + input);
    }

    private static void assertExactDistribution(GameTestHelper helper, ItemStack input,
                                                Mats.MaterialStack... expected) {
        List<Mats.MaterialStack> actual = Mats.getSmeltingMaterialsFromItem(input);
        boolean matches = actual.size() == expected.length;
        if (matches) {
            for (int i = 0; i < expected.length; i++) {
                matches &= actual.get(i).material == expected[i].material
                        && actual.get(i).amount == expected[i].amount;
            }
        }
        helper.assertTrue(matches, "Material distribution drift for " + input + ": " + actual);
    }

    private static void assertPress(GameTestHelper helper, ItemStack input, Item stamp, Item output, int count) {
        ItemStack result = PressRecipes.getOutput(input, new ItemStack(stamp));
        helper.assertTrue(result.is(output) && result.getCount() == count,
                "Press recipe produced " + result + ", expected " + count + " " + output);
    }

    private static void assertTagged(GameTestHelper helper, ItemStack stack, String path) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Ids.of("c", path));
        helper.assertTrue(stack.is(tag), "Missing c:" + path + " membership for " + stack);
    }
}
