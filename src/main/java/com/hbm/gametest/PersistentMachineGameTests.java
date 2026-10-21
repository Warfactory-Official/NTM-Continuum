package com.hbm.gametest;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.machine.MachineExplosionDamage;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.block.CarriedTankInfo;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7's {@code IPersistentNBT}: a fluid tank, a refinery and the three drills keep their contents in the item they drop,
 * from whichever part is broken, and give them back when placed. An explosion drops nothing, and breaking in creative
 * drops nothing. The derrick and pumpjack let gas out of uranium and asbestos ore.
 */
public final class PersistentMachineGameTests {

    private PersistentMachineGameTests() {}

    private static final BlockPos CORE = new BlockPos(5, 1, 5);

    private static ServerPlayer miner(GameTestHelper helper, GameType mode) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setGameMode(mode);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        return player;
    }

    private static ItemStack dropped(GameTestHelper helper, Item item) {
        ServerLevel level = helper.getLevel();
        AABB around = new AABB(helper.absolutePos(CORE)).inflate(10);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, around)) {
            if (entity.getItem().is(item)) return entity.getItem();
        }
        return ItemStack.EMPTY;
    }

    private static void clearDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(CORE)).inflate(10))) {
            entity.discard();
        }
    }

    private static BlockPos aCell(GameTestHelper helper, BlockPos core, Block block) {
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-1, 0, -1), core.offset(1, 1, 1))) {
            BlockState state = helper.getLevel().getBlockState(p);
            if (!p.equals(core) && state.getBlock() == block && state.getValue(BlockMultiblockMachine.CELL)) return p.immutable();
        }
        return null;
    }

    /** Places {@code stack} on the plot floor with {@code player} and returns the machine block entity found near (9, 1, 9). */
    private static <T extends BlockEntity> T place(GameTestHelper helper, ServerPlayer player, ItemStack stack, Class<T> type) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(new BlockPos(9, 0, 9));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        InteractionResult result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        for (BlockPos p : BlockPos.betweenClosed(floor.offset(-3, 0, -3), floor.offset(3, 4, 3))) {
            BlockEntity be = helper.getLevel().getBlockEntity(p);
            if (type.isInstance(be)) return type.cast(be);
        }
        helper.fail("the placed machine has no " + type.getSimpleName() + ": " + result + ", above floor " + helper.getLevel().getBlockState(floor.above()) + ", floor " + helper.getLevel().getBlockState(floor));
        return null;
    }

    /** A tank with fluid, mode and all, travels with the item broken at its core, and again when broken at a cell. */
    public static void fluidTank(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_fluidtank.get();
        for (int round = 0; round < 2; round++) {
            BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
            TileEntityMachineFluidTank tank = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineFluidTank.class);
            tank.tank.setTankType(Fluids.OIL);
            tank.tank.setFill(5_000);
            tank.mode = 2;
            ServerPlayer player = miner(helper, GameType.SURVIVAL);
            BlockPos target = round == 0 ? core : aCell(helper, core, block);
            helper.assertTrue(target != null, "the tank has a cell to break");
            player.gameMode.destroyBlock(target);
            ItemStack stack = dropped(helper, block.asItem());
            helper.assertTrue(!stack.isEmpty(), "breaking the " + (round == 0 ? "core" : "cell") + " drops the tank");
            helper.assertTrue(ItemStacks.carriedInt(stack, "tank") == 5_000
                            && ItemStacks.carriedString(stack, "tank_type").equals("hbm:oil"),
                    "the item must carry 5000 mB of oil, carries " + ItemStacks.carriedInt(stack, "tank") + " of "
                            + ItemStacks.carriedString(stack, "tank_type"));
            List<Component> lines = CarriedTankInfo.lines(stack, "tank");
            helper.assertTrue(lines.size() == 1 && lines.get(0).getString().startsWith("5000/"), "the tooltip names the fill: " + lines);
            clearDrops(helper);
            helper.getLevel().removeBlock(core, false);
            TileEntityMachineFluidTank placed = place(helper, player, stack.copy(), TileEntityMachineFluidTank.class);
            helper.assertTrue(placed.tank.getFill() == 5_000 && placed.tank.getTankType() == Fluids.OIL && placed.mode == 2,
                    "the placed tank must hold 5000 mB of oil in mode 2: " + placed.tank.getFill() + " " + placed.mode);
            helper.getLevel().removeBlock(placed.getBlockPos(), false);
        }
        helper.succeed();
    }

    /** An empty tank drops a plain item, as 1.7's writeNBT returns without a tag. */
    public static void emptyTank(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
        miner(helper, GameType.SURVIVAL).gameMode.destroyBlock(core);
        ItemStack stack = dropped(helper, block.asItem());
        helper.assertTrue(!stack.isEmpty() && ItemStacks.carriedString(stack, "tank_type").isEmpty(),
                "an empty tank drops a plain item, got " + stack);
        helper.succeed();
    }

    /** All five refinery tanks and the torn state travel. */
    public static void refinery(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_refinery.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
        TileEntityMachineRefinery te = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineRefinery.class);
        te.tanks[0].setFill(12_000);
        te.tanks[2].setFill(300);
        te.hasExploded = true;
        te.onFire = true;
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        player.gameMode.destroyBlock(core);
        ItemStack stack = dropped(helper, block.asItem());
        helper.assertTrue(ItemStacks.carriedInt(stack, "tank0") == 12_000 && ItemStacks.carriedInt(stack, "tank2") == 300
                        && ItemStacks.carriedBoolean(stack, "exploded"),
                "the item must carry the hot oil, the naphtha and the torn state");
        clearDrops(helper);
        TileEntityMachineRefinery placed = place(helper, player, stack.copy(), TileEntityMachineRefinery.class);
        helper.assertTrue(placed.tanks[0].getFill() == 12_000 && placed.tanks[2].getFill() == 300 && placed.hasExploded && placed.onFire,
                "the placed refinery must be torn and hold its fluids");
        helper.succeed();
    }

    /** The derrick keeps its power and both tanks; the pumpjack and fracking tower share the class. */
    public static void derrick(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_well.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
        TileEntityMachineOilWell te = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineOilWell.class);
        te.power = 4_000;
        te.tanks[0].setFill(2_500);
        te.tanks[1].setFill(700);
        ServerPlayer player = miner(helper, GameType.SURVIVAL);
        player.gameMode.destroyBlock(core);
        ItemStack stack = dropped(helper, block.asItem());
        helper.assertTrue(ItemStacks.carriedInt(stack, "t0") == 2_500 && ItemStacks.carriedInt(stack, "t1") == 700
                        && ItemStacks.carriedInt(stack, "power") == 4_000, "the item must carry power and both tanks");
        clearDrops(helper);
        TileEntityMachineOilWell placed = place(helper, player, stack.copy(), TileEntityMachineOilWell.class);
        helper.assertTrue(placed.power == 4_000 && placed.tanks[0].getFill() == 2_500 && placed.tanks[1].getFill() == 700,
                "the placed derrick must have its power and fluids back: " + placed.power);
        helper.succeed();
    }

    /** Breaking in creative drops nothing; a fresh empty machine and a full one alike. */
    public static void creativeBreak(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
        GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineFluidTank.class).tank.setFill(1_000);
        miner(helper, GameType.CREATIVE).gameMode.destroyBlock(core);
        helper.assertTrue(dropped(helper, block.asItem()).isEmpty(), "breaking in creative must drop nothing");
        helper.succeed();
    }

    /** 1.7 canDropFromExplosion is false: the first blast tears the tank open, the second removes it, neither drops. */
    public static void explosion(GameTestHelper helper) {
        BlockMultiblockCore block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, CORE);
        TileEntityMachineFluidTank tank = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineFluidTank.class);
        tank.tank.setFill(1_000);
        ServerLevel level = helper.getLevel();
        MachineExplosionDamage.hit(level, core, new Object(), null);
        helper.assertTrue(tank.hasExploded && level.getBlockState(core).getBlock() == block, "the first blast tears the tank open");
        MachineExplosionDamage.hit(level, core, new Object(), null);
        helper.assertTrue(level.getBlockState(core).isAir() && dropped(helper, block.asItem()).isEmpty(),
                "the second blast removes the tank and nothing drops");
        helper.succeed();
    }

    /**
     * 1.7 derrick: for uranium ore the loop tests (j, j) and fills (k, k) at y + 10, so only the diagonal gets dense
     * radon; asbestos ore gives asbestos.
     */
    public static void derrickGas(GameTestHelper helper) {
        helper.setBlock(CORE, ModMachineBlocks.machine_well.get().defaultBlockState());
        BlockPos core = helper.absolutePos(CORE);
        TileEntityMachineOilWell te = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachineOilWell.class);
        ServerLevel level = helper.getLevel();
        level.setBlock(core.below(), ModNuclearBlocks.ore_uranium.get().defaultBlockState(), 3);
        te.onDrill(core.getY() - 1);
        for (int k = -1; k <= 1; k++) {
            helper.assertTrue(level.getBlockState(core.offset(k, 10, k)).is(ModNuclearBlocks.gas_radon_dense.get()),
                    "dense radon on the diagonal at " + k);
        }
        helper.assertTrue(level.getBlockState(core.offset(1, 10, -1)).isAir() && level.getBlockState(core.offset(0, 10, 1)).isAir(),
                "no gas off the diagonal");
        helper.succeed();
    }

    /** 1.7 pumpjack: the four horizontal neighbours of the core get the gas; asbestos ore lets out asbestos. */
    public static void pumpjackGas(GameTestHelper helper) {
        helper.setBlock(CORE, ModMachineBlocks.machine_pumpjack.get().defaultBlockState());
        BlockPos core = helper.absolutePos(CORE);
        TileEntityMachinePumpjack te = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachinePumpjack.class);
        ServerLevel level = helper.getLevel();
        level.setBlock(core.below(), ModNuclearBlocks.ore_uranium.get().defaultBlockState(), 3);
        te.onDrill(core.getY() - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(level.getBlockState(core.relative(dir)).is(ModNuclearBlocks.gas_radon_dense.get()),
                    "dense radon beside the pumpjack at " + dir);
        }
        helper.succeed();
    }

    /** Asbestos ore lets out asbestos. */
    public static void pumpjackAsbestos(GameTestHelper helper) {
        helper.setBlock(CORE, ModMachineBlocks.machine_pumpjack.get().defaultBlockState());
        BlockPos core = helper.absolutePos(CORE);
        TileEntityMachinePumpjack te = GameTestHelpers.blockEntity(helper, CORE, TileEntityMachinePumpjack.class);
        ServerLevel level = helper.getLevel();
        level.setBlock(core.below(), ModBlocks.ore_asbestos.get().defaultBlockState(), 3);
        te.onDrill(core.getY() - 1);
        helper.assertTrue(level.getBlockState(core.relative(Direction.NORTH)).is(ModNuclearBlocks.gas_asbestos.get()),
                "asbestos ore lets out asbestos");
        helper.succeed();
    }
}
