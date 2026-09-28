package com.hbm.items.tool;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.PlayerInformPayload.Translated;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemPollutionDetector}: anywhere in the inventory, every 10 ticks the soot, poison and heavy
 * metal of the player's pollution region go to the info HUD (ids 100-102, four seconds), truncated to two
 * decimals. 1.7 had the fallout line commented out, and so does this.
 */
public class ItemPollutionDetector extends CompatItem {

    public ItemPollutionDetector(Properties properties) {
        super(properties);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (!(entity instanceof ServerPlayer player) || world.getGameTime() % 10 != 0) return;

        PollutionData data = PollutionHandler.getPollutionData(world,
                BlockPos.containing(Math.floor(entity.getX()), Math.floor(entity.getY()), Math.floor(entity.getZ())));
        if (data == null) data = new PollutionData();

        float soot = data.pollution[PollutionType.SOOT.ordinal()];
        float poison = data.pollution[PollutionType.POISON.ordinal()];
        float heavymetal = data.pollution[PollutionType.HEAVYMETAL.ordinal()];

        soot = ((int) (soot * 100)) / 100F;
        poison = ((int) (poison * 100)) / 100F;
        heavymetal = ((int) (heavymetal * 100)) / 100F;

        send(player, 100, "pollution.soot", soot);
        send(player, 101, "pollution.poison", poison);
        send(player, 102, "pollution.heavymetal", heavymetal);
    }

    private static void send(ServerPlayer player, int id, String key, float value) {
        GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(id, 4000,
                ChatFormatting.YELLOW, new Translated(key), ": " + value));
    }
}
