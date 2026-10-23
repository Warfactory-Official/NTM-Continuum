package com.hbm.gametest;

import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.neutron.NeutronHandler;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.inventory.recipes.RBMKFuelDisassemblyRecipe;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDebris;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

/** Deterministic RBMK contracts copied from the current ntm-next implementation. */
public final class RBMKGameTests {

    private RBMKGameTests() {}

    public static void registry(GameTestHelper helper) {
        checkId(helper, ModRBMKBlocks.rbmk_debris.get(), "rbmk_debris");
        checkId(helper, ModRBMKBlocks.block_graphite.get(), "block_graphite");
        checkId(helper, ModRBMKItems.rbmk_lid_glass.get(), "rbmk_lid_glass");
        checkId(helper, ModRBMKItems.particle_digamma.get(), "particle_digamma");
        checkId(helper, ModRBMKItems.rbmk_fuel_drx.get(), "rbmk_fuel_drx");
        var control = ModTabs.CONTENTS.get(ModTabs.get(HbmTab.CONTROL).getKey());
        helper.assertTrue(control.contains(ModRBMKItems.rbmk_lid) && control.contains(ModRBMKItems.rbmk_lid_glass),
                "Both RBMK lids are listed in the control tab, as in 1.7");
        helper.assertTrue(ModTabs.CONTENTS.get(ModTabs.get(HbmTab.CONSUMABLES).getKey()).contains(ModRBMKItems.rbmk_tool),
                "The RBMK tool is listed in the consumables tab, as in 1.7");

        ItemRBMKRod drx = ModRBMKItems.rbmk_fuel_drx.get();
        helper.assertTrue(drx.pelletItem() == ModRBMKItems.pelletFor(drx),
                "The post-bootstrap DRX rod-to-pellet link must resolve to its registered pellet");
        helper.assertTrue(ModRBMKItems.pelletFor(drx).isXenonEnabled(),
                "DRX pellets retain xenon variants; only the explicit source families suppress them");
        ItemRBMKRod heu233 = ModRBMKItems.rbmk_fuel_heu233.get();
        helper.assertTrue(heu233.getFuncDescription(new ItemStack(heu233)).contains("x / 100 *"),
                "The fuel tooltip must retain the exact linear flux formula");
        helper.assertTrue(ItemRBMKRod.EnumBurnFunc.LOG_TEN.title.equals("trait.rbmx.flux.logten")
                        && ItemRBMKRod.NType.SLOW.unlocalized.equals("trait.rbmk.neutron.slow"),
                "RBMK tooltip enum metadata must retain its translation keys");
        helper.succeed();
    }

    public static void foldedColumn(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        var block = ModRBMKBlocks.rbmk_fuel_rod.get();
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.setBlock(core, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(core), block.defaultBlockState(), player,
                new ItemStack(block));

        helper.assertTrue(GameTestHelpers.blockEntity(helper, core, BlockEntityRBMKRod.class) != null,
                "The folded RBMK core must create its fuel-channel block entity");
        for (int y = 1; y <= 3; y++) {
            BlockPos cell = core.above(y);
            RBMKBase.Part expected = y == 3 ? RBMKBase.Part.TOP : RBMKBase.Part.MIDDLE;
            helper.assertTrue(helper.getBlockState(cell).is(block)
                            && helper.getBlockState(cell).getValue(RBMKBase.PART) == expected,
                    "Column segment " + y + " must be the expected folded part");
            helper.assertTrue(helper.absolutePos(core).equals(MultiblockSurface.coreOfCell(helper.getLevel(), helper.absolutePos(cell))),
                    "Folded segment " + y + " must resolve to its owning core");
        }

        helper.getLevel().removeBlock(helper.absolutePos(core.above(1)), false);
        for (int y = 0; y <= 3; y++) {
            helper.assertFalse(helper.getBlockState(core.above(y)).is(block),
                    "Breaking one folded segment must tear down the entire column, leaked segment " + y);
        }
        helper.succeed();
    }

