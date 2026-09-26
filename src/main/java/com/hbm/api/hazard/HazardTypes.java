package com.hbm.api.hazard;

import com.hbm.hazard.HazardRegistry;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Built-in hazard types and the named-type registry. */
public final class HazardTypes {

    private HazardTypes() {}

    public static final IHazardType RADIATION = HazardRegistry.RADIATION;
    public static final IHazardType CONTAMINATING = HazardRegistry.CONTAMINATING;
    public static final IHazardType DIGAMMA = HazardRegistry.DIGAMMA;
    public static final IHazardType HOT = HazardRegistry.HOT;
    public static final IHazardType BLINDING = HazardRegistry.BLINDING;
    public static final IHazardType ASBESTOS = HazardRegistry.ASBESTOS;
    public static final IHazardType COAL = HazardRegistry.COAL;
    public static final IHazardType HYDROACTIVE = HazardRegistry.HYDROACTIVE;
    public static final IHazardType EXPLOSIVE = HazardRegistry.EXPLOSIVE;
    public static final IHazardType TOXIC = HazardRegistry.TOXIC;
    public static final IHazardType COLD = HazardRegistry.COLD;

    /** Names {@code type}; returns it, for the constant holding it. */
    public static <T extends IHazardType> T register(Identifier id, T type) {
        return HazardRegistry.registerType(id, type);
    }

    public static @Nullable IHazardType get(Identifier id) {
        return HazardRegistry.getType(id);
    }

    public static Map<Identifier, IHazardType> all() {
        return HazardRegistry.types();
    }
}
