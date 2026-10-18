package com.hbm.gametest;

import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.config.ServerConfig;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.module.IParse.EnumStatementReturn;
import com.hbm.module.IParse.ParseContext;
import com.hbm.module.ParseMSES1Ext1;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.hbm.tileentity.machine.TileEntityMachineThresher;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** 1.7 machine behaviour: doors, conveyors, reactors, AUTOCAL and the lantern, with values worked out by hand. */
public final class MachineUpdateGameTests {

    private MachineUpdateGameTests() {}

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    public static void doorSkinsAndLarp(GameTestHelper helper) {
        helper.assertTrue(DoorDecl.SLIDE_DOOR.getSkinCount() == 2 && DoorDecl.SLIDING_SEAL_DOOR.getSkinCount() == 3
                        && DoorDecl.QE_SLIDING.getSkinCount() == 5 && DoorDecl.LARGE_VEHICLE_DOOR.getSkinCount() == 4,
                "door skin counts are 2 / 3 / 5 / 4");
        helper.assertTrue(DoorDecl.QE_SLIDING.getSkinFromIndex(6) == DoorDecl.PHEO_SLIDING_DOOR_BLACK_TEX,
                "skin index 6 wraps to the second sliding door skin");

        boolean previous = ServerConfig.ULTRA_LARP_MODE.get();
        try {
            ServerConfig.ULTRA_LARP_MODE.set(false);
            helper.assertTrue(BlockDoorGeneric.manuallyOperable(DoorDecl.FIRE_DOOR), "doors open by hand by default");
            ServerConfig.ULTRA_LARP_MODE.set(true);
            helper.assertTrue(!BlockDoorGeneric.manuallyOperable(DoorDecl.FIRE_DOOR), "ULTRA_LARP_MODE locks the fire door");
            helper.assertTrue(BlockDoorGeneric.manuallyOperable(DoorDecl.WATER_DOOR), "the water door keeps its cranks");
        } finally {
            ServerConfig.ULTRA_LARP_MODE.set(previous);
        }
        helper.succeed();
    }

    /** Placing the vertical blast door refuses an occupied cell; closing it forces the leaf through. */
    public static void blastDoorPlacement(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(core, state(ModDoorBlocks.BLAST_DOOR.get()), 3);
        TileEntityBlastDoor door = (TileEntityBlastDoor) helper.getLevel().getBlockEntity(core);
        helper.getLevel().setBlock(core.above(2), Blocks.STONE.defaultBlockState(), 3);
        helper.assertTrue(!door.placeDummy(core.above(2), false)
                && helper.getLevel().getBlockState(core.above(2)).is(Blocks.STONE), "placement kept the stone");
        helper.assertTrue(door.placeDummy(core.above(2), true)
                && helper.getLevel().getBlockState(core.above(2)).is(ModDoorBlocks.DUMMY_BLOCK_BLAST.get()), "closing broke it");
        helper.succeed();
    }

    /** 25 items in one cell pop at the default {@code CONVEYOR_CRAM_MAX}; the belt survives a jam younger than 400 ticks. */
    public static void conveyorCram(GameTestHelper helper) {
        BlockPos belt = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(belt, state(ModConveyorBlocks.CONVEYOR.get()), 3);
        List<EntityMovingItem> young = jam(helper, belt, 25);
        young.get(0).cramCheck(helper.getLevel());
        helper.assertTrue(young.stream().allMatch(EntityMovingItem::isRemoved), "25 items are a jam");
        helper.assertTrue(helper.getLevel().getBlockState(belt).is(ModConveyorBlocks.CONVEYOR.get()), "a young jam spares the belt");

        List<EntityMovingItem> under = jam(helper, belt, 24);
        under.get(0).tickCount = 401;
        under.get(0).cramCheck(helper.getLevel());
        helper.assertTrue(under.stream().noneMatch(EntityMovingItem::isRemoved), "24 items are no jam");
        under.forEach(EntityMovingItem::discard);

        List<EntityMovingItem> old = jam(helper, belt, 25);
        old.get(0).tickCount = 401;
        old.get(0).cramCheck(helper.getLevel());
        helper.assertTrue(!helper.getLevel().getBlockState(belt).is(ModConveyorBlocks.CONVEYOR.get()), "an old jam breaks the belt");
        helper.succeed();
    }

