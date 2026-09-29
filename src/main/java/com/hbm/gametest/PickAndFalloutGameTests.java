package com.hbm.gametest;

import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.items.ModConveyorItems;
import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.tileentity.deco.TileEntityTrinket;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * 2026-09-29 playtest fixes: pick-block of a bobblehead / conveyor, and where fallout ash may land.
 *
 * <p>The pick tests go through {@link CompatBlock#pickBlockStack}, which is the same {@code onPickBlock} hook
 * Core's {@code @Override} of this era's vanilla {@code getCloneItemStack} calls: before, HBM declared the
 * 1.20.1/1.21.1 spellings only, which on 26.1 (an extra {@code includeData} flag) were plain dead overloads.
 */
public final class PickAndFalloutGameTests {

    private PickAndFalloutGameTests() {}

    /** 1.7 {@code BlockBobble.getPickBlock}: the item for the figure's own type, not type 0. */
    public static void bobblePick(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockBobble block = ModDecoCrateBlocks.bobblehead.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        TileEntityTrinket bobble = (TileEntityTrinket) helper.getLevel().getBlockEntity(pos);
        int type = BlockBobble.BobbleType.PU238.ordinal();
        bobble.setTypeOrdinal(type);

        ItemStack picked = block.pickBlockStack(helper.getLevel(), pos, helper.getLevel().getBlockState(pos));
        helper.assertTrue(picked.getItem() instanceof ItemBlockDecoVariant item && item.variant(picked) == type,
                "Picking a Pu-238 bobblehead must give the Pu-238 bobblehead, got " + picked);
        helper.succeed();
    }

    /** Every belt picks the wand it drops (1.7 getItemDropped conveyor_wand, damageDropped 0/1/2/3). */
    public static void conveyorPick(GameTestHelper helper) {
        check(helper, ModConveyorBlocks.CONVEYOR.get(), ModConveyorItems.CONVEYOR_WAND_REGULAR.get());
        check(helper, ModConveyorBlocks.CONVEYOR_EXPRESS.get(), ModConveyorItems.CONVEYOR_WAND_EXPRESS.get());
        check(helper, ModConveyorBlocks.CONVEYOR_DOUBLE.get(), ModConveyorItems.CONVEYOR_WAND_DOUBLE.get());
        check(helper, ModConveyorBlocks.CONVEYOR_TRIPLE.get(), ModConveyorItems.CONVEYOR_WAND_TRIPLE.get());
        check(helper, ModConveyorBlocks.CONVEYOR_CHUTE.get(), ModConveyorItems.CONVEYOR_WAND_REGULAR.get());
        check(helper, ModConveyorBlocks.CONVEYOR_LIFT.get(), ModConveyorItems.CONVEYOR_WAND_REGULAR.get());
        helper.succeed();
    }

    private static void check(GameTestHelper helper, Block block, Item wand) {
        ItemStack picked = ((CompatBlock) block).pickBlockStack(helper.getLevel(), BlockPos.ZERO, block.defaultBlockState());
        helper.assertTrue(picked.is(wand), "Picking " + block + " must give " + wand + ", got " + picked);
    }

    /**
     * One column's stomp over dry stone, open water and seagrass. The roll is forced (a random source whose
     * nextDouble is 0 always passes the 70%-scale chance) so the dry column proves the ash is laid at all.
     */
    public static void falloutPlacement(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos dry = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos water = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos grass = helper.absolutePos(new BlockPos(9, 1, 1));
        helper.getLevel().setBlock(dry, stone, 3);
        basin(helper, water, Blocks.WATER.defaultBlockState());
        basin(helper, grass, Blocks.SEAGRASS.defaultBlockState());

        Block ash = ModFalloutBlocks.fallout.get();
        Map<BlockPos, BlockState> dryWrites = stomp(helper, dry);
        helper.assertTrue(dryWrites.getOrDefault(dry.above(), Blocks.AIR.defaultBlockState()).is(ash),
                "A forced roll must lay ash on dry stone; writes " + dryWrites);
        for (BlockPos column : new BlockPos[] {water, grass}) {
            Map<BlockPos, BlockState> writes = stomp(helper, column);
            for (Map.Entry<BlockPos, BlockState> write : writes.entrySet()) {
                helper.assertTrue(!write.getValue().is(ash),
                        "No ash may be laid in or on water, got one at " + write.getKey() + " in " + writes);
            }
        }

        // Inside 65% the vegetation rule removes seagrass; at 50% the forced roll still lays ash (0.1 - 0.2^2 > 0).
        // The seagrass must become water, not air, or the floor under it gets ash deep under the sea.
        BlockPos nearGrass = helper.absolutePos(new BlockPos(13, 1, 1));
        basin(helper, nearGrass, Blocks.SEAGRASS.defaultBlockState());
        Map<BlockPos, BlockState> nearWrites = stomp(helper, nearGrass, 50.0D);
        for (Map.Entry<BlockPos, BlockState> write : nearWrites.entrySet()) {
            helper.assertTrue(!write.getValue().is(ash),
                    "No ash under water where the sweep removed seagrass, got one at " + write.getKey() + " in " + nearWrites);
            helper.assertTrue(!write.getValue().isAir() || !write.getKey().equals(nearGrass.above()),
                    "Removed seagrass must leave water, not air: " + nearWrites);
        }

        helper.assertTrue(!BlockFallout.canOccupy(Blocks.SEAGRASS.defaultBlockState())
                        && !BlockFallout.canOccupy(Blocks.WATER.defaultBlockState())
                        && BlockFallout.canOccupy(Blocks.AIR.defaultBlockState())
                        && BlockFallout.canOccupy(Blocks.DEAD_BUSH.defaultBlockState()),
                "Ash may replace air and dry plants, never a cell holding fluid");
        helper.succeed();
    }

    /** 1.7 Material.sand blocks liquid movement: water next to ash must not flow in and wash it out as items. */
    public static void falloutHoldsWater(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = 1; x <= 4; x++) helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 1, 2)), stone, 3);
        BlockPos ash = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(ash, ModFalloutBlocks.fallout.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(helper.absolutePos(new BlockPos(3, 2, 2)), Blocks.WATER.defaultBlockState(), 3);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(ash).is(ModFalloutBlocks.fallout.get()),
                    "Water must not flow into ash, got " + helper.getLevel().getBlockState(ash));
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(ash).inflate(3D)).isEmpty(),
                    "Water next to ash must not wash it out as items");
            helper.succeed();
        });
    }

    /** 1.7 onNeighborBlockChange: ash that loses its support is set to air, dropping nothing. */
    public static void falloutPopsWithoutDrops(GameTestHelper helper) {
        BlockPos support = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(support.above(), ModFalloutBlocks.fallout.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(support.above()).is(ModFalloutBlocks.fallout.get()),
                "Ash must stand on stone");

        helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockState(support.above()).isAir(),
                "Ash must vanish once its support is gone");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(support).inflate(3D)).isEmpty(),
                "Unsupported ash must vanish without dropping an item");
        helper.succeed();
    }

    /** A stone-rimmed one-cell basin holding {@code fill} over a stone floor, so the water cannot flow. */
    private static void basin(GameTestHelper helper, BlockPos floor, BlockState fill) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.getLevel().setBlock(floor.offset(dx, 0, dz), stone, 3);
                if (dx != 0 || dz != 0) {
                    helper.getLevel().setBlock(floor.offset(dx, 1, dz), stone, 3);
                    helper.getLevel().setBlock(floor.offset(dx, 2, dz), stone, 3);
                }
            }
        }
        helper.getLevel().setBlock(floor.above(), fill, 3);
        helper.getLevel().setBlock(floor.above(2), Blocks.WATER.defaultBlockState(), 3);
    }

    private static Map<BlockPos, BlockState> stomp(GameTestHelper helper, BlockPos column) {
        return stomp(helper, column, 70.0D);
    }

    private static Map<BlockPos, BlockState> stomp(GameTestHelper helper, BlockPos column, double distPercent) {
        LegacyRandomSource rolls = new LegacyRandomSource(0L) {
            @Override
            public double nextDouble() {
                return 0.0D;
            }

            // Vanilla's polar gaussian loops until two nextDouble() draws land inside the unit circle; with
            // nextDouble() pinned to 0 they never do, and a falloff-tail rule (reached inside 65%) hangs the server.
            @Override
            public double nextGaussian() {
                return 0.0D;
            }
        };
        return EntityFalloutRain.stompColumnForTest(helper.getLevel(), column.getX(), column.getZ(), distPercent, rolls);
    }
}
