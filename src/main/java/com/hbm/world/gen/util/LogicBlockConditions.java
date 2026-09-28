package com.hbm.world.gen.util;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.generic.LogicBlock.TileEntityLogicBlock;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.tileentity.bomb.TileEntityCharge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/**
 * 1.7 {@code LogicBlockConditions}: the phase conditions a logic block can run, by name.
 *
 * <p>1.7 built its player boxes as {@code (x, y, z, x + 1, y - 2, z + 1).expand(n, n, n)} - a box whose maximum Y
 * sits below its minimum until the expansion flips it - so the result runs from {@code y - n} to {@code y - 2 + n}.
 * {@link #cube} spells that box out; letting {@link AABB} normalise the corners first would move it.
 */
public class LogicBlockConditions {

    public static LinkedHashMap<String, Function<TileEntityLogicBlock, Boolean>> conditions;

    /** 1.7 {@code AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y - 2, z + 1).expand(n, n, n)}. */
    public static AABB cube(BlockPos pos, int n) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new AABB(x - n, y - n, z - n, x + 1 + n, y - 2 + n, z + 1 + n);
    }

    static boolean playerIn(Level level, AABB box) {
        return !level.getEntitiesOfClass(Player.class, box).isEmpty();
    }

    /** For use with interactions, for having them handle all conditional tasks. */
    public static Function<TileEntityLogicBlock, Boolean> EMPTY = (tile) -> false;

    public static Function<TileEntityLogicBlock, Boolean> ABERRATOR = (tile) -> {
        Level world = tile.getLevel();
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        BlockPos pos = tile.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        boolean aoeCheck = playerIn(world, cube(pos, 10));
        if (tile.phase == 0) {
            if (world.getGameTime() % 20 != 0) return false;
            return aoeCheck;
        }
        if (tile.phase < 3) {
            if (world.getGameTime() % 20 != 0 || tile.timer < 60) return false;
            // 1.7 (x, y, z, x - 2, y + 1, z + 1).expand(50, 20, 50)
            return world.getEntitiesOfClass(EntityUndeadSoldier.class,
                    new AABB(x - 50, y - 20, z - 50, x + 48, y + 21, z + 51)).isEmpty() && aoeCheck;
        }
        return false;
    };

    public static Function<TileEntityLogicBlock, Boolean> PLAYER_CUBE_3 = (tile) -> playerIn(tile.getLevel(), cube(tile.getBlockPos(), 3));
    public static Function<TileEntityLogicBlock, Boolean> PLAYER_CUBE_5 = (tile) -> playerIn(tile.getLevel(), cube(tile.getBlockPos(), 5));
    public static Function<TileEntityLogicBlock, Boolean> PLAYER_CUBE_25 = (tile) -> playerIn(tile.getLevel(), cube(tile.getBlockPos(), 25));

    public static Function<TileEntityLogicBlock, Boolean> REDSTONE = (tile) -> tile.getLevel().hasNeighborSignal(tile.getBlockPos());

    public static Function<TileEntityLogicBlock, Boolean> PUZZLE_TEST = (tile) -> {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();

        if (tile.phase == 0 && world.hasNeighborSignal(pos)) {
            Player closest = world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 25, false);
            if (closest != null) closest.sendSystemMessage(Component.literal("Find a ")
                    .append(Component.literal("great").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" ancient weapon, of questionable use in the modern age")));
            world.setBlockAndUpdate(pos.above(), ModDungeonBlocks.pedestal.get().defaultBlockState());
            return true;
        }

        BlockEntity pedestal = world.getBlockEntity(pos.above());
        return tile.phase == 1
                && pedestal instanceof BlockPedestal.TileEntityPedestal p
                && !p.getItem().isEmpty()
                && p.getItem().is(ModMeleeKitItems.big_sword.get());
    };

    public static Function<TileEntityLogicBlock, Boolean> BOMB_CRANE = (tile) -> {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();

        if (tile.phase == 0) {
            world.setBlock(pos.above(), ModExplosivesBlocks.charge_c4.get().defaultBlockState()
                    .setValue(BlockChargeBase.FACING, Direction.UP), 3);
            if (world.getBlockEntity(pos.above()) instanceof TileEntityCharge bomb) bomb.timer = 200;
        }

        return playerIn(world, cube(pos, 10));
    };

    public static List<String> getConditionNames() {
        return new ArrayList<>(conditions.keySet());
    }

    // register new conditions here
    static {
        initialize();
    }

    public static void initialize() {
        conditions = new LinkedHashMap<>();

        conditions.put("EMPTY", EMPTY);
        conditions.put("PLAYER_CUBE_3", PLAYER_CUBE_3);
        conditions.put("PLAYER_CUBE_5", PLAYER_CUBE_5);
        conditions.put("PLAYER_CUBE_25", PLAYER_CUBE_25);

        conditions.put("BOMB_CRANE", BOMB_CRANE);

        // example conditions
        conditions.put("ABERRATOR", ABERRATOR);
        conditions.put("REDSTONE", REDSTONE);
        conditions.put("PUZZLE_TEST", PUZZLE_TEST);
    }
}
