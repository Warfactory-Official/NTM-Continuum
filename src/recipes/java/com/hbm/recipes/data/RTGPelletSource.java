package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.RTGPellet;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code ModItems}: {@code new ItemRTGPellet(heat).setDecays(material, (long) (RTGUtil.getLifespan(halfLife, type,
 * false) * 1.5))}. The gold and lead pellets' {@code VersatileConfig.rtgDecay() ? a : b} heat is both values.
 */
public final class RTGPelletSource extends DataMapSource<RTGPellet> {

    private enum HalfLifeType { LONG, MEDIUM, SHORT }

    public RTGPelletSource() {
        super(ReactorDataMaps.RTG_PELLET, false);
    }

    /** 1.7 {@code RTGUtil.getLifespan}. */
    private static long getLifespan(float halfLife, HalfLifeType type, boolean realYears) {
        float life = 0;
        switch (type) {
            case LONG -> life = (48000 * (realYears ? 365 : 100) * 100) * halfLife;
            case MEDIUM -> life = (48000 * (realYears ? 365 : 100)) * halfLife;
            case SHORT -> life = 48000 * halfLife;
        }
        return (long) life;
    }

    private void pellet(String id, int heat, Integer heatDecaying, String material, float halfLife, HalfLifeType type) {
        pellet(id, heat, heatDecaying, material, (long) (getLifespan(halfLife, type, false) * 1.5));
    }

    private void pellet(String id, int heat, Integer heatDecaying, String material, long lifespan) {
        put("hbm:" + id, new RTGPellet(heat, Optional.ofNullable(heatDecaying), lifespan, "hbm:pellet_rtg_depleted_" + material));
    }

    @Override
    protected void values() {
        pellet("pellet_rtg_radium", 3, null, "lead", 16.0F, HalfLifeType.LONG);
        pellet("pellet_rtg_weak", 5, null, "lead", 1.0F, HalfLifeType.LONG);
        pellet("pellet_rtg", 10, null, "lead", 87.7F, HalfLifeType.MEDIUM);
        pellet("pellet_rtg_strontium", 15, null, "zirconium", 29.0F, HalfLifeType.MEDIUM);
        pellet("pellet_rtg_cobalt", 15, null, "nickel", 5.3F, HalfLifeType.MEDIUM);
        pellet("pellet_rtg_actinium", 20, null, "lead", 21.8F, HalfLifeType.MEDIUM);
        pellet("pellet_rtg_americium", 20, null, "neptunium", 4.7F, HalfLifeType.LONG);
        pellet("pellet_rtg_polonium", 50, null, "lead", 138.0F, HalfLifeType.SHORT);
        pellet("pellet_rtg_gold", 100, 200, "mercury", 2.7F, HalfLifeType.SHORT);
        pellet("pellet_rtg_lead", 200, 600, "bismuth", 0.3F, HalfLifeType.SHORT);
        // port: pellet_rtg_balefire is not a 1.7 item; port-only content, kept (user rule)
        pellet("pellet_rtg_balefire", 6000, null, "neptunium", 12000L);
    }
}
