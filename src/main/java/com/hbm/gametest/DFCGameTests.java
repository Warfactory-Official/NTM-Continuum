package com.hbm.gametest;

import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemLens;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;

/**
 * GameTests for the dark fusion core: the beam walking from the emitter through the chamber to the
 * receiver, the fuel the injector pushes in, the field the stabiliser holds, and what happens when it
 * lets go.
 */
public final class DFCGameTests {

    private DFCGameTests() {}

    /** The reaction chamber, with the aimed parts arranged around it. */
    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    private static final BlockPos EMITTER = new BlockPos(1, 2, 4);
    private static final BlockPos RECEIVER = new BlockPos(7, 2, 4);
    private static final BlockPos INJECTOR = new BlockPos(4, 2, 1);
    private static final BlockPos STABILIZER = new BlockPos(4, 2, 7);

    public static void beam(GameTestHelper helper) {
        TileEntityCore core = build(helper);
        TileEntityCoreEmitter emitter = be(helper, EMITTER, TileEntityCoreEmitter.class);
        TileEntityCoreReceiver receiver = be(helper, RECEIVER, TileEntityCoreReceiver.class);

        emitter.watts = 100;
        emitter.isOn = true;
        emitter.power = TileEntityCoreEmitter.maxPower;
        emitter.tank.setFill(emitter.tank.getMaxFill());
        receiver.tank.setFill(receiver.tank.getMaxFill());

        tick(helper, EMITTER, emitter, TileEntityCoreEmitter::serverTick);
        helper.assertTrue(emitter.beam > 0, "A running emitter should project a beam");

        tick(helper, EMITTER, emitter, TileEntityCoreEmitter::serverTick);
        helper.assertTrue(receiver.joules > 0, "The receiver should have caught the beam");

        tick(helper, RECEIVER, receiver, TileEntityCoreReceiver::serverTick);
        helper.assertTrue(receiver.power > 0, "Caught joules should become HE");
        helper.assertTrue(receiver.tank.getFill() < receiver.tank.getMaxFill(),
                "A working receiver should be spending cryogel");
        helper.succeed();
    }

    public static void reaction(GameTestHelper helper) {
        TileEntityCore core = build(helper);
        try {
            fuel(core);
            core.inventory.setStackInSlot(1, new ItemStack(ModDFCBlocks.ams_core_sing.get()));
            catalysts(core);

            helper.assertTrue(core.hasCore(), "The chamber should see its exotic core");
            helper.assertTrue(core.getCore() == 500, "The vibrant singularity multiplies by five hundred");

            tick(helper, CORE, core, TileEntityCore::serverTick);
            helper.assertTrue(core.isReady(), "A fuelled, cored, catalysed chamber should be ready");
            helper.assertTrue(core.color != 0, "Two catalysts should give the chamber a colour");

            long before = core.tanks[0].getFill();
            long out = core.burn(1_000_000L);
            helper.assertTrue(out > 1_000_000L, "The reaction should give back more than it was handed");
            helper.assertTrue(core.heat > 0, "Burning should leave heat behind");
            helper.assertTrue(core.tanks[0].getFill() < before, "Burning should consume fuel");

            core.inventory.setStackInSlot(1, ItemStack.EMPTY);
            helper.assertTrue(core.burn(1_000_000L) == 1_000_000L,
                    "Without an exotic core the beam should pass through untouched");
            // The direct burn above intentionally leaves heat behind to prove the reaction.  Quench the
            // fixture before returning: the real block entity keeps ticking after helper.succeed(), and
            // would otherwise detonate on the next server tick while the suite is running.
            helper.succeed();
        } finally {
            quench(core);
        }
    }