    private static List<EntityMovingItem> jam(GameTestHelper helper, BlockPos belt, int count) {
        List<EntityMovingItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EntityMovingItem item = new EntityMovingItem(helper.getLevel());
            item.setItemStack(new ItemStack(Items.COBBLESTONE));
            item.setPos(belt.getX() + 0.5, belt.getY() + 0.25, belt.getZ() + 0.5);
            helper.getLevel().addFreshEntity(item);
            items.add(item);
        }
        return items;
    }

    public static void thresherCrops(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState young = Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 2);
        BlockState ripe = Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 3);
        helper.assertTrue(TileEntityMachineThresher.canCut(ripe) && TileEntityMachineThresher.shouldIgnore(helper.getLevel(), pos, young)
                && !TileEntityMachineThresher.shouldIgnore(helper.getLevel(), pos, ripe), "nether wart is cut at age 3 only");
        helper.assertTrue(TileEntityMachineThresher.canCut(Blocks.PUMPKIN.defaultBlockState())
                && TileEntityMachineThresher.canCut(Blocks.MELON.defaultBlockState()), "pumpkins and melons are cut");
        helper.assertTrue(TileEntityMachineThresher.shouldIgnore(helper.getLevel(), pos, Blocks.PUMPKIN_STEM.defaultBlockState())
                && TileEntityMachineThresher.shouldIgnore(helper.getLevel(), pos, Blocks.ATTACHED_MELON_STEM.defaultBlockState()),
                "stems are ignored");
        helper.succeed();
    }

    /** Thorium-232 lives for 35,000 absorbed neutrons, then breeds into the thorium fuel rod. */
    public static void pileThoriumBreeding(GameTestHelper helper) {
        ItemStack rod = ModPileBlocks.pile_rod.stack(EnumPileRod.THORIUM);
        ItemPileRodMK2.setDepletion(rod, 34_998D);
        ItemStack after = ItemPileRodMK2.react(rod, 1D);
        helper.assertTrue(after == rod && ItemPileRodMK2.getDepletion(rod) == 34_999D, "34,999 neutrons keep the rod");
        ItemStack bred = ItemPileRodMK2.react(rod, 1D);
        helper.assertTrue(ItemPileRodMK2.typeOf(bred) == EnumPileRod.THORIUM_FUEL, "35,000 neutrons breed thorium fuel");
        ItemPileRodMK2.setDepletion(bred, 1_999D);
        helper.assertTrue(ItemPileRodMK2.typeOf(ItemPileRodMK2.react(bred, 1D)) == EnumPileRod.WASTE, "thorium fuel burns out at 2,000");
        helper.succeed();
    }

    /** The monitor slot reports 1.7's enum name, the burn-up and the lifetime; breeding rods report nothing. */
    public static void zirnoxMonitorSlot(GameTestHelper helper) {
        TileEntityReactorZirnox zirnox = new TileEntityReactorZirnox(helper.absolutePos(new BlockPos(2, 1, 2)),
                state(ModMachineBlocks.machine_zirnox.get()));
        ItemZirnoxRod uranium = ModZirnoxItems.active(ZirnoxFuel.URANIUM);
        ItemStack rod = new ItemStack(uranium);
        uranium.setLife(rod, 1_234);
        zirnox.inventory.setStackInSlot(3, rod);
        zirnox.inventory.setStackInSlot(4, new ItemStack(ModZirnoxItems.active(ZirnoxFuel.TH232)));
        zirnox.runRORFunction("FUN:setslot", new String[]{"3"});
        helper.assertTrue("URANIUM_FUEL".equals(zirnox.provideRORValue("VAL:slottype"))
                        && "1234".equals(zirnox.provideRORValue("VAL:slotdep"))
                        && "200000".equals(zirnox.provideRORValue("VAL:slotmaxdep")), "slot 3 reports the uranium rod");
        zirnox.runRORFunction("FUN:setslot", new String[]{"24"});
        helper.assertTrue("1234".equals(zirnox.provideRORValue("VAL:slotdep")), "slot 24 is out of range and ignored");
        zirnox.runRORFunction("FUN:setslot", new String[]{"4"});
        helper.assertTrue("".equals(zirnox.provideRORValue("VAL:slottype")) && zirnox.provideRORValue("VAL:slotdep") == null,
                "a breeding rod is no fuel rod");
        helper.succeed();
    }

    /** Coolant type changes are refused while the PWR holds fuel, or while an RBMK column is above 50 °C with feed. */
    public static void coolantLocks(GameTestHelper helper) {
        TileEntityPWRController pwr = new TileEntityPWRController(helper.absolutePos(new BlockPos(1, 1, 1)),
                state(ModPwrBlocks.PWR_CONTROLLER.get()));
        pwr.inventory.setStackInSlot(2, ItemFluidId.make(Fluids.WATER));
        pwr.amountLoaded = 1;
        pwr.updateTypeFromFluidId();
        helper.assertTrue(pwr.tanks[0].getTankType() == Fluids.COOLANT, "a fuelled PWR keeps its coolant");
        pwr.amountLoaded = 0;
        pwr.updateTypeFromFluidId();
        helper.assertTrue(pwr.tanks[0].getTankType() == Fluids.WATER, "an empty PWR takes the new coolant");

        BlockEntityRBMKHeater heater = new BlockEntityRBMKHeater(helper.absolutePos(new BlockPos(2, 1, 1)),
                state(ModRBMKBlocks.rbmk_heater.get()));
        heater.inventory.setStackInSlot(0, ItemFluidId.make(Fluids.WATER));
        heater.heat = 51D;
        heater.feed.setFill(1);
        heater.updateFeedType();
        helper.assertTrue(heater.feed.getTankType() == Fluids.COOLANT, "a hot heater with feed keeps its type");
        heater.heat = 50D;
        heater.updateFeedType();
        helper.assertTrue(heater.feed.getTankType() == Fluids.WATER && heater.feed.getFill() == 0, "at 50 °C the type changes and empties");

        BlockEntityRBMKBoiler boiler = new BlockEntityRBMKBoiler(helper.absolutePos(new BlockPos(3, 1, 1)),
                state(ModRBMKBlocks.rbmk_boiler.get()));
        boiler.heat = 51D;
        boiler.feed.setFill(1);
        boiler.cycleCompressor();
        helper.assertTrue(boiler.steam.getTankType() == Fluids.STEAM, "a hot boiler with feed keeps its compression");
        boiler.feed.setFill(0);
        boiler.cycleCompressor();
        helper.assertTrue(boiler.steam.getTankType() == Fluids.HOTSTEAM, "an empty feed lets it cycle");
        helper.succeed();
    }

    public static void autocalReturnStates(GameTestHelper helper) {
        ParseContext ctx = new ParseContext(helper.getLevel());
        ParseMSES1Ext1 parser = new ParseMSES1Ext1();
        helper.assertTrue(parser.eval(ctx, "eval") == EnumStatementReturn.BUFFER_EMPTY, "eval on an empty buffer");
        helper.assertTrue(parser.eval(ctx, "clockspeed 21") == EnumStatementReturn.PARAMETER_OOB, "clock 21 exceeds the default 20");
        helper.assertTrue(parser.eval(ctx, "clockspeed fast") == EnumStatementReturn.PARAMETER_PARSE_ERROR, "clock speed must be a number");
        helper.assertTrue(parser.eval(ctx, "jmp nowhere") == EnumStatementReturn.NO_DESTINATION, "jump to an unknown point");
        helper.assertTrue(parser.eval(ctx, "eval 2 +") == EnumStatementReturn.ARITHMETIC_EXCEPTION, "a broken expression");
        helper.assertTrue(parser.eval(ctx, "save ") == EnumStatementReturn.PARAMETER_MISSING, "save without a name");
        ctx.writeBuffer("a;b");
        helper.assertTrue(parser.eval(ctx, "split 3") == EnumStatementReturn.PARAMETER_OOB, "two fragments have no third");

        int previous = ServerConfig.MAX_BUFFER_LENGTH.get();
        try {
            ServerConfig.MAX_BUFFER_LENGTH.set(4);
            helper.assertTrue(!ctx.writeBuffer("abcdef") && "abcd".equals(ctx.readBuffer()), "MAX_BUFFER_LENGTH 4 truncates");
        } finally {
            ServerConfig.MAX_BUFFER_LENGTH.set(previous);
        }

        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, state(ModMachineBlocks.radio_autocal.get()));
        TileEntityRadioAUTOCAL autocal = GameTestHelpers.blockEntity(helper, pos, TileEntityRadioAUTOCAL.class);
        CompoundTag script = new CompoundTag();
        script.putString("payload", "jmp nowhere\nendtick");
        autocal.receiveControl(script);
        autocal.ignoreError = true;
        autocal.isOn = true;
        autocal.serverTick();
        helper.assertTrue(autocal.isOn && "no_destination".equals(autocal.ctx.getVariable("last_error")),
                "an ignored error is saved to last_error");
        helper.succeed();
    }

    /** The lantern pays one token per reputation point of the repairer, and remembers it for the bonus. */
    public static void lanternRepair(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntityLanternBehemoth lantern = new TileEntityLanternBehemoth(helper.absolutePos(pos),
                state(ModDungeonBlocks.lantern_behemoth.get()));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        HbmPlayerProps.setReputation(player, 12);
        lantern.isBroken = true;
        lantern.repair(player);
        helper.assertTrue(!lantern.isBroken && lantern.comTimer == 400 && lantern.reputation == 12,
                "repair starts the 400-tick call and keeps reputation 12");
        helper.succeed();
    }

    public static void batterySocketExtraction(GameTestHelper helper) {
        TileEntityBatterySocket socket = new TileEntityBatterySocket(helper.absolutePos(new BlockPos(2, 1, 2)),
                state(ModBatteryBlocks.machine_battery_socket.get()));
        socket.setLevel(helper.getLevel());
        ItemBattery potato = ModFinalConsumableItems.battery_potato.get();
        ItemStack full = new ItemStack(potato);
        ItemStack empty = full.copy();
        potato.setCharge(empty, 0);
        socket.redLow = TileEntityBatteryBase.mode_output;
        helper.assertTrue(socket.canTakeItemThroughFace(0, empty, Direction.DOWN) && !socket.canTakeItemThroughFace(0, full, Direction.DOWN),
                "discharging gives up empty batteries");
        socket.redLow = TileEntityBatteryBase.mode_input;
        helper.assertTrue(socket.canTakeItemThroughFace(0, full, Direction.DOWN) && !socket.canTakeItemThroughFace(0, empty, Direction.DOWN),
                "charging gives up full batteries");
        helper.succeed();
    }

    public static void rorMachineCommands(GameTestHelper helper) {
        TileEntityFusionKlystron klystron = new TileEntityFusionKlystron(helper.absolutePos(new BlockPos(1, 1, 1)),
                state(ModMachineBlocks.fusion_klystron.get()));
        klystron.runRORFunction("FUN:settarget", new String[]{"500"});
        helper.assertTrue(klystron.outputTarget == 500 && "500".equals(klystron.provideRORValue("VAL:target")), "klystron target 500");

        TileEntityMachineGasFlare flare = new TileEntityMachineGasFlare(helper.absolutePos(new BlockPos(2, 1, 1)),
                state(ModMachineBlocks.machine_flare.get()));
        flare.runRORFunction("FUN:setflowstate", new String[]{"1"});
        flare.runRORFunction("FUN:setignitionstate", new String[]{"1"});
        helper.assertTrue(flare.isOn && flare.doesBurn && "1".equals(flare.provideRORValue("VAL:ignitionstate")), "flare switched on and lit");
        helper.succeed();
    }
}
