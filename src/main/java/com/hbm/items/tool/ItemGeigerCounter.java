package com.hbm.items.tool;

import java.util.ArrayList;
import java.util.List;

import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import dev.golem.core.world.CompatItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Handheld Geiger counter — clicks faster the more radiation the carrier is taking (reads the carrier's
 * <i>actual</i> rads = accumulated contamination × armor reduction), and prints a full survey readout to
 * chat on right-click. Ported from 1.12.2's {@code ItemGeigerCounter} (the Baubles/FSB integration is
 * dropped; the click ladder + thresholds are copied verbatim).
 */
public class ItemGeigerCounter extends CompatItem {

    public ItemGeigerCounter(Properties props) {
        super(props);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                                       @Nullable EquipmentSlot slot, boolean held) {
        if (owner instanceof Player player) {
            playGeiger(level, player);
        }
    }

    public static void playGeiger(ServerLevel level, Player player) {
        double x = ContaminationUtil.getActualPlayerRads(player);

        if (TickPhase.every(player, 5)) {
            if (x > 1e-5) {
                List<Integer> list = new ArrayList<>();
                if (x < 1) list.add(0);
                if (x < 5) list.add(0);
                if (x < 10) list.add(1);
                if (x > 5 && x < 15) list.add(2);
                if (x > 10 && x < 20) list.add(3);
                if (x > 15 && x < 25) list.add(4);
                if (x > 20 && x < 30) list.add(5);
                if (x > 25) list.add(6);
                int r = list.get(level.getRandom().nextInt(list.size()));

                if (r > 0) play(level, player, ModSounds.GEIGER_SOUNDS[r - 1].get());
            } else if (level.getRandom().nextInt(100) == 0) {
                play(level, player, ModSounds.GEIGER_SOUNDS[level.getRandom().nextInt(1)].get());
            }
        }
    }

    private static void play(ServerLevel level, Player player, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return hbm$use(level, player, hand);
    }

    private InteractionResult hbm$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ContaminationUtil.printGeigerData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
