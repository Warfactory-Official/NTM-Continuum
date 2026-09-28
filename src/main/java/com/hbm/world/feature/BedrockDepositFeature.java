package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:bedrock_ore} / {@code hbm:bedrock_ore_nether}: 1.7's bedrock-ore deposit for the chunk
 * ({@code BedrockOre.generateAuto} in the overworld, the weighted {@code weightedOresNether} pick in the nether).
 * 1.7's {@code rand.nextInt(10) == 0} gate is the placed feature's {@code rarity_filter}; the deposit then picks its
 * own column {@code chunk + 8 + rand(2)} exactly as 1.7 did, so the placement adds no {@code in_square}.
 */
public class BedrockDepositFeature extends Feature<NoneFeatureConfiguration> {

    private final boolean nether;

    public BedrockDepositFeature(Codec<NoneFeatureConfiguration> codec, boolean nether) {
        super(codec);
        this.nether = nether;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int cx = context.origin().getX() & ~15, cz = context.origin().getZ() & ~15;
        if (nether) BedrockOreFeature.generateNetherChunk(context.level(), context.random(), cx, cz);
        else BedrockOreFeature.generateAutoChunk(context.level(), context.random(), cx, cz);
        return true;
    }
}
