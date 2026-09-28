package com.hbm.items.weapon;

import com.hbm.entity.missile.EntityMissileCustom;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * One assembly part of a custom missile - chip, warhead, fuselage, fins or thruster. Every distinct stat combination
 * is its own Item; the builder call in ModItems decides which of the five it is and what its attribute row holds.
 *
 * 1.7 kept a {@code HashMap<Integer, ItemCustomMissilePart> parts} so it could map a datawatcher-synced numeric item
 * id back to the part. 26.2 syncs the ItemStack itself, so the map has no callers and is dropped.
 */
public class ItemCustomMissilePart extends CompatItem {

    public PartType type;
    public PartSize top;
    public PartSize bottom;
    public Rarity rarity;
    public float health;

    /**
     * 1.7 render/util/MissilePart's row for this part. {@code height} is what the part adds to the stack above it
     * in an assembled missile, NOT the {@code guiheight} the part's own icon scales itself by. A chip leaves all
     * three unset, exactly as 1.7 left the chips out of that table.
     */
    public float height;
    public @Nullable Identifier mesh;
    public @Nullable Identifier skin;

    private String title;
    private String author;
    private String witty;

    /**
     * Untyped attribute row, indexed exactly as 1.7 indexed it so the assembler and the missile read the same slots:
     *
     * <pre>
     * Chips:     [0] inaccuracy
     * Warheads:  [0] WarheadType, [1] strength/radius/cluster count, [2] weight
     * Fuselages: [0] FuelType,    [1] tank size
     * Fins:      [0] inaccuracy mod
     * Thrusters: [0] FuelType,    [1] consumption, [2] lift strength
     * </pre>
     */
    public Object[] attributes;

    public ItemCustomMissilePart(Properties properties) {
        super(properties);
    }

    public enum PartType {
        CHIP,
        WARHEAD,
        FUSELAGE,
        FINS,
        THRUSTER
    }

    public enum PartSize {

        /** chips fit anything */
        ANY,
        /** missile tips and thrusters cap the stack */
        NONE,
        SIZE_10,
        SIZE_15,
        SIZE_20;

        public Component getDisplay() {
            switch (this) {
                case ANY: return Component.translatable("item.missile.part.size.any");
                case SIZE_10: return Component.literal("1.0m");
                case SIZE_15: return Component.literal("1.5m");
                case SIZE_20: return Component.literal("2.0m");
                default: return Component.translatable("item.missile.part.size.none");
            }
        }
    }

    public enum WarheadType {

        HE,
        INC,
        BUSTER,
        CLUSTER,
        NUCLEAR,
        TX,
        N2,
        BALEFIRE,
        SCHRAB,
        TAINT,
        CLOUD,
        TURBINE,

        // shit solution but it works: dummy slots letting custom warheads attach traits
        CUSTOM0, CUSTOM1, CUSTOM2, CUSTOM3, CUSTOM4, CUSTOM5, CUSTOM6, CUSTOM7, CUSTOM8, CUSTOM9;

        /** Overrides that type's impact effect. Only runs serverside */
        public Consumer<EntityMissileCustom> impactCustom = null;
        /** Runs at the beginning of the missile's update cycle, both client and serverside. */
        public Consumer<EntityMissileCustom> updateCustom = null;
        /** Override for the warhead's name in the missile description */
        public String labelCustom = null;

