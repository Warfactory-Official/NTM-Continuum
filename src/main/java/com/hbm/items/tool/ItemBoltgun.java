package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Boltgun — the {@link IToolable.ToolType#BOLT} tool. Right-clicking a {@link IToolable} block bolts it
 * into its upgraded form, consuming the bolts the conversion asks for. Its job in this wave is turning
 * plain Watz casing into the bolted casing the reactor structure requires.
 *
 * <p>The 1.12.2 original doubled as a weapon (left-click fires a bolt from the player's inventory for
 * 10 armour-piercing damage, with a recoil animation); that half belongs with the gun wave and is not
 * ported here. It is unfuelled either way — the bolts are the ammunition and the conversion cost.
 */
public class ItemBoltgun extends CompatItem {

    public ItemBoltgun(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof IToolable toolable)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (toolable.onScrew(level, player, pos, context.getClickedFace(), context.getClickLocation(),
                context.getHand(), IToolable.ToolType.BOLT)) {
            level.playSound(null, pos, ModSounds.BOLTGUN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
