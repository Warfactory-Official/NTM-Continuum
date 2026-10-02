package com.hbm.inventory.fluid.trait;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 1.7 {@code FT_Polluting}: how much of each pollutant one millibucket of a fluid adds to the
 * region when it is spilled ({@link #releaseMap}) or burned ({@link #burnMap}).
 *
 * <p>1.7 stores both tables in {@code HashMap}s keyed by the enum. An {@link EnumMap} holds the same
 * pairs and walks them in declaration order, so tooltip lines no longer depend on identity hashes.
 *
 * <p>1.7's {@code onFluidRelease} override is not carried: nothing in 1.7 calls a trait's
 * {@code onFluidRelease} (the {@code FluidType} delegate it would come through is an empty method), so
 * every real release goes through {@link #pollute} or {@code TileEntityMachinePolluting.pollute}.
 */
public final class FT_Polluting {

    // original draft had both of them inside a hashmap for the release type but honestly handling hash maps in hash maps adds more complexity than it removes
    public final Map<PollutionType, Float> releaseMap = new EnumMap<>(PollutionType.class);
    public final Map<PollutionType, Float> burnMap = new EnumMap<>(PollutionType.class);

    public FT_Polluting release(PollutionType type, float amount) {
        releaseMap.put(type, amount);
        return this;
    }

    public FT_Polluting burn(PollutionType type, float amount) {
        burnMap.put(type, amount);
        return this;
    }

    /** 1.7 {@code addInfo}: the gold bracketed trait tag. */
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.polluting")).append("]")
                .withStyle(ChatFormatting.GOLD));
    }

    /** 1.7 {@code addInfoHidden}: the per-mB tables, spilled in green then burned in red. */
    public void addInfoHidden(List<Component> info) {
        if (!releaseMap.isEmpty()) {
            info.add(Component.translatable("hbmfluid.trait.spilled").append(":").withStyle(ChatFormatting.GREEN));
            for (Map.Entry<PollutionType, Float> entry : releaseMap.entrySet()) info.add(line(entry, ChatFormatting.GREEN));
        }

        if (!burnMap.isEmpty()) {
            info.add(Component.translatable("hbmfluid.trait.burned").append(":").withStyle(ChatFormatting.RED));
            for (Map.Entry<PollutionType, Float> entry : burnMap.entrySet()) info.add(line(entry, ChatFormatting.RED));
        }
    }

    /** {@code " - 0.004 SOOT per mB"}: 1.7 concatenates the raw float, so {@link String#valueOf(float)} it is. */
    private static Component line(Map.Entry<PollutionType, Float> entry, ChatFormatting style) {
        return Component.literal(" - " + entry.getValue() + " ")
                .append(Component.translatable("pollution.trait." + entry.getKey().name().toLowerCase(Locale.US)))
                .append(" ")
                .append(Component.translatable("hbmfluid.trait.perMB"))
                .withStyle(style);
    }

    /** 1.7 {@code FT_Polluting.pollute}: {@code mB} of {@code type} released straight into the region. */
    public static void pollute(Level world, BlockPos pos, FluidType type, FluidReleaseType release, float mB) {
        FT_Polluting trait = type.getTrait(FT_Polluting.class);
        if (trait == null) return;
        if (release == FluidReleaseType.VOID) return;

        Map<PollutionType, Float> map = release == FluidReleaseType.BURN ? trait.burnMap : trait.releaseMap;

        for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
            PollutionHandler.incrementPollution(world, pos, entry.getKey(), entry.getValue() * mB);
        }
    }
}
