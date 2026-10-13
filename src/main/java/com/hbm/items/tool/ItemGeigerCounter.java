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
 * 1.7 {@code ItemGeigerCounter}: carried by any living entity, it clicks on every fifth world tick by the carrier's
 * environmental radiation buffer ({@code HbmLivingProps.getRadBuf}, the RAD/s of the last second, before armor):
 * {@code hbm:item.geiger1..6}, chosen at random from a ladder ({@link #click}); with no radiation one click tick in 50
 * plays geiger1. A right-click plays {@code hbm:item.tech_boop} and prints the GEIGER COUNTER readout
 * ({@link ContaminationUtil#printGeigerData}).
 *
 * <p>This is not 1.12.2's counter: 1.7 reads no armor-reduced dose plus neutron activation, ticks on no
 * per-player phase and rolls no 1-in-100 background click.
 */
public class ItemGeigerCounter extends CompatItem {

    /**
     * hbm-space {@code ItemGeigerCounter}: a click tick with a dose below 1 goes ahead only with that probability
     * ({@code world.rand.nextFloat() > x} returns before the ladder). Off in plain HBM (1.7); NTM: Space turns it on
     * (ADDON-API-INVENTORY V-188; the placed counter's gate is V-183, the dosimeter's V-186).
     */
    public static boolean probabilisticClicks = false;

    public ItemGeigerCounter(Properties props) {
        super(props);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                                       @Nullable EquipmentSlot slot, boolean held) {
        if (!(owner instanceof LivingEntity living)) return;
        float x = (float) HbmLivingProps.getRadBuf(living);
        // 1.7: world.getTotalWorldTime() % 5, the same tick for every carrier
        if (level.getGameTime() % 5 == 0) {
            int r = click(x, level.getRandom(), level.getRandom());
            if (r > 0) play(level, owner, ModSounds.GEIGER_SOUNDS[r - 1].get());
        }
    }

    /**
     * 1.7 {@code onUpdate}'s choice on a click tick: the geiger sound number 1..6, or 0 for silence. {@code rand} is
     * 1.7's item {@code Random} (the ladder and the background roll), {@code worldRand} hbm-space's {@code world.rand}
     * (the gate); the port passes the level's random for both.
     */
    public static int click(float x, RandomSource rand, RandomSource worldRand) {
        if (x > 1E-5) {
            if (probabilisticClicks && worldRand.nextFloat() > x) return 0;
            List<Integer> list = new ArrayList<>();
            if (x < 1) list.add(0);
            if (x < 5) list.add(0);
            if (x < 10) list.add(1);
            if (x > 5 && x < 15) list.add(2);
            if (x > 10 && x < 20) list.add(3);
            if (x > 15 && x < 25) list.add(4);
            if (x > 20 && x < 30) list.add(5);
            if (x > 25) list.add(6);
            return list.get(rand.nextInt(list.size()));
        } else if (rand.nextInt(50) == 0) {
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
            ContaminationUtil.printGeigerData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