        public Component getDisplay() {

            if (this.labelCustom != null) return Component.literal(this.labelCustom);

            switch (this) {
                case HE: return key("he", ChatFormatting.YELLOW);
                case INC: return key("incendiary", ChatFormatting.GOLD);
                case CLUSTER: return key("cluster", ChatFormatting.GRAY);
                case BUSTER: return key("bunker_buster", ChatFormatting.WHITE);
                case NUCLEAR: return key("nuclear", ChatFormatting.DARK_GREEN);
                case TX: return key("thermonuclear", ChatFormatting.DARK_PURPLE);
                case N2: return key("n2", ChatFormatting.RED);
                case BALEFIRE: return key("balefire", ChatFormatting.GREEN);
                case SCHRAB: return key("schrabidium", ChatFormatting.AQUA);
                case TAINT: return key("taint", ChatFormatting.DARK_PURPLE);
                case CLOUD: return key("cloud", ChatFormatting.LIGHT_PURPLE);
                // 1.7 flickered the turbine label off the wall clock at 2Hz
                case TURBINE: return key("turbine", System.currentTimeMillis() % 1000 < 500
                        ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE);
                default: return Component.translatable("general.na").withStyle(ChatFormatting.BOLD);
            }
        }

        private static Component key(String name, ChatFormatting color) {
            return Component.translatable("item.warhead.desc." + name).withStyle(color);
        }
    }

    public enum FuelType {

        KEROSENE("kerosene_peroxide", ChatFormatting.LIGHT_PURPLE),
        SOLID("solid", ChatFormatting.GOLD),
        HYDROGEN("hydrogen", ChatFormatting.DARK_AQUA),
        XENON("xenon", ChatFormatting.DARK_PURPLE),
        BALEFIRE("balefire", ChatFormatting.GREEN);

        private final String key;
        private final ChatFormatting color;

        FuelType(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable("item.missile.fuel." + key).withStyle(color);
        }
    }

    public enum Rarity {

        COMMON("common", ChatFormatting.GRAY),
        UNCOMMON("uncommon", ChatFormatting.YELLOW),
        RARE("rare", ChatFormatting.AQUA),
        EPIC("epic", ChatFormatting.LIGHT_PURPLE),
        LEGENDARY("legendary", ChatFormatting.DARK_GREEN),
        STRANGE("strange", ChatFormatting.DARK_AQUA);

        private final String key;
        private final ChatFormatting color;

        Rarity(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable("item.missile.part.rarity." + key).withStyle(color);
        }
    }