    public static void stabilizer(GameTestHelper helper) {
        TileEntityCore core = build(helper);
        TileEntityCoreStabilizer stabilizer = be(helper, STABILIZER, TileEntityCoreStabilizer.class);

        stabilizer.watts = 50;
        stabilizer.power = TileEntityCoreStabilizer.maxPower;
        ItemStack lens = new ItemStack(ModDFCBlocks.ams_lens.get());
        stabilizer.inventory.setStackInSlot(0, lens);

        tick(helper, STABILIZER, stabilizer, TileEntityCoreStabilizer::serverTick);

        helper.assertTrue(stabilizer.beam > 0, "The stabiliser should have found the chamber");
        helper.assertTrue(core.field >= 50, "The chamber should be holding the field it was given");
        helper.assertTrue(ItemLens.getLensDamage(stabilizer.inventory.getStackInSlot(0)) == 50,
                "Holding a field should wear the lens by the watts it took");
        helper.assertTrue(stabilizer.power < TileEntityCoreStabilizer.maxPower,
                "Holding a field should cost power");

        helper.assertTrue(stabilizer.provideRORValue("VAL:durability") != null
                        && !"0".equals(stabilizer.provideRORValue("VAL:durability")),
                "VAL:durability should answer while a lens is in");
        stabilizer.runRORFunction("FUN:setpower", new String[]{"7"});
        helper.assertTrue(stabilizer.watts == 7, "setpower should move the field strength");
        helper.succeed();
    }

    public static void injector(GameTestHelper helper) {
        TileEntityCore core = build(helper);
        TileEntityCoreInjector injector = be(helper, INJECTOR, TileEntityCoreInjector.class);

        injector.tanks[0].setTankType(Fluids.DEUTERIUM);
        injector.tanks[1].setTankType(Fluids.TRITIUM);
        injector.tanks[0].setFill(50_000);
        injector.tanks[1].setFill(50_000);
        core.tanks[0].setFill(0);
        core.tanks[1].setFill(0);

        tick(helper, INJECTOR, injector, TileEntityCoreInjector::serverTick);

        helper.assertTrue(injector.beam > 0, "The injector should have found the chamber");
        helper.assertTrue(core.tanks[0].getFill() == 50_000 && core.tanks[1].getFill() == 50_000,
                "Both fuels should have moved into the chamber");
        helper.assertTrue(injector.tanks[0].getFill() == 0, "The injector should have emptied itself");
        helper.succeed();
    }

    /**
     * The field gate, without the blast. A full chamber at full heat makes a thousand-block crater, which
     * would take out every neighbouring test plot, so this checks the decision and the size it would have
     * used rather than letting it fire.
     */
    public static void meltdown(GameTestHelper helper) {
        TileEntityCore core = build(helper);
        try {
            fuel(core);
            core.inventory.setStackInSlot(1, new ItemStack(ModDFCBlocks.ams_core_sing.get()));
            catalysts(core);

            core.heat = 50;
            core.field = 100;
            tick(helper, CORE, core, TileEntityCore::serverTick);
            helper.assertBlockPresent(ModDFCBlocks.dfc_core.get(), CORE);
            helper.assertTrue(!core.shouldDetonate(), "A field above the heat should hold the reaction");

            helper.assertTrue(core.field == 99, "The field should decay by one each tick");

            core.heat = 100;
            core.field = 0;
            helper.assertTrue(core.shouldDetonate(), "Heat above the field should open the gate");
            helper.assertTrue(core.blastSize() == 1000, "A full chamber at full heat is the largest blast");

            core.tanks[0].setFill(0);
            core.tanks[1].setFill(0);
            helper.assertTrue(core.blastSize() == 50, "An empty chamber still makes the smallest blast");
            helper.succeed();
        } finally {
            quench(core);
        }
    }

