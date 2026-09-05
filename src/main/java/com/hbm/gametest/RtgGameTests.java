package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.ContainerMachineRTG;
import com.hbm.inventory.recipes.RTGRecipes;
import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.DepletedRTGMaterial;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.tileentity.machine.TileEntityMachineRTG;
import com.hbm.util.RTGUtil;
import dev.golem.core.gametest.GameTests;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Behavioral contracts for the grey RTG and its fresh/depleted pellet family.
 * The render assertions intentionally record the user-requested 3D-only override: the old RTG world used
 * a MODEL cube plus BER and the old inventory item used a cube, while this port is BER-only/OBJ-only.
 */
public final class RtgGameTests {
    private RtgGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function VARIANTS = TESTS.function("rtg_variants", 20, () -> RtgGameTests::variants);
    public static final GameTests.Function DECAY = TESTS.function("rtg_decay", 20, () -> RtgGameTests::decay);
    public static final GameTests.Function SLOTS_OUTPUT = TESTS.function("rtg_slots_output", 40, () -> RtgGameTests::slotsAndOutput);
    public static final GameTests.Function PERSISTENCE_SYNC = TESTS.function("rtg_persistence_sync", 40, () -> RtgGameTests::persistenceAndSync);
    public static final GameTests.Function MENU = TESTS.function("rtg_menu", 20, () -> RtgGameTests::menu);
    public static final GameTests.Function HAZARDS = TESTS.function("rtg_hazards", 20, () -> RtgGameTests::hazards);
    public static final GameTests.Function ASSETS = TESTS.function("rtg_assets", 20, () -> RtgGameTests::assets);
    public static final GameTests.Function RECIPES = TESTS.function("rtg_recipes", 20, () -> RtgGameTests::recipes);

    public static void init() {}

    public static void variants(GameTestHelper helper) {
        int[] heat = {3, 5, 10, 12, 16, 20, 25, 50, 200, 600, 6_000};
        long[] halfLife = {14_016_000_000L, 876_000_000L, 768_252_000L, 252_200_400L, 46_176_588L,
                190_705_200L, 3_786_072_000L, 3_321_024L, 64_728L, 3_253L, 1_000L};
        int[] halfLives = {1, 2, 2, 2, 2, 2, 2, 3, 4, 6, 12};
        DepletedRTGMaterial[] residual = {DepletedRTGMaterial.LEAD, DepletedRTGMaterial.LEAD,
                DepletedRTGMaterial.LEAD, DepletedRTGMaterial.ZIRCONIUM, DepletedRTGMaterial.ZIRCONIUM,
                DepletedRTGMaterial.LEAD, DepletedRTGMaterial.NEPTUNIUM, DepletedRTGMaterial.LEAD,
                DepletedRTGMaterial.MERCURY, DepletedRTGMaterial.BISMUTH, DepletedRTGMaterial.NEPTUNIUM};
        helper.assertTrue(ModRtgContent.PELLETS.size() == 11, "The old RTG family must contain eleven fuel pellets");
        helper.assertTrue(ModRtgContent.DEPLETED.size() == 6, "The old depleted family must contain six materials");
        int index = 0;
        for (var registered : ModRtgContent.PELLETS.values()) {
            ItemRTGPellet pellet = registered.get();
            helper.assertTrue(pellet.getHeat() == heat[index]
                            && pellet.getHalfLife() == halfLife[index]
                            && pellet.getMaxLifespan() == halfLife[index] * halfLives[index]
                            && pellet.getDecayItem().material == residual[index],
                    "RTG pellet table drifted at index " + index);
            index++;
        }
        helper.succeed();
    }

