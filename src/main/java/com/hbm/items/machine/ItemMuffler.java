package com.hbm.items.machine;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.LockTargets;
import dev.golem.core.world.CompatItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code ItemMuffler}: clicked on any machine (resolved to its multiblock core) that is not muffled yet, it
 * muffles it - every {@link TileEntityLoadedBase} plays at a tenth of the volume and skips its incidental sounds -
 * and is used up with the upgrade-plug click. 1.7 decremented the stack even in creative.
 */
public class ItemMuffler extends CompatItem {

    public ItemMuffler(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity te = LockTargets.coreAt(level, context.getClickedPos());
        if (te instanceof TileEntityLoadedBase tile && !tile.muffled) {
            tile.setMuffled(true);
            Player player = context.getPlayer();
            if (!level.isClientSide() && player != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.UPGRADE_PLUG.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            context.getItemInHand().shrink(1);
            tile.setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
