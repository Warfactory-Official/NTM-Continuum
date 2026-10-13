package com.hbm.hazard.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.Codecs;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Named hazard modifiers, so the hazards data map ({@code hbm:hazards}) can state them as
 * {@code {"type": "hbm:rbmk_radiation", "target": 7500.0, "linear": false}}. An addon registers its own modifier record
 * here (at mod construction, before any datapack loads) and uses its id in its JSON; a modifier that is only a lambda
 * still works through code, it just cannot be written in a file.
 */
public final class HazardModifiers {

    private HazardModifiers() {}

    private static final Map<String, MapCodec<? extends IHazardModifier>> CODECS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> IDS = new ConcurrentHashMap<>();

    /** Registers {@code type} under {@code id} ({@code <namespace>:<path>}). */
    public static <M extends IHazardModifier> void register(String id, Class<M> type, MapCodec<M> codec) {
        if (CODECS.putIfAbsent(id, codec) != null) throw new IllegalStateException("Hazard modifier " + id + " registered twice");
        IDS.put(type, id);
    }

    public static final Codec<IHazardModifier> CODEC = Codecs.<String, IHazardModifier>partialDispatch(Codec.STRING, "type",
            modifier -> {
                String id = IDS.get(modifier.getClass());
                return id != null ? DataResult.success(id) : DataResult.error(() -> "Unnamed hazard modifier " + modifier.getClass());
            },
            id -> {
                MapCodec<? extends IHazardModifier> codec = CODECS.get(id);
                if (codec == null) return DataResult.<MapCodec<? extends IHazardModifier>>error(() -> "Unknown hazard modifier " + id);
                return DataResult.<MapCodec<? extends IHazardModifier>>success(codec);
            });

    static {
        register("hbm:rbmk_radiation", HazardModifierRBMKRadiation.class, RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("target").forGetter(HazardModifierRBMKRadiation::target),
                Codec.BOOL.optionalFieldOf("linear", false).forGetter(HazardModifierRBMKRadiation::linear)
        ).apply(i, HazardModifierRBMKRadiation::new)));
        register("hbm:rbmk_hot", HazardModifierRBMKHot.class, MapCodec.unit(HazardModifierRBMKHot::new));
        register("hbm:fuel_radiation", HazardModifierFuelRadiation.class, RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("target").forGetter(HazardModifierFuelRadiation::target)
        ).apply(i, HazardModifierFuelRadiation::new)));
        register("hbm:rtg_radiation", HazardModifierRTGRadiation.class, RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("target").forGetter(HazardModifierRTGRadiation::target)
        ).apply(i, HazardModifierRTGRadiation::new)));
        register("hbm:hot_scale", HazardModifierHotScale.class, RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.DOUBLE.fieldOf("cold_factor").forGetter(HazardModifierHotScale::coldFactor)
        ).apply(i, HazardModifierHotScale::new)));
    }
}