    public static void decay(GameTestHelper helper) {
        ItemRTGPellet plutonium = ModRtgContent.PELLET_RTG.get();
        ItemStack stack = new ItemStack(plutonium);
        helper.assertTrue(plutonium.getLifespan(stack) == 1_536_504_000L
                        && ItemRTGPellet.getScaledPower(plutonium, stack) == 10,
                "A fresh Pu-238 pellet must start at full life and 10 heat");
        plutonium.setLifespan(stack, plutonium.getMaxLifespan() - plutonium.getHalfLife());
        helper.assertTrue(ItemRTGPellet.getDecay(plutonium, stack) == 0.5D
                        && ItemRTGPellet.getScaledPower(plutonium, stack) == 5,
                "One elapsed half-life must produce exactly one-half output");

        ItemRTGPellet lead = ModRtgContent.PELLET_RTG_LEAD.get();
        ItemStack terminal = new ItemStack(lead);
        lead.setLifespan(terminal, 1L);
        com.hbm.inventory.NTMItemHandler inventory = new com.hbm.inventory.NTMItemHandler(1);
        inventory.setStackInSlot(0, terminal);
        helper.assertTrue(RTGUtil.updateRTGs(inventory, new int[]{0}) == 10
                        && inventory.getStackInSlot(0).is(lead)
                        && lead.getLifespan(inventory.getStackInSlot(0)) == 0L,
                "The final positive-life tick must output before decrement and remain a fuel pellet");
        helper.assertTrue(RTGUtil.updateRTGs(inventory, new int[]{0}) == 10
                        && inventory.getStackInSlot(0).is(ModRtgContent.PELLET_RTG_DEPLETED_BISMUTH.get()),
                "The zero-life tick must output once, then replace the pellet");
        helper.succeed();
    }

    public static void slotsAndOutput(GameTestHelper helper) {
        TileEntityMachineRTG rtg = place(helper, new BlockPos(1, 1, 1));
        helper.assertTrue(rtg.getContainerSize() == 15, "Grey RTG must expose fifteen slots");
        helper.assertTrue(rtg.canPlaceItem(0, new ItemStack(ModRtgContent.PELLET_RTG.get())),
                "RTG slots must accept RTG pellets");
        helper.assertFalse(rtg.canPlaceItem(0, new ItemStack(Items.COAL)), "RTG slots must reject non-pellets");
        helper.assertFalse(rtg.canPlaceItem(0, new ItemStack(ModRtgContent.PELLET_RTG_DEPLETED_LEAD.get())),
                "RTG slots must reject depleted pellets");
        for (int slot = 0; slot < 15; slot++) {
            rtg.inventory.setStackInSlot(slot, new ItemStack(ModRtgContent.PELLET_RTG_BALEFIRE.get()));
        }
        TileEntityMachineRTG.serverTick(helper.getLevel(), rtg.getBlockPos(), rtg.getBlockState(), rtg);
        helper.assertTrue(rtg.heat == 6_000 && rtg.power == 30_000L,
                "Fifteen 6,000-heat pellets must clamp to 6,000 heat and generate 30,000 HE/t");
        helper.succeed();
    }

    public static void persistenceAndSync(GameTestHelper helper) {
        TileEntityMachineRTG original = place(helper, new BlockPos(1, 1, 1));
        original.heat = 321;
        original.power = 654_321L;
        ItemStack pellet = new ItemStack(ModRtgContent.PELLET_RTG.get());
        ModRtgContent.PELLET_RTG.get().setLifespan(pellet, 123_456_789L);
        original.inventory.setStackInSlot(7, pellet);

        CompoundTag tag = dev.golem.core.registry.BlockEntities.save(original, helper.getLevel().registryAccess());
        BlockEntity loaded = dev.golem.core.registry.BlockEntities.load(original.getBlockPos(), original.getBlockState(),
                tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof TileEntityMachineRTG, "Saved grey RTG did not recreate its registered type");
        TileEntityMachineRTG restored = (TileEntityMachineRTG) loaded;
        helper.assertTrue(restored.heat == 321 && restored.power == 654_321L
                        && restored.inventory.getStackInSlot(7).is(ModRtgContent.PELLET_RTG.get())
                        && ModRtgContent.PELLET_RTG.get().getLifespan(restored.inventory.getStackInSlot(7)) == 123_456_789L,
                "Grey RTG heat, power, inventory, or pellet component did not persist");

        TileEntityMachineRTG synced = new TileEntityMachineRTG(BlockPos.ZERO,
                ModMachineBlocks.machine_rtg_grey.get().defaultBlockState());
        var buffer = dev.golem.core.net.GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        original.serialize(buffer);
        synced.deserialize(buffer);
        buffer.release();
        helper.assertTrue(synced.heat == original.heat && synced.power == original.power,
                "Grey RTG heat/power sync did not round-trip");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineRTG rtg = place(helper, new BlockPos(1, 1, 1));
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineRTG menu = new ContainerMachineRTG(0, player.getInventory(), rtg);
        helper.assertTrue(menu.slots.size() == 51, "RTG menu must have 15 machine + 36 player slots");
        for (int slot = 0; slot < 15; slot++) {
            int expectedX = 26 + slot % 5 * 18;
            int expectedY = 22 + slot / 5 * 18;
            helper.assertTrue(menu.slots.get(slot).x == expectedX && menu.slots.get(slot).y == expectedY,
                    "RTG machine slot " + slot + " is misplaced");
        }
        helper.assertTrue(menu.slots.get(15).x == 8 && menu.slots.get(15).y == 94
                        && menu.slots.get(42).x == 8 && menu.slots.get(42).y == 152,
                "RTG player inventory layout drifted");
        helper.assertFalse(menu.slots.get(0).mayPlace(new ItemStack(Items.COAL)),
                "RTG menu slot bypasses machine validation");
        helper.succeed();
    }

