package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.capability.ForeignFluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidPump;
import dev.golem.core.fluid.FluidCompat;
import dev.golem.core.fluid.FluidView;
import dev.golem.core.fluid.TokenFluids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

/**
 * HBM fluids as vanilla {@code Fluid}s and HBM machines/containers on the loader's standard fluid capability
 * (History section 19ek-fluids). Every move here goes through {@link FluidCompat}, i.e. through whatever the loader's
 * capability lookup answers (NeoForge {@code Capabilities}, Forge {@code ForgeCapabilities}, Fabric
 * {@code FluidStorage}), never through HBM's classes directly, so the per-loader wiring is what is tested.
 */
public final class ForeignFluidGameTests {

    /** Long enough for every machine to claim its ports and an unclaiming class to pass its PortClaims grace. */
    private static final int PORT_CLAIM_WAIT = (int) com.hbm.uninos.graph.PortClaims.GRACE_TICKS + 10;


    private ForeignFluidGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function REGISTRY =
            TESTS.function("foreign_fluids_vanilla_registry", 20, () -> ForeignFluidGameTests::registry);
    public static final GameTests.Function PUMP =
            TESTS.function("foreign_fluids_pump_typed_sides", 200, 8, () -> ForeignFluidGameTests::pump);
    public static final GameTests.Function BARREL =
            TESTS.function("foreign_fluids_barrel_modes_and_save", 200, 8, () -> ForeignFluidGameTests::barrel);
    public static final GameTests.Function MACHINE_TO_MACHINE =
            TESTS.function("foreign_fluids_machine_to_machine_and_bucket", 200, 8, () -> ForeignFluidGameTests::machineToMachine);
    public static final GameTests.Function ITEMS =
            TESTS.function("foreign_fluids_container_items", 20, () -> ForeignFluidGameTests::items);
    public static final GameTests.Function INPUT_NOT_DRAINED =
            TESTS.function("fluid_net_consumer_input_not_drained", 200, 8, () -> ForeignFluidGameTests::inputNotDrained);
    public static final GameTests.Function INPUT_NOT_PROVIDED =
            TESTS.function("fluid_providers_offer_only_sending_tanks", 20, () -> ForeignFluidGameTests::inputNotProvided);

    public static void init() {}

    private static final int Y = 2;

    /** Every FluidType is a vanilla Fluid with the right id, 1:1, and none of them is placeable or bucketable. */
    private static void registry(GameTestHelper helper) {
        int tokens = 0;
        Set<Fluid> seen = new HashSet<>();
        for (FluidType type : Fluids.getAll()) {
            Fluid fluid = type.getFluid();
            String id = BuiltInRegistries.FLUID.getKey(fluid).toString();
            String expected = type == Fluids.NONE ? "minecraft:empty"
                    : type == Fluids.WATER ? "minecraft:water"
                    : type == Fluids.LAVA ? "minecraft:lava" : type.getRegistryName();
            helper.assertTrue(id.equals(expected), type + " is registered as " + id + ", expected " + expected);
            helper.assertTrue(Fluids.fromVanilla(fluid) == type, type + " does not map back from " + id);
            helper.assertTrue(seen.add(fluid), type + " shares its vanilla fluid " + id);
            if (type != Fluids.NONE && type != Fluids.WATER && type != Fluids.LAVA) {
                tokens++;
                helper.assertTrue(TokenFluids.isToken(fluid), id + " is not a token fluid");
                helper.assertTrue(fluid.getBucket() == Items.AIR, id + " has a bucket");
                helper.assertTrue(fluid.defaultFluidState().createLegacyBlock().isAir(), id + " has a world block");
                helper.assertTrue(fluid.defaultFluidState().isSource(), id + " must be a source so recipe viewers list it");
                // the saved form is unchanged: still the FluidType registry name
                helper.assertTrue(Fluids.byRegistryName(id) == type, id + " does not read back as a save");
            }
        }
        helper.assertTrue(tokens == Fluids.getAll().length - 3, "token fluids: " + tokens);
        helper.assertTrue(Fluids.fromVanilla(net.minecraft.world.level.material.Fluids.FLOWING_WATER) == Fluids.WATER,
                "flowing water is water");
        Constants.LOG.info("[foreign fluids] {} HBM fluids as vanilla Fluids ({} token fluids + water, lava, empty)",
                Fluids.getAll().length, tokens);
        helper.succeed();
    }

