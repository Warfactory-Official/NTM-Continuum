package com.hbm.tileentity.deco;

import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.tileentity.ModDecoBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1.7 {@code TileEntityLantern}: the glyphid deterrent on top of a lantern post.
 *
 * <p>Once a second it blinds every glyphid within 7.5 blocks of a point 5.5 above the core - the lamp
 * itself, not the base, which is why the sphere is centred there rather than on the block entity.
 * Holds no state; the only reason it exists is the sweep and the tall render box.
 */
public class TileEntityLantern extends CompatBlockEntity {

    /** 1.7 sweeps once per 20 ticks off the world clock. */
    private static final int SWEEP_INTERVAL = 20;
    private static final double SWEEP_RADIUS = 7.5D;
    /** Height of the lamp above the core cell; the post is five blocks tall. */
    private static final double LAMP_HEIGHT = 5.5D;
    private static final int BLINDNESS_TICKS = 100;

    public TileEntityLantern(BlockPos pos, BlockState state) {
        super(ModDecoBlockEntities.LANTERN.get(), pos, state);
    }

    public void serverTick() {
        if (level == null || level.getGameTime() % SWEEP_INTERVAL != 0L) return;

        AABB sphere = new AABB(worldPosition.getX() + 0.5D, worldPosition.getY() + LAMP_HEIGHT, worldPosition.getZ() + 0.5D,
                worldPosition.getX() + 0.5D, worldPosition.getY() + LAMP_HEIGHT, worldPosition.getZ() + 0.5D)
                .inflate(SWEEP_RADIUS);

        List<EntityGlyphid> glyphids = level.getEntitiesOfClass(EntityGlyphid.class, sphere);
        for (EntityGlyphid glyphid : glyphids) {
            glyphid.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, 0));
        }
    }
}
