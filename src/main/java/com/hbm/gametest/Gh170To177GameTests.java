package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModConveyorItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModelState;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.items.weapon.ItemAmmoArty.ArtilleryShellType;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import com.hbm.uninos.networkproviders.PneumaticNetwork;
import com.hbm.util.ConveyorInventories;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * GitHub issues 170, 174, 175 and 177: a cargo elevator takes riders that cannot fall back down with it, a cargo shell
 * sticks where it hits and draws closed once filled, a wand run keeps the side it was started on, and tar sand draws
 * 1.7's {@code ore_oil_sand_alt}. The player's ride is the harness scenario {@code Gh170CargoElevator}.
 */
public final class Gh170To177GameTests {

    private Gh170To177GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ELEVATOR_FLOATERS =
            TESTS.function("gh170_elevator_carries_riders_that_cannot_fall", 200, 16, () -> Gh170To177GameTests::floaters);
    public static final GameTests.Function TUBE_CELLS =
            TESTS.function("gh171_tubes_take_only_1_7_item_cells", 40, 32, () -> Gh170To177GameTests::tubeCells);
    public static final GameTests.Function CRANE_CELLS =
            TESTS.function("gh171_cranes_take_only_1_7_item_cells", 20, 8, () -> Gh170To177GameTests::craneCells);
    public static final GameTests.Function RBMK_SLOTS =
            TESTS.function("gh171_rbmk_rod_and_heater_slots_closed_to_automation", 20, 4, () -> Gh170To177GameTests::rbmkSlots);
    public static final GameTests.Function COKER =
            TESTS.function("gh171_coker_ports_take_tubes", 20, 6, () -> Gh170To177GameTests::coker);
    public static final GameTests.Function TUBE_RANGE =
            TESTS.function("gh171_tube_range_counts_holder_cells", 260, 16, () -> Gh170To177GameTests::tubeRange);
    public static final GameTests.Function SHELL_STICKS =
            TESTS.function("gh174_cargo_shell_sticks_at_impact", 60, 4, () -> Gh170To177GameTests::shellSticks);
    public static final GameTests.Function SHELL_SPRITE =
            TESTS.function("gh174_filled_cargo_shell_draws_closed", 20, () -> Gh170To177GameTests::shellSprite);
    public static final GameTests.Function WAND_SIDE =
            TESTS.function("gh175_wand_run_keeps_clicked_side", 20, 8, () -> Gh170To177GameTests::wandSide);
    public static final GameTests.Function TAR_SAND =
            TESTS.function("gh177_tar_sand_draws_1_7_texture", 20, () -> Gh170To177GameTests::tarSand);

    public static void init() {}

