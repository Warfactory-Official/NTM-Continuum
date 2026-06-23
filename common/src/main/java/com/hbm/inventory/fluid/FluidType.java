package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public class FluidType {

    private int id;

    private final String stringId;

    private final int color;

    private final String unlocalized;

    private int guiTint;
    private final Identifier texture;

    public static final int ROOM_TEMPERATURE = 20;

    public int temperature = ROOM_TEMPERATURE;

    public FluidType(String name, int color) {
        this.stringId = name;
        this.color = color;
        this.guiTint = color;
        this.unlocalized = "hbmfluid." + name.toLowerCase(Locale.US);
        this.texture = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/fluids/" + name.toLowerCase(Locale.US) + ".png");
        this.id = Fluids.registerSelf(this);
    }

    public FluidType(int forcedId, String name, int color) {
        this(name, color);
        if (this.id != forcedId) {
            throw new IllegalStateException("Fluid ID mismatch for '" + name + "': expected " + forcedId + " but got " + this.id);
        }
    }

    public FluidType setTemp(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public FluidType setTint(int tint) {
        this.guiTint = tint;
        return this;
    }

    public int getID() { return this.id; }

    void setID(int id) { this.id = id; }

    public String getName() { return this.stringId; }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        for (String part : this.stringId.toLowerCase(java.util.Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public int getColor() { return this.color; }

    public int getTint() { return this.guiTint; }

    public Identifier getTexture() { return this.texture; }

    public String getTranslationKey() { return this.unlocalized; }

    public boolean isHot() { return this.temperature >= 100; }

    protected INetworkProvider<FluidNetMK2> NETWORK_PROVIDER = () -> new FluidNetMK2(this);

    public INetworkProvider<FluidNetMK2> getNetworkProvider() { return NETWORK_PROVIDER; }

    @Override
    public String toString() {
        return "FluidType[" + stringId + "#" + id + "]";
    }
}
