package com.hbm.items.tool;

import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ItemMeteorRemote}: two uses (max damage 2), each calling a meteorite down near the player
 * ({@code BossSpawnHandler.spawnMeteorAtPlayer(player, false)}).
 */
public class ItemMeteorRemote extends CompatItem {

    public ItemMeteorRemote(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Right click to summon a meteorite!"));
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) dev.golem.core.world.ItemStacks.hurtAndBreak(stack, 1, player, hand);

        if (!level.isClientSide()) {
            spawnMeteorAtPlayer(player, false);
            player.sendSystemMessage(Component.literal("Watch your head!"));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code BossSpawnHandler.spawnMeteorAtPlayer}, which the remote called; it shares that handler's Random. */
    public static void spawnMeteorAtPlayer(Player player, boolean repell) {
        com.hbm.handler.BossSpawnHandler.spawnMeteorAtPlayer(player, repell);
    }
}
