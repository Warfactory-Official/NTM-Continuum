package com.hbm.items;

import com.hbm.packet.ToolAnimationPayload;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code IAnimatedItem} for the non-gun tools: the server decides that an animation plays and tells the holder's
 * client, which starts it on the held slot's first rail ({@code HbmAnimationPacket.handleItem}).
 *
 * <p>1.7 kept {@code getAnimation} on the item; here the animation tables are client code
 * ({@code com.hbm.client.anim.ToolAnimations}), the way the guns' tables are, so no common class names a client type.
 * The Sedna guns have their own path ({@code GunAnimationPayload}).
 */
public interface IAnimatedItem {

    /** 1.7 {@code playAnimation}: only a server player is sent anything. */
    default void playAnimation(Player player, ToolAnimation type, ItemStack stack) {
        if (player instanceof ServerPlayer server) {
            GolemNetwork.sendToPlayer(server, ToolAnimationPayload.CODEC, new ToolAnimationPayload(type.ordinal(),
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        }
    }
}
