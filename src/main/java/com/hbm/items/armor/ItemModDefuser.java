package com.hbm.items.armor;

import com.hbm.sound.ModDoorSounds;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;
import com.hbm.handler.ExplosivesEntityHooks;

/**
 * 1.7 {@code ItemModDefuser} ("Golden Wire Cutter"): a special-slot armor module that defuses every
 * creeper within 5 blocks once a second. 1.7 pulled the {@code EntityAICreeperSwell} task out of the
 * creeper; a mob's goal selector is not reachable here without a Golem seam, so a defused creeper is
 * remembered (in memory, like the removed task was) and its swell direction is forced back to -1 every
 * tick by {@link ExplosivesEntityHooks}, which comes to the same thing: it never reaches its fuse.
 */
public class ItemModDefuser extends ItemArmorMod {

    private static final Set<Creeper> DEFUSED = Collections.newSetFromMap(new WeakHashMap<>());

    public ItemModDefuser(Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Defuses nearby creepers").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(""));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.literal("  ").append(stack.getHoverName()).append(" (Defuses creepers)")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || entity.level().getGameTime() % 20 != 0) return;
        List<Creeper> creepers = entity.level().getEntitiesOfClass(Creeper.class, entity.getBoundingBox().inflate(5, 5, 5));
        for (Creeper creeper : creepers) castrateCreeper(creeper, entity, true);
    }

    /** "my bualls" - upstream. */
    public static boolean castrateCreeper(Creeper creeper, @Nullable LivingEntity entity, boolean dropItem) {
        creeper.setSwellDir(-1);
        if (creeper.level().isClientSide() || !(creeper.level() instanceof ServerLevel level)) return false;
        if (!DEFUSED.add(creeper)) return false;

        if (dropItem) {
            level.playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(), ModDoorSounds.PIN_BREAK.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            Compat.spawnAtLocation(creeper, level, new ItemStack(ModItems.safety_fuse.get()));
            Compat.hurt(creeper, level, entity != null ? level.damageSources().mobAttack(entity)
                    : level.damageSources().magic(), 1.0F);
            Effects.add(creeper, MobEffects.WEAKNESS, 200, 0);
        }
        return true;
    }

    /** Keeps a defused creeper from swelling; runs after its own tick so the goal's +1 is undone. */
    public static void tickDefused(Creeper creeper) {
        if (DEFUSED.contains(creeper) && creeper.getSwellDir() > 0) creeper.setSwellDir(-1);
    }

    public static boolean isDefused(Creeper creeper) {
        return DEFUSED.contains(creeper);
    }
}
