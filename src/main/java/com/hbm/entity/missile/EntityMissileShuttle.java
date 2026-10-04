package com.hbm.entity.missile;

import com.hbm.entity.ModNtmEntities;
import com.hbm.items.ModMissileItems;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

// 1.7 EntityMissileShuttle - heavy conventional missile; impact blasts silently, then paints
// the rbmkmush cloud and plays the robin burst itself.
public class EntityMissileShuttle extends EntityMissileBaseNT {

    public EntityMissileShuttle(EntityType<? extends EntityMissileShuttle> type, Level level) {
        super(type, level);
    }

    public EntityMissileShuttle(Level level, float x, float y, float z, int targetX, int targetZ) {
        super(ModNtmEntities.MISSILE_SHUTTLE.get(), level, x, y, z, targetX, targetZ);
    }

    @Override
    public void onMissileImpact(HitResult mop) {
        // 1.7: ExplosionNT strength 20F / overrideResolution(64), ExAttrib.NOSOUND | NOPARTICLE -
        // vanilla FX suppressed, the robin burst below carries all feedback
        new ExplosionVNT(level(), getX() + 0.5F, getY() + 0.5F, getZ() + 0.5F, 20.0F)
                .setBlockAllocator(new BlockAllocatorStandard(64))
                .setBlockProcessor(new BlockProcessorStandard())
                .setEntityProcessor(new EntityProcessorStandard()) // 1.7 ExplosionNT: vanilla entity damage
                .setPlayerProcessor(new PlayerProcessorStandard())
                .explode(); // no setSFX: NOSOUND | NOPARTICLE

        // 1.7: AuxParticlePacketNT "rbmkmush" scale 10 at +1Y broadcast over 250 blocks
        // (+ MainRegistry.proxy.effectNT); port preset has a fixed scale
        ExplosionCreator.composeEffectRBMKMush(level(), getX() + 0.5D, getY() + 1D, getZ() + 0.5D, 10F);

        level().playSound(null, getX(), getY(), getZ(), ModSounds.ROBIN_EXPLOSION.get(),
                SoundSource.BLOCKS, 4.0F, (1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F) * 0.7F);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();

        // 1.7: plate_steel; explicit material-shape claims resolve through plateOf.
        list.add(new ItemStack(ModItems.plate_steel.get(), 8));
        list.add(new ItemStack(ModItems.thruster_medium.get(), 2));
        list.add(new ItemStack(ModItems.canister_empty.get())); // 1.7 canister_empty
        list.add(new ItemStack(Blocks.GLASS_PANE, 2));

        return list;
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return new ItemStack(ModMissileItems.missile_generic.get());
    }

    @Override
    public String getRadarName() {
        return "radar.target.shuttle";
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(ModMissileItems.missile_shuttle.get());
    }
}
