package com.hbm.items.tool;

import com.hbm.packet.PlayerInformPayload;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Reports the distance to the block in the crosshair on the info HUD, up to 300 blocks. */
public final class ItemRangefinder extends CompatItem {
    public static final int META_POLARIZED = 1;
    private static final double RANGE = 300D;
    /** 1.7 {@code ServerProxy.ID_DETONATOR}. */
    private static final int ID_DETONATOR = 8;

    public ItemRangefinder(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK && player instanceof ServerPlayer serverPlayer) {
            double distance = start.distanceTo(hit.getLocation());
            String message = ((int) (distance * 10D)) / 10D + "m";
            ChatFormatting color = player.getItemInHand(hand).getDamageValue() == META_POLARIZED
                    ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE;
            GolemNetwork.sendToPlayer(serverPlayer, PlayerInformPayload.CODEC,
                    PlayerInformPayload.of(ID_DETONATOR, 5000, color, message));
        }
        return InteractionResult.SUCCESS;
    }
}
