package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModArmorSetBBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1.7 {@code TileEntityDecoBlockAltF}, the statue's aura: every tick, any player within 4 blocks of the block's
 * corner (and with their eyes within 8) gets 5 ticks of instant health and saturation at level 100.
 *
 * <p>The distances are measured to the integer block coordinates, not the block centre, exactly as 1.7's
 * {@code getDistance(xCoord, yCoord, zCoord)} did. The {@code strength}/{@code wat} scaffolding in 1.7 was copied from
 * an explosion and does nothing but size the search box (9 blocks out); only the numbers that matter are kept.
 */
public class TileEntityDecoBlockAltF extends CompatBlockEntity {

    public TileEntityDecoBlockAltF(BlockPos pos, BlockState state) {
        super(ModArmorSetBBlockEntities.STATUE_ELB_F.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDecoBlockAltF te) {
        double wat = 4 * 2;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int i = Mth.floor(x - wat - 1.0D);
        int j = Mth.floor(x + wat + 1.0D);
        int k = Mth.floor(y - wat - 1.0D);
        int i2 = Mth.floor(y + wat + 1.0D);
        int l = Mth.floor(z - wat - 1.0D);
        int j2 = Mth.floor(z + wat + 1.0D);
        List<Entity> list = level.getEntities((Entity) null, new AABB(i, k, l, j, i2, j2));

        for (Entity entity : list) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / 4;
            if (d4 > 1.0D) continue;
            double d5 = entity.getX() - x;
            double d6 = entity.getY() + entity.getEyeHeight() - y;
            double d7 = entity.getZ() - z;
            double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
            if (d9 < wat && entity instanceof Player player) {
                player.addEffect(Effects.instance(Effects.instantHealth(), 5, 99));
                player.addEffect(Effects.instance(MobEffects.SATURATION, 5, 99));
            }
        }
    }
}
