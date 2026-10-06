package com.hbm.blocks.generic;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.world.gen.util.LogicBlockActions;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Spawns;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 1.7 {@code DungeonSpawner}: the unbreakable heart of a dungeon encounter. Its block entity runs one
 * {@link EnumSpawnerType} as a small phase machine: each server tick the phase's action runs, then the phase
 * condition is asked whether to advance (the timer counts ticks since the last advance).
 *
 * <p>The only type, {@link EnumSpawnerType#ABERRATOR}: a player within 20 blocks (checked once a second) starts
 * it; phases 1 and 2 each raise a ring of ten undead soldiers five blocks below, ten blocks out, and advance once
 * all soldiers within 50 blocks are dead (after at least three seconds); in phase 3 the skeleton holder 18 blocks
 * up is given its reward - one time in five an Aberrator part, otherwise the second clay tablet - and the spawner
 * turns to obsidian. Nothing happens on peaceful.
 *
 * <p>1.7's detection boxes were built with their maximum Y below their minimum and then expanded; the port
 * spells the resulting boxes out rather than let {@link AABB} normalise the corners first, which would move them.
 */
public class DungeonSpawner extends CompatBlock implements TickingBlock {

    public DungeonSpawner(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDungeonSpawner(pos, state);
    }

    public static class TileEntityDungeonSpawner extends CompatBlockEntity {

        public int phase = 0;
        public int timer = 0;
        public EnumSpawnerType type = EnumSpawnerType.ABERRATOR;

        public TileEntityDungeonSpawner(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.DUNGEON_SPAWNER.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDungeonSpawner tile) {
            tile.type.phase.accept(tile);
            if (tile.type.phaseCondition.apply(tile)) {
                tile.phase++;
                tile.timer = 0;
            } else {
                tile.timer++;
            }
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putInt("phase", phase);
            com.hbm.util.SavedIds.putEnum(out, "type", type); // by name, never an ordinal (History 19ej-ids)
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            this.phase = in.getIntOr("phase", 0);
            this.type = com.hbm.util.SavedIds.getEnum(in, "type", EnumSpawnerType.values()[0]);
        }
    }

    public enum EnumSpawnerType {
        ABERRATOR(DungeonSpawner::conAberrator, DungeonSpawner::phaseAberrator);

        public final Function<TileEntityDungeonSpawner, Boolean> phaseCondition;
        public final Consumer<TileEntityDungeonSpawner> phase;

        EnumSpawnerType(Function<TileEntityDungeonSpawner, Boolean> con, Consumer<TileEntityDungeonSpawner> ph) {
            this.phaseCondition = con;
            this.phase = ph;
        }
    }

    private static boolean conAberrator(TileEntityDungeonSpawner tile) {
        Level world = tile.getLevel();
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        int x = tile.getBlockPos().getX(), y = tile.getBlockPos().getY(), z = tile.getBlockPos().getZ();
        if (tile.phase == 0) {
            if (world.getGameTime() % 20 != 0) return false;
            // 1.7 (x, y, z, x + 1, y - 2, z + 1).expand(20, 10, 20)
            return !world.getEntitiesOfClass(Player.class, new AABB(x - 20, y - 10, z - 20, x + 21, y + 8, z + 21)).isEmpty();
        }
        if (tile.phase < 3) {
            if (world.getGameTime() % 20 != 0 || tile.timer < 60) return false;
            // 1.7 (x, y, z, x - 2, y + 1, z + 1).expand(50, 20, 50)
            return world.getEntitiesOfClass(EntityUndeadSoldier.class,
                    new AABB(x - 50, y - 20, z - 50, x + 48, y + 21, z + 51)).isEmpty();
        }
        return false;
    }

    private static void phaseAberrator(TileEntityDungeonSpawner tile) {
        ServerLevel world = (ServerLevel) tile.getLevel();
        int x = tile.getBlockPos().getX(), y = tile.getBlockPos().getY(), z = tile.getBlockPos().getZ();
        if (tile.phase == 1 || tile.phase == 2) {
            if (tile.timer == 0) {
                double vx = 10, vz = 0;
                for (int i = 0; i < 10; i++) {
                    EntityUndeadSoldier mob = Entities.createTriggered(ModNtmEntities.UNDEAD_SOLDIER.get(), world);
                    // 1.7 tried seven times, each at the same spot
                    for (int j = 0; j < 7 && mob != null; j++) {
                        mob.snapTo(x + 0.5 + vx, y - 5, z + 0.5 + vz, i * 36F, 0);
                        if (mob.canSpawnAtCurrentPosition()) {
                            Spawns.finalizeTriggered(mob, world);
                            world.addFreshEntity(mob);
                            break;
                        }
                    }
                    double[] turned = LogicBlockActions.rotateAroundYDeg(vx, vz, 36D);
                    vx = turned[0];
                    vz = turned[1];
                }
            }
        }
        if (tile.phase > 2) {
            BlockEntity te = world.getBlockEntity(tile.getBlockPos().above(18));
            if (te instanceof BlockSkeletonHolder.TileEntitySkeletonHolder skeleton) {
                if (world.getRandom().nextInt(5) == 0) {
                    skeleton.setItem(LogicBlockActions.stack("hbm:item_secret_aberrator", 1));
                } else {
                    skeleton.setItem(com.hbm.items.special.ItemClayTablet.of(1));
                }
            }
            world.setBlockAndUpdate(tile.getBlockPos(), Blocks.OBSIDIAN.defaultBlockState());
        }
    }
}
