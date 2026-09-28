package com.hbm.items.tool;

import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.CompatItem;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 1.7 {@code ItemDigammaDiagnostic}: a boop and the player's digamma report in chat. */
public class ItemDigammaDiagnostic extends CompatItem {

    public ItemDigammaDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printDiagnosticData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
