package com.hbm.delegacy.legacy;

import com.hbm.config.StructureConfig;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/** DE's {@code hbm:config_scaled} rolls: 1.7's structure loot count, at least one, scaled by {@code lootAmountFactor}. */
public record ScaledRolls(NumberProvider value) implements NumberProvider {
    public static final MapCodec<ScaledRolls> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            NumberProviders.CODEC.fieldOf("value").forGetter(ScaledRolls::value)).apply(i, ScaledRolls::new));
    public static final LootNumberProviderType TYPE = new LootNumberProviderType(CODEC);

    @Override
    public LootNumberProviderType getType() {
        return TYPE;
    }

    @Override
    public float getFloat(LootContext context) {
        return getInt(context);
    }

    @Override
    public int getInt(LootContext context) {
        return Math.max(1, (int) Math.floor(value.getInt(context) * StructureConfig.lootAmountFactor));
    }
}