    public ItemCustomMissilePart makeChip(float inaccuracy) {
        this.type = PartType.CHIP;
        this.top = PartSize.ANY;
        this.bottom = PartSize.ANY;
        this.attributes = new Object[]{inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeWarhead(WarheadType type, float punch, float weight, PartSize size) {
        this.type = PartType.WARHEAD;
        this.top = PartSize.NONE;
        this.bottom = size;
        this.attributes = new Object[]{type, punch, weight};
        return this;
    }

    public ItemCustomMissilePart makeFuselage(FuelType type, float fuel, PartSize top, PartSize bottom) {
        this.type = PartType.FUSELAGE;
        this.top = top;
        this.bottom = bottom;
        this.attributes = new Object[]{type, fuel};
        return this;
    }

    public ItemCustomMissilePart makeStability(float inaccuracy, PartSize size) {
        this.type = PartType.FINS;
        this.top = size;
        this.bottom = size;
        this.attributes = new Object[]{inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeThruster(FuelType type, float consumption, float lift, PartSize size) {
        this.type = PartType.THRUSTER;
        this.top = size;
        this.bottom = PartSize.NONE;
        this.attributes = new Object[]{type, consumption, lift};
        return this;
    }

    public ItemCustomMissilePart setAssembly(float height, Identifier mesh, Identifier skin) {
        this.height = height;
        this.mesh = mesh;
        this.skin = skin;
        return this;
    }

    public ItemCustomMissilePart setAuthor(String author) {
        this.author = author;
        return this;
    }

    public ItemCustomMissilePart setTitle(String title) {
        this.title = title;
        return this;
    }

    public ItemCustomMissilePart setWittyText(String witty) {
        this.witty = witty;
        return this;
    }

    public ItemCustomMissilePart setHealth(float health) {
        this.health = health;
        return this;
    }

    /**
     * Stating a rarity is also what enters the part into a loot crate's pool; a part left at no rarity is
     * unreachable from every crate.
     */
    public ItemCustomMissilePart setRarity(Rarity rarity) {
        this.rarity = rarity;

        // 1.7 also filed the part into ItemLootCrate's list10/list15/listMisc here; the port's ItemLootCrate
        // reads the same pools back from the registry (every part with a rarity) when a crate is first opened.

        return this;
    }

    // typed reads of the attribute row; every caller in the port goes through these rather than casting Object[]

    public WarheadType warheadType() {
        return this.type == PartType.WARHEAD ? (WarheadType) this.attributes[0] : null;
    }

    public float warheadStrength() {
        return this.type == PartType.WARHEAD ? (Float) this.attributes[1] : 0F;
    }

    public FuelType fuelType() {
        return this.type == PartType.FUSELAGE || this.type == PartType.THRUSTER
                ? (FuelType) this.attributes[0] : null;
    }

    public float fuelAmount() {
        return this.type == PartType.FUSELAGE ? (Float) this.attributes[1] : 0F;
    }

    public float consumption() {
        return this.type == PartType.THRUSTER ? (Float) this.attributes[1] : 0F;
    }

    public float inaccuracy() {
        return this.type == PartType.CHIP || this.type == PartType.FINS ? (Float) this.attributes[0] : 0F;
    }

    /** The part carried by this stack, or null if the stack is empty or holds something else. */
    public static ItemCustomMissilePart part(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCustomMissilePart part ? part : null;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {

        if (this.title != null) {
            tooltip.add(Component.literal("\"" + this.title + "\"").withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (this.type != null && this.attributes != null) {
            switch (this.type) {
                case CHIP:
                    tooltip.add(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case WARHEAD:
                    tooltip.add(stat("size", this.bottom.getDisplay()));
                    tooltip.add(stat("type", warheadType().getDisplay()));
                    tooltip.add(stat("strength", Component.literal(String.valueOf(warheadStrength()))));
                    tooltip.add(stat("weight", Component.literal(this.attributes[2] + "t")));
                    break;
                case FUSELAGE:
                    tooltip.add(stat("topSize", this.top.getDisplay()));
                    tooltip.add(stat("bottomSize", this.bottom.getDisplay()));
                    tooltip.add(stat("fuelType", fuelType().getDisplay()));
                    tooltip.add(stat("fuelAmount", Component.literal(fuelAmount() + "l")));
                    break;
                case FINS:
                    tooltip.add(stat("size", this.top.getDisplay()));
                    tooltip.add(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case THRUSTER:
                    tooltip.add(stat("size", this.top.getDisplay()));
                    tooltip.add(stat("fuelType", fuelType().getDisplay()));
                    tooltip.add(stat("fuelConsumption", Component.literal(consumption() + "l/tick")));
                    tooltip.add(stat("maxPayload", Component.literal(this.attributes[2] + "t")));
                    break;
            }
        }

        if (this.type != PartType.CHIP) {
            tooltip.add(stat("health", Component.literal(this.health + "HP")));
        }
        if (this.rarity != null) {
            tooltip.add(stat("rarity", this.rarity.getDisplay()));
        }
        // 1.7 indented the byline and the witty line with three spaces
        if (this.author != null) {
            tooltip.add(Component.literal("   ").append(Component.translatable("item.missile.part.by"))
                                  .append(" " + this.author).withStyle(ChatFormatting.WHITE));
        }
        if (this.witty != null) {
            tooltip.add(Component.literal("   \"" + this.witty + "\"")
                                  .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    /**
     * 1.7 {@code BOLD + key + ": " + GRAY + value}. 1.7's tooltip renderer prefixed every line after the name
     * with gray, so the bold key reads gray too; the value's own colour code, where it has one, wins.
     */
    static Component stat(String key, Component value) {
        return Component.translatable(key.contains(".") ? key : "item.missile.part." + key).append(": ")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)
                        .append(value.copy().withStyle(style -> style.getColor() == null
                                ? style.withColor(ChatFormatting.GRAY).withBold(false) : style.withBold(false)));
    }
}
