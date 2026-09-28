package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.machine.BlockCustomMachine;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MachineDynConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.CustomMachineRecipes.ChanceStack;
import com.hbm.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModArmorSetItems;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIngredientItems.PlateFuelType;
import com.hbm.items.ModItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import com.hbm.items.special.CellVariants;
import com.hbm.items.special.ItemCell;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Contracts of the 2026-09-26 gap batch A (§19ed-gaps-a), each against the 1.7 behaviour it restores: dropped
 * AMAT/ASCHRAB cells, the anti-schrabidium recipes, the radiovoltaic batteries and the orbus recipe, the spent
 * crucible's modifiers, the meteorite sword's machine steps, the
 * custom machine's flux and heat inputs from a real research reactor and a real firebox, expensive-mode and 528
 * inputs, the icfLaser machine config and the old numeric cell variant.
 */
public final class FinalGapsAGameTests {
    private FinalGapsAGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CELL_DROPS =
            TESTS.function("gapsa_cell_drops", 40, () -> FinalGapsAGameTests::cellDrops);
    public static final GameTests.Function ASCHRAB_RECIPES =
            TESTS.function("gapsa_aschrab_recipes", 20, () -> FinalGapsAGameTests::aschrabRecipes);
    public static final GameTests.Function BATTERY_SC =
            TESTS.function("gapsa_battery_sc", 20, () -> FinalGapsAGameTests::batterySC);
    public static final GameTests.Function CRUCIBLE =
            TESTS.function("gapsa_crucible", 20, () -> FinalGapsAGameTests::crucible);
    public static final GameTests.Function SWORD_STEPS =
            TESTS.function("gapsa_meteorite_steps", 40, () -> FinalGapsAGameTests::swordSteps);
    public static final GameTests.Function CM_FLUX_HEAT =
            TESTS.function("gapsa_cm_flux_heat", 400, () -> FinalGapsAGameTests::customMachineFluxHeat);
    public static final GameTests.Function CONFIG_INPUTS =
            TESTS.function("gapsa_expensive_528_icf", 20, () -> FinalGapsAGameTests::configInputs);
    public static final GameTests.Function CELL_MIGRATION =
            TESTS.function("gapsa_cell_migration", 20, () -> FinalGapsAGameTests::cellMigration);

    public static void init() {}

    // ------------------------------------------------------------------ 1. dropped cells

    public static void cellDrops(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        BlockPos abs = helper.absolutePos(floor);

        ItemEntity amat = new ItemEntity(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 1D, abs.getZ() + 0.5D,
                ItemCell.getFullCell(Fluids.AMAT));
        amat.setOnGround(false);
        helper.getLevel().addFreshEntity(amat);
        ItemCell.tickDropped(amat);
        helper.assertTrue(!amat.isRemoved(), "1.7 checked onGround only: an airborne antimatter cell must not go off");
        amat.setOnGround(true);
        ItemCell.tickDropped(amat);
        helper.assertTrue(amat.isRemoved(), "a grounded antimatter cell must be consumed");
        // ExplosionVNT 3F amat: 16 rays, no drops - the stone under the cell is gone and nothing dropped
        helper.assertTrue(helper.getBlockState(floor).isAir(), "the 3F annihilation must remove the block under the cell");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(6D));
        helper.assertTrue(drops.isEmpty(), "makeAmat is setNoDrop, got drops " + drops);

