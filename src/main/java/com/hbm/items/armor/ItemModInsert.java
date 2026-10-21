package com.hbm.items.armor;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorModules;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Explosions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Ballistic insert with the legacy damage multipliers and finite module durability. */
public final class ItemModInsert extends ItemArmorMod {

    private final float damageMod;
    private final float projectileMod;
    private final float explosionMod;
    private final float speed;

    public ItemModInsert(int durability, float damageMod, float projectileMod, float explosionMod, float speed,
                         Item.Properties properties) {
        super(ArmorModHandler.kevlar, false, true, false, false, properties.durability(durability));
        this.damageMod = damageMod;
        this.projectileMod = projectileMod;
        this.explosionMod = explosionMod;
        this.speed = speed;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (damageMod != 1F) tooltip.add(line(ChatFormatting.RED, (damageMod < 1F ? "-" : "+")
                + Math.abs(Math.round((1F - damageMod) * 100)) + "% damage"));
        if (projectileMod != 1F) tooltip.add(line(ChatFormatting.YELLOW,
                "-" + Math.round((1F - projectileMod) * 100) + "% projectile damage"));
        if (explosionMod != 1F) tooltip.add(line(ChatFormatting.YELLOW,
                "-" + Math.round((1F - explosionMod) * 100) + "% explosion damage"));
        if (speed != 1F) tooltip.add(line(ChatFormatting.BLUE, "-" + Math.round((1F - speed) * 100) + "% speed"));
        if (isPolonium()) tooltip.add(line(ChatFormatting.DARK_RED, "+2000 RAD/s"));
        tooltip.add(Component.literal((stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + "HP"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    private static Component line(ChatFormatting color, String value) {
        return Component.literal(value).withStyle(color);
    }

    private boolean isPolonium() {
        return this == ModArmorModules.insert_polonium.get();
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        float amount = event.amount() * damageMod;
        if (event.source().is(DamageTypeTags.IS_PROJECTILE)) amount *= projectileMod;
        if (event.source().is(DamageTypeTags.IS_EXPLOSION)) amount *= explosionMod;
        event.setAmount(amount);
        ItemStack insert = ArmorModHandler.pryMod(armor, ArmorModHandler.kevlar);
        if (insert.isEmpty()) return;
        int damage = insert.getDamageValue() + 1;
        Level level = event.entity().level();
        if (!level.isClientSide() && this == ModArmorModules.insert_era.get()) {
            Entity entity = event.entity();
            Explosions.explode(level, entity, entity.getX(), entity.getY(0.5D), entity.getZ(), 0.05F,
                    Level.ExplosionInteraction.NONE);
        }
        if (damage >= insert.getMaxDamage()) ArmorModHandler.removeMod(armor, ArmorModHandler.kevlar);
        else {
            insert.setDamageValue(damage);
            ArmorModHandler.applyMod(armor, insert);
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        // port: 1.7 adds 100 per tick but its tooltip says 100 per second; the tooltip shows the real rate.
        if (!entity.level().isClientSide() && isPolonium()) HbmLivingProps.incrementRadiation(entity, 100D);
    }

    @Override
    public void collectModifiers(ItemStack armor, ArmorSlot slot, List<Modifier> modifiers) {
        if (speed != 1F) modifiers.add(new Modifier(Modifier.MOVEMENT_SPEED, -1F + speed, Attributes.Op.MULTIPLY_TOTAL));
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        List<String> descriptions = new ArrayList<>();
        if (damageMod != 1F) descriptions.add((damageMod < 1F ? "-" : "+")
                + Math.abs(Math.round((1F - damageMod) * 100)) + "% dmg");
        if (projectileMod != 1F) descriptions.add("-" + Math.round((1F - projectileMod) * 100) + "% proj");
        if (explosionMod != 1F) descriptions.add("-" + Math.round((1F - explosionMod) * 100) + "% exp");
        // 1.7 gates the speed entry on explosionMod, not speed
        if (explosionMod != 1F) descriptions.add("-" + Math.round((1F - speed) * 100) + "% speed");
        if (isPolonium()) descriptions.add("+2000 RAD/s");
        String details = String.join(" / ", descriptions);
        tooltip.add(Component.literal("  ").append(stack.getHoverName().copy().withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" (" + details + " / " + (stack.getMaxDamage() - stack.getDamageValue())
                        + "HP)").withStyle(ChatFormatting.DARK_PURPLE)));
    }
}
