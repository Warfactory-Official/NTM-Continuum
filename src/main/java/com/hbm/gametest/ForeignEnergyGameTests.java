package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.EnergyConversion;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.capability.CapRole;
import com.hbm.capability.NativeContacts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.blocks.machine.BlockCustomMachine;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCMPort;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.util.EnergyPush;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.world.item.ItemStack;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.BlockCable;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The foreign energy bridge against a real third-party mod rather than against our own facades meeting
 * at the capability. Pipez is the peer: it is published for all three forge-like nodes, it needs no
 * other mod, and its energy pipe both exposes a handler per side and drives the transfer itself.
 *
 * <p>A real peer proves the transaction contract. Pipez decides where to send by opening a transaction,
 * calling {@code insert(1, …)} on every candidate destination, and then <b>closing that transaction without
 * committing it</b>: every tick, against every connection. A handler that treats the call as final rather
 * than journalling it does not fail loudly; it quietly mints energy, one unit per destination per tick.
 * The conservation assertion in {@link #roundTrip} catches that.
 *
 * <p>These tests skip where there is nothing to test: Fabric ships no standard energy capability, and a
 * node whose {@code deps.pipez} is blank has no peer on the runtime classpath.
 */
public final class ForeignEnergyGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    private static final int SWEEP_PADDING = 24;

    public static final GameTests.Function ROUND_TRIP =
            TESTS.function("power_foreign_mod_round_trip", 120, () -> ForeignEnergyGameTests::roundTrip);
    public static final GameTests.Function FACE =
            TESTS.function("power_foreign_mod_face", 40, () -> ForeignEnergyGameTests::face);

    public static final GameTests.Function CABLE_ARMS =
            TESTS.function("power_foreign_cable_arms", 160, () -> ForeignEnergyGameTests::cableArms);

    public static final GameTests.Function MACHINE_FACES =
            TESTS.function("power_foreign_machine_faces", 6000, SWEEP_PADDING, () -> ForeignEnergyGameTests::machineFaces);
    public static final GameTests.Function PORT_FACE_ONCE =
            TESTS.function("power_port_only_face_counted_once", 60, 6, () -> ForeignEnergyGameTests::portFaceOnce);
    public static final GameTests.Function NO_PUSH_INTO_OWN =
            TESTS.function("power_no_foreign_push_into_own_machine", 40, 6, () -> ForeignEnergyGameTests::noPushIntoOwn);
    public static final GameTests.Function CUSTOM_PORT =
            TESTS.function("power_foreign_custom_machine_port", 80, 8, () -> ForeignEnergyGameTests::customMachinePort);
    public static final GameTests.Function CHARGER =
            TESTS.function("power_foreign_charger", 40, 4, () -> ForeignEnergyGameTests::charger);
    public static final GameTests.Function CHARGER_ONE =
            TESTS.function("power_foreign_charger_one_battery", 60, 4, () -> helper -> chargerExact(helper, 1));
    public static final GameTests.Function CHARGER_TWO =
            TESTS.function("power_foreign_charger_two_batteries", 60, 4, () -> helper -> chargerExact(helper, 2));
    public static final GameTests.Function CHARGER_FIVE =
            TESTS.function("power_foreign_charger_five_batteries", 60, 4, () -> helper -> chargerExact(helper, 5));
    public static final GameTests.Function TURRET_CELL_PUSH =
            TESTS.function("power_push_reaches_turret_through_cell", 40, 6, () -> ForeignEnergyGameTests::turretCellPush);
    public static final GameTests.Function PUSH_COST =
            TESTS.function("power_foreign_push_cost", 40, 4, () -> ForeignEnergyGameTests::pushCost);
    public static final GameTests.Function PIPE_FEEDS_MACHINE =
            TESTS.function("power_foreign_pipe_socket_to_furnace", 120, 4, () -> ForeignEnergyGameTests::socketToFurnace);
    public static final GameTests.Function PIPE_DRAWS_GENERATOR =
            TESTS.function("power_foreign_pipe_generator_to_furnace", 120, () -> ForeignEnergyGameTests::generatorToFurnace);

    /** Pipez's energy pipe, the peer these tests drive. */
    private static final String PIPE_NAMESPACE = "pipez", PIPE_PATH = "energy_pipe";

    /**
     * Pipez gives a pipe a block entity only where its state says it carries configuration, and a pipe
     * with no block entity exposes no handler and moves nothing. Set by name rather than compiled
     * against, like everything else these tests touch of the peer.
     */
    private static final String PIPE_DATA_PROPERTY = "has_data";

    /**
     * A row per test at a height nothing else uses. Plots sit close enough that a cable run reaching one
     * edge is collinear with its neighbour's, and two collinear runs are one network, so a test would
     * measure the plot next door through cells that park the moment that plot is released. {@code PowerNetworkGameTests} keeps its runs at y=1; these stay clear of them entirely.
     */
    private static final int Y = 3;
    private static final int ROUND_TRIP_Z = 6;
    private static final int FACE_Z = 12;
    private static final int ARMS_Z = 18;
    private static final int MACHINE_Z = 6;

    /** The pipe sits between the two runs, so neither is a neighbour of the other. */
    private static final int RUN_A_FROM = 6, RUN_A_TO = 10, PIPE_X = 11, RUN_B_FROM = 12, RUN_B_TO = 16;

    private ForeignEnergyGameTests() {}

    public static void init() {}

    /**
     * A charged net, a pipez pipe, and a second net that shares no cable with the first. Energy that
     * arrives at the far battery crossed the capability boundary twice: out of our handler under
     * pipez's transactions, and back into it, so the two directions are asserted by one reading.
     */
    private static void roundTrip(GameTestHelper helper) {
        if (skip(helper)) return;
        run(helper, RUN_A_FROM, RUN_A_TO, ROUND_TRIP_Z);
        run(helper, RUN_B_FROM, RUN_B_TO, ROUND_TRIP_Z);
        BlockPos pipe = pipe(helper, PIPE_X, ROUND_TRIP_Z, Direction.WEST);

        TileEntityMachineBattery source = battery(helper, RUN_A_FROM, ROUND_TRIP_Z - 1,
                TileEntityMachineBattery.MODE_OUTPUT, TileEntityMachineBattery.MAX_POWER);
        TileEntityMachineBattery sink = battery(helper, RUN_B_TO, ROUND_TRIP_Z + 1,
                TileEntityMachineBattery.MODE_INPUT, 0L);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(sink.power > 0L,
                    "Nothing crossed a real mod's pipe at " + pipe + ": the far battery is still empty");
            helper.assertTrue(source.power < TileEntityMachineBattery.MAX_POWER,
                    "The far battery filled without the near one being debited");
            helper.assertTrue(source.power + sink.power <= TileEntityMachineBattery.MAX_POWER,
                    "Energy was created: " + source.power + " + " + sink.power + " exceeds the "
                            + TileEntityMachineBattery.MAX_POWER + " the net started with, which is what a "
                            + "rolled-back insert looks like when it is not journalled");
            helper.succeed();
        });
    }

    /** Each side can see the other's handler: the lookup both mods' discovery is built on. */
    private static void face(GameTestHelper helper) {
        if (skip(helper)) return;
        ServerLevel level = helper.getLevel();

        run(helper, RUN_A_FROM, RUN_A_TO, FACE_Z);
        BlockPos pipe = pipe(helper, PIPE_X, FACE_Z, Direction.WEST);
        BlockPos cable = helper.absolutePos(new BlockPos(RUN_A_TO, Y, FACE_Z));

        EnergyView theirs = EnergyCompat.find(level, pipe, Direction.WEST);
        helper.assertTrue(theirs != null, "A real mod's energy block exposed nothing our net can find");

        EnergyView ours = EnergyCompat.find(level, cable, Direction.EAST);
        helper.assertTrue(ours != null,
                "A cable exposed nothing at the face a real mod's connection check reads");
        helper.succeed();
    }

    /**
     * Places a pipe already pulling from {@code from}, or a plain one when it is null. Pipez calls the side it draws from its extracting
     * side and answers a pusher on that side too, so nothing moves either way, and no handler is even
     * exposed until one is set. Its one public setter is reached reflectively: the peer is a
     * runtime-only dependency, and a node without it must still build.
     */
    private static BlockPos pipe(GameTestHelper helper, int x, int z, @Nullable Direction from) {
        Block block = pipeBlock();
        BlockPos at = helper.absolutePos(new BlockPos(x, Y, z));
        // a pipe works out its connections only when placed by hand or when a neighbour changes
        BlockState state = Block.updateFromNeighbourShapes(block.defaultBlockState(), helper.getLevel(), at);
        if (block.getStateDefinition().getProperty(PIPE_DATA_PROPERTY) instanceof BooleanProperty carriesData) {
            state = state.setValue(carriesData, true);
        }
        helper.getLevel().setBlock(at, state, 3);

        BlockEntity be = helper.getLevel().getBlockEntity(at);
        helper.assertTrue(be != null, "A pipez pipe placed with " + PIPE_DATA_PROPERTY
                + " set still has no block entity, so it can carry nothing");
        if (from == null) return at;
        try {
            be.getClass().getMethod("setExtracting", Direction.class, boolean.class).invoke(be, from, true);
        } catch (ReflectiveOperationException e) {
            helper.fail("Pipez no longer has setExtracting(Direction, boolean): " + e);
        }
        return at;
    }

    /**
     * A cable draws an arm toward a foreign energy block exactly when the power net can push into it, and the arm
     * goes with the block. The fixture cell stands on every node (the loader's capability, Team Reborn Energy on
     * Fabric), so this runs everywhere there is a foreign energy system.
     */
    private static void cableArms(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        int z = ARMS_Z;
        BlockPos cable = place(helper, 5, z, ModMachineBlocks.red_cable.get());
        place(helper, 4, z, ModMachineBlocks.creative_generator.get());
        BlockPos cell = place(helper, 6, z, ForeignEnergyFixture.CELL.get());
        BlockPos stone = place(helper, 5, z + 1, Blocks.STONE);
        helper.assertTrue(helper.getLevel().getBlockState(cable).getValue(BlockCable.EAST),
                "No arm toward a foreign energy block the net can push into");
        helper.assertFalse(helper.getLevel().getBlockState(cable).getValue(BlockCable.SOUTH),
                "An arm toward a block that exposes no energy");
        helper.assertTrue(helper.getLevel().getBlockState(cable).getValue(BlockCable.WEST),
                "No arm toward the generator");
        helper.assertTrue(EnergyCompat.find(helper.getLevel(), cell, Direction.WEST) != null,
                "The fixture exposes nothing on the face the cable touches");

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(ForeignEnergyFixture.stored(helper.getLevel(), cell) > 0L,
                    "Power did not reach the foreign cell");
            helper.getLevel().setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(stone, Blocks.AIR.defaultBlockState(), 3);
            helper.assertFalse(helper.getLevel().getBlockState(cable).getValue(BlockCable.EAST),
                    "The arm stayed after the foreign block went");
            helper.succeed();
        });
    }

    /** Every HBM energy machine answers a foreign lookup on each face a cable can use. */
    private static void machineFaces(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        ForeignEnergySweep.run(helper, ForeignEnergySweep.energyBlocks(Constants.MOD_ID), SWEEP_PADDING, result -> {
            Constants.LOG.info("[foreign] {} machines swept, port faces the cable rule does not reach: {}",
                    result.checked().size(), result.portOnly());
            helper.assertTrue(result.checked().size() > 100, "only " + result.checked().size() + " machines were checked");
            helper.assertTrue(result.missing().isEmpty(), result.missing().size()
                    + " cable faces of HBM machines expose no foreign energy: " + result.missing());
            helper.succeed();
        });
    }

    /**
     * A cable under the radar reaches it through a port only (its canConnect is horizontal). Radar and furnace, both
     * empty 100,000 HE buffers, take half each; a radar also counted as foreign would take two thirds.
     */
    private static void portFaceOnce(GameTestHelper helper) {
        BlockPos radar = GameTests.paddedCentre(6, 3);
        for (int x = -2; x <= 2; x++) helper.setBlock(radar.offset(x, -1, 0), ModMachineBlocks.red_cable.get());
        helper.setBlock(radar, ModRadarUtilityBlocks.machine_radar.get());
        BlockPos furnaceAt = radar.offset(2, 0, 0);
        helper.setBlock(furnaceAt, ModMachineBlocks.machine_electric_furnace_off.get());
        BlockPos dieselAt = radar.offset(-2, 0, 0);
        helper.setBlock(dieselAt, ModMachineBlocks.machine_diesel.get());
        TileEntityMachineRadarNT target = GameTestHelpers.blockEntity(helper, radar, TileEntityMachineRadarNT.class);
        TileEntityMachineElectricFurnace furnace = GameTestHelpers.blockEntity(helper, furnaceAt, TileEntityMachineElectricFurnace.class);
        TileEntityMachineDiesel diesel = GameTestHelpers.blockEntity(helper, dieselAt, TileEntityMachineDiesel.class);
        helper.runAfterDelay(10, () -> {
            target.power = 0;
            furnace.power = 0;
            diesel.power = 50_000L;
            helper.runAfterDelay(1, () -> {
                long toRadar = target.power, toFurnace = furnace.power;
                helper.assertTrue(toRadar > 0 && toFurnace > 0, "nothing reached the machines: radar " + toRadar
                        + ", furnace " + toFurnace);
                helper.assertTrue(Math.abs(toRadar - toFurnace) <= toFurnace / 10, "the radar took " + toRadar
                        + " HE against the furnace's " + toFurnace + ": its port face was counted twice");
                helper.succeed();
            });
        });
    }

    private static void noPushIntoOwn(GameTestHelper helper) {
        BlockPos radar = GameTests.paddedCentre(6, 3);
        helper.setBlock(radar, ModRadarUtilityBlocks.machine_radar.get());
        helper.setBlock(radar.below(), ModMachineBlocks.machine_diesel.get());
        TileEntityMachineDiesel diesel = GameTestHelpers.blockEntity(helper, radar.below(), TileEntityMachineDiesel.class);
        helper.runAfterDelay(10, () -> {
            diesel.power = 50_000L;
            helper.assertFalse(EnergyPush.toForeign(helper.getLevel(), diesel,
                    new DirPos(helper.absolutePos(radar), ForgeDirection.UP)), "the radar counted as another mod's block");
            helper.assertTrue(diesel.power == 50_000L, "the diesel pushed " + (50_000L - diesel.power)
                    + " HE into an HBM radar as if it were another mod's block");
            helper.succeed();
        });
    }

    /** FE put into a custom machine's port block lands in the controller's buffer. */
    private static void customMachinePort(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        BlockPos rel = new BlockPos(3, 3, 3);
        helper.setBlock(rel, ModCustomMachineBlocks.custom_machine.get().defaultBlockState()
                .setValue(BlockCustomMachine.FACING, Direction.NORTH));
        TileEntityCustomMachine controller = GameTestHelpers.blockEntity(helper, rel, TileEntityCustomMachine.class);
        controller.machineType = "paperPress";
        controller.init();
        controller.buildStructure();
        helper.assertTrue(controller.checkStructure(), "the paper press structure failed its check");
        BlockPos port = helper.absolutePos(rel).below();
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getLevel().getBlockEntity(port) instanceof TileEntityCMPort, "no port below the controller");
            EnergyView face = null;
            for (Direction side : Direction.values()) {
                if (!helper.getLevel().getBlockState(port.relative(side)).isAir()) continue;
                EnergyView view = EnergyCompat.find(helper.getLevel(), port, side);
                if (view != null) face = view;
            }
            helper.assertTrue(face != null, "the custom machine port exposes no foreign energy on any open face");
            controller.power = 0;
            long taken = face.insert(1_000);
            helper.assertTrue(taken > 0 && controller.power == EnergyConversion.heFromFe(taken),
                    "FE into the port reached the controller as " + controller.power + " HE for " + taken + " FE");
            helper.succeed();
        });
    }

    private static void charger(GameTestHelper helper) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModEnergyBedrockBlocks.charger.get());
        BlockPos abs = helper.absolutePos(at);
        helper.runAfterDelay(5, () -> {
            int faces = 0;
            for (Direction side : Direction.values()) {
                boolean cable = PowerNetwork.handlerAtFace(helper.getLevel(), abs, side) != null;
                boolean foreign = EnergyCompat.find(helper.getLevel(), abs, side) != null;
                helper.assertTrue(cable == foreign, "charger face " + side + ": cable " + cable + ", foreign " + foreign);
                if (foreign) faces++;
            }
            helper.assertTrue(faces == 1, "the charger exposes " + faces + " faces, 1.7 connects one");
            helper.succeed();
        });
    }

    /** The charger splits an offer a fifth per item: what a foreign insert was told it took must reach the batteries. */
    private static void chargerExact(GameTestHelper helper, int items) {
        if (!EnergyCompat.supported()) {
            helper.succeed();
            return;
        }
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModEnergyBedrockBlocks.charger.get());
        BlockPos abs = helper.absolutePos(at);
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.teleportTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        Item lead = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "battery_lead"));
        EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
                EquipmentSlot.HEAD};
        for (int i = 0; i < items; i++) player.setItemSlot(slots[i], new ItemStack(lead));
        helper.runAfterDelay(30, () -> {
            EnergyView face = null;
            for (Direction side : Direction.values()) {
                EnergyView view = EnergyCompat.find(helper.getLevel(), abs, side);
                if (view != null) face = view;
            }
            helper.assertTrue(face != null, "the charger exposes no foreign energy");
            long before = charge(player, slots), accepted = face.insert(1_000);
            long gained = charge(player, slots) - before;
            helper.assertTrue(accepted > 0, "the charger took no FE with " + items + " batteries");
            helper.assertTrue(gained == EnergyConversion.heFromFe(accepted), items + " batteries gained " + gained
                    + " HE for " + accepted + " FE accepted (" + EnergyConversion.heFromFe(accepted) + " HE)");
            helper.succeed();
        });
    }

    private static long charge(ServerPlayer player, EquipmentSlot[] slots) {
        long total = 0;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof IBatteryItem battery) total += battery.getCharge(stack);
        }
        return total;
    }

    /** A push into a turret's pad cell reaches the turret, also on Forge where the cell carries a capability proxy. */
    private static void turretCellPush(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(GameTests.paddedCentre(6, 2));
        MultiblockHandlerXR.placeAssembled(level, core, ModTurretBlocks.turret_chekhov.get(), Direction.NORTH);
        helper.runAfterDelay(5, () -> {
            IEnergyHandlerMK2 turret = (IEnergyHandlerMK2) level.getBlockEntity(core);
            for (BlockPos cell : new BlockPos[]{core.east(), core.south(), core.east().south()}) {
                for (Direction side : Direction.values()) {
                    if (NativeContacts.powerAt(level, cell, side, CapRole.POWER_IN) == null) continue;
                    turret.setPower(0);
                    long pushed = EnergyPush.push(level, cell, ForgeDirection.getOrientation(side.getOpposite()), 1_000, null);
                    helper.assertTrue(pushed > 0 && turret.getPower() == pushed, "a push into the pad cell at " + cell
                            + " moved " + pushed + " HE, the turret holds " + turret.getPower());
                    helper.succeed();
                    return;
                }
            }
            helper.fail("no pad cell face reaches the turret's power");
        });
    }

    /** A provider's foreign push at its worst, a chest at all six ports, against the same diesel in the open. */
    private static void pushCost(GameTestHelper helper) {
        BlockPos boxed = new BlockPos(2, 2, 2), open = new BlockPos(-2, 2, -2);
        helper.setBlock(boxed, ModMachineBlocks.machine_diesel.get());
        helper.setBlock(open, ModMachineBlocks.machine_diesel.get());
        for (Direction side : Direction.values()) helper.setBlock(boxed.relative(side), Blocks.CHEST);
        TileEntityMachineDiesel chests = GameTestHelpers.blockEntity(helper, boxed, TileEntityMachineDiesel.class);
        TileEntityMachineDiesel air = GameTestHelpers.blockEntity(helper, open, TileEntityMachineDiesel.class);
        helper.runAfterDelay(5, () -> {
            chests.power = chests.getMaxPower();
            air.power = air.getMaxPower();
            long beside = nanosPerProvide(chests), alone = nanosPerProvide(air);
            Constants.LOG.info("[foreign] providePower: {} ns a tick with six chests at its ports, {} ns in the open",
                    beside, alone);
            // uncached, six capability lookups cost about 2 microseconds each
            helper.assertTrue(beside <= 4 * alone + 1_000L, "providePower takes " + beside + " ns a tick next to six chests"
                    + " against " + alone + " ns in the open: the foreign lookup is not cached");
            helper.succeed();
        });
    }

    private static long nanosPerProvide(TileEntityMachineDiesel diesel) {
        int calls = 20_000;
        for (int i = 0; i < 2_000; i++) diesel.providePower();
        long start = System.nanoTime();
        for (int i = 0; i < calls; i++) diesel.providePower();
        return (System.nanoTime() - start) / calls;
    }

    /** A real mod's pipe draws a battery socket in output mode and feeds an electric furnace with it. */
    private static void socketToFurnace(GameTestHelper helper) {
        if (skip(helper)) return;
        BlockPos core = GameTests.paddedCentre(4, 1).atY(Y);
        FoldedTestSupport.assemble(helper, ModBatteryBlocks.machine_battery_socket.get(), core);
        TileEntityBatterySocket socket = GameTestHelpers.blockEntity(helper, core, TileEntityBatterySocket.class);
        ItemBatteryPack capacitor = (ItemBatteryPack) BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "capacitor_copper"));
        ItemStack full = new ItemStack(capacitor);
        capacitor.setCharge(full, capacitor.getMaxCharge(full));
        socket.inventory.setStackInSlot(0, full);
        socket.redLow = TileEntityBatteryBase.mode_output;
        socket.setChanged();
        Direction front = socket.getConPos()[0].getDir().toDirection();
        BlockPos pipeRel = core.relative(front);
        pipe(helper, pipeRel.getX(), pipeRel.getZ(), front.getOpposite());
        helper.setBlock(pipeRel.relative(front), ModMachineBlocks.machine_electric_furnace_off.get());
        TileEntityMachineElectricFurnace furnace = GameTestHelpers.blockEntity(helper, pipeRel.relative(front),
                TileEntityMachineElectricFurnace.class);
        long before = capacitor.getCharge(socket.inventory.getStackInSlot(0));
        helper.runAfterDelay(80, () -> {
            long after = capacitor.getCharge(socket.inventory.getStackInSlot(0));
            helper.assertTrue(after < before, "a foreign pipe drew nothing from the battery socket");
            helper.assertTrue(furnace.power > 0L, "a foreign pipe fed the electric furnace nothing");
            helper.succeed();
        });
    }

    /** A diesel generator pushes into a real mod's pipe at its port, and the pipe feeds an electric furnace. */
    private static void generatorToFurnace(GameTestHelper helper) {
        if (skip(helper)) return;
        BlockPos generatorRel = new BlockPos(RUN_A_FROM, Y, MACHINE_Z);
        helper.setBlock(generatorRel, ModMachineBlocks.machine_diesel.get());
        TileEntityMachineDiesel generator = GameTestHelpers.blockEntity(helper, generatorRel, TileEntityMachineDiesel.class);
        generator.power = generator.getMaxPower();
        pipe(helper, RUN_A_FROM + 1, MACHINE_Z, Direction.WEST);
        BlockPos furnaceRel = new BlockPos(RUN_A_FROM + 2, Y, MACHINE_Z);
        helper.setBlock(furnaceRel, ModMachineBlocks.machine_electric_furnace_off.get());
        TileEntityMachineElectricFurnace furnace = GameTestHelpers.blockEntity(helper, furnaceRel,
                TileEntityMachineElectricFurnace.class);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(generator.power < generator.getMaxPower(), "a foreign pipe drew nothing from the generator");
            helper.assertTrue(furnace.power > 0L, "a foreign pipe fed the electric furnace nothing from the generator");
            helper.succeed();
        });
    }

    /** Nothing to test where there is no foreign energy system, or no foreign mod to be one. */
    private static boolean skip(GameTestHelper helper) {
        if (EnergyCompat.supported() && pipeBlock() != null) return false;
        helper.succeed();
        return true;
    }

    private static Block pipeBlock() {
        return BuiltInRegistries.BLOCK.getOptional(Ids.of(PIPE_NAMESPACE, PIPE_PATH)).orElse(null);
    }

    private static void run(GameTestHelper helper, int fromX, int toX, int z) {
        for (int x = fromX; x <= toX; x++) place(helper, x, z, ModMachineBlocks.red_cable.get());
    }

    private static BlockPos place(GameTestHelper helper, int x, int z, Block block) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, Y, z));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        return pos;
    }

    private static TileEntityMachineBattery battery(GameTestHelper helper, int x, int z, int mode, long power) {
        TileEntityMachineBattery battery = PortedBattery.place(helper.getLevel(), helper.absolutePos(new BlockPos(x, Y, z)));
        battery.redLow = (short) mode;
        battery.redHigh = (short) mode;
        battery.power = power;
        return battery;
    }
}
