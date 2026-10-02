package com.hbm.inventory.fluid;

import com.hbm.Constants;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.inventory.fluid.trait.FT_Pheromone;
import com.hbm.inventory.fluid.trait.FT_Poison;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.inventory.fluid.trait.FT_VentRadiation;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.util.BobMathUtil;
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
 * <p>The original also carried Forge-fluid interop, potion hooks, and client-side rendering. Those remain
 * outside this type; tooltip facts use the ported thermal, combustion and pollution data below.
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
        this.stringId = name;
        this.color = color;
        this.guiTint = color;
        this.key = name.toLowerCase(Locale.US);
        this.unlocalized = "hbmfluid." + this.key;
        this.fallbackName = titleCase(name);
        this.texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/gui/fluids/" + this.key + ".png");
        this.icon = dev.golem.core.Ids.of(Constants.MOD_ID, ICON_ART + "_" + this.key);
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

    /**
     * Whether the tooltip's hidden trait lines are shown: 1.7 polls physical left Shift. Common code cannot
     * reach the keyboard, so the client installs the real poll at startup; a server-side call sees false.
     */
    private static volatile java.util.function.BooleanSupplier shiftHeld = () -> false;

    /** Installs the client's physical-Shift poll; called once from client registration. */
    public static void setShiftHeldPoll(java.util.function.BooleanSupplier poll) {
        shiftHeld = java.util.Objects.requireNonNull(poll);
    }

    /** Adds the fluid facts currently represented by the port's fluid model to a tooltip. */
    public void addInfo(List<Component> info) {
        addInfo(info, shiftHeld.getAsBoolean());
    }

    /**
     * 1.7 {@code addInfo}: the temperature, then every trait in 1.7's {@code FluidTrait.traitList} registration
     * order: corrosive, flammable, combustible, polluting, heatable, coolable, pwrmoderator, poison, toxin,
     * ventradiation, pheromone, gaseous, gaseous_art, liquid, viscous, plasma, amat, leadcontainer, delicious,
     * (noid, nocontainer: no lines), unsiphonable. Each trait adds its always-visible lines, then its hidden
     * lines when Shift is held; the "Hold &lt;LSHIFT&gt;" hint closes the tooltip when any trait has hidden lines
     * and Shift is up. The order is the registry's, never the order a fluid's traits were added in.
     */
    public void addInfo(List<Component> info, boolean shiftHeld) {
        List<Component> hidden = new java.util.ArrayList<>();

        if (temperature != ROOM_TEMPERATURE) {
            if (temperature < 0) info.add(Component.literal(temperature + "\u00b0C").withStyle(ChatFormatting.BLUE));
            if (temperature > 0) info.add(Component.literal(temperature + "\u00b0C").withStyle(ChatFormatting.RED));
        }

        FT_Corrosive corrosive = getTrait(FT_Corrosive.class);
        if (corrosive != null) corrosive.addInfo(info);

        // FT_Flammable / FT_Combustible live in FluidCombustion; their lines are 1.7's.
        if (FluidCombustion.isFlammable(this)) {
            long energy = FluidCombustion.getFlammableEnergy(this);
            info.add(bracketed("hbmfluid.trait.flammable", ChatFormatting.YELLOW));
            if (energy > 0) info.add(provides(ChatFormatting.YELLOW, energy, "TU "));
        }

        FluidCombustion.Combustible combustible = FluidCombustion.getCombustible(this);
        if (combustible != null) {
            info.add(bracketed("hbmfluid.trait.combustible", ChatFormatting.GOLD));
            if (combustible.energyPerBucket() > 0) {
                info.add(provides(ChatFormatting.GOLD, combustible.energyPerBucket(), "HE "));
                info.add(Component.translatable("hbmfluid.trait.fuelGrade").append(": ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(fuelGradeKey(combustible.grade())).withStyle(ChatFormatting.RED)));
            }
        }

        FT_Polluting polluting = getTrait(FT_Polluting.class);
        if (polluting != null) {
            polluting.addInfo(info);
            hidden(info, hidden, shiftHeld, polluting::addInfoHidden);
        }

        FT_Heatable heatable = getTrait(FT_Heatable.class);
        if (heatable != null) hidden(info, hidden, shiftHeld, heatable::addInfoHidden);

        FT_Coolable coolable = getTrait(FT_Coolable.class);
        if (coolable != null) hidden(info, hidden, shiftHeld, coolable::addInfoHidden);

        FT_PWRModerator moderator = getTrait(FT_PWRModerator.class);
        if (moderator != null) {
            moderator.addInfo(info);
            hidden(info, hidden, shiftHeld, moderator::addInfoHidden);
        }

        FT_Poison poison = getTrait(FT_Poison.class);
        if (poison != null) hidden(info, hidden, shiftHeld, poison::addInfoHidden);

        FT_Toxin toxin = getTrait(FT_Toxin.class);
        if (toxin != null) hidden(info, hidden, shiftHeld, toxin::addInfoHidden);

        FT_VentRadiation vent = getTrait(FT_VentRadiation.class);
        if (vent != null) vent.addInfo(info);

        FT_Pheromone pheromone = getTrait(FT_Pheromone.class);
        if (pheromone != null) pheromone.addInfo(info);

        FluidTraitSimple.FT_Gaseous gaseous = getTrait(FluidTraitSimple.FT_Gaseous.class);
        if (gaseous != null) hidden(info, hidden, shiftHeld, gaseous::addInfoHidden);
        FluidTraitSimple.FT_Gaseous_ART evap = getTrait(FluidTraitSimple.FT_Gaseous_ART.class);
        if (evap != null) hidden(info, hidden, shiftHeld, evap::addInfoHidden);
        FluidTraitSimple.FT_Liquid liquid = getTrait(FluidTraitSimple.FT_Liquid.class);
        if (liquid != null) hidden(info, hidden, shiftHeld, liquid::addInfoHidden);
        FluidTraitSimple.FT_Viscous viscous = getTrait(FluidTraitSimple.FT_Viscous.class);
        if (viscous != null) hidden(info, hidden, shiftHeld, viscous::addInfoHidden);
        FluidTraitSimple.FT_Plasma plasma = getTrait(FluidTraitSimple.FT_Plasma.class);
        if (plasma != null) hidden(info, hidden, shiftHeld, plasma::addInfoHidden);
        FluidTraitSimple.FT_Amat amat = getTrait(FluidTraitSimple.FT_Amat.class);
        if (amat != null) amat.addInfo(info);
        FluidTraitSimple.FT_LeadContainer leadContainer = getTrait(FluidTraitSimple.FT_LeadContainer.class);
        if (leadContainer != null) leadContainer.addInfo(info);
        FluidTraitSimple.FT_Delicious delicious = getTrait(FluidTraitSimple.FT_Delicious.class);
        if (delicious != null) hidden(info, hidden, shiftHeld, delicious::addInfoHidden);
        FluidTraitSimple.FT_Unsiphonable unsiphonable = getTrait(FluidTraitSimple.FT_Unsiphonable.class);
        if (unsiphonable != null) hidden(info, hidden, shiftHeld, unsiphonable::addInfoHidden);

        if (!hidden.isEmpty() && !shiftHeld) {
            info.add(Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                    .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                    .append(Component.literal("> to display more info")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }
    }

    /** 1.7's per-trait {@code if(shiftHeld) trait.addInfoHidden(info); trait.addInfoHidden(hidden);}. */
    private static void hidden(List<Component> info, List<Component> hidden, boolean shiftHeld,
            java.util.function.Consumer<List<Component>> adder) {
        if (shiftHeld) adder.accept(info);
        adder.accept(hidden);
    }

    private static Component bracketed(String key, ChatFormatting style) {
        return Component.literal("[").append(Component.translatable(key)).append("]").withStyle(style);
    }

    /** 1.7 {@code "Provides " + RED + getShortNumber(energy) + unit + "per bucket"} in the trait's colour. */
    private static Component provides(ChatFormatting style, long energy, String unit) {
        return Component.translatable("hbmfluid.trait.provides").append(" ").withStyle(style)
                .append(Component.literal(BobMathUtil.getShortNumber(energy) + unit).withStyle(ChatFormatting.RED))
                .append(Component.translatable("hbmfluid.trait.perBucket").withStyle(style));
    }

    /** 1.7 {@code FuelGrade.getLocalizedName}: {@code hbmfluid.trait.fuel.<grade>}. */
    private static String fuelGradeKey(FluidCombustion.FuelGrade grade) {
        return switch (grade) {
            case LOW -> "hbmfluid.trait.fuel.low";
            case MEDIUM -> "hbmfluid.trait.fuel.medium";
            case HIGH -> "hbmfluid.trait.fuel.high";
            case AERO -> "hbmfluid.trait.fuel.aviation";
            case GAS -> "hbmfluid.trait.fuel.gaseous";
        };
    }

    // 1.7 FluidType's trait shorthands.
    public boolean isCorrosive() { return hasTrait(FT_Corrosive.class); }
    public boolean isAntimatter() { return hasTrait(FluidTraitSimple.FT_Amat.class); }
    public boolean hasNoContainer() { return hasTrait(FluidTraitSimple.FT_NoContainer.class); }
    public boolean hasNoID() { return hasTrait(FluidTraitSimple.FT_NoID.class); }
    public boolean needsLeadContainer() { return hasTrait(FluidTraitSimple.FT_LeadContainer.class); }
    public boolean isDispersable() {
        return !(hasTrait(FluidTraitSimple.FT_Amat.class) || hasTrait(FluidTraitSimple.FT_NoContainer.class)
                || hasTrait(FluidTraitSimple.FT_Viscous.class));
    }

    @Override
    public String toString() {
        return "FluidType[" + stringId + "#" + id + "]";
    }
}
