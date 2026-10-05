package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Teleports;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code TileEntityMachineTeleporter}: takes power from all six sides and, once it has an exit, sends every
 * entity standing in the middle of its top ({@code x+0.25..x+0.75}, two blocks tall) to that exit, one million
 * HE per entity, landing them with their feet 1.5 blocks above the exit block. The client shows blue dust
 * above a charged, linked teleporter.
 *
 * <p>1.7's hand-rolled cross-dimension transfer (copied from {@code ServerConfigurationManager}) and its
 * reflective tracker packet are what vanilla's own teleport does now; the move goes through
 * {@link Teleports}, and a same-level move of a non-player is pushed to trackers with {@link Compat#syncPosition}.
 */
public class TileEntityMachineTeleporter extends TileEntityLoadedBase implements IEnergyReceiverMK2, com.hbm.tileentity.IRelocatable {

    /** A sub-level move carried this teleporter; its destination follows only when the block there was carried too. */
    @Override
    public void relocate(dev.golem.core.world.SubLevels.Move move) {
        if (targetY == -1) return;
        net.minecraft.core.BlockPos target = com.hbm.tileentity.IRelocatable.follow(move, new net.minecraft.core.BlockPos(targetX, targetY, targetZ));
        targetX = target.getX(); targetY = target.getY(); targetZ = target.getZ();
        setChanged();
    }


    public long power = 0;
    public int targetX = -1;
    public int targetY = -1;
    public int targetZ = -1;
    public String targetDim = "minecraft:overworld";
    public static final int maxPower = 1_500_000;
    public static final int consumption = 1_000_000;

    public TileEntityMachineTeleporter(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.TELEPORTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTeleporter tele) {
        if (tele.targetY != -1) {
            List<Entity> entities = level.getEntities((Entity) null, new AABB(pos.getX() + 0.25, pos.getY(),
                    pos.getZ() + 0.25, pos.getX() + 0.75, pos.getY() + 2, pos.getZ() + 0.75));
            for (Entity e : entities) tele.teleport((ServerLevel) level, e);
        }
        tele.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTeleporter tele) {
        if (tele.targetY != -1 && tele.power >= consumption) {
            double x = pos.getX() + 0.5 + level.getRandom().nextGaussian() * 0.25D;
            double y = pos.getY() + 1 + level.getRandom().nextDouble() * 2D;
            double z = pos.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.25D;
            // 1.7 "reddust" with the velocity arguments read as the colour (0.4, 0.8, 1.0)
            level.addParticle(Compat.dust(0x66CCFF, 1.0F), x, y, z, 0, 0, 0);
        }
    }

    public void teleport(ServerLevel level, Entity entity) {

        if (this.power < consumption) return;

        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

        double x = this.targetX + 0.5D;
        double y = this.targetY + 1.5D;
        double z = this.targetZ + 0.5D;
        String here = dev.golem.core.Ids.keyId(level.dimension()).toString();

        if (here.equals(this.targetDim)) {
            if (entity instanceof ServerPlayer player) {
                player.teleportTo(x, y, z);
            } else {
                entity.teleportTo(x, y, z);
                Compat.syncPosition(entity);
            }
        } else {
            ServerLevel target = level.getServer().getLevel(
                    ResourceKey.create(Registries.DIMENSION, dev.golem.core.Ids.parse(this.targetDim)));
            if (target != null) Teleports.toLevel(entity, target, x, y, z);
        }

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 1.0F, 1.0F);

        this.power -= consumption;
        this.setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(power);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeUtf(targetDim);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.power = buf.readLong();
        this.targetX = buf.readInt();
        this.targetY = buf.readInt();
        this.targetZ = buf.readInt();
        this.targetDim = buf.readUtf();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        targetX = input.getIntOr("x1", -1);
        targetY = input.getIntOr("y1", -1);
        targetZ = input.getIntOr("z1", -1);
        targetDim = input.getStringOr("dim", "minecraft:overworld");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("x1", targetX);
        output.putInt("y1", targetY);
        output.putInt("z1", targetZ);
        output.putString("dim", targetDim);
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
}
