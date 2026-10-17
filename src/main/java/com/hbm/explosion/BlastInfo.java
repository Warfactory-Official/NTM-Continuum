package com.hbm.explosion;

import com.hbm.config.BombConfig;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The blast lines on bomb, charge and missile tooltips: a kind heading, the radius each one passes to its 1.7 explosion,
 * and for MK5 blasts the fallout radius. 1.7 itself shows none of these; the format is 1.12.2's nuke tooltips.
 */
public enum BlastInfo {

    FAT_MAN(Kind.NUCLEAR, () -> BombConfig.manRadius, 0),
    LITTLE_BOY(Kind.NUCLEAR, () -> BombConfig.boyRadius, 0),
    GADGET(Kind.NUCLEAR, () -> BombConfig.gadgetRadius, 0),
    TSAR(Kind.THERMONUCLEAR, () -> BombConfig.tsarRadius, 0),
    IVY_MIKE(Kind.THERMONUCLEAR, () -> BombConfig.mikeRadius, 0),
    N2(Kind.EXTREME, () -> BombConfig.n2Radius, BlastInfo.NO_FALLOUT),
    FLEIJA(Kind.SCHRABIDIUM, () -> BombConfig.fleijaRadius, BlastInfo.NO_FALLOUT),
    PROTOTYPE(Kind.SCHRABIDIUM, () -> BombConfig.prototypeRadius, BlastInfo.NO_FALLOUT),
    SOLINIUM(Kind.SOLINIUM, () -> BombConfig.soliniumRadius, BlastInfo.NO_FALLOUT),
    BALEFIRE(Kind.BALEFIRE, () -> 250, BlastInfo.NO_FALLOUT),
    DUD_BALEFIRE(Kind.BALEFIRE, () -> (int) (BombConfig.fatmanRadius * 1.25), BlastInfo.NO_FALLOUT),
    DUD_NUKE(Kind.NUCLEAR, () -> 35, 0),
    DUD_SALTED(Kind.NUCLEAR, () -> 25, 25),
    NUCLEAR_CHARGE(Kind.NUCLEAR, () -> BombConfig.missileRadius, 0),
    FISSURE(Kind.NUCLEAR, () -> (int) ExplosionNukeSmall.PARAMS_MEDIUM.blastRadius, BlastInfo.NO_FALLOUT),
    FAT_MINE(Kind.NUCLEAR, () -> 10, BlastInfo.NO_FALLOUT),
    MISSILE_MICRO(Kind.NUCLEAR, () -> BombConfig.fatmanRadius, 0),
    MISSILE_SCHRABIDIUM(Kind.SCHRABIDIUM, () -> BombConfig.aSchrabRadius, BlastInfo.NO_FALLOUT),
    MISSILE_NUCLEAR(Kind.NUCLEAR, () -> BombConfig.missileRadius, 0),
    MISSILE_MIRV(Kind.NUCLEAR, () -> BombConfig.missileRadius * 2, 0),
    MISSILE_DOOMSDAY(Kind.NUCLEAR, () -> BombConfig.missileRadius * 2, 100),
    MISSILE_DOOMSDAY_RUSTED(Kind.NUCLEAR, () -> BombConfig.missileRadius, 100);

    private static final int NO_FALLOUT = -1;

    private final Kind kind;
    private final IntSupplier radius;
    private final int falloutAdd;

    BlastInfo(Kind kind, IntSupplier radius, int falloutAdd) {
        this.kind = kind;
        this.radius = radius;
        this.falloutAdd = falloutAdd;
    }

    public int radius() {
        return radius.getAsInt();
    }

    public boolean hasFallout() {
        return falloutAdd != NO_FALLOUT;
    }

    /** 1.7 EntityNukeExplosionMK5 sizes its fallout rain as (int) (radius × 2.5 + moreFallout) × falloutRange / 100. */
    public int falloutRadius() {
        return (int) (radius() * 2.5 + falloutAdd) * BombConfig.falloutRange / 100;
    }

    /** The N2 mine's share per charge, integer arithmetic. */
    public static int n2ChargeRadius() {
        return BombConfig.n2Radius / 12;
    }

    public void addTo(List<Component> tooltip) {
        addHeading(tooltip, kind.key, kind.color);
        addRadius(tooltip, radius());
        if (this == N2) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.chargeadds", n2ChargeRadius()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (hasFallout() && !BombConfig.disableNuclear) {
            addHeading(tooltip, "trait.fallout", ChatFormatting.DARK_GREEN);
            addRadius(tooltip, falloutRadius());
        }
    }

    private static void addHeading(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.literal("[").append(Component.translatable(key)).append(Component.literal("]"))
                .withStyle(color));
    }

    /** The radius as shown, rounded to the nearest 5. */
    public static int shownRadius(int radius) {
        return Math.round(radius / 5F) * 5;
    }

    private static void addRadius(List<Component> tooltip, int radius) {
        tooltip.add(Component.literal(" ").append(Component.translatable("desc.radius", shownRadius(radius)))
                .withStyle(ChatFormatting.YELLOW));
    }

    private enum Kind {
        NUCLEAR("trait.nuclearbomb", ChatFormatting.DARK_GREEN),
        THERMONUCLEAR("trait.thermobomb", ChatFormatting.GOLD),
        EXTREME("trait.extremebomb", ChatFormatting.RED),
        SCHRABIDIUM("trait.schrabbomb", ChatFormatting.AQUA),
        SOLINIUM("trait.soliniumbomb", ChatFormatting.DARK_AQUA),
        BALEFIRE("trait.balefirebomb", ChatFormatting.GREEN);

        private final String key;
        private final ChatFormatting color;

        Kind(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }
    }
}
