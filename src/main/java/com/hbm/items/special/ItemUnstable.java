package com.hbm.items.special;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The metastable U-238M2 ingot: it counts down while carried and then detonates as a nuke on the carrier.
 * The three "ELEMENTS / ARSENIC / VAULT" metadata easter eggs of the old item (magic recipes) are not ported.
 */
public class ItemUnstable extends CompatItem {

    public static final GolemComponent<Integer> TIMER = GolemComponent.integer("hbm", "unstable_timer");

    private final int radius;
    private final int timer;

    public ItemUnstable(Properties properties, int radius, int timer) {
        super(properties);
        this.radius = radius;
        this.timer = timer;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Decay: " + (getTimer(stack) * 100 / timer) + "%"));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot, boolean held) {
        setTimer(stack, getTimer(stack) + 1);

        if (getTimer(stack) == timer) {
            EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(level, radius, entity.getX(), entity.getY(), entity.getZ());
            if (level.addFreshEntity(blast)) blast.startChunkLoading();
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.OLD_EXPLOSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Compat.hurt(entity, level, ModDamageSources.source(level, ModDamageSources.NUCLEAR_BLAST), 10000);
            stack.setCount(0);
        }
    }

    private static void setTimer(ItemStack stack, int time) {
        TIMER.set(stack, time);
    }

    private static int getTimer(ItemStack stack) {
        return TIMER.getOr(stack, 0);
    }
}