    /** A no-gravity armor stand and a no-AI pig ride a four-block lift up and back down; neither stays in the air. */
    private static void floaters(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(new BlockPos(4, 1, 4));
        for (int i = 0; i < 5; i++) {
            MultiblockHandlerXR.placeAssembled(helper.getLevel(), core.above(i), ModMachineBlocks.cargo_elevator.get(),
                    Direction.NORTH);
        }
        TileEntityCargoElevator lift = GameTestHelpers.blockEntity(helper, new BlockPos(4, 1, 4), TileEntityCargoElevator.class);
        helper.runAfterDelay(10, () -> {
            ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(3, 2, 4));
            stand.setNoGravity(true);
            Mob pig = helper.spawn(EntityType.PIG, new BlockPos(5, 2, 4));
            pig.setNoAi(true);
            double standY = stand.getY();
            double pigY = pig.getY();
            lift.toggleElevator();
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(Math.abs(stand.getY() - standY - 4D) < 0.01D, "the stand rode up: " + (stand.getY() - standY));
                helper.assertTrue(Math.abs(pig.getY() - pigY - 4D) < 0.01D, "the pig rode up: " + (pig.getY() - pigY));
                lift.toggleElevator();
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(lift.extension == 0D, "platform retracted: " + lift.extension);
                    helper.assertTrue(Math.abs(stand.getY() - standY) < 0.01D,
                            "the no-gravity stand came down with the platform: " + (stand.getY() - standY));
                    helper.assertTrue(Math.abs(pig.getY() - pigY) < 0.01D,
                            "the no-AI pig came down with the platform: " + (pig.getY() - pigY));
                    helper.succeed();
                });
            });
        });
    }

    /** Every HBM multiblock: tubes and cranes reach exactly the cells 1.7's {@code createNewTileEntity} proxied with items. */
    private static void tubeCells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(16, 2, 16);
        List<String> wrong = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockMultiblockCore machine)) continue;
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            if (!Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            BlockPos placed = FoldedTestSupport.assemble(helper, machine, at);
            Set<BlockPos> expected = new HashSet<>();
            Set<BlockPos> tubes = new HashSet<>();
            Set<BlockPos> cranes = new HashSet<>();
            for (BlockPos pos : BlockPos.betweenClosed(placed.offset(-14, -2, -14), placed.offset(14, 24, 14))) {
                BlockState state = level.getBlockState(pos);
                if (pos.equals(placed) || !MultiblockSurface.isFoldedCell(state)) continue;
                boolean proxy = EVERY_CELL_17.contains(name)
                        || LID_17.contains(name) && state.getValue(RBMKBase.PART) == RBMKBase.Part.TOP
                        || EXTRA_17.containsKey(name)
                        && MultiblockSurface.carriesPassive(level, pos, BlockMultiblockCore.PASSIVE_ITEMS);
                if (proxy) expected.add(pos.immutable());
                if (PneumaticNetwork.containerAt(level, pos) != null) tubes.add(pos.immutable());
                if (ConveyorInventories.containerAt(level, pos) != null) cranes.add(pos.immutable());
            }
            if (!tubes.equals(expected) || !cranes.equals(expected)
                    || EXTRA_17.containsKey(name) && expected.size() != EXTRA_17.get(name)) {
                wrong.add(name + " stamps " + expected.size() + " (1.7 " + EXTRA_17.getOrDefault(name, -1) + ") tubes "
                        + tubes.size() + " cranes " + cranes.size());
            }
            clear(level, placed);
        }
        helper.assertTrue(wrong.isEmpty(), "tube or crane cells differ from 1.7's inventory proxies: " + wrong);
        helper.succeed();
    }

    private static final Set<String> EVERY_CELL_17 = Set.of("furnace_iron", "furnace_steel", "machine_furnace_combination",
            "machine_ammo_press", "machine_soldering_station", "machine_arc_welder", "machine_crucible", "machine_ashpit",
            "heater_oven", "sat_dock", "machine_reactor", "turret_chekhov", "turret_friendly", "turret_howard",
            "turret_tauon", "turret_maxwell", "turret_jeremy", "turret_richard", "turret_fritz", "turret_himars",
            "turret_arty", "rbmk_storage", "rbmk_autoloader", "machine_wood_burner", "heater_firebox", "launch_pad_large");
    private static final Set<String> LID_17 = Set.of("rbmk_outgasser", "rbmk_fuel_rod", "rbmk_fuel_rod_moderated",
            "rbmk_fuel_rod_reasim", "rbmk_fuel_rod_reasim_moderated");
    // inventory proxies on every dummy cell; on an RBMK lid
    // makeExtra cells (distinct, beside the core) of machines whose gated 1.7 proxy has inventory
    private static final Map<String, Integer> EXTRA_17 = Map.ofEntries(Map.entry("machine_radgen", 3),
            Map.entry("machine_zirnox", 5), Map.entry("watz", 9), Map.entry("fusion_torus", 25),
            Map.entry("fusion_breeder", 5), Map.entry("fusion_plasma_forge", 10), Map.entry("machine_epress", 0),
            Map.entry("machine_press", 0), Map.entry("machine_sawmill", 4), Map.entry("machine_assembly_machine", 8),
            Map.entry("machine_assembly_factory", 16), Map.entry("machine_precass", 8),
            Map.entry("machine_chemical_plant", 8), Map.entry("machine_chemical_factory", 26),
            Map.entry("machine_electrolyser", 16), Map.entry("machine_exposure_chamber", 5),
            Map.entry("machine_blast_furnace", 7), Map.entry("machine_purex", 16), Map.entry("machine_rockmill", 8),
            Map.entry("machine_supercomputer", 5), Map.entry("machine_liquefactor", 5), Map.entry("machine_solidifier", 5),
            Map.entry("machine_refinery", 4), Map.entry("machine_coker", 4), Map.entry("machine_pyrooven", 6),
            Map.entry("machine_arc_furnace", 6), Map.entry("machine_crystallizer", 4), Map.entry("machine_cyclotron", 12),
            Map.entry("machine_silex", 2), Map.entry("machine_strand_caster", 8), Map.entry("launch_pad", 4),
            Map.entry("launchpad_soyuz", 12), Map.entry("launchpad_lambda", 5), Map.entry("machine_mining_laser", 4),
            Map.entry("pa_source", 10), Map.entry("pa_detector", 5), Map.entry("machine_rotary_furnace", 8),
            Map.entry("machine_ore_slopper", 8), Map.entry("machine_annihilator", 3), Map.entry("machine_battery_socket", 3),
            Map.entry("icf", 5), Map.entry("machine_radiolysis", 4));

    /** 1.7's rod and heater are slotted with no slot open to any side; the rod is reached at its lid as in 1.7. */
    private static void rbmkSlots(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rod = FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), new BlockPos(1, 1, 1));
        BlockPos heater = FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_heater.get(), new BlockPos(3, 1, 1));
        BlockPos lid = rod;
        while (MultiblockSurface.isFoldedCell(level.getBlockState(lid.above())) || lid.equals(rod)) lid = lid.above();
        helper.assertTrue(PneumaticNetwork.containerAt(level, lid) != null, "a tube finds the rod at its lid");
        for (BlockPos at : List.of(rod, lid, heater)) {
            Container found = ConveyorInventories.containerAt(level, at);
            helper.assertTrue(found instanceof WorldlyContainer, "no sided container at " + at + ": " + found);
            for (Direction side : Direction.values()) {
                helper.assertTrue(((WorldlyContainer) found).getSlotsForFace(side).length == 0,
                        "automation sees a slot at " + at + " from " + side);
            }
        }
        helper.succeed();
    }

    private static void coker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_coker.get(), new BlockPos(3, 1, 3));
        for (BlockPos port : List.of(core.offset(1, 0, 1), core.offset(-1, 0, 1), core.offset(1, 0, -1), core.offset(-1, 0, -1))) {
            helper.assertTrue(PneumaticNetwork.containerAt(level, port) != null && ConveyorInventories.containerAt(level, port) != null,
                    "no inventory at the coker port " + port + ": " + level.getBlockState(port) + " core "
                            + MultiblockSurface.coreOfAny(level, port) + " items "
                            + MultiblockSurface.carriesPassive(level, port, BlockMultiblockCore.PASSIVE_ITEMS));
        }
        helper.assertTrue(PneumaticNetwork.containerAt(level, core.offset(0, 0, 1)) == null, "a coker side cell is no port");
        helper.succeed();
    }

    /** Cranes look up 1.7's cells as tubes do: the centrifuge's core and not the cell above, a firebox side, a chest. */
    private static void craneCells(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centrifuge = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_centrifuge.get(), new BlockPos(1, 1, 1));
        BlockPos firebox = FoldedTestSupport.assemble(helper, ModMachineBlocks.heater_firebox.get(), new BlockPos(5, 1, 5));
        helper.setBlock(new BlockPos(1, 1, 5), Blocks.CHEST);
        helper.assertTrue(ConveyorInventories.containerAt(level, centrifuge) != null, "a crane reaches the centrifuge core");
        helper.assertTrue(ConveyorInventories.containerAt(level, centrifuge.above()) == null,
                "a crane must not reach the centrifuge above its core: 1.7 has no proxy there");
        helper.assertTrue(ConveyorInventories.containerAt(level, firebox.west()) != null,
                "a crane reaches a firebox side cell (1.7 proxies its inventory on every cell)");
        helper.assertTrue(ConveyorInventories.containerAt(level, helper.absolutePos(new BlockPos(1, 1, 5))) != null,
                "a crane still reaches a vanilla chest");
        helper.succeed();
    }

    private static void clear(ServerLevel level, BlockPos core) {
        BlockMultiblockCore.withoutTeardown(() -> {
            for (BlockPos pos : BlockPos.betweenClosed(core.offset(-14, -2, -14), core.offset(14, 24, 14))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        });
    }

    /** Chest to PWR port 12 blocks off (a wrapper, not a block entity): out of range at pressure 1 (10), in at 2. */
    private static void tubeRange(GameTestHelper helper) {
        TileEntityPWRController controller = PwrGameTests.build(helper);
        BlockPos chest = new BlockPos(-8, 3, 8);
        helper.setBlock(chest, Blocks.CHEST);
        for (int x = -7; x <= 3; x++) helper.setBlock(new BlockPos(x, 3, 8), ModPneumaticBlocks.PNEUMATIC_TUBE.get());
        TileEntityPneumoTube compressor = GameTestHelpers.blockEntity(helper, new BlockPos(-7, 3, 8), TileEntityPneumoTube.class);
        compressor.insertionDir = Direction.WEST;
        compressor.compair.setFill(compressor.compair.getMaxFill());
        compressor.setChanged();
        TileEntityPneumoTube endpoint = GameTestHelpers.blockEntity(helper, new BlockPos(3, 3, 8), TileEntityPneumoTube.class);
        endpoint.ejectionDir = Direction.EAST;
        endpoint.setChanged();
        Item fuel = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "pwr_fuel_meu"));
        ((Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest))).setItem(0, new ItemStack(fuel, 4));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!controller.getItem(0).is(fuel),
                    "pressure 1 reaches 10 blocks, the PWR port is 12 away: " + controller.getItem(0));
            compressor.compair.withPressure(2);
            compressor.compair.setFill(compressor.compair.getMaxFill());
            helper.runAfterDelay(120, () -> {
                helper.assertTrue(controller.getItem(0).is(fuel), "pressure 2 reaches the PWR port: " + controller.getItem(0));
                helper.succeed();
            });
        });
    }

    /** Falling at 3 blocks a tick onto the floor, the shell must stop on it, not three blocks into it. */
    private static void shellSticks(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        }
        BlockPos top = helper.absolutePos(new BlockPos(2, 1, 2));
        EntityArtilleryShell shell = new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), helper.getLevel());
        shell.setType(ArtilleryShellType.CARGO.ordinal());
        shell.setCargo(new ItemStack(Items.DIAMOND));
        shell.setPos(top.getX() + 0.5D, top.getY() + 12.2D, top.getZ() + 0.5D);
        shell.setDeltaMovement(0D, -3D, 0D);
        helper.getLevel().addFreshEntity(shell);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(shell.isAlive(), "a cargo shell stays where it landed");
            double ground = top.getY() + 1D;
            helper.assertTrue(Math.abs(shell.getY() - ground) < 0.01D,
                    "the shell stuck at y " + shell.getY() + ", the floor's top is " + ground);
            helper.succeed();
        });
    }

    /** 1.7 {@code getIconIndex}: a cargo shell with a stack in it draws {@code ammo_arty_cargo_full}. */
    private static void shellSprite(GameTestHelper helper) {
        ItemStack empty = new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.CARGO));
        ItemStack filled = empty.copy();
        ModDataComponents.ARTY_CARGO.set(filled, new ItemStack(Items.DIAMOND));
        ItemStack plain = new ItemStack(ModAmmoItems.ammo_arty.get(ArtilleryShellType.NORMAL));
        helper.assertTrue(ModelState.of(empty) == 0, "an empty cargo shell draws ammo_arty_cargo");
        helper.assertTrue(ModelState.of(filled) == 1, "a filled cargo shell draws ammo_arty_cargo_full");
        helper.assertTrue(ModelState.of(plain) == 0, "other shells keep their one sprite");
        helper.succeed();
    }

    /** Started on the north face of an ejector and ended on the floor north of it, the run starts beside that face. */
    private static void wandSide(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
        }
        BlockPos crane = helper.absolutePos(new BlockPos(4, 2, 7));
        helper.getLevel().setBlockAndUpdate(crane, ModConveyorBlocks.CRANE_EXTRACTOR.get().defaultBlockState()
                .setValue(BlockCraneBase.INPUT, Direction.SOUTH).setValue(BlockCraneBase.OUTPUT, Direction.NORTH));
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack wand = new ItemStack(ModConveyorItems.CONVEYOR_WAND_REGULAR.get(), 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.setPos(crane.getX() + 0.5D, crane.getY(), crane.getZ() - 2.5D);

        Vec3 face = Vec3.atCenterOf(crane).relative(Direction.NORTH, 0.5D);
        wand.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(face, Direction.NORTH, crane, false)));
        ItemConveyorWand.Run run = ItemConveyorWand.pendingRun(player.getMainHandItem());
        helper.assertTrue(run != null && run.side() == Direction.NORTH,
                "the run started on the ejector's north face keeps that side: " + run);

        BlockPos floor = crane.offset(0, -1, -5);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).relative(Direction.UP, 0.5D), Direction.UP, floor, false)));
        helper.assertTrue(helper.getLevel().getBlockState(crane.north()).getBlock() instanceof BlockConveyorBase,
                "the first belt lies beside the clicked face: " + helper.getLevel().getBlockState(crane.north()));
        helper.assertTrue(helper.getLevel().getBlockState(crane.above()).isAir(),
                "nothing is built on top of the ejector: " + helper.getLevel().getBlockState(crane.above()));
        helper.succeed();
    }

    private static void tarSand(GameTestHelper helper) {
        String model = resourceText("assets/hbm/models/block/ore_oil_sand.json");
        helper.assertTrue(model.contains("hbm:blocks/ore_oil_sand_alt"), "tar sand's model: " + model);
        helper.assertTrue(GameTestHelpers.resourceExists(Gh170To177GameTests.class,
                "assets/hbm/textures/blocks/ore_oil_sand_alt.png"), "1.7's ore_oil_sand_alt texture ships");
        helper.succeed();
    }

    private static String resourceText(String path) {
        try (InputStream stream = Gh170To177GameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // a missing or unreadable resource fails the assertion that reads it
            return "";
        }
    }
}
