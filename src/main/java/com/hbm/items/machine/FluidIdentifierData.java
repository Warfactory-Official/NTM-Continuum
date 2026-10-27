package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The identifier's two picked fluids, as the {@code fluid_identifier} component (1.7's fluid1/fluid2 pair). */
public record FluidIdentifierData(FluidType primary, FluidType secondary) {

    public static final Codec<FluidIdentifierData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Fluids.EXACT_CODEC.fieldOf("primary").forGetter(FluidIdentifierData::primary),
            Fluids.EXACT_CODEC.fieldOf("secondary").forGetter(FluidIdentifierData::secondary)
    ).apply(instance, FluidIdentifierData::new));

    public FluidIdentifierData withPrimary(FluidType primary) {
        return new FluidIdentifierData(primary, this.secondary);
    }

    public FluidIdentifierData withSecondary(FluidType secondary) {
        return new FluidIdentifierData(this.primary, secondary);
    }
}
