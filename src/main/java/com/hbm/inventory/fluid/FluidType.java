package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * A single fluid kind in HBM's own fluid system (NOT a vanilla/Forge {@code Fluid}). Each FluidType
 * owns its own {@link INetworkProvider} so that, in UNINOS, every fluid kind has a wholly separate
 * nodespace — fluid networks are segregated by type (water pipes never mix with oil pipes).
 *
 * <p>26.1 slim port: the 1.12.2 original carried Forge {@code Fluid}/{@code FluidRegistry} interop, a
 * trait subsystem (corrosive/flammable/…), pollution/potion hooks, and client-side tooltip/render
 * code. None of that is needed by the fluid <em>network</em>, so this port keeps only the identity
 * (id/name/color/temperature/texture) and the network provider. Traits etc. can be layered back on
 * later when the fluid-consuming machines that need them are ported.
 *
 * @author hbm, mlbv
 */
public class FluidType {

    /** The numeric ID of the fluid (assigned by {@link Fluids} on registration). */
    private int id;
    /** The internal/string name (e.g. {@code "water"}). */
    private final String stringId;
    /** Approximate HEX colour of the fluid, used for pipe/GUI rendering. */
    private final int color;
    /** Unlocalized translation key. */
    private final String unlocalized;
    /** GUI tint (defaults to the colour; overridable for custom fluids). */
    private int guiTint;
    private final Identifier texture;

    public static final int ROOM_TEMPERATURE = 20;
    /** How hot this fluid is, in °C. */
    public int temperature = ROOM_TEMPERATURE;

    public FluidType(String name, int color) {
        this.stringId = name;
        this.color = color;
        this.guiTint = color;
        this.unlocalized = "hbmfluid." + name.toLowerCase(Locale.US);
        this.texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/gui/fluids/" + name.toLowerCase(Locale.US) + ".png");
        this.id = Fluids.registerSelf(this);
    }

    /** Constructor with an asserted ID, mirroring the 1.12.2 safeguard against fluid-ID drift. */
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

    /** Used internally by {@link Fluids} when (re)assigning IDs. */
    void setID(int id) { this.id = id; }

    /** The unique mapping name for this fluid, usually matching the unlocalized name minus the prefix. */
    public String getName() { return this.stringId; }

    /** A human-readable, title-cased name for display, e.g. {@code HEAVY_OIL → "Heavy Oil"}. */
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