    public static void fuel(GameTestHelper helper) {
        ItemRBMKRod rod = ModRBMKItems.rbmk_fuel_heu233.get();
        ItemStack fuel = new ItemStack(rod);
        ItemRBMKRod.setYield(fuel, 50_000_000D);
        ItemRBMKRod.setPoison(fuel, 20D);
        double output = rod.burn(helper.getLevel(), fuel, 100D);
        // Xenon burns against the original 100 flux, then reduces it to 80. The default gentle-slope
        // enrichment factor at 50% is 5/6, so linear HEU-233 emits 80 * 5/6 * 27.5 / 100 = 55/3.
        helper.assertTrue(Math.abs(output - 55D / 3D) < 1.0E-9 && ItemRBMKRod.getYield(fuel) == 49_999_920D,
                "HEU-233 burn must emit 55/3 flux and deplete exactly the post-xenon 80 flux");
        helper.assertTrue(ItemRBMKRod.getPoison(fuel) == 0D
                        && Math.abs(ItemRBMKRod.getCoreHeat(fuel) - (20D + 275D / 12D)) < 1.0E-9,
                "Fuel components must retain the analytic xenon and core-heat results");

        ItemStack depleted = new ItemStack(rod);
        ItemRBMKRod.setYield(depleted, 60_000_000D);
        ItemRBMKRod.setPoison(depleted, 50D);
        ItemRBMKRod.setCoreHeat(depleted, 49D);
        ItemRBMKRod.setHullHeat(depleted, 49D);
        var recipe = RBMKFuelDisassemblyRecipe.INSTANCE;
        helper.assertTrue(recipe.matchesItemStacks(List.of(depleted), helper.getLevel()),
                "A cool linked rod must match fuel disassembly");
        ItemStack pellets = recipe.assembleItemStacks(List.of(depleted));
        helper.assertTrue(pellets.is(ModRBMKItems.pelletFor(rod)) && pellets.getCount() == 8
                        && ((ItemRBMKPellet) pellets.getItem()).getVariant(pellets) == 7
                        && ModDataComponents.RBMK_PELLET.getOr(pellets, null) != null
                        && ModDataComponents.RBMK_PELLET.getOr(pellets, null).depletion() == 2
                        && ModDataComponents.RBMK_PELLET.getOr(pellets, null).xenon(),
                "60% depleted, xenon-poisoned fuel must disassemble into eight variant-7 pellets");
        ItemRBMKRod.setHullHeat(depleted, 50D);
        helper.assertFalse(recipe.matchesItemStacks(List.of(depleted), helper.getLevel()),
                "Fuel at the 50C safety boundary must not disassemble");
        helper.succeed();
    }

