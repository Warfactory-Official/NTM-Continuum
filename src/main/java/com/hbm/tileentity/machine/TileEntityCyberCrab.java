package com.hbm.tileentity.machine;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.registry.Entities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code TileEntityCyberCrab}: the meteor nest, which restocks itself with cybercrabs.
 *
 * <p>Once the timer passes {@link #SPAWN_INTERVAL} the nest looks for headroom and a player inside
 * {@link #PLAYER_RANGE}, and adds a crab if fewer than {@link #POPULATION_CAP} are already nearby -
 * one in five of them the tesla variant. {@code EntityTeslaCrab} extends {@code EntityCyberCrab} on
 * both eras, so the census counts the base class and catches both.
 *
 * <p>The timer resets only once all three gates pass, not once the interval elapses: a nest with no
 * headroom or no player keeps counting, so it spawns on the tick the last gate opens rather than
 * waiting out another interval. Resetting it beside the interval check would be the natural
 * transcription and would make a nest idle for up to ten seconds after a player walks up.
 */
public class TileEntityCyberCrab extends CompatBlockEntity {

    /** 1.7 spawns no more often than once per 200 ticks. */
    private static final int SPAWN_INTERVAL = 200;
    /** How far a player may be and still wake the nest. */
    private static final double PLAYER_RANGE = 25D;
    /** Crabs already within the census box that stop another from being added. */
    private static final int POPULATION_CAP = 5;
    /** One spawn in this many is the tesla variant. */
    private static final int TESLA_CHANCE = 5;

    private int age = 0;

    public TileEntityCyberCrab(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METEOR_SPAWNER.get(), pos, state);
    }

    public void serverTick() {
        if (level == null) return;

        age++;
        if (age <= SPAWN_INTERVAL) return;
        if (!level.getBlockState(worldPosition.above()).is(Blocks.AIR)) return;

        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1D;
        double z = worldPosition.getZ() + 0.5D;
        if (level.getNearestPlayer(x, y, z, PLAYER_RANGE, false) == null) return;

        AABB census = new AABB(
                worldPosition.getX() - 5, worldPosition.getY() - 2, worldPosition.getZ() - 5,
                worldPosition.getX() + 6, worldPosition.getY() + 4, worldPosition.getZ() + 6);
        List<EntityCyberCrab> nearby = level.getEntitiesOfClass(EntityCyberCrab.class, census);

        if (nearby.size() < POPULATION_CAP) {
            EntityCyberCrab crab = level.getRandom().nextInt(TESLA_CHANCE) == 0
                    ? Entities.create(ModNtmEntities.TESLA_CRAB.get(), level, EntitySpawnReason.SPAWNER)
                    : Entities.create(ModNtmEntities.CYBER_CRAB.get(), level, EntitySpawnReason.SPAWNER);
            if (crab != null) {
                crab.snapTo(x, y, z, crab.getYRot(), crab.getXRot());
                level.addFreshEntity(crab);
            }
        }

        age = 0;
    }
}
