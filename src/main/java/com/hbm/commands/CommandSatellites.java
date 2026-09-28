package com.hbm.commands;

import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 1.7 {@code CommandSatellites}, {@code /ntmsatellites}: {@code orbit} launches the held satellite chip straight
 * into orbit on its frequency, {@code descend <frequency>} deletes a satellite, {@code list} prints every
 * satellite as "frequency - class". Player only; permission level 4 (1.7 {@code CommandBase}'s default).
 */
public final class CommandSatellites {

    private CommandSatellites() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmsatellites")
                .requires(dev.golem.core.command.Permissions.level(4))
                .then(Commands.literal("orbit").executes(ctx -> orbit(ctx.getSource())))
                .then(Commands.literal("descend")
                        .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(frequencies(ctx.getSource()), builder))
                                .executes(ctx -> descend(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "frequency")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
    }

    private static Iterable<String> frequencies(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) return java.util.List.of();
        return SatelliteSavedData.get(source.getLevel()).sats.keySet().stream().map(String::valueOf).toList();
    }

    private static boolean notPlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer) return false;
        source.sendFailure(Component.translatable("commands.satellite.should_be_run_as_player").withStyle(ChatFormatting.RED));
        return true;
    }

    private static int orbit(CommandSourceStack source) {
        if (notPlayer(source)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ItemStack held = player.getMainHandItem();

        // 1.7: any ISatChip but the blank ID chip. A chip whose item launches nothing is still consumed and still
        // reported as launched, because XSatelliteRegistry.orbit quietly does nothing for it.
        if (held.getItem() instanceof ISatChip && held.getItem() != ModSatelliteItems.sat_chip.get()) {
            Satellite.orbit(player.level() instanceof ServerLevel level ? level : source.getLevel(), held,
                    ISatChip.getFreqS(held), player.getX(), player.getY(), player.getZ());
            held.shrink(1);
            source.sendSuccess(() -> Component.translatable("commands.satellite.satellite_orbited")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }

        source.sendFailure(Component.translatable("commands.satellite.not_a_satellite").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int descend(CommandSourceStack source, int freq) {
        if (notPlayer(source)) return 0;
        SatelliteSavedData data = SatelliteSavedData.get(source.getLevel());
        if (data.descend(freq)) {
            source.sendSuccess(() -> Component.translatable("commands.satellite.satellite_descended")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.satellite.no_satellite").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int list(CommandSourceStack source) {
        if (notPlayer(source)) return 0;
        SatelliteSavedData data = SatelliteSavedData.get(source.getLevel());
        if (data.sats.isEmpty()) {
            source.sendFailure(Component.translatable("commands.satellite.no_active_satellites").withStyle(ChatFormatting.RED));
            return 0;
        }
        for (Map.Entry<Integer, Satellite> entry : data.sats.entrySet()) {
            String messageText = entry.getKey() + " - " + entry.getValue().getClass().getSimpleName();
            source.sendSuccess(() -> Component.literal(messageText).withStyle(ChatFormatting.GREEN), false);
        }
        return data.sats.size();
    }
}