    public static void cores(GameTestHelper helper) {
        helper.assertTrue(ItemAMSCore.getPowerBase(new ItemStack(ModDFCBlocks.ams_core_sing.get())) == 500,
                "The vibrant singularity is the weakest core");
        helper.assertTrue(ItemAMSCore.getPowerBase(new ItemStack(ModDFCBlocks.ams_core_thingy.get())) == 2500,
                "The thingy is the strongest core");
        helper.assertTrue(ItemAMSCore.getHeatBase(new ItemStack(ModDFCBlocks.ams_core_thingy.get())) < 1F,
                "The thingy runs cooler than the rest");
        helper.assertTrue(ItemAMSCore.getPowerBase(new ItemStack(ModDFCBlocks.ams_lens.get())) == 0,
                "A lens is not a core");

        ItemStack lens = new ItemStack(ModDFCBlocks.ams_lens.get());
        helper.assertTrue(!ModDFCBlocks.ams_lens.get().isBarVisible(lens), "A fresh lens shows no wear");
        ItemLens.setLensDamage(lens, ModDFCBlocks.LENS_LIFETIME / 2);
        helper.assertTrue(ModDFCBlocks.ams_lens.get().isBarVisible(lens), "A worn lens shows its wear");
        helper.assertTrue(ModDFCBlocks.ams_lens.get().getBarWidth(lens) < 13, "Half a lens is a half bar");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/blockstates/dfc_core.json",
                "assets/hbm/blockstates/dfc_emitter.json",
                "assets/hbm/models/block/dfc_stabilizer.json",
                "assets/hbm/models/core_emitter.obj",
                "assets/hbm/models/core_injector.obj",
                "assets/hbm/models/core_receiver.obj",
                "assets/hbm/models/sphere_uv.obj",
                "assets/hbm/models/sphere_ruv.obj",
                "assets/hbm/textures/models/machines/core_emitter.png",
                "assets/hbm/textures/models/machines/core_stabilizer.png",
                "assets/hbm/textures/blocks/dfc_core.png",
                "assets/hbm/textures/gui/dfc/gui_core.png",
                "assets/hbm/textures/gui/dfc/gui_emitter.png",
                "assets/hbm/textures/gui/dfc/gui_stabilizer.png",
                "assets/hbm/textures/items/ams_lens.png",
                "assets/hbm/textures/items/ams_core_thingy.png",
                "assets/hbm/items/ams_lens.json",
                "assets/hbm/items/dfc_core.json",
                "data/hbm/loot_table/blocks/dfc_core.json",
                "data/hbm/damage_type/ams.json",
                "data/hbm/damage_type/ams_core.json",
                "data/hbm/recipe/ams_lens.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing dark fusion core asset: " + path);
        }
        helper.succeed();
    }

    // --- helpers ---

    /** Places the chamber with an emitter, receiver, injector and stabiliser all aimed at it. */
    private static TileEntityCore build(GameTestHelper helper) {
        helper.setBlock(CORE, ModDFCBlocks.dfc_core.get().defaultBlockState());
        aim(helper, EMITTER, ModDFCBlocks.dfc_emitter.get(), Direction.EAST);
        aim(helper, RECEIVER, ModDFCBlocks.dfc_receiver.get(), Direction.WEST);
        aim(helper, INJECTOR, ModDFCBlocks.dfc_injector.get(), Direction.SOUTH);
        aim(helper, STABILIZER, ModDFCBlocks.dfc_stabilizer.get(), Direction.NORTH);

        TileEntityCore core = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, CORE, TileEntityCore.class);
        helper.assertTrue(core != null, "Dark fusion core BlockEntity was not created");
        return core;
    }

    private static void aim(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.Block block,
                           Direction facing) {
        helper.setBlock(pos, block.defaultBlockState().setValue(CoreComponent.FACING, facing));
    }

    private static void fuel(TileEntityCore core) {
        core.tanks[0].setTankType(Fluids.DEUTERIUM);
        core.tanks[1].setTankType(Fluids.TRITIUM);
        core.tanks[0].setFill(core.tanks[0].getMaxFill());
        core.tanks[1].setFill(core.tanks[1].getMaxFill());
    }

    private static void catalysts(TileEntityCore core) {
        ItemStack catalyst = new ItemStack(com.hbm.items.ModCatalystItems.iron.get());
        core.inventory.setStackInSlot(0, catalyst.copy());
        core.inventory.setStackInSlot(2, catalyst.copy());
    }

    /** Stop the deliberately overheated test core from detonating after its assertions complete. */
    private static void quench(TileEntityCore core) {
        core.heat = 0;
        core.field = 0;
        core.tanks[0].setFill(0);
        core.tanks[1].setFill(0);
    }

    private static <T extends BlockEntity> T be(GameTestHelper helper, BlockPos pos, Class<T> type) {
        T be = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, type);
        helper.assertTrue(be != null, "No " + type.getSimpleName() + " at " + pos);
        return be;
    }

    private interface Ticker<T extends BlockEntity> {
        void tick(Level level, BlockPos pos, BlockState state, T be);
    }

    private static <T extends BlockEntity> void tick(GameTestHelper helper, BlockPos relative, T be, Ticker<T> ticker) {
        BlockPos absolute = helper.absolutePos(relative);
        ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), be);
    }

    private static boolean resourceExists(String path) {
        return dev.golem.core.gametest.GameTestHelpers.resourceExists(DFCGameTests.class, path);
    }
}
