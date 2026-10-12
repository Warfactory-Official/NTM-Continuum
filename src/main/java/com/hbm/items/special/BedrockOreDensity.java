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

    /**
     * The amount of one of HBM's six types; 0 for any other type (an addon type keeps its amounts in its own item
     * component, as the six are the overworld noise's channels and the stack's saved shape).
     */
    public double get(BedrockOreType type) {
        if (type == BedrockOreType.LIGHT_METAL) return light;
        if (type == BedrockOreType.HEAVY_METAL) return heavy;
        if (type == BedrockOreType.RARE_EARTH) return rare;
        if (type == BedrockOreType.ACTINIDE) return actinide;
        if (type == BedrockOreType.NON_METAL) return nonmetal;
        if (type == BedrockOreType.CRYSTALLINE) return crystal;
        return 0D;
    }
}
