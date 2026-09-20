package com.hbm.blocks.generic;

import com.hbm.lib.ModDamageSources;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.client.render.SecurityObjRenderer;
import com.hbm.sound.ModSecuritySounds;
import com.hbm.tileentity.TileEntitySecurityObj;

/**
 * Spikes, port of 1.7.10's {@code Spikes}: no collision box; a living entity falling into them
 * faster than -0.1 blocks/tick takes 100 armour-bypassing "impaled" damage, with the slicer sound
 * when the hit lands. Invisible block + {@code SecurityObjRenderer} drawing {@code spikes.obj}.
 */
public class Spikes extends CompatBlock implements EntityBlock {

    public Spikes(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel server)) return;

        if (entity instanceof LivingEntity && entity.getDeltaMovement().y < -0.1) {
            if (Compat.hurt(entity, server, ModDamageSources.source(level, ModDamageSources.SPIKES), 100))
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSecuritySounds.SLICER.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySecurityObj(pos, state);
    }
}
