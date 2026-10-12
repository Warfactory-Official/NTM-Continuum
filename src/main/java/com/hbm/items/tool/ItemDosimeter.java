package com.hbm.items.tool;

import com.hbm.capability.HbmLivingProps;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.CompatItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemDosimeter}: carried, it clicks on every fifth world tick by the carrier's environmental radiation
 * buffer ({@code HbmLivingProps.getRadBuf}, the RAD/s of the last second): {@code hbm:item.geiger1..3}, chosen at random
 * from a short ladder ({@link #click}); with no radiation one click tick in 100 plays geiger1. A right-click plays
 * {@code hbm:item.tech_boop} and prints the DOSIMETER readout (environmental RAD/s, one decimal, capped at 3.6).
 */
public class ItemDosimeter extends CompatItem {

    /**
     * hbm-space {@code ItemDosimeter}: a click tick with a dose below 1 goes ahead only with that probability
     * ({@code world.rand.nextFloat() > x} returns before the ladder). Off in plain HBM (1.7); NTM: Space turns it on
     * (ADDON-API-INVENTORY V-186).
     */
    public static boolean probabilisticClicks = false;

    public ItemDosimeter(Properties props) {
        super(props);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                                       @Nullable EquipmentSlot slot, boolean held) {
        if (!(owner instanceof LivingEntity living)) return;
        float x = (float) HbmLivingProps.getRadBuf(living);
        if (level.getGameTime() % 5 == 0) {
            int r = click(x, level.getRandom(), level.getRandom());
            if (r > 0) play(level, owner, ModSounds.GEIGER_SOUNDS[r - 1].get());
        }
    }

    /**
     * 1.7 {@code onUpdate}'s choice on a click tick: the geiger sound number 1..3, or 0 for silence. {@code rand} is 1.7's
     * item {@code Random} (the ladder and the background roll), {@code worldRand} hbm-space's {@code world.rand} (the
     * gate); the port passes the level's random for both.
     */
    public static int click(float x, RandomSource rand, RandomSource worldRand) {
        if (x > 1E-5) {
            if (probabilisticClicks && worldRand.nextFloat() > x) return 0;
            List<Integer> list = new ArrayList<>();
            if (x < 0.5) list.add(0);
            if (x < 1) list.add(1);
            if (x >= 0.5 && x < 2) list.add(2);
            // 1.7 wrote "x >= 1 && x >= 2": only x >= 2 reaches geiger3, and 1 <= x < 2 has geiger2 alone
            if (x >= 1 && x >= 2) list.add(3);
            return list.get(rand.nextInt(list.size()));
        } else if (rand.nextInt(100) == 0) {
            return 1;
        }
        return 0;
    }

    /** 1.7 {@code world.playSoundAtEntity(entity, sound, 1.0F, 1.0F)}: at the entity, heard by everyone near it. */
    private static void play(Level level, Entity entity, SoundEvent sound) {
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return hbm$use(level, player, hand);
    }

    private InteractionResult hbm$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            play(level, player, ModSounds.TECH_BOOP.get());
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