    public static void control(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_control.get().defaultBlockState());
        BlockEntityRBMKControl control = GameTestHelpers.blockEntity(helper, pos,
                BlockEntityRBMKControl.class);
        CompoundTag packet = new CompoundTag();
        packet.putDouble("level", 4D);
        control.receiveControl(packet);
        helper.assertTrue(control.targetLevel == 1D, "Control packets must clamp levels above one");
        packet.putDouble("level", -1D);
        control.receiveControl(packet);
        helper.assertTrue(control.targetLevel == 0D, "Control packets must clamp levels below zero");
        helper.succeed();
    }

    public static void neutrons(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 1, 1), targetPos = sourcePos.east();
        helper.setBlock(sourcePos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        helper.setBlock(targetPos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        BlockEntityRBMKRod source = GameTestHelpers.blockEntity(helper, sourcePos, BlockEntityRBMKRod.class);
        BlockEntityRBMKRod target = GameTestHelpers.blockEntity(helper, targetPos, BlockEntityRBMKRod.class);
        source.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_po210be.get()));
        target.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        target.tickServer(); // marks the receiving channel as fuelled without creating a stream
        source.tickServer();
        helper.assertTrue(NeutronNodeWorld.getNode(helper.getLevel(), helper.absolutePos(sourcePos)) != null,
                "A producing rod must retain a cached neutron node until its output reaches zero");
        NeutronHandler.onServerTick(helper.getLevel().getServer());
        helper.assertTrue(target.fluxQuantity == 50D,
                "The passive Po-210/Be source must deliver its analytic 50 slow flux to the adjacent fuel rod");
        target.inventory.setStackInSlot(0, ItemStack.EMPTY);
        target.tickServer();
        helper.succeed();
    }

    public static void boiler(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_boiler.get().defaultBlockState());
        BlockEntityRBMKBoiler boiler = GameTestHelpers.blockEntity(helper, pos,
                BlockEntityRBMKBoiler.class);
        boiler.feed.setFill(100);
        boiler.heat = 110D;
        boiler.tickServer();
        helper.assertTrue(boiler.consumption == 100 && boiler.output == 10_000
                        && boiler.feed.getFill() == 0 && boiler.steam.getFill() == 10_000,
                "At 110C, 100 mB water must become exactly 10,000 mB steam at the 0.1 heat cost");
        helper.succeed();
    }

    public static void debris(GameTestHelper helper) {
        for (var block : List.of(ModRBMKBlocks.rbmk_debris.get(), ModRBMKBlocks.rbmk_debris_burning.get(),
                ModRBMKBlocks.rbmk_debris_radiating.get(), ModRBMKBlocks.rbmk_debris_digamma.get())) {
            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, block.defaultBlockState());
            helper.assertTrue(GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKDebris.class) != null,
                    "Debris variant " + BuiltInRegistries.BLOCK.getKey(block) + " must use the RBMK debris state carrier");
        }
        helper.succeed();
    }

    /**
     * The RoR console panels against their 1.7 contracts: a non-polling key broadcasts its command once,
     * a lever takes ten ticks to throw and then sends its ON command, the indicator window inverts when
     * max < min, the graph shifts its 30 samples left, and a steam connector two blocks under a column
     * opens its four sides and underside to the column's output.
     */
    public static void rorPanels(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos keyPos = new BlockPos(1, 1, 1);
        helper.setBlock(keyPos, ModRBMKBlocks.rbmk_key_pad.get().defaultBlockState());
        var keypad = GameTestHelpers.blockEntity(helper, keyPos, BlockEntityRBMKKeyPad.class);
        helper.assertTrue(keypad != null, "The keypad must create its block entity");
        String keyChannel = "gt_rbmk_kp_" + level.getGameTime();
        keypad.keys[0].active = true;
        keypad.keys[0].rtty = keyChannel;
        keypad.keys[0].command = "42";
        keypad.keys[0].click();
        var keyKey = new RTTYSystem.ChannelKey(level.dimension(), keyChannel);
        helper.assertTrue("42".equals(String.valueOf(RTTYSystem.newMessages.get(keyKey)))
                        && keypad.keys[0].isPressed && keypad.keys[0].clickTimer == 7,
                "A non-polling key must broadcast its command and hold down for seven ticks");
        RTTYSystem.clearChannel(level, keyChannel);

        BlockPos leverPos = new BlockPos(2, 1, 1);
        helper.setBlock(leverPos, ModRBMKBlocks.rbmk_lever.get().defaultBlockState());
        var lever = GameTestHelpers.blockEntity(helper, leverPos, BlockEntityRBMKLever.class);
        String leverChannel = "gt_rbmk_lv_" + level.getGameTime();
        lever.levers[1].active = true;
        lever.levers[1].rtty = leverChannel;
        lever.levers[1].commandOn = "7";
        lever.levers[1].click();
        for (int i = 0; i < 9; i++) lever.tickServer();
        helper.assertTrue(lever.levers[1].flipProgress < 1F, "The lever must still be moving after nine ticks");
        lever.tickServer();
        var leverKey = new RTTYSystem.ChannelKey(level.dimension(), leverChannel);
        helper.assertTrue(lever.levers[1].flipProgress == 1F
                        && "7".equals(String.valueOf(RTTYSystem.newMessages.get(leverKey))),
                "A non-polling lever must reach the bottom on the tenth tick and send its ON command once");
        RTTYSystem.clearChannel(level, leverChannel);

        BlockPos lampPos = new BlockPos(3, 1, 1);
        helper.setBlock(lampPos, ModRBMKBlocks.rbmk_indicator.get().defaultBlockState());
        var lamp = GameTestHelpers.blockEntity(helper, lampPos, BlockEntityRBMKIndicator.class).indicators[0];
        lamp.min = 10; lamp.max = 20;
        lamp.decideLight(15); boolean inside = lamp.light;
        lamp.decideLight(25); boolean outside = lamp.light;
        lamp.min = 20; lamp.max = 10;
        lamp.decideLight(25); boolean invertedOutside = lamp.light;
        lamp.decideLight(15); boolean invertedInside = lamp.light;
        helper.assertTrue(inside && !outside && invertedOutside && !invertedInside,
                "The indicator lights inside [min, max] and outside it once max < min");

        BlockPos graphPos = new BlockPos(4, 1, 1);
        helper.setBlock(graphPos, ModRBMKBlocks.rbmk_graph.get().defaultBlockState());
        var graph = GameTestHelpers.blockEntity(helper, graphPos, BlockEntityRBMKGraph.class).graphs[0];
        graph.pushValue(5); graph.pushValue(9);
        helper.assertTrue(graph.values[29] == 9 && graph.values[28] == 5 && graph.values[27] == 0,
                "Graph samples must shift left with the newest at the end");

        BlockPos core = new BlockPos(1, 3, 3);
        helper.setBlock(core.below(2), ModRBMKBlocks.rbmk_loader.get().defaultBlockState());
        var positions = RBMKLoader.outputPositions(level, helper.absolutePos(core), false);
        helper.assertTrue(positions.length == 6 && positions[5].getPos().equals(helper.absolutePos(core.below(3))),
                "A connector two blocks below the core must add its four sides and underside to the outputs");
        helper.assertTrue(RBMKLoader.outputPositions(level, helper.absolutePos(new BlockPos(5, 3, 5)), false).length == 1,
                "Without a connector only the column top is an output");
        helper.succeed();
    }

    private static void checkId(GameTestHelper helper, Object entry, String path) {
        Identifier expected = Ids.of("hbm", path);
        if (entry instanceof Block block) {
            helper.assertTrue(expected.equals(BuiltInRegistries.BLOCK.getKey(block)), "Wrong block id for " + path);
        } else if (entry instanceof Item item) {
            helper.assertTrue(expected.equals(BuiltInRegistries.ITEM.getKey(item)), "Wrong item id for " + path);
        } else {
            helper.fail("Unsupported registry entry for " + path);
        }
    }
}
