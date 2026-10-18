package com.hbm.entity.item;

import dev.golem.core.data.CompatEntity;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.api.conveyor.ConveyorLookup;
import com.hbm.config.ServerConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code entity/item/EntityMovingConveyorObject}. 1.7's hand-rolled {@code turnProgress} client lerp is
 * dropped for {@link EntityInterpolation}, which is that mechanism and already runs at the finer granularity
 * the 1.7 comment asked for.
 */
public abstract class EntityMovingConveyorObject extends CompatEntity {


    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    protected EntityMovingConveyorObject(EntityType<? extends EntityMovingConveyorObject> type, Level level) {
        super(type, level);
        this.noPhysics = true; // 1.7 noClip
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) discard();
        return false;
    }

    // 1.7 EntityMovingItem:60-65 / EntityMovingPackage: any damage routes to hitByEntity, so explosions and
    // arrows take the object off the belt, not only a melee hit
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        skipAttackInteraction(source.getEntity());
        return true;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;

        if (this.tickCount <= 5) return;

        if ((this.tickCount + getId()) % 400 == 0) cramCheck(server);

        BlockPos pos = blockPosition();
        Vec3 self = position();
        // a folded machine acts as a belt on a cell its core owns, so the block here can be that cell
        IConveyorBelt belt = ConveyorLookup.at(IConveyorBelt.class, server, pos);

        if (belt == null || !belt.canItemStay(server, pos, self)) {
            if (onLeaveConveyor()) return;
        } else {
            setDeltaMovement(belt.getTravelLocation(server, pos, self, getMoveSpeed()).subtract(self));
        }

        BlockPos lastPos = blockPosition();
        move(MoverType.SELF, getDeltaMovement());
        BlockPos newPos = blockPosition();

        if (lastPos.equals(newPos)) return;

        IEnterableBlock entered = ConveyorLookup.at(IEnterableBlock.class, server, newPos);

        if (entered != null) {
            enterBlock(entered, newPos, enterDirection(lastPos, newPos));
        } else if (!server.getBlockState(newPos).isSolid()) {
            IEnterableBlock below = ConveyorLookup.at(IEnterableBlock.class, server, newPos.below());
            if (below != null) enterBlockFalling(below, newPos);
        }
    }

    /**
     * 1.7 derived the entry side from the sign of the block-position delta, one axis at a time, and left it
     * {@code UNKNOWN} for diagonals; {@code null} is that case.
     */
    private static @Nullable Direction enterDirection(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());

        if (dx != 0 && dy == 0 && dz == 0) return dx < 0 ? Direction.EAST : Direction.WEST;
        if (dy != 0 && dx == 0 && dz == 0) return dy < 0 ? Direction.UP : Direction.DOWN;
        if (dz != 0 && dx == 0 && dy == 0) return dz < 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    /** 1.7 anti-cram: a jam of {@code CONVEYOR_CRAM_MAX} pops, and takes the belt under it with it. */
    public void cramCheck(ServerLevel server) {
        List<EntityMovingConveyorObject> objs = server.getEntitiesOfClass(EntityMovingConveyorObject.class,
                getBoundingBox().inflate(0.125D));

        if (objs.size() < ServerConfig.CONVEYOR_CRAM_MAX.get()) return;

        for (EntityMovingConveyorObject obj : objs) obj.discard();

        ExplosionVNT vnt = new ExplosionVNT(server, getX(), getY() + 0.125, getZ(), 1, this);
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();

        BlockPos pos = blockPosition();
        // a jam younger than one check cycle piled up while its chunk loaded and spares the belt
        if (server.getBlockState(pos).getBlock() instanceof IConveyorBelt && this.tickCount > 400
                && ServerConfig.CONVEYOR_CRAM_EXPLODE.get()) {
            server.destroyBlock(pos, false);
        }
    }

    public abstract void enterBlock(IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir);

    public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.below(), Direction.UP);
    }

    /** @return true if the update loop should end */
    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }
}
