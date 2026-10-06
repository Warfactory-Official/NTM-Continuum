package com.hbm.gametest;

import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileChannel;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileChannelType;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileOrientation;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;

/**
 * GameTests for the Chicago Pile: assembling a graphite box into a reactor, drilling the three kinds of
 * channel through it, running the neutron economy, and the devices that dock onto the channel mouths.
 */
public final class PileGameTests {

    private PileGameTests() {}

    /** The clicked cell that becomes the core: centre of the box's north face. */
    private static final BlockPos CORE = new BlockPos(4, 3, 2);
    /** Mouth of the fuel channel drilled south through the box. */
    private static final BlockPos FUEL_MOUTH = new BlockPos(5, 3, 2);
    /** Mouth of the control channel drilled down through the box. */
    private static final BlockPos CONTROL_MOUTH = new BlockPos(4, 5, 4);
    /** Mouth of the ventilation channel drilled west through the box. */
    private static final BlockPos VENT_MOUTH = new BlockPos(6, 4, 3);

    public static void assembly(GameTestHelper helper) {
        TileEntityPileCore core = assemble(helper);

        helper.assertTrue(core.orientation == PileOrientation.NORTH_SOUTH,
                "Pile took its axis from the wrong direction, got " + core.orientation);
        helper.assertTrue(core.height == 5 && core.width == 5 && core.depth == 5,
                "Pile measured itself wrong: " + core.height + "x" + core.width + "x" + core.depth);
        helper.assertTrue(core.left == 2 && core.right == 2 && core.up == 2,
                "Pile measured its core offset wrong");

        assertMeta(helper, CORE, BlockPile.META_CORE);
        // A corner shares two extremes, so it is an edge; a face centre shares only one.
        assertMeta(helper, new BlockPos(2, 1, 2), BlockPile.META_EDGE);
        assertMeta(helper, new BlockPos(6, 5, 6), BlockPile.META_EDGE);
        assertMeta(helper, new BlockPos(4, 3, 4), BlockPile.META_DUMMY);

        TileEntityPileBaseMK2 cell = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, new BlockPos(4, 3, 4), TileEntityPileBaseMK2.class);
        helper.assertTrue(cell != null && cell.getCore() == core, "Pile cell did not point at its core");
        helper.succeed();
    }

    public static void channels(GameTestHelper helper) {
        TileEntityPileCore core = assemble(helper);
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);

        drill(helper, player, FUEL_MOUTH, Direction.NORTH);
        drill(helper, player, CONTROL_MOUTH, Direction.UP);
        drill(helper, player, VENT_MOUTH, Direction.EAST);

        helper.assertTrue(core.fuelChannels.size() == 1, "Pile did not record a fuel channel");
        helper.assertTrue(core.controlChannels.size() == 1, "Pile did not record a control channel");
        helper.assertTrue(core.ventilationChannels.size() == 1, "Pile did not record a ventilation channel");

        PileChannel fuel = core.fuelChannels.get(0);
        helper.assertTrue(fuel.type == PileChannelType.FUEL && fuel.length == 5,
                "Fuel channel has the wrong type or length");
        helper.assertTrue(core.controlChannels.get(0).type == PileChannelType.CONTROL,
                "Vertical bore should always be a control channel");
        helper.assertTrue(core.ventilationChannels.get(0).type == PileChannelType.VENTILATION,
                "Bore across the pile's axis should be a ventilation channel");

        assertMeta(helper, FUEL_MOUTH, BlockPile.META_FUEL_IN);
        assertMeta(helper, new BlockPos(5, 3, 4), BlockPile.META_CHANNEL);
        assertMeta(helper, new BlockPos(5, 3, 6), BlockPile.META_FUEL_OUT);
        assertMeta(helper, CONTROL_MOUTH, BlockPile.META_CONTROL);
        assertMeta(helper, new BlockPos(4, 1, 4), BlockPile.META_CONTROL);
        assertMeta(helper, VENT_MOUTH, BlockPile.META_AIR_IN);
        assertMeta(helper, new BlockPos(2, 4, 3), BlockPile.META_AIR_OUT);

        // Drilling the same mouth again fills the channel back in.
        drill(helper, player, FUEL_MOUTH, Direction.NORTH);
        helper.assertTrue(core.fuelChannels.isEmpty(), "Re-drilling a mouth did not remove its channel");
        assertMeta(helper, FUEL_MOUTH, BlockPile.META_DUMMY);
        helper.succeed();
    }

    /** A neutron source alone makes flux; a breeding rod beside it takes burn-up and heats the channel. */
    public static void reaction(GameTestHelper helper) {
        TileEntityPileCore core = assemble(helper);
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        drill(helper, player, FUEL_MOUTH, Direction.NORTH);

        PileChannel fuel = core.fuelChannels.get(0);
        fuel.loadItem(rod(EnumPileRod.RA226BE));
        fuel.loadItem(rod(EnumPileRod.NU));
        fuel.heat = 20D;

        BlockPos corePos = helper.absolutePos(CORE);
        BlockState state = helper.getLevel().getBlockState(corePos);
        for (int i = 0; i < 20; i++) {
            TileEntityPileCore.serverTick(helper.getLevel(), corePos, state, core);
        }

        helper.assertTrue(fuel.outgoingNeutrons > 0D,
                "Fuel channel produced no neutrons with a source in it");
        helper.assertTrue(fuel.heat > 20D,
                "Fuel channel never heated up, got " + fuel.heat);

        ItemStack uranium = fuel.rods[0];
        helper.assertTrue(ItemPileRodMK2.typeOf(uranium) == EnumPileRod.NU,
                "The uranium rod should still be the rod nearest the mouth");
        helper.assertTrue(ItemPileRodMK2.getDepletion(uranium) > 0D,
                "The uranium rod took no burn-up");
        helper.succeed();
    }

    /** The loader docks to the fuel mouth and reports the far rod; the control rod docks to its channel. */
    public static void devices(GameTestHelper helper) {
        TileEntityPileCore core = assemble(helper);
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        drill(helper, player, FUEL_MOUTH, Direction.NORTH);
        drill(helper, player, CONTROL_MOUTH, Direction.UP);
        drill(helper, player, VENT_MOUTH, Direction.EAST);

        PileChannel fuel = core.fuelChannels.get(0);
        for (int i = 0; i < 5; i++) fuel.loadItem(rod(EnumPileRod.NU));
        fuel.heat = 123D;

        TileEntityPileLoader loader = device(helper, new BlockPos(5, 3, 1), 0, TileEntityPileLoader.class);
        TileEntityPileControl control = device(helper, new BlockPos(4, 6, 4), 8, TileEntityPileControl.class);
        TileEntityPileVent vent = device(helper, new BlockPos(7, 4, 3), 7, TileEntityPileVent.class);

        helper.assertTrue(loader.getOrientation() == ForgeDirection.NORTH, "Loader faces the wrong way");
        helper.assertTrue(vent.getOrientation() == ForgeDirection.EAST, "Vent faces the wrong way");

        tick(helper, new BlockPos(5, 3, 1), loader, TileEntityPileLoader::serverTick);
        helper.assertTrue(!loader.channelStack.isEmpty(),
                "Loader did not find the rod at the far end of its channel");
        helper.assertTrue("123".equals(loader.provideRORValue("VAL:temp")),
                "Loader reported the wrong channel temperature, got " + loader.provideRORValue("VAL:temp"));
        helper.assertTrue(("" + EnumPileRod.NU.ordinal()).equals(loader.provideRORValue("VAL:meta")),
                "Loader reported the wrong rod type");
        helper.assertTrue("25000".equals(loader.provideRORValue("VAL:lifetime")),
                "Loader reported the wrong rod lifetime");

        control.runRORFunction("FUN:setrods", new String[]{"100"});
        helper.assertTrue(control.targetExtraction == 1D, "Radio setrods did not set the target");
        control.runRORFunction("FUN:extendrods", new String[]{"-40"});
        helper.assertTrue(Math.abs(control.targetExtraction - 0.6D) < 1e-9,
                "Radio extendrods did not shift the target, got " + control.targetExtraction);

        for (int i = 0; i < 100; i++) {
            tick(helper, new BlockPos(4, 6, 4), control, TileEntityPileControl::serverTick);
        }
        helper.assertTrue(Math.abs(control.extraction - 0.6D) < 1e-6,
                "Control rod did not travel to its target, got " + control.extraction);
        helper.assertTrue(Math.abs(core.controlChannels.get(0).control - control.extraction) < 1e-6,
                "Control rod did not push its position into the channel");

        vent.compair.setFill(vent.compair.getMaxFill());
        tick(helper, new BlockPos(7, 4, 3), vent, TileEntityPileVent::serverTick);
        helper.assertTrue(vent.isActive && core.ventilationChannels.get(0).air > 0,
                "Vent did not move air into its channel");
        helper.succeed();
    }

    /** Breaking one cell takes the core with it, and the rest of the pile reverts to loose graphite. */
    public static void collapse(GameTestHelper helper) {
        TileEntityPileCore core = assemble(helper);
        BlockPos far = new BlockPos(4, 3, 4);

        helper.destroyBlock(far);
        helper.assertTrue(core.isRemoved() || helper.getLevel()
                        .getBlockState(helper.absolutePos(CORE)).is(ModPileBlocks.pile_brick.get()),
                "Breaking a pile cell did not take the core with it");

        BlockPos cell = new BlockPos(2, 3, 4);
        for (int i = 0; i < 5; i++) {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(cell)) instanceof TileEntityPileBaseMK2 pile) {
                TileEntityPileBaseMK2.serverTick(helper.getLevel(), helper.absolutePos(cell),
                        helper.getLevel().getBlockState(helper.absolutePos(cell)), pile);
            }
        }
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(cell))
                        .is(ModPileBlocks.pile_brick.get()),
                "A coreless pile cell did not revert to graphite");
        helper.succeed();
    }

    public static void rods(GameTestHelper helper) {
        ItemStack uranium = rod(EnumPileRod.NU);
        helper.assertTrue(ItemPileRodMK2.typeOf(uranium) == EnumPileRod.NU, "Rod lost its variant");
        helper.assertTrue(ItemPileRodMK2.getReactivity(uranium, 0D) == 0D,
                "A breeding rod with no incoming flux must not produce neutrons");
        helper.assertTrue(ItemPileRodMK2.getReactivity(rod(EnumPileRod.RA226BE), 0D) == 1D,
                "A neutron source must produce flux with no input");
        helper.assertTrue(ItemPileRodMK2.getReactivity(rod(EnumPileRod.ZR), 100D) == 0D,
                "Zirconium must stay transparent to neutrons");

        ItemStack spent = ItemPileRodMK2.react(uranium, EnumPileRod.NU.life);
        helper.assertTrue(ItemPileRodMK2.typeOf(spent) == EnumPileRod.PU239,
                "A spent uranium rod should breed into plutonium-239");
        helper.assertTrue(ItemPileRodMK2.getDepletion(spent) == 0D,
                "The bred rod should start fresh");

        ItemStack partial = ItemPileRodMK2.react(rod(EnumPileRod.NU), 100D);
        helper.assertTrue(ItemPileRodMK2.getDepletionPercent(partial) > 0D
                        && ItemPileRodMK2.getDepletionPercent(partial) < 1D,
                "A partly burnt rod should report a small depletion");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/models/pile/pile_loader.obj",
                "assets/hbm/models/pile/pile_vent.obj",
                "assets/hbm/models/pile/pile_control.obj",
                "assets/hbm/textures/models/pile/pile_loader.png",
                "assets/hbm/textures/models/pile/pile_vent.png",
                "assets/hbm/textures/models/pile/pile_control.png",
                "assets/hbm/textures/blocks/pile_block.png",
                "assets/hbm/textures/blocks/pile_block_core.png",
                "assets/hbm/textures/blocks/pile_brick.png",
                "assets/hbm/textures/items/pile_rod_nu.png",
                "assets/hbm/blockstates/pile_block.json",
                "assets/hbm/blockstates/pile_brick.json",
                "assets/hbm/blockstates/pile_device.json",
                "assets/hbm/items/pile_loader.json",
                "assets/hbm/items/pile_rod_mk2_nu.json",
                "assets/hbm/sounds/weapon/reload/boltopen.ogg",
                "data/hbm/loot_table/blocks/pile_brick.json",
                "data/hbm/loot_table/blocks/pile_device.json",
                "data/hbm/recipe/pile_device_loader.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing Chicago Pile asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    /** Builds the 5x5x5 graphite box and drills its core out of the centre of the north face. */
    private static TileEntityPileCore assemble(GameTestHelper helper) {
        BlockState brick = ModPileBlocks.pile_brick.get().defaultBlockState();
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 5; y++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(new BlockPos(x, y, z), brick);
                }
            }
        }

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        boolean handled = ModPileBlocks.pile_brick.get().onScrew(helper.getLevel(), player,
                helper.absolutePos(CORE), Direction.NORTH, Vec3.ZERO, InteractionHand.MAIN_HAND,
                com.hbm.api.block.IToolable.ToolType.HAND_DRILL);
        helper.assertTrue(handled, "Hand drill did not act on the graphite box");

        TileEntityPileCore core = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, CORE, TileEntityPileCore.class);
        helper.assertTrue(core != null, "Pile core BlockEntity was not created");
        return core;
    }

    private static void drill(GameTestHelper helper, Player player, BlockPos relative, Direction side) {
        boolean handled = ModPileBlocks.pile_block.get().onScrew(helper.getLevel(), player,
                helper.absolutePos(relative), side, Vec3.ZERO, InteractionHand.MAIN_HAND,
                com.hbm.api.block.IToolable.ToolType.HAND_DRILL);
        helper.assertTrue(handled, "Hand drill was refused at " + relative + " from " + side);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T device(
            GameTestHelper helper, BlockPos relative, int meta, Class<T> type) {
        helper.setBlock(relative, ModPileBlocks.pile_device.get().defaultBlockState()
                .setValue(BlockPileDevice.META, meta));
        T device = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, relative, type);
        helper.assertTrue(device != null, "Pile device BlockEntity was not created at " + relative);
        return device;
    }

    private interface Ticker<T extends net.minecraft.world.level.block.entity.BlockEntity> {
        void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, T device);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> void tick(
            GameTestHelper helper, BlockPos relative, T device, Ticker<T> ticker) {
        BlockPos absolute = helper.absolutePos(relative);
        ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), device);
    }

    private static ItemStack rod(EnumPileRod type) {
        return ModPileBlocks.pile_rod.stack(type);
    }

    private static void assertMeta(GameTestHelper helper, BlockPos relative, int expected) {
        BlockState state = helper.getLevel().getBlockState(helper.absolutePos(relative));
        helper.assertTrue(state.hasProperty(BlockPile.META) && state.getValue(BlockPile.META) == expected,
                "Pile cell at " + relative + " should have meta " + expected + ", got "
                        + (state.hasProperty(BlockPile.META) ? state.getValue(BlockPile.META) : state));
    }

    private static boolean resourceExists(String path) {
        return dev.golem.core.gametest.GameTestHelpers.resourceExists(PileGameTests.class, path);
    }
}
