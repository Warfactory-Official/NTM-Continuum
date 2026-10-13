package com.hbm.client.particle;

import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Something burned down to ash: a cloud of embers and ash flakes filling the volume the entity
 * occupied.
 *
 * <p>Ported from 1.12.2 {@code AshesCreator}.
 *
 * <p><b>Not ported: hiding the source entity.</b> Upstream calls {@code ClientProxy.vanish(entityID)}
 * so the corpse disappears the instant the ashes appear. That needs a client-side render suppression
 * list consulted by the entity renderer, which this port does not have. So the ashes play over a
 * still-visible entity until whatever killed it removes it. The id is still carried in the tag so the
 * behaviour drops in when that hook exists.
 */
public class AshesCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        Entity entity = level.getEntity(Tags.getIntOr(data, "entityID", -1));
        if (entity == null) return;
        if (entity instanceof LivingEntity living)
            DisintegrationParticles.vanish(living);

        int amount = Tags.getIntOr(data, "ashesCount", 0);
        float scale = Tags.getFloatOr(data, "ashesScale", 1F);

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();

        for (int i = 0; i < amount; i++) {
            double px = entity.getX() + (width + scale * 2) * (rand.nextDouble() - 0.5);
            double py = entity.getY() + height * rand.nextDouble();
            double pz = entity.getZ() + (width + scale * 2) * (rand.nextDouble() - 0.5);

            Particles.add(new ParticleAshes(level, px, py, pz, scale));

            // Upstream pairs each ash flake with a ParticleFlameNT, which was an empty subclass of
            // vanilla's flame that existed only to dodge 1.12's @SideOnly construction rules. The
            // modern flame particle's constructor is private, so the vanilla particle is requested
            // directly. Same particle, one indirection fewer.
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0, 0);
        }
    }
}
