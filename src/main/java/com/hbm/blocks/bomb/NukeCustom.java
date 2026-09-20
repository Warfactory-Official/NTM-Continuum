package com.hbm.blocks.bomb;

import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;

/**
 * The Custom Nuke ({@code nuke_custom}). Its 27 slots are weighed every tick by
 * {@link TileEntityNukeCustom} into the seven stage levels; detonation hands those to
 * {@link ExplosionNukeCustom#explodeCustom} (the 1.7 {@code NukeCustom.explodeCustom}, moved so the falling
 * bomb entity can reach it). A {@code custom_fall} anywhere in the inventory turns detonation into a drop
 * instead: the block becomes an {@link EntityFallingNuke} that explodes on impact.
 */
public class NukeCustom extends NukeBomb2Block {

    public NukeCustom(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeBomb newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeCustom(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        return explode(level, pos, detonator, detonator == null ? null : detonator.getUUID());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator, @Nullable UUID detonatorId) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeCustom entity)) return BombReturnCode.UNDEFINED;

        if (!entity.isFalling()) {
            float tnt = entity.tnt, nuke = entity.nuke, hydro = entity.hydro, amat = entity.amat;
            float dirty = entity.dirty, schrab = entity.schrab, euph = entity.euph;
            entity.clearSlots();
            level.removeBlock(pos, false);
            // 1.7 passed the block centre here even though several stages add another half block inside.
            ExplosionNukeCustom.explodeCustom(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    tnt, nuke, hydro, amat, dirty, schrab, euph);
            return BombReturnCode.DETONATED;
        }

        EntityFallingNuke bomb = new EntityFallingNuke(level, entity.tnt, entity.nuke, entity.hydro, entity.amat,
                entity.dirty, entity.schrab, entity.euph);
        bomb.setNukeMeta(level.getBlockState(pos).getValue(NukeBombBlock.FACING));
        bomb.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.clearSlots();
        level.removeBlock(pos, false);
        level.addFreshEntity(bomb);
        return BombReturnCode.TRIGGERED;
    }
}