    /**
     * The flow-control pump: typed to water, it takes water only through its intake, gives it only through its
     * outlet, refuses lava and every other face, a pressurised buffer answers neither way, and exact amounts move
     * (its 100 mB buffer is the rate limit, as a duct sees it).
     */
    private static void pump(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, Y, 3));
        BlockState state = ModFluidNetBlocks.fluid_pump.get().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        level.setBlock(pos, state, 3);
        TileEntityFluidPump pump = (TileEntityFluidPump) level.getBlockEntity(pos);
        pump.tank[0].setTankType(Fluids.WATER);
        Direction in = direction(pump.inSide());
        Direction out = direction(pump.outSide());
        Fluid water = Fluids.WATER.getFluid();

        // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            FluidView intake = FluidCompat.find(level, pos, in);
            FluidView outlet = FluidCompat.find(level, pos, out);
            helper.assertTrue(intake != null && outlet != null, "the pump exposes its intake and outlet");
            helper.assertTrue(FluidCompat.find(level, pos, Direction.UP) == null, "the pump's top is not a fluid port");
            helper.assertTrue(intake.tanks() == 1 && intake.capacity(0) == 100, "one 100 mB buffer, got " + intake.tanks() + "/" + intake.capacity(0));

            helper.assertTrue(intake.insert(Fluids.LAVA.getFluid(), 1000, false) == 0, "a water pump took lava");
            helper.assertTrue(outlet.insert(water, 1000, false) == 0, "the outlet took water");
            long simulated = intake.insert(water, 1000, true);
            helper.assertTrue(simulated == 100 && pump.tank[0].getFill() == 0, "a simulation moved fluid: " + simulated);
            long moved = intake.insert(water, 1000, false);
            helper.assertTrue(moved == 100 && pump.tank[0].getFill() == 100, "the intake took " + moved + ", buffer " + pump.tank[0].getFill());
            helper.assertTrue(intake.amount(0) == 100 && intake.fluid(0) == water, "the buffer reads 100 mB water");

            helper.assertTrue(intake.extract(water, 1000, false) == 0, "the intake gave water");
            long given = outlet.extract(water, 30, false);
            helper.assertTrue(given == 30 && pump.tank[0].getFill() == 70, "the outlet gave " + given + ", buffer " + pump.tank[0].getFill());

            pump.tank[0].withPressure(1);
            helper.assertTrue(intake.insert(water, 10, false) == 0, "a pressurised buffer took unpressurised water");
            helper.succeed();
        });
    }

    /**
     * The barrel's 1.7 modes hold for a foreign pipe (0: in only, 2: out only), a typed barrel takes only its type, and
     * its save after a capability fill is exactly the save of a barrel filled by hand: still {@code "hbm:diesel"}.
     */
    private static void barrel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, Y, 3));
        level.setBlock(pos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel barrel = (TileEntityBarrel) level.getBlockEntity(pos);
        barrel.tank.setTankType(Fluids.DIESEL);
        barrel.mode = 0;
        Fluid diesel = Fluids.DIESEL.getFluid();

        // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            FluidView view = FluidCompat.find(level, pos, Direction.EAST);
            helper.assertTrue(view != null, "the barrel exposes its tank");
            helper.assertTrue(view.insert(Fluids.KEROSENE.getFluid(), 1000, false) == 0, "a diesel barrel took kerosene");
            helper.assertTrue(view.insert(diesel, 1234, false) == 1234 && barrel.tank.getFill() == 1234,
                    "mode 0 takes 1234 mB, got " + barrel.tank.getFill());
            helper.assertTrue(view.extract(diesel, 100, false) == 0, "mode 0 (input) gave diesel");
            helper.assertTrue(view.insert(diesel, 1_000_000, false) == 16000 - 1234 && barrel.tank.getFill() == 16000,
                    "the barrel fills to its 16,000 mB exactly, got " + barrel.tank.getFill());

            barrel.mode = 2;
            // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(view.insert(diesel, 100, false) == 0, "mode 2 (output) took diesel");
                helper.assertTrue(view.extract(diesel, 766, false) == 766 && barrel.tank.getFill() == 15234,
                        "mode 2 gives 766 mB, left " + barrel.tank.getFill());

                // The save is the IDS format, byte-identical to a hand-filled barrel's.
                var registries = level.registryAccess();
                CompoundTag saved = BlockEntities.saveWithoutMetadata(barrel, registries);
                helper.assertTrue(saved.toString().contains("\"hbm:diesel\""), "the barrel saves diesel by name: " + saved);
                BlockState state = level.getBlockState(pos);
                TileEntityBarrel byHand = (TileEntityBarrel) ((net.minecraft.world.level.block.EntityBlock) state.getBlock()).newBlockEntity(pos, state);
                byHand.tank.setTankType(Fluids.DIESEL);
                byHand.tank.setFill(15234);
                byHand.mode = 2;
                CompoundTag expected = BlockEntities.saveWithoutMetadata(byHand, registries);
                helper.assertTrue(saved.equals(expected), "capability fill saved " + saved + ", hand fill " + expected);
                helper.succeed();
            });
        });
    }

    /**
     * Fluid moved between two HBM machines through nothing but the capability, and a vanilla water bucket (the
     * loader's own bucket handler) emptied into a water barrel through it.
     */
    private static void machineToMachine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos from = helper.absolutePos(new BlockPos(2, Y, 3));
        BlockPos to = from.east(2);
        BlockPos waterPos = from.south(2);
        level.setBlock(from, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        level.setBlock(to, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        level.setBlock(waterPos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel source = (TileEntityBarrel) level.getBlockEntity(from);
        TileEntityBarrel sink = (TileEntityBarrel) level.getBlockEntity(to);
        TileEntityBarrel waterBarrel = (TileEntityBarrel) level.getBlockEntity(waterPos);
        source.tank.setTankType(Fluids.KEROSENE);
        source.tank.setFill(5000);
        source.mode = 2;
        sink.tank.setTankType(Fluids.KEROSENE);
        sink.mode = 0;
        waterBarrel.tank.setTankType(Fluids.WATER);
        waterBarrel.mode = 0;

        // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            Fluid kerosene = Fluids.KEROSENE.getFluid();
            FluidView out = FluidCompat.find(level, from, Direction.EAST);
            FluidView in = FluidCompat.find(level, to, Direction.WEST);
            helper.assertTrue(out != null && in != null, "both barrels expose their tanks");
            long offered = out.extract(kerosene, 1500, true);
            long room = in.insert(kerosene, offered, true);
            long taken = out.extract(kerosene, room, false);
            long put = in.insert(kerosene, taken, false);
            helper.assertTrue(taken == 1500 && put == 1500 && source.tank.getFill() == 3500 && sink.tank.getFill() == 1500,
                    "moved " + taken + "/" + put + ", source " + source.tank.getFill() + ", sink " + sink.tank.getFill());

            FluidView bucket = FluidCompat.findItem(new ItemStack(Items.WATER_BUCKET));
            helper.assertTrue(bucket != null, "the loader gives the vanilla water bucket a fluid handler");
            Fluid water = Fluids.WATER.getFluid();
            long drained = bucket.extract(water, 1000, false);
            FluidView barrelView = FluidCompat.find(level, waterPos, Direction.UP);
            long filled = barrelView == null ? -1 : barrelView.insert(water, drained, false);
            helper.assertTrue(drained == 1000 && filled == 1000 && waterBarrel.tank.getFill() == 1000,
                    "bucket gave " + drained + ", barrel took " + filled + ", holds " + waterBarrel.tank.getFill());
            helper.assertTrue(bucket.container().is(Items.BUCKET), "the drained bucket is an empty bucket: " + bucket.container());
            helper.succeed();
        });
    }

    /**
     * Container items: a full canister shows its diesel and drains through the capability; an empty canister fills
     * whole into a diesel canister (1.7 FluidContainerRegistry rows are whole) and refuses a fluid with no canister;
     * the universal tank fills partially.
     */
    private static void items(GameTestHelper helper) {
        Fluid diesel = Fluids.DIESEL.getFluid();
        ItemFluidTank canister = ModFluidNetItems.canister_full.get();
        FluidView full = FluidCompat.findItem(canister.make(Fluids.DIESEL, 1000));
        helper.assertTrue(full != null && full.tanks() == 1 && full.fluid(0) == diesel && full.amount(0) == 1000,
                "a full diesel canister reads 1000 mB diesel");
        helper.assertTrue(full.extract(Fluids.KEROSENE.getFluid(), 1000, false) == 0, "the diesel canister gave kerosene");
        helper.assertTrue(full.extract(diesel, 1000, false) == 1000, "the canister drains 1000 mB");
        helper.assertTrue(ItemFluidTank.getType(full.container()) == Fluids.NONE && canister.getFill(full.container()) == 0,
                "the drained canister is empty: " + full.container());

        FluidView empty = FluidCompat.findItem(new ItemStack(ModItems.canister_empty.get()));
        helper.assertTrue(empty != null, "the empty canister has a fluid handler");
        helper.assertTrue(empty.insert(diesel, 500, false) == 0, "an empty canister filled partially");
        helper.assertTrue(empty.insert(Fluids.WATER.getFluid(), 1000, false) == 0, "1.7 had no water canister");
        helper.assertTrue(empty.insert(diesel, 1000, false) == 1000, "the empty canister takes 1000 mB diesel");
        ItemStack filled = empty.container();
        helper.assertTrue(filled.is(canister) && ItemFluidTank.getType(filled) == Fluids.DIESEL && canister.getFill(filled) == 1000,
                "the canister became a full diesel canister: " + filled);

        FluidView tank = FluidCompat.findItem(new ItemStack(com.hbm.items.ModFluidItems.fluid_tank.get()));
        helper.assertTrue(tank != null && tank.insert(Fluids.STEAM.getFluid(), 250, false) == 250,
                "the universal tank takes 250 mB steam");
        helper.assertTrue(tank.amount(0) == 250 && tank.fluid(0) == Fluids.STEAM.getFluid(), "the tank reads 250 mB steam");
        helper.assertTrue(!ForeignFluids.rowItemsForTest().isEmpty(), "the 1.7 container rows are exposed");
        helper.succeed();
    }

    /**
     * Playtest 2026-10-12: "fluid can be stolen from machines". In 1.7 a machine offers a net only its
     * {@code getSendingTanks} (it calls {@code sendFluid} on those alone); its input tanks are never supply. A diesel
     * generator (switched off, so it burns nothing) holding diesel beside an empty input barrel and an empty buffer
     * barrel on one diesel duct keeps every mB, a foreign extract on its tank face gets nothing while a foreign insert
     * is taken, and the run is proven live by an output barrel then filling the generator.
     */
    private static void inputNotDrained(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos dieselPos = helper.absolutePos(new BlockPos(2, Y, 3));
        level.setBlock(dieselPos, com.hbm.blocks.machine.ModMachineBlocks.machine_diesel.get().defaultBlockState(), 3);
        com.hbm.tileentity.machine.TileEntityMachineDiesel diesel =
                (com.hbm.tileentity.machine.TileEntityMachineDiesel) level.getBlockEntity(dieselPos);
        helper.assertTrue(diesel != null, "the diesel generator did not appear");
        diesel.isOn = false;
        diesel.tank.setTankType(Fluids.DIESEL);
        diesel.tank.setFill(8000);

        BlockPos ductPos = dieselPos.east();
        level.setBlock(ductPos, ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        ((com.hbm.tileentity.network.TileEntityFluidDuctNT) level.getBlockEntity(ductPos)).setFluidType(Fluids.DIESEL);
        BlockPos inPos = ductPos.east();
        BlockPos bufferPos = ductPos.south();
        level.setBlock(inPos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        level.setBlock(bufferPos, ModFluidNetBlocks.barrel_steel.get().defaultBlockState(), 3);
        TileEntityBarrel input = (TileEntityBarrel) level.getBlockEntity(inPos);
        TileEntityBarrel buffer = (TileEntityBarrel) level.getBlockEntity(bufferPos);
        input.tank.setTankType(Fluids.DIESEL);
        input.mode = 0;
        buffer.tank.setTankType(Fluids.DIESEL);
        buffer.mode = 1;

        // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
        helper.runAfterDelay(PORT_CLAIM_WAIT, () -> {
            for (int pass = 0; pass < 8; pass++) com.hbm.api.fluidmk2.FluidNetwork.distributeAll(level);
            helper.assertTrue(input.tank.getFill() == 0 && buffer.tank.getFill() == 0,
                    "barrels drew a consumer's input through a duct: input " + input.tank.getFill() + ", buffer " + buffer.tank.getFill());
            helper.assertTrue(diesel.tank.getFill() == 8000, "the generator lost fuel to the net: " + diesel.tank.getFill());

            // A foreign pipe on the generator's other side: it may fill the input tank, never empty it.
            FluidView face = FluidCompat.find(level, dieselPos, Direction.WEST);
            helper.assertTrue(face != null, "the generator exposes its fuel tank to a foreign pipe");
            Fluid fuel = Fluids.DIESEL.getFluid();
            long extracted = face.extract(fuel, 1000, false);
            helper.assertTrue(extracted == 0 && diesel.tank.getFill() == 8000,
                    "a foreign extract took " + extracted + " mB from an input tank, left " + diesel.tank.getFill());
            long inserted = face.insert(fuel, 500, false);
            helper.assertTrue(inserted == 500 && diesel.tank.getFill() == 8500,
                    "a foreign insert into the input tank moved " + inserted + ", tank " + diesel.tank.getFill());

            input.mode = 2;
            // PortClaims: a machine is on a net only through the ports it claimed on its own ticks (1.7 trySubscribe/tryProvide)
            helper.runAfterDelay(3, () -> {
                input.tank.setFill(1000);
                for (int pass = 0; pass < 8 && diesel.tank.getFill() == 8500; pass++) com.hbm.api.fluidmk2.FluidNetwork.distributeAll(level);
                helper.assertTrue(diesel.tank.getFill() > 8500, "the duct never reached the generator, so the first half proved nothing");
                helper.succeed();
            });
        });
    }

    /**
     * Every HBM machine that is both a provider and a standard receiver: a tank it receives into but does not send
     * from adds nothing to {@code getFluidAvailable}, the only figure the duct network and the loader capabilities read
     * as supply. Catches a custom {@code getFluidAvailable} or a {@code getSendingTanks} that grew an input tank.
     */
    private static void inputNotProvided(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Set<Class<?>> seen = new HashSet<>();
        java.util.List<String> leaks = new java.util.ArrayList<>();
        int checked = 0;
        for (net.minecraft.world.level.block.Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) continue;
            if (!(block instanceof net.minecraft.world.level.block.EntityBlock entityBlock)) continue;
            try {
                net.minecraft.world.level.block.entity.BlockEntity be = entityBlock.newBlockEntity(pos, block.defaultBlockState());
                if (be == null || !seen.add(be.getClass())) continue;
                if (!(be instanceof com.hbm.api.fluidmk2.IFluidProviderMK2 provider)
                        || !(be instanceof com.hbm.api.fluidmk2.IFluidStandardReceiverMK2 receiver)) continue;
                com.hbm.inventory.fluid.tank.FluidTankNTM[] receiving = receiver.getReceivingTanks();
                com.hbm.inventory.fluid.tank.FluidTankNTM[] sending = provider instanceof com.hbm.api.fluidmk2.IFluidStandardSenderMK2 sender
                        ? sender.getSendingTanks() : new com.hbm.inventory.fluid.tank.FluidTankNTM[0];
                boolean any = false;
                for (com.hbm.inventory.fluid.tank.FluidTankNTM tank : receiving) {
                    if (tank == null || contains(sending, tank)) continue;
                    FluidType type = tank.getTankType() == Fluids.NONE ? Fluids.WATER : tank.getTankType();
                    if (tank.getTankType() == Fluids.NONE) tank.setTankType(type);
                    tank.setFill(tank.getMaxFill());
                    long expected = 0;
                    for (com.hbm.inventory.fluid.tank.FluidTankNTM out : sending) {
                        if (out != null && out != tank && out.getTankType() == type && out.getPressure() == tank.getPressure()) expected += out.getFill();
                    }
                    long available = provider.getFluidAvailable(type, tank.getPressure());
                    if (available != expected) {
                        leaks.add(BuiltInRegistries.BLOCK.getKey(block) + " offers " + available + " mB " + type.getName()
                                + " from an input tank (expected " + expected + ")");
                    }
                    any = true;
                }
                if (any) checked++;
            } catch (RuntimeException | LinkageError e) {
                // a machine that needs a level to build its tanks is covered by its own tests
            }
        }
        helper.assertTrue(checked >= 20, "expected at least 20 HBM machines with an input tank and a sending role, checked " + checked);
        helper.assertTrue(leaks.isEmpty(), "input tanks offered as supply: " + leaks);
        Constants.LOG.info("[network theft] {} provider machines with input tanks offer none of them as supply", checked);
        helper.succeed();
    }

    private static boolean contains(com.hbm.inventory.fluid.tank.FluidTankNTM[] tanks, com.hbm.inventory.fluid.tank.FluidTankNTM tank) {
        if (tanks == null) return false;
        for (com.hbm.inventory.fluid.tank.FluidTankNTM candidate : tanks) if (candidate == tank) return true;
        return false;
    }

    private static Direction direction(ForgeDirection dir) {
        return Direction.from3DDataValue(dir.ordinal());
    }
}
