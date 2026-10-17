package com.hbm.items.weapon;

import com.hbm.explosion.BlastInfo;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A launchable missile, as an item, from 1.12.2's {@code ItemMissileStandard}.
 *
 * <p>The item <em>is</em> the datasheet. A launch pad reads three things off it and needs nothing else:
 * the {@link MissileFormFactor} (which pad it fits and how big it is drawn), the {@link MissileTier}
 * (what a radar calls it), and the {@link MissileFuel} (which two fluids the pad conforms its tanks to,
 * and how much of each a launch costs). The entity that actually flies is looked up separately, from
 * {@code TileEntityLaunchPadBase}'s registry.
 *
 * <p>{@link #notLaunchable()} exists for the one item that is a missile but must never fly: the rusted
 * doomsday missile, which is worldgen loot.
 *
 * <p>Separately from that flag, a missile may have no flying <em>entity</em> in this build because its
 * warhead depends on a subsystem that is not ported. The tooltip says so, because the launch pad refuses
 * such a missile at the slot and a silent refusal is indistinguishable from a broken pad.
 */
public class ItemMissileStandard extends ItemCustomLore {

    public final MissileFormFactor formFactor;
    public final MissileTier tier;
    public final MissileFuel fuel;

    public int fuelCap;
    public boolean launchable = true;
    private BlastInfo blast;

    public ItemMissileStandard(Properties properties, MissileFormFactor form, MissileTier tier) {
        this(properties, form, tier, form.defaultFuel);
    }

    public ItemMissileStandard(Properties properties, MissileFormFactor form, MissileTier tier, MissileFuel fuel) {
        super(properties.stacksTo(1));
        this.formFactor = form;
        this.tier = tier;
        this.fuel = fuel;
        this.fuelCap = fuel.defaultCap;
    }

    public ItemMissileStandard notLaunchable() {
        this.launchable = false;
        return this;
    }

    public ItemMissileStandard blast(BlastInfo blast) {
        this.blast = blast;
        return this;
    }

    public ItemMissileStandard setFuelCap(int fuelCap) {
        this.fuelCap = fuelCap;
        return this;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(this.tier.display).withStyle(ChatFormatting.ITALIC));

        if (!this.launchable) {
            tooltip.add(Component.literal("Not launchable!").withStyle(ChatFormatting.RED));
        } else if (!TileEntityLaunchPadBase.canBeInstantiated(this)) {
            // port: a missile whose warhead needs an unported subsystem has no flying entity, so a launch pad
            // refuses it at the slot. Without this line the refusal is silent and reads as a bug.
            tooltip.add(Component.translatable("desc.missile.unimplemented").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Fuel: ").append(Component.literal(this.fuel.display).withStyle(this.fuel.colour)));
            if (this.fuelCap > 0) {
                tooltip.add(Component.literal("Fuel capacity: " + this.fuelCap + "mB"));
            }
            super.appendCompatHoverText(stack, context, tooltip, flag);
        }
        if (blast != null) blast.addTo(tooltip);
    }

    /** Physical shape: decides which launch pad accepts it and at what scale the GUI draws it. */
    public enum MissileFormFactor {
        ABM(MissileFuel.SOLID),
        MICRO(MissileFuel.SOLID),
        V2(MissileFuel.ETHANOL_PEROXIDE),
        STRONG(MissileFuel.KEROSENE_PEROXIDE),
        HUGE(MissileFuel.KEROSENE_LOXY),
        ATLAS(MissileFuel.JETFUEL_LOXY),
        OTHER(MissileFuel.KEROSENE_PEROXIDE);

        private final MissileFuel defaultFuel;

        MissileFormFactor(MissileFuel defaultFuel) {
            this.defaultFuel = defaultFuel;
        }
    }

    /** Radar class. Also what the missile entity reports as its blip level and name. */
    public enum MissileTier {
        TIER0("Tier 0"),
        TIER1("Tier 1"),
        TIER2("Tier 2"),
        TIER3("Tier 3"),
        TIER4("Tier 4");

        public final String display;

        MissileTier(String display) {
            this.display = display;
        }
    }

    /**
     * The fuel/oxidiser pair a launch consumes, and how much of each. {@code SOLID} means pre-fuelled:
     * capacity 0, so the pad's tanks are irrelevant and only its power matters.
     */
    public enum MissileFuel {
        SOLID("Solid Fuel (pre-fueled)", ChatFormatting.GOLD, 0),
        ETHANOL_PEROXIDE("Ethanol / Hydrogen Peroxide", ChatFormatting.AQUA, 4_000),
        KEROSENE_PEROXIDE("Kerosene / Hydrogen Peroxide", ChatFormatting.BLUE, 8_000),
        KEROSENE_LOXY("Kerosene / Liquid Oxygen", ChatFormatting.LIGHT_PURPLE, 12_000),
        JETFUEL_LOXY("Jet Fuel / Liquid Oxygen", ChatFormatting.RED, 16_000);

        public final String display;
        public final ChatFormatting colour;
        public final int defaultCap;

        MissileFuel(String display, ChatFormatting colour, int defaultCap) {
            this.display = display;
            this.colour = colour;
            this.defaultCap = defaultCap;
        }
    }
}
