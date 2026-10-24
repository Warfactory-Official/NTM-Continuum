package com.hbm.inventory.fluid;

import com.hbm.api.data.FluidDataMaps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A detached, resolved metadata view; it never owns a tank's exact resource identity. */
public final class FluidMetadata {
    private final FluidProperties.Properties properties;
    private final Map<Class<?>, Object> traits;
    private final FluidDataMaps.@org.jspecify.annotations.Nullable Containers containers;
    private final FluidType naming;
    private final boolean defined;

    FluidMetadata(FluidProperties.Properties properties, FluidDataMaps.@org.jspecify.annotations.Nullable Containers containers,
                  FluidType naming, boolean defined) {
        this.properties = properties;
        var traits = new HashMap<Class<?>, Object>();
        for (var trait : properties.traits()) traits.put(trait.getClass(), trait);
        this.traits = Map.copyOf(traits);
        this.containers = containers;
        this.naming = naming;
        this.defined = defined;
    }

    public int getColor() { return properties.color(); }
    public int getTint() { return properties.tint() != null ? properties.tint() : getColor(); }
    public int getTemperature() { return properties.temperature(); }
    public FluidHazard getHazard() { return properties.hazard(); }
    public boolean hasTrait(Class<?> type) { return traits.containsKey(type); }
    public <T> T getTrait(Class<T> type) { return type.cast(traits.get(type)); }
    public FluidProperties.Properties properties() { return properties; }
    public FluidDataMaps.@org.jspecify.annotations.Nullable Containers containers() { return containers; }
    public FluidType naming() { return naming; }
    public net.minecraft.network.chat.Component getDisplayName() { return naming.ownDisplayName(); }
    public net.minecraft.resources.Identifier getIconSprite() { return naming.ownIconSprite(); }
    public net.minecraft.resources.Identifier getTexture() { return naming.ownTexture(); }
    public boolean hasProperties() { return defined; }

    public void addInfo(List<net.minecraft.network.chat.Component> info, boolean shiftHeld) {
        FluidType.addInfo(info, shiftHeld, this);
    }
}
