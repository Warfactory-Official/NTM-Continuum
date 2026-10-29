package com.hbm.recipes.data;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.api.data.FluidDataMaps.Container;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code FluidContainerRegistry.register}: the fixed containers (full item, empty item, fluid, amount). The 1.7
 * fluid cells ({@code cell_deuterium}, ...) are one component cell in the port, and the per-fluid canisters, gas tanks,
 * tanks, barrels, disperser canisters and glands of its loop are component items too, so neither is listed here.
 */
public final class FluidContainerSource extends DataMapSource<Container> {

    public FluidContainerSource() {
        super(FluidDataMaps.FLUID_CONTAINER, false);
    }

    private void container(String full, String empty, String fluid, int amount) {
        put(full, new Container(fluid, amount, Optional.ofNullable(empty)));
    }

    @Override
    protected void values() {
        container("minecraft:water_bucket", "minecraft:bucket", "minecraft:water", 1000);
        // 1.7 new ItemStack(Items.potionitem) (meta 0, the water bottle), 250 mB of water: one item for every potion here,
        // so FluidContainerRegistry states it in code (potion contents minecraft:water)
        container("minecraft:lava_bucket", "minecraft:bucket", "minecraft:lava", 1000);
        container("hbm:bucket_mud", "minecraft:bucket", "hbm:watz", 1000);
        container("hbm:bucket_schrabidic_acid", "minecraft:bucket", "hbm:schrabidic", 1000);
        container("hbm:bucket_sulfuric_acid", "minecraft:bucket", "hbm:sulfuric_acid", 1000);

        // 1.7 new FluidContainer(new ItemStack(ModBlocks.red_barrel), new ItemStack(ModItems.tank_steel), Fluids.DIESEL, 10000)
        container("hbm:red_barrel", "hbm:tank_steel", "hbm:diesel", 10000);
        container("hbm:pink_barrel", "hbm:tank_steel", "hbm:kerosene", 10000);
        container("hbm:lox_barrel", "hbm:tank_steel", "hbm:oxygen", 10000);

        container("hbm:ore_oil", null, "hbm:oil", 250);
        // 1.7: GeneralConfig.enable528 ? 50 : 250
        put("hbm:ore_gneiss_gas", new Container("hbm:petroleum", 250, Optional.empty(), Optional.of(50)));

        container("hbm:bottle_mercury", "minecraft:glass_bottle", "hbm:mercury", 1000);
        // 1.7 ingot_mercury is the port's nugget_mercury
        container("hbm:nugget_mercury", null, "hbm:mercury", 125);

        container("hbm:rod_zirnox_tritium", "hbm:rod_zirnox_empty", "hbm:tritium", 2000);

        container("hbm:particle_hydrogen", "hbm:particle_empty", "hbm:hydrogen", 1000);
        container("hbm:particle_amat", "hbm:particle_empty", "hbm:amat", 1000);
        container("hbm:particle_aschrab", "hbm:particle_empty", "hbm:aschrab", 1000);

        container("hbm:iv_blood", "hbm:iv_empty", "hbm:blood", 100);
        container("hbm:iv_xp", "hbm:iv_xp_empty", "hbm:xpjuice", 100);
        // 1.7 new FluidContainer(new ItemStack(Items.experience_bottle), new ItemStack(Items.glass_bottle), Fluids.XPJUICE, 100)
        container("minecraft:experience_bottle", "minecraft:glass_bottle", "hbm:xpjuice", 100);

        // 1.7 new FluidContainer(new ItemStack(ModItems.can_mug), new ItemStack(ModItems.can_empty), Fluids.MUG, 100)
        container("hbm:can_mug", "hbm:can_empty", "hbm:mug", 100);
    }
}
