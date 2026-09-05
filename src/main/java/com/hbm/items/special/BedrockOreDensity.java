package com.hbm.items.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** The six noise-derived ore amounts stored on one raw bedrock-ore stack. */
public record BedrockOreDensity(double light, double heavy, double rare, double actinide,
                                double nonmetal, double crystal) {

    public static final BedrockOreDensity ZERO = new BedrockOreDensity(0, 0, 0, 0, 0, 0);

    public static final Codec<BedrockOreDensity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("light").forGetter(BedrockOreDensity::light),
            Codec.DOUBLE.fieldOf("heavy").forGetter(BedrockOreDensity::heavy),
            Codec.DOUBLE.fieldOf("rare").forGetter(BedrockOreDensity::rare),
            Codec.DOUBLE.fieldOf("actinide").forGetter(BedrockOreDensity::actinide),
            Codec.DOUBLE.fieldOf("nonmetal").forGetter(BedrockOreDensity::nonmetal),
            Codec.DOUBLE.fieldOf("crystal").forGetter(BedrockOreDensity::crystal)
    ).apply(instance, BedrockOreDensity::new));

    public double get(BedrockOreType type) {
        return switch (type) {
            case LIGHT_METAL -> light;
            case HEAVY_METAL -> heavy;
            case RARE_EARTH -> rare;
            case ACTINIDE -> actinide;
            case NON_METAL -> nonmetal;
            case CRYSTALLINE -> crystal;
        };
    }
}
