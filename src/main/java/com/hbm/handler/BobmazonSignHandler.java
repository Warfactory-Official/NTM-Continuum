package com.hbm.handler;

import com.hbm.items.ModMeleeKitItems;
import com.hbm.util.ShadyUtil;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.ItemFirstUse;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** A sign's four authored phrase lines unlock the hidden catalogue at the actual clicked position. */
public final class BobmazonSignHandler {
    private BobmazonSignHandler() {}

    public static int pickupDelay = 10;
    public static final Hook<BiConsumer<Player, BlockPos>> OBTAINED = Hook.notifying();
    private static boolean installed;

    public static void init() {
        if (installed) return;
        installed = true;
        ItemFirstUse.registerBlock((stack, context) ->
                onSignUsed(context.getLevel(), context.getClickedPos(), context.getPlayer())
                        ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND);
    }

    public static boolean onSignUsed(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) return false;
        var text = sign.getFrontText();
        var digest = ShadyUtil.smoosh(text.getMessage(0, false).getString(), text.getMessage(1, false).getString(),
                text.getMessage(2, false).getString(), text.getMessage(3, false).getString());
        if (!ShadyUtil.HASHES.contains(digest)) return false;
        if (!level.destroyBlock(pos, false)) return false;
        var drop = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                new ItemStack(ModMeleeKitItems.bobmazon_hidden.get()));
        drop.setPickUpDelay(pickupDelay);
        level.addFreshEntity(drop);
        for (var handler : OBTAINED) handler.accept(player, pos);
        return true;
    }
}
