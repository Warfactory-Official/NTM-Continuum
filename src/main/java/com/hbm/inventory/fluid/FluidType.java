package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.FluidNetMK2;
import com.hbm.uninos.INetworkProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * A single fluid kind in HBM's own fluid system (NOT a vanilla/Forge {@code Fluid}). Each FluidType
 * owns its own {@link INetworkProvider} so that, in UNINOS, every fluid kind has a wholly separate
 * nodespace — fluid networks are segregated by type (water pipes never mix with oil pipes).
 *
 * <p>The original also carried Forge-fluid interop, pollution/potion hooks, and client-side tooltip/render
 * code. Those remain outside this type; its trait map retains the thermal conversion data used by machines.
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
    /** Title-cased id, used only when a fluid has no {@code hbmfluid.*} entry. */
    private final String fallbackName;
    /** GUI tint (defaults to the colour; overridable for custom fluids). */
    private int guiTint;
    private final Identifier texture;
    private final Map<Class<?>, Object> traits = new HashMap<>();

    public static final int ROOM_TEMPERATURE = 20;
    /** How hot this fluid is, in °C. */
    public int temperature = ROOM_TEMPERATURE;

    public FluidType(String name, int color) {
        this.stringId = name;
        this.color = color;
        this.guiTint = color;
        this.unlocalized = "hbmfluid." + name.toLowerCase(Locale.US);
        this.fallbackName = titleCase(name);
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

    /**
     * The fluid's name as the player should see it — the localised {@code hbmfluid.*} entry.
     *
     * <p>Every display of a fluid name must come through here. Title-casing the internal id is
     * <b>not</b> a substitute: it agrees with the real name for only 39 of the 157 fluids, and
     * silently disagrees for the rest ({@code HEAVYOIL} is "Heavy Oil", {@code OIL} is "Crude Oil",
     * {@code SMEAR} is "Industrial Oil", {@code WATZ} is "Poisonous Mud"). The title-cased id survives
     * only as the fallback for a fluid that has no lang entry yet.
     */
    public Component getDisplayName() {
        return Component.translatableWithFallback(this.unlocalized, this.fallbackName);
    }

    /** {@link #getDisplayName()} flattened, for the {@code &[colour]&]} look-overlay strings. */
    public String getDisplayNameString() {
        return getDisplayName().getString();
    }

    /** {@code HEAVY_OIL → "Heavy Oil"}. The fallback only; see {@link #getDisplayName()}. */
    private static String titleCase(String id) {
        StringBuilder sb = new StringBuilder();
        for (String part : id.toLowerCase(Locale.US).split("_")) {
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

    public FluidType addTraits(Object... traits) {
        for (Object trait : traits) this.traits.put(trait.getClass(), trait);
        return this;
    }

    public boolean hasTrait(Class<?> trait) { return traits.containsKey(trait); }

    public <T> T getTrait(Class<T> trait) { return trait.cast(traits.get(trait)); }

    protected INetworkProvider<FluidNetMK2> NETWORK_PROVIDER = () -> new FluidNetMK2(this);

    public INetworkProvider<FluidNetMK2> getNetworkProvider() { return NETWORK_PROVIDER; }

    @Override
    public String toString() {
        return "FluidType[" + stringId + "#" + id + "]";
    }
}