    public static void hazards(GameTestHelper helper) {
        ModRtgContent.registerHazards();
        ItemRTGPellet plutonium = ModRtgContent.PELLET_RTG.get();
        ItemStack stack = new ItemStack(plutonium);
        helper.assertTrue(close(radiation(stack), 15D), "Fresh Pu-238 pellet radiation must be 15 RAD/s");
        plutonium.setLifespan(stack, plutonium.getMaxLifespan() - plutonium.getHalfLife());
        helper.assertTrue(close(radiation(stack), 7.5D), "Pu-238 radiation must halve with its output");
        plutonium.setLifespan(stack, 0L);
        ItemStack depletedLead = ItemRTGPellet.handleDecay(stack, plutonium);
        helper.assertTrue(close(radiation(stack), 3.75D) && close(radiation(depletedLead), 0D),
                "Pu-238 must reach its two-half-life radiation, then decay to inert lead");

        ItemRTGPellet lead = ModRtgContent.PELLET_RTG_LEAD.get();
        ItemStack agedLead = new ItemStack(lead);
        lead.setLifespan(agedLead, 0L);
        helper.assertTrue(close(radiation(agedLead), 234.375D)
                        && close(HazardSystem.getHazardLevelFromStack(agedLead, HazardRegistry.HOT), 7D)
                        && close(HazardSystem.getHazardLevelFromStack(agedLead, HazardRegistry.BLINDING), 50D),
                "Lead-209 radiation must decay while heat and blinding remain constant");
        helper.assertTrue(close(radiation(new ItemStack(ModRtgContent.PELLET_RTG_DEPLETED_NEPTUNIUM.get())), 3.75D),
                "Depleted neptunium pellet must retain 3.75 RAD/s");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/models/machines/rtg.obj", "assets/hbm/models/machines/rtg_connector.obj",
                "assets/hbm/textures/models/machines/rtg.png", "assets/hbm/textures/gui/gui_rtg.png",
                "assets/hbm/blockstates/machine_rtg_grey.json", "assets/hbm/items/machine_rtg_grey.json"
        };
        for (String path : required) helper.assertTrue(resourceExists(path), "Missing grey RTG asset: " + path);
        for (String id : ModRtgContent.PELLETS.keySet()) {
            helper.assertTrue(resourceExists("assets/hbm/items/" + id + ".json")
                            && resourceExists("assets/hbm/models/item/" + id + ".json")
                            && resourceExists("assets/hbm/textures/items/" + id + ".png"),
                    "Missing RTG pellet asset set: " + id);
        }
        for (DepletedRTGMaterial material : DepletedRTGMaterial.values()) {
            String id = material.id;
            helper.assertTrue(resourceExists("assets/hbm/items/" + id + ".json")
                            && resourceExists("assets/hbm/models/item/" + id + ".json")
                            && resourceExists("assets/hbm/textures/items/pellet_rtg_depleted."
                            + material.name().toLowerCase(java.util.Locale.ROOT) + ".png"),
                    "Missing depleted RTG pellet asset set: " + id);
        }
        helper.assertTrue(com.hbm.tileentity.ModRtgBlockEntities.MACHINE_RTG.get().isValid(
                        ModMachineBlocks.machine_rtg_grey.get().defaultBlockState()),
                "Grey RTG renderer target block is not registered on its block-entity type");
        helper.assertTrue(ModMachineBlocks.machine_rtg_grey.get().getRenderShape(
                        ModMachineBlocks.machine_rtg_grey.get().defaultBlockState()) == RenderShape.INVISIBLE,
                "Grey RTG must not submit its baked cube alongside RTGRenderer");
        String blockstate = resourceText("assets/hbm/blockstates/machine_rtg_grey.json");
        helper.assertTrue(blockstate.contains("hbm:block/machine_dummy_empty")
                        && !blockstate.contains("hbm:block/machine_rtg_grey"),
                "Grey RTG blockstate must leave world geometry exclusively to RTGRenderer");
        String itemDefinition = resourceText("assets/hbm/items/machine_rtg_grey.json");
        helper.assertTrue(itemDefinition.contains("minecraft:special")
                        && itemDefinition.contains("hbm:item/machine_obj_base")
                        && itemDefinition.contains("hbm:machine_obj")
                        && itemDefinition.contains("hbm:models/machines/rtg.obj")
                        && itemDefinition.contains("hbm:textures/models/machines/rtg.png")
                        && !itemDefinition.contains("hbm:block/machine_rtg_grey"),
                "Grey RTG item must resolve to the 3D OBJ, not the old cube model");
        helper.assertFalse(com.hbm.inventory.gui.RtgGuiLayout.overlaps(
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_X,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_Y,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_WIDTH,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_HEIGHT,
                        com.hbm.inventory.gui.RtgGuiLayout.HEAT_X,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_Y,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_WIDTH,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_HEIGHT),
                "JEI Show Recipes area must not overlap the RTG heat tooltip");
        helper.assertFalse(com.hbm.inventory.gui.RtgGuiLayout.overlaps(
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_X,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_Y,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_WIDTH,
                        com.hbm.inventory.gui.RtgGuiLayout.RECIPE_CLICK_HEIGHT,
                        com.hbm.inventory.gui.RtgGuiLayout.POWER_X,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_Y,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_WIDTH,
                        com.hbm.inventory.gui.RtgGuiLayout.GAUGE_HEIGHT),
                "JEI Show Recipes area must not overlap the RTG energy tooltip");
        helper.succeed();
    }

    public static void recipes(GameTestHelper helper) {
        for (var depleted : ModRtgContent.DEPLETED.values()) {
            ItemStack remainder = dev.golem.core.world.Compat.craftingRemainder(new ItemStack(depleted.get()));
            helper.assertTrue(remainder.is(com.hbm.items.ModItems.plate_iron.get()),
                    "A recycled depleted pellet must return its iron plate casing");
        }
        for (String id : new String[]{"rtg_fuel_pu238", "rtg_fuel_radium", "rtg_fuel_weak",
                "rtg_fuel_strontium", "rtg_fuel_cobalt", "rtg_fuel_actinium", "rtg_fuel_americium",
                "rtg_fuel_polonium", "rtg_fuel_gold", "rtg_fuel_lead", "rtg_recover_bismuth",
                "rtg_recover_lead", "rtg_recover_neptunium", "rtg_recover_zirconium"}) {
            helper.assertTrue(dev.golem.core.world.Recipes.hasRecipe(helper.getLevel(), "hbm", id),
                    "Missing dependency-complete old RTG recipe: " + id);
        }
        RTGRecipes.registerAssembly();
        var assembly = com.hbm.inventory.recipes.AssemblyMachineRecipes.INSTANCE.getRecipe("ass.rtg");
        helper.assertTrue(assembly != null && assembly.duration == 200 && assembly.power == 100
                        && assembly.outputItem.length == 1
                        && assembly.outputItem[0].is(ModMachineBlocks.machine_rtg_grey_item.get()),
                "Old ass.rtg recipe is missing or has incorrect duration/power/output");
        helper.succeed();
    }

    private static TileEntityMachineRTG place(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModMachineBlocks.machine_rtg_grey.get().defaultBlockState());
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, relative, TileEntityMachineRTG.class);
    }

    private static double radiation(ItemStack stack) {
        return HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.000001D; }

    private static boolean resourceExists(String path) {
        try (InputStream stream = RtgGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String resourceText(String path) {
        try (InputStream stream = RtgGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }
}
