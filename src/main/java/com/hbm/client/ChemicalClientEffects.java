package com.hbm.client;

import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.render.Particles;
import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;

/** Client-only particle half of the original chemical projectile tick. */
public final class ChemicalClientEffects {

    private ChemicalClientEffects() {}

    public static void tick(EntityChemical chemical) {
        if (!(chemical.level() instanceof ClientLevel level)) return;

        EntityChemical.ChemicalStyle style = chemical.style();
        if (style == EntityChemical.ChemicalStyle.LIQUID) {
            FluidType type = chemical.getFluid();
            Color color = new Color(type.clientMetadata().getColor());
            Particles.dust(level, chemical.getX(), chemical.getY(), chemical.getZ(),
                    color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 1F,
                    chemical.getDeltaMovement().x + level.getRandom().nextGaussian() * 0.05D,
                    chemical.getDeltaMovement().y - 0.2D + level.getRandom().nextGaussian() * 0.05D,
                    chemical.getDeltaMovement().z + level.getRandom().nextGaussian() * 0.05D);
        } else if (style == EntityChemical.ChemicalStyle.BURNING) {
            double motion = Math.min(chemical.getDeltaMovement().length(), 0.1D);
            for (double d = 0D; d < motion; d += 0.0625D) {
                level.addParticle(ParticleTypes.FLAME,
                        (chemical.xo - chemical.getX()) * d + chemical.getX(),
                        (chemical.yo - chemical.getY()) * d + chemical.getY(),
                        (chemical.zo - chemical.getZ()) * d + chemical.getZ(), 0D, 0D, 0D);
            }
        }
    }
}