        ItemEntity aschrab = new ItemEntity(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 1D, abs.getZ() + 0.5D,
                ItemCell.getFullCell(Fluids.ASCHRAB));
        aschrab.setOnGround(true);
        helper.getLevel().addFreshEntity(aschrab);
        ItemCell.tickDropped(aschrab);
        helper.assertTrue(aschrab.isRemoved(), "a grounded anti-schrabidium cell must be consumed");
        AABB around = new AABB(abs).inflate(4D);
        List<EntityNukeExplosionMK3> fields = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK3.class, around);
        List<EntityCloudFleija> clouds = helper.getLevel().getEntitiesOfClass(EntityCloudFleija.class, around);
        helper.assertTrue(fields.size() == 1 && clouds.size() == 1,
                "1.7 statFacFleija(aSchrabRadius) + EntityCloudFleija, got fields=" + fields.size() + " clouds=" + clouds.size());
        // the FLEIJA field would carve 20 blocks into the neighbouring test plots
        fields.forEach(e -> e.discard());
        clouds.forEach(e -> e.discard());

        // an empty cell on the ground is an ordinary item
        ItemEntity empty = new ItemEntity(helper.getLevel(), abs.getX() + 0.5D, abs.getY() + 1D, abs.getZ() + 0.5D,
                new ItemStack(ModSingularityItems.cell.get()));
        empty.setOnGround(true);
        helper.getLevel().addFreshEntity(empty);
        ItemCell.tickDropped(empty);
        helper.assertTrue(!empty.isRemoved(), "an empty cell is not an ItemDrop");
        empty.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------ 2. ASCHRAB recipes

    public static void aschrabRecipes(GameTestHelper helper) {
        ItemStack aschrab = ItemCell.getFullCell(Fluids.ASCHRAB);
        ItemStack amat = ItemCell.getFullCell(Fluids.AMAT);
        helper.assertTrue(craft(helper, singularity(aschrab)).is(ModSingularityItems.singularity.get()),
                "1.7 CraftingManager:325: the singularity takes anti-schrabidium cells");
        helper.assertTrue(craft(helper, singularity(amat)).isEmpty(), "antimatter cells no longer make a singularity");
        ItemStack sing = new ItemStack(ModSingularityItems.singularity.get());
        ItemStack euph = new ItemStack(ModItems.plate_euphemium.get());
        List<ItemStack> ams = grid(euph, aschrab, euph, aschrab, sing, aschrab, euph, aschrab, euph);
        ItemStack core = craft(helper, ams);
        helper.assertTrue(!core.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(core.getItem()).getPath().equals("ams_core_sing"),
                "1.7 CraftingManager:600: ams_core_sing takes anti-schrabidium cells, got " + core);
        List<ItemStack> amsAmat = grid(euph, amat, euph, amat, sing, amat, euph, amat, euph);
        helper.assertTrue(craft(helper, amsAmat).isEmpty(), "ams_core_sing no longer takes antimatter cells");
        List<ItemStack> remainders = Recipes.craftingRemainders(helper.getLevel(), 3, 3, singularity(aschrab)).orElse(List.of());
        helper.assertTrue(remainders.stream().filter(ItemCell::isEmptyCell).count() == 4,
                "the four anti-schrabidium cells come back empty: " + remainders);
        helper.succeed();
    }

    private static List<ItemStack> singularity(ItemStack cell) {
        ItemStack e = new ItemStack(ModItems.nugget_euphemium.get());
        ItemStack b = new ItemStack(com.hbm.blocks.ModHazardBlocks.block_schrabidium.get().asItem());
        return grid(e, cell, e, cell, b, cell, e, cell, e);
    }

    private static List<ItemStack> grid(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : stacks) list.add(s.copy());
        return list;
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
        return Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ 3. radiovoltaic batteries, ass.orbus

    public static void batterySC(GameTestHelper helper) {
        long[] power = {0, 150, 200, 500, 750, 1_000, 1_250, 1_500, 2_000, 2_500};
        for (EnumBatterySC pack : EnumBatterySC.VALUES) {
            ItemBatterySC item = ModBatterySCItems.get(pack);
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(item.getMaxCharge(stack) == power[pack.ordinal()]
                            && item.getCharge(stack) == power[pack.ordinal()]
                            && item.getDischargeRate(stack) == power[pack.ordinal()] && item.getChargeRate(stack) == 0,
                    "1.7 EnumBatterySC." + pack + " rates");
            item.dischargeBattery(stack, 100);
            helper.assertTrue(item.getCharge(stack) == power[pack.ordinal()], pack + " never drains");
            helper.assertTrue(stack.getMaxStackSize() == 1, pack + " stacks to one");
        }
        ItemStack empty = new ItemStack(ModBatterySCItems.get(EnumBatterySC.EMPTY));
        ItemStack po = new ItemStack(ModItems.billet_polonium.get());
        List<ItemStack> shapeless = new ArrayList<>(List.of(empty, po, po, ItemStack.EMPTY));
        ItemStack out = Recipes.craftingResult(helper.getLevel(), 2, 2, shapeless).orElse(ItemStack.EMPTY);
        helper.assertTrue(out.is(ModBatterySCItems.get(EnumBatterySC.PO210)),
                "1.7 CraftingManager:268: empty SC + two polonium billets = Po-210, got " + out);

        AssemblyMachineRecipes.INSTANCE.registerRecipes(); // lazily populated, as in every recipe test
        GenericRecipe orbus = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.orbus");
        helper.assertTrue(orbus != null, "ass.orbus is registered");
        boolean po210 = false;
        for (AStack in : orbus.inputItem) {
            if (in.matchesRecipe(new ItemStack(ModBatterySCItems.get(EnumBatterySC.PO210)), true)) po210 = true;
        }
        helper.assertTrue(po210 && orbus.inputItem.length == 4 && orbus.duration == 300,
                "ass.orbus takes the Po-210 radiovoltaic battery (1.7 inputs, 300 ticks)");
        helper.assertTrue(orbus.outputItem[0].is(com.hbm.blocks.ModRadarUtilityBlocks.machine_orbus.get().asItem()),
                "ass.orbus makes the orbus");
        GenericRecipe taint = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.mpw10taint");
        helper.assertTrue(taint != null && taint.inputItem.length == 5, "ass.mpw10taint is registered with its five inputs");
        helper.succeed();
    }

    // ------------------------------------------------------------------ 4. crucible

    public static void crucible(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModAbilityToolItems.crucible.get());
        helper.assertTrue(ItemStacks.hasAttackDamageModifier(fresh), "a charged crucible has its attack damage");
        ItemStack spent = ItemCrucible.withSpent(fresh.copy(), 2);
        helper.assertTrue(ItemStacks.hasAttackDamageModifier(spent), "one charge left keeps the modifiers");
        ItemCrucible.withSpent(spent, 3);
        helper.assertTrue(!ItemStacks.hasAttackDamageModifier(spent) && ItemStacks.attributeModifiersSuppressed(spent),
                "1.7 getAttributeModifiers: an empty crucible has no modifiers at all");
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, spent);
        player.tick();
        double damage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        helper.assertTrue(damage <= 1.0001D, "holding an empty crucible hits like a bare hand, attack damage " + damage);

        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, com.hbm.blocks.ModDecoCrateBlocks.crate_tungsten.get().defaultBlockState());
        var crate = (com.hbm.tileentity.machine.storage.TileEntityCrateTungsten) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(crate != null, "tungsten crate");
        crate.setItem(0, ItemCrucible.withSpent(new ItemStack(ModAbilityToolItems.crucible.get()), 3));
        crate.addEnergy(helper.getLevel(), helper.absolutePos(rel), 10_000_001, Direction.UP);
        ItemStack back = crate.getItem(0);
        helper.assertTrue(ItemCrucible.spent(back) == 0 && ItemStacks.hasAttackDamageModifier(back),
                "the tungsten crate's 10 MHE laser returns a charged crucible with its modifiers: " + back);
        helper.succeed();
    }

    // ------------------------------------------------------------------ 5. meteorite sword steps

    public static void swordSteps(GameTestHelper helper) {
        // research reactor: bred -> irradiated in any rod slot while the rods are in
        BlockPos rr = new BlockPos(1, 1, 1);
        helper.setBlock(rr, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        TileEntityReactorResearch reactor = (TileEntityReactorResearch) helper.getLevel().getBlockEntity(helper.absolutePos(rr));
        reactor.inventory.setStackInSlot(5, new ItemStack(ModAbilityToolItems.meteorite_sword_bred.get()));
        reactor.setTarget(1.0D);
        for (int i = 0; i < 3; i++) TileEntityReactorResearch.serverTick(helper.getLevel(), helper.absolutePos(rr),
                helper.getLevel().getBlockState(helper.absolutePos(rr)), reactor);
        helper.assertTrue(reactor.inventory.getStackInSlot(5).is(ModAbilityToolItems.meteorite_sword_irradiated.get()),
                "1.7 TileEntityReactorResearch.reaction: bred -> irradiated, got " + reactor.inventory.getStackInSlot(5));

        // ZIRNOX: the same step while switched on
        BlockPos zp = new BlockPos(4, 1, 1);
        helper.setBlock(zp, ModMachineBlocks.machine_zirnox.get().defaultBlockState());
        TileEntityReactorZirnox zirnox = GameTestHelpers.blockEntity(helper, zp, TileEntityReactorZirnox.class);
        zirnox.inventory.setStackInSlot(3, new ItemStack(ModAbilityToolItems.meteorite_sword_bred.get()));
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(zp), zirnox.getBlockState(), zirnox);
        helper.assertTrue(zirnox.inventory.getStackInSlot(3).is(ModAbilityToolItems.meteorite_sword_bred.get()),
                "an idle ZIRNOX leaves the sword alone");
        zirnox.isOn = true;
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(zp), zirnox.getBlockState(), zirnox);
        helper.assertTrue(zirnox.inventory.getStackInSlot(3).is(ModAbilityToolItems.meteorite_sword_irradiated.get()),
                "1.7 TileEntityReactorZirnox: bred -> irradiated while on");

        // fusion breeder: irradiated -> fused, no outgasser recipe involved
        BlockPos bp = new BlockPos(9, 1, 2);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_breeder.get(), bp);
        TileEntityFusionBreeder breeder = GameTestHelpers.blockEntity(helper, bp, TileEntityFusionBreeder.class);
        breeder.inventory.setStackInSlot(1, new ItemStack(ModAbilityToolItems.meteorite_sword_irradiated.get()));
        breeder.receiveFusionPower(0L, TileEntityFusionBreeder.CAPACITY + 1D, 0F, 0F, 0F);
        helper.assertTrue(breeder.inventory.getStackInSlot(2).is(ModAbilityToolItems.meteorite_sword_fused.get())
                        && breeder.inventory.getStackInSlot(1).isEmpty(),
                "1.7 TileEntityFusionBreeder.processSolid: irradiated -> fused");

        // assembler and chemical plant: any working tick turns the sword in the battery slot
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        ChemicalPlantRecipes.INSTANCE.registerRecipes();
        BlockPos ap = new BlockPos(2, 1, 6);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_machine.get(), ap);
        TileEntityMachineAssemblyMachine assembler = GameTestHelpers.blockEntity(helper, ap, TileEntityMachineAssemblyMachine.class);
        GenericRecipe ar = loadRunnable(AssemblyMachineRecipes.INSTANCE, assembler.assemblerModule, assembler.inventory);
        helper.assertTrue(ar != null, "no runnable assembler recipe found");
        assembler.power = assembler.maxPower;
        assembler.inventory.setStackInSlot(0, new ItemStack(ModAbilityToolItems.meteorite_sword_alloyed.get()));
        BlockPos aAbs = helper.absolutePos(ap);
        TileEntityMachineAssemblyMachine.serverTick(helper.getLevel(), aAbs, helper.getLevel().getBlockState(aAbs), assembler);
        helper.assertTrue(assembler.didProcess, "the assembler worked on " + ar.name);
        helper.assertTrue(assembler.inventory.getStackInSlot(0).is(ModAbilityToolItems.meteorite_sword_machined.get()),
                "1.7 assembler: alloyed -> machined");

        BlockPos cp = new BlockPos(7, 1, 6);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_chemical_plant.get(), cp);
        TileEntityMachineChemicalPlant plant = GameTestHelpers.blockEntity(helper, cp, TileEntityMachineChemicalPlant.class);
        GenericRecipe cr = loadRunnable(ChemicalPlantRecipes.INSTANCE, plant.chemplantModule, plant.inventory);
        helper.assertTrue(cr != null, "no runnable chemical plant recipe found");
        if (cr.inputFluid != null) {
            for (int i = 0; i < cr.inputFluid.length; i++) {
                plant.inputTanks[i].setTankType(cr.inputFluid[i].type);
                plant.inputTanks[i].setFill(plant.inputTanks[i].getMaxFill());
            }
        }
        plant.power = plant.maxPower;
        plant.inventory.setStackInSlot(0, new ItemStack(ModAbilityToolItems.meteorite_sword_machined.get()));
        BlockPos cAbs = helper.absolutePos(cp);
        TileEntityMachineChemicalPlant.serverTick(helper.getLevel(), cAbs, helper.getLevel().getBlockState(cAbs), plant);
        helper.assertTrue(plant.didProcess, "the chemical plant worked on " + cr.name);
        helper.assertTrue(plant.inventory.getStackInSlot(0).is(ModAbilityToolItems.meteorite_sword_treated.get()),
                "1.7 chemical plant: machined -> treated");
        helper.succeed();
    }

    /** Picks the first unpooled recipe whose item inputs all resolve and whose power fits, and fills its inputs. */
    private static GenericRecipe loadRunnable(GenericRecipes set, com.hbm.modules.machine.ModuleMachineBase module,
                                              com.hbm.inventory.NTMItemHandler inventory) {
        for (GenericRecipe recipe : set.recipeOrderedList) {
            if (recipe.isPooled() || recipe.power > 100_000 || recipe.inputItem == null) continue;
            if (recipe.inputItem.length > module.inputSlots.length) continue;
            if (recipe.inputFluid != null && recipe.inputFluid.length > 0
                    && java.util.Arrays.stream(recipe.inputFluid).anyMatch(f -> f.pressure != 0)) continue;
            List<ItemStack> fills = new ArrayList<>();
            boolean ok = true;
            for (AStack in : recipe.inputItem) {
                List<ItemStack> shown = in.getDisplayStacks();
                if (shown.isEmpty() || shown.get(0).isEmpty() || in.stacksize > shown.get(0).getMaxStackSize()) { ok = false; break; }
                fills.add(ItemStacks.copyWithCount(shown.get(0), in.stacksize));
            }
            if (!ok) continue;
            module.recipe = recipe.name;
            for (int i = 0; i < fills.size(); i++) inventory.setStackInSlot(module.inputSlots[i], fills.get(i));
            return recipe;
        }
        return null;
    }

    // 9. trenchmaster: covered by harness FinalGapsA with a real survival player. GameTest mock players are creative
    //    (1.20.1 Forge's mock overrides isCreative()), so no hit ever lands on them.

    // ------------------------------------------------------------------ 10. custom machine flux / heat

    public static void customMachineFluxHeat(GameTestHelper helper) {
        String fluxKey = "gapsa_test_flux", heatKey = "gapsa_test_heat";
        register(fluxKey, true, 0, "hbm:tile.cm_flux", 50, 0);
        register(heatKey, false, 10_000, "hbm:tile.cm_heat", 0, 100);

        // flux machine: controller at (2,2,2), the flux block one south, the research reactor diagonal above it
        BlockPos fluxCore = new BlockPos(2, 2, 2);
        TileEntityCustomMachine flux = controller(helper, fluxCore, fluxKey);
        BlockPos fluxBlock = componentAt(helper, flux);
        helper.setBlock(fluxBlock, ModCustomMachineBlocks.cm_flux.get().defaultBlockState());
        BlockPos reactorRel = fluxBlock.offset(1, 1, 0);
        helper.setBlock(reactorRel, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        TileEntityReactorResearch reactor = (TileEntityReactorResearch) helper.getLevel().getBlockEntity(helper.absolutePos(reactorRel));
        for (int i = 0; i < 4; i++) reactor.inventory.setStackInSlot(i, new ItemStack(ModIngredientItems.plateFuel(PlateFuelType.RA226BE)));
        reactor.setTarget(1.0D);

        // heat machine: controller at (8,2,2), the heat block one south, a burning firebox core two east and one down
        BlockPos heatCore = new BlockPos(8, 2, 2);
        TileEntityCustomMachine heat = controller(helper, heatCore, heatKey);
        BlockPos heatBlock = componentAt(helper, heat);
        helper.setBlock(heatBlock, ModCustomMachineBlocks.cm_heat.get().defaultBlockState());
        BlockPos fireboxRel = heatBlock.offset(2, -1, 0);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.heater_firebox.get(), fireboxRel);
        TileEntityHeaterFirebox firebox = GameTestHelpers.blockEntity(helper, fireboxRel, TileEntityHeaterFirebox.class);
        firebox.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 64));

        for (TileEntityCustomMachine te : new TileEntityCustomMachine[]{flux, heat}) {
            te.power = te.config.maxPower;
            te.inventory.setStackInSlot(4, new ItemStack(Items.STONE, 64));
            te.checkStructure();
        }

        helper.succeedWhen(() -> {
            helper.assertTrue(flux.structureOK && heat.structureOK, "both test machines are complete");
            helper.assertTrue(reactor.totalFlux == 120, "the research reactor makes 120 flux, got " + reactor.totalFlux);
            helper.assertTrue(flux.flux == reactor.totalFlux, "1.7: the flux machine reads the reactor's totalFlux, got " + flux.flux);
            helper.assertTrue(firebox.getHeatStored() > 0 || heat.heat > 0, "the firebox burns");
            helper.assertTrue(flux.inventory.getStackInSlot(16).is(Items.COBBLESTONE),
                    "the flux recipe (flux 50) ran on the reactor's flux: " + flux.inventory.getStackInSlot(16));
            helper.assertTrue(heat.inventory.getStackInSlot(16).is(Items.COBBLESTONE),
                    "the heat recipe (heat 100) ran on the firebox's heat: " + heat.inventory.getStackInSlot(16)
                            + " heat " + heat.heat + " firebox " + firebox.getHeatStored());
            CustomMachineConfigJSON.customMachines.remove(fluxKey);
            CustomMachineConfigJSON.customMachines.remove(heatKey);
        });
    }

    private static void register(String key, boolean fluxMode, int maxHeat, String block, int recipeFlux, int recipeHeat) {
        MachineConfiguration conf = new MachineConfiguration();
        conf.recipeKey = key;
        conf.unlocalizedName = key;
        conf.localizedName = key;
        conf.itemInCount = 1;
        conf.itemOutCount = 1;
        conf.fluxMode = fluxMode;
        conf.maxHeat = maxHeat;
        conf.maxPower = 100_000;
        conf.maxPollutionCap = 100;
        conf.components = new ArrayList<>();
        MachineConfiguration.ComponentDefinition comp = new MachineConfiguration.ComponentDefinition();
        comp.blockName = block;
        comp.metas = new com.google.gson.JsonArray();
        comp.metas.add(0);
        comp.x = 0;
        comp.y = 0;
        comp.z = 1;
        conf.components.add(comp);
        CustomMachineConfigJSON.customMachines.put(key, conf);

        CustomMachineRecipe recipe = new CustomMachineRecipe();
        recipe.inputFluids = new com.hbm.inventory.fluid.FluidStack[0];
        recipe.inputItems = new AStack[]{new ComparableStack(Items.STONE, 1)};
        recipe.outputFluids = new com.hbm.inventory.fluid.FluidStack[0];
        recipe.outputItems = new ChanceStack[]{new ChanceStack(new ItemStack(Items.COBBLESTONE), 1F)};
        recipe.duration = 20;
        recipe.consumptionPerTick = 1;
        recipe.pollutionType = "";
        recipe.flux = recipeFlux;
        recipe.heat = recipeHeat;
        CustomMachineRecipes.get("paperPress"); // load the file first, so the test entries survive the lazy load
        CustomMachineRecipes.recipes.put(key, new ArrayList<>(List.of(recipe)));
    }

    private static TileEntityCustomMachine controller(GameTestHelper helper, BlockPos rel, String key) {
        helper.setBlock(rel, ModCustomMachineBlocks.custom_machine.get().defaultBlockState()
                .setValue(BlockCustomMachine.FACING, Direction.NORTH));
        TileEntityCustomMachine te = GameTestHelpers.blockEntity(helper, rel, TileEntityCustomMachine.class);
        te.machineType = key;
        te.init();
        return te;
    }

    private static BlockPos componentAt(GameTestHelper helper, TileEntityCustomMachine te) {
        BlockPos abs = TileEntityCustomMachine.componentPos(te.getBlockPos(), te.facing(), te.config.components.get(0));
        return abs.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    // ------------------------------------------------------------------ 13. expensive / 528 / icfLaser

    public static void configInputs(GameTestHelper helper) {
        boolean expensive = GeneralConfig.enableExpensiveMode, five28 = GeneralConfig.enable528;
        try {
            GeneralConfig.enableExpensiveMode = false;
            GeneralConfig.enable528 = false;
            GenericRecipe normal = new GenericRecipe("gapsa.test").inputItems(new ComparableStack(Items.STONE, 1))
                    .inputItemsEx(new ComparableStack(Items.DIRT, 2)).setPools528("528.gapsa");
            helper.assertTrue(normal.inputItem.length == 1 && normal.inputItem[0].matchesRecipe(new ItemStack(Items.STONE), true)
                    && !normal.isPooled(), "default config: primary inputs, no 528 pool");
            GeneralConfig.enableExpensiveMode = true;
            GeneralConfig.enable528 = true;
            GenericRecipe harsh = new GenericRecipe("gapsa.test2").inputItems(new ComparableStack(Items.STONE, 1))
                    .inputItemsEx(new ComparableStack(Items.DIRT, 2)).setPools528("528.gapsa");
            helper.assertTrue(harsh.inputItem.length == 1 && harsh.inputItem[0].matchesRecipe(new ItemStack(Items.DIRT), true)
                    && harsh.isPooled(), "1.7 inputItemsEx / setPools528 replace the inputs and pool the recipe");
            GenericRecipes.blueprintPools.remove("528.gapsa");
        } finally {
            GeneralConfig.enableExpensiveMode = expensive;
            GeneralConfig.enable528 = five28;
        }
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe autoloader = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.rbmkautoloader");
        helper.assertTrue(autoloader != null && autoloader.inputItem.length == 4 && !autoloader.isPooled(),
                "the default-config autoloader keeps its four primary inputs");

        helper.assertTrue(Files.isRegularFile(MachineDynConfig.file()), "hbmConfig/hbmMachines.json was written at start");
        try {
            String json = Files.readString(MachineDynConfig.file());
            helper.assertTrue(json.contains("\"icfLaser\"") && json.contains("\"I:capacitorPower\": " + TileEntityICFController.capacitorPower)
                            && json.contains("\"I:turboPower\": " + TileEntityICFController.turboPower),
                    "icfLaser entry with the values in use: " + json);
        } catch (java.io.IOException e) {
            helper.fail("unreadable machine config: " + e);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ 14. old numeric cell variant

    public static void cellMigration(GameTestHelper helper) {
        ItemStack old = new ItemStack(ModSingularityItems.cell.get());
        ModDataComponents.fluidId(old, Fluids.AMAT.getID());
        ModDataComponents.fluidAmount(old, ItemCell.CAPACITY);
        ModDataComponents.ITEM_VARIANT.set(old, Fluids.AMAT.getID()); // the pre-2026-09-25 variant (fluid id 33)
        ItemCell.migrateVariant(old);
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(old, -1) == CellVariants.ANTIMATTER,
                "an old antimatter cell is rewritten to variant 3");
        helper.assertTrue(ItemStacks.sameItemAndData(old, ItemCell.getFullCell(Fluids.AMAT)),
                "a migrated cell stacks with a new one");
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack held = new ItemStack(ModSingularityItems.cell.get());
        ModDataComponents.fluidId(held, Fluids.SAS3.getID());
        ModDataComponents.fluidAmount(held, ItemCell.CAPACITY);
        ModDataComponents.ITEM_VARIANT.set(held, Fluids.SAS3.getID());
        player.getInventory().setItem(5, held);
        player.getInventory().tick();
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(player.getInventory().getItem(5), -1) == CellVariants.SAS3,
                "a cell in a player's inventory is migrated by its inventory tick");
        helper.succeed();
    }
}
