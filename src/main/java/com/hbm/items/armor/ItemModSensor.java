package com.hbm.items.armor;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * 1.7 {@code ItemModSensor} (Gas Sensor): once a second, samples a 7x3x7 lattice (every second block)
 * around the head and beeps near poisonous gas. Works carried or installed.
 *
 * <p>Upstream played {@code hbm:weapon.follyAquired} for flammable/explosive gas, but that event had no
 * {@code sounds.json} entry, so it was silent - and, being checked first, it also suppressed the poison
 * beep. The port keeps that: explosive gas is detected and nothing is heard.
 */
public class ItemModSensor extends ItemArmorMod {

    public ItemModSensor(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.YELLOW, "Beeps near hazardous gasses"));
        tooltip.add(coloured(ChatFormatting.YELLOW, "Works in the inventory or when applied to armor"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.YELLOW, stack, " (Detects gasses)"));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot,
                                       boolean held) {
        if (owner instanceof LivingEntity living) modUpdate(living, ItemStack.EMPTY);
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || entity.level().getGameTime() % 20 != 0) return;

        int x = Mth.floor(entity.getX());
        int y = Mth.floor(entity.getEyeY());
        int z = Mth.floor(entity.getZ());

        boolean poison = false;
        boolean explosive = false;

        for (int i = -3; i <= 3; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -3; k <= 3; k++) {
                    Block b = entity.level().getBlockState(new BlockPos(x + i * 2, y + j * 2, z + k * 2)).getBlock();
                    if (b == ModNuclearBlocks.gas_asbestos.get() || b == ModTerrainBlocks.gas_coal.get()
                            || b == ModNuclearBlocks.gas_radon.get() || b == ModNuclearBlocks.gas_monoxide.get()
                            || b == ModNuclearBlocks.gas_radon_dense.get() || b == ModTerrainBlocks.chlorine_gas.get()) {
                        poison = true;
                    }
                    if (b == ModOreBlocks.gas_flammable.get() || b == ModOreBlocks.gas_explosive.get()) {
                        explosive = true;
                    }
                }
            }
        }

        if (!explosive && poison) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 2F, 1.5F);
        }
    }
}
