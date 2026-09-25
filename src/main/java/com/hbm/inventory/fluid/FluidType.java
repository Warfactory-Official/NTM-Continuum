package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.lib.Library;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A single fluid kind in HBM's own fluid system (NOT a vanilla/Forge {@code Fluid}). Fluid networks are
 * segregated by type — water pipes never mix with oil pipes — which the fluid graph gets from
 * {@link com.hbm.api.fluidmk2.FluidGraphProvider#dataCompatible} rather than from a nodespace per type.
 *
 * <p>The original also carried Forge-fluid interop, pollution/potion hooks, and client-side rendering. Those
 * remain outside this type; tooltip facts use the ported thermal and combustion data below.
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
    /** The lowercase id, which every asset path and translation key for this fluid is built from. */
    private final String key;
    private final Identifier texture;
    private final Identifier icon;
    private final Map<Class<?>, Object> traits = new HashMap<>();

    /**
     * The greyscale droplet art every fluid's icon sprite is recoloured from, and therefore the stem of
     * every one of those sprite ids. {@link com.hbm.client.FluidSprites} declares the recolouring from
     * this same path, so the id minted there and the id {@link #getIconSprite()} hands a screen cannot
     * drift apart.
     */
    public static final String ICON_ART = "items/fluid_icon";

    public static final int ROOM_TEMPERATURE = 20;
    /** How hot this fluid is, in °C. */
    public int temperature = ROOM_TEMPERATURE;

    public FluidType(String name, int color) {
        this(name, color, null);
        this.id = Fluids.registerSelf(this);
    }

    /** 1.7 {@code setupCustom}: an explicit id, and any GUI texture. */
    public FluidType(int id, String name, int color, Identifier texture) {
        this(name, color, texture);
        Fluids.register(this, id);
    }

    private FluidType(String name, int color, Identifier texture) {
        this.stringId = name;
        this.color = color;
        this.guiTint = color;
        this.key = name.toLowerCase(Locale.US);
        this.unlocalized = "hbmfluid." + this.key;
        this.fallbackName = titleCase(name);
        this.texture = texture != null ? texture
                : dev.golem.core.Ids.of(Constants.MOD_ID, "textures/gui/fluids/" + this.key + ".png");
        this.icon = dev.golem.core.Ids.of(Constants.MOD_ID, ICON_ART + "_" + this.key);
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

    /** The lowercase id every asset this fluid names is built from. */
    public String getKey() { return this.key; }

    /**
     * The droplet sprite for this fluid: the grayscale {@code fluid_icon} art recoloured onto
     * {@link #getColor()}, stitched into the item atlas by {@link com.hbm.client.FluidSprites}.
     *
     * <p>A sprite id, not a texture path — there is no file behind it. Draw it with
     * {@link com.hbm.client.FluidSprites#draw}, which is what a screen or a JEI category wants when it
     * has somewhere to put a droplet but no item slot to put it in.
     */
    public Identifier getIconSprite() { return this.icon; }

    public String getTranslationKey() { return this.unlocalized; }

    public boolean isHot() { return this.temperature >= 100; }

    public FluidType addTraits(Object... traits) {
        for (Object trait : traits) this.traits.put(trait.getClass(), trait);
        return this;
    }

    public boolean hasTrait(Class<?> trait) { return traits.containsKey(trait); }

    public <T> T getTrait(Class<T> trait) { return trait.cast(traits.get(trait)); }

    /** Adds the fluid facts currently represented by the port's fluid model to a tooltip. */
    public void addInfo(List<Component> info) {
        if (temperature != ROOM_TEMPERATURE) {
            if (temperature < 0) info.add(Component.literal(temperature + "\u00b0C").withStyle(ChatFormatting.BLUE));
            if (temperature > 0) info.add(Component.literal(temperature + "\u00b0C").withStyle(ChatFormatting.RED));
        }

        com.hbm.inventory.fluid.trait.FT_Corrosive corrosive = getTrait(
                com.hbm.inventory.fluid.trait.FT_Corrosive.class);
        if (corrosive != null) corrosive.addInfo(info);

        com.hbm.inventory.fluid.trait.FT_PWRModerator moderator = getTrait(
                com.hbm.inventory.fluid.trait.FT_PWRModerator.class);
        if (moderator != null) moderator.addInfo(info);

        long flammable = FluidCombustion.getFlammableEnergy(this);
        if (flammable > 0) {
            info.add(bracketed("trait.flammable", ChatFormatting.YELLOW));
            info.add(Component.translatable("trait.flammable.desc", Library.getShortNumber(flammable)));
        }

        FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(this);
        if (combustible != null) {
            info.add(bracketed("trait.combustable", ChatFormatting.GOLD));
            if (combustible.energyPerBucket() > 0) {
                info.add(Component.translatable("trait.combustable.desc",
                        Library.getShortNumber(combustible.energyPerBucket())));
                info.add(Component.translatable("trait.combustable.desc2",
                        Component.translatable(fuelGradeKey(combustible.grade()))));
            }
        }
    }

    private static Component bracketed(String key, ChatFormatting style) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style);
    }

    private static String fuelGradeKey(FluidCombustion.FuelGrade grade) {
        return switch (grade) {
            case LOW -> "trait.combustable.low";
            case MEDIUM -> "trait.combustable.medium";
            case HIGH -> "trait.combustable.high";
            case AERO -> "trait.combustable.avi";
            case GAS -> "trait.combustable.gas";
        };
    }



    @Override
    public String toString() {
        return "FluidType[" + stringId + "#" + id + "]";
    }
}
