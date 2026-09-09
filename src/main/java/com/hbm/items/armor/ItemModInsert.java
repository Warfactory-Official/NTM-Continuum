package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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
        if (damageMod != 1F) tooltip.add(line((damageMod < 1F ? "-" : "+")
                + Math.abs(Math.round((1F - damageMod) * 100)) + "% Damage"));
        if (projectileMod != 1F) tooltip.add(line("-" + Math.round((1F - projectileMod) * 100) + "% Projectile Damage"));
        if (explosionMod != 1F) tooltip.add(line("-" + Math.round((1F - explosionMod) * 100) + "% Explosion Damage"));
        if (speed != 1F) tooltip.add(Component.literal("-" + Math.round((1F - speed) * 100)
                + "% Speed").withStyle(ChatFormatting.RED));
        tooltip.add(line("Durability: " + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage()));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    private static Component line(String value) {
        return Component.literal(value).withStyle(ChatFormatting.GREEN);
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
        if (damage >= insert.getMaxDamage()) ArmorModHandler.removeMod(armor, ArmorModHandler.kevlar);
        else {
            insert.setDamageValue(damage);
            ArmorModHandler.applyMod(armor, insert);
        }
    }

    public float speed() {
        return speed;
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        List<String> descriptions = new java.util.ArrayList<>();
        if (damageMod != 1F) descriptions.add((damageMod < 1F ? "-" : "+")
                + Math.abs(Math.round((1F - damageMod) * 100)) + "% dmg");
        if (projectileMod != 1F) descriptions.add("-" + Math.round((1F - projectileMod) * 100) + "% proj");
        if (explosionMod != 1F) descriptions.add("-" + Math.round((1F - explosionMod) * 100) + "% exp");
        // The old condition intentionally checked explosionMod here, not speed.
        if (explosionMod != 1F) descriptions.add("-" + Math.round((1F - speed) * 100) + "% speed");
        String details = String.join(" / ", descriptions);
        tooltip.add(Component.literal("  ").append(stack.getHoverName().copy().withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" (" + details + " / " + (stack.getMaxDamage() - stack.getDamageValue())
                        + "HP)").withStyle(ChatFormatting.DARK_PURPLE)));
    }
}
