package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code ItemSwordAbility}: a sword whose main-hand attack modifier is exactly {@code damage} (plus a
 * movement modifier), with weapon abilities that run on every hit.
 */
public class ItemSwordAbility extends CompatSwordItem {

    protected final float damage;
    protected final double movement;
    private final AvailableAbilities abilities = new AvailableAbilities();
    /** 1.7 compared {@code this == ModItems.mese_gavel} in {@code hitEntity}; the gavel sets this instead. */
    private boolean gavelWhack;

    public ItemSwordAbility(Properties properties, float damage, double movement, ToolTier material) {
        this(properties, damage, Tools.SWORD_SPEED, movement, material);
    }

    /** With an explicit attack-speed modifier (old-hbm's crucible named none: 0). */
    protected ItemSwordAbility(Properties properties, float damage, float attackSpeed, double movement, ToolTier material) {
        super(properties, material, damage, attackSpeed, movement);
        this.damage = damage;
        this.movement = movement;
    }

    public ItemSwordAbility addAbility(IWeaponAbility weaponAbility, int level) {
        this.abilities.addAbility(weaponAbility, level);
        return this;
    }

    /** 1.7's "hacky hacky hack": the mese gavel whacks on every hit. */
    public ItemSwordAbility setGavel() {
        this.gavelWhack = true;
        return this;
    }

    public AvailableAbilities getAvailableAbilities() {
        return abilities;
    }

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel world && attacker instanceof Player player && canOperate(stack)) {
            if (gavelWhack) {
                world.playSound(null, victim.getX(), victim.getY(), victim.getZ(), ModGunSounds.WHACK.get(),
                        SoundSource.PLAYERS, 3.0F, 1.0F);
            }

            this.abilities.getWeaponAbilities().forEach((ability, level) ->
                    ability.onHit(level, world, player, victim, this));
        }
        spendOnHit(stack, attacker);
    }

    /** What a hit costs besides durability; the powered sword pays in charge. */
    protected void spendOnHit(ItemStack stack, LivingEntity attacker) {
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        abilities.addInformation(list);
    }

    protected boolean canOperate(ItemStack stack) {
        return true;
    }
}
