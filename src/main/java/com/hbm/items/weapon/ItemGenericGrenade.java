package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityGrenadeBouncyGeneric;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// 1.7: ItemGenericGrenade throws an EntityGrenadeBouncyGeneric that asks the item for its fuse, bounce and
// detonation behavior.
// 26.2's ProjectileItem (the dispenser hook) is 1.20.5+; 1.7 registered no dispenser behaviour for these,
// so the port carries none.
public class ItemGenericGrenade extends ItemGrenade {

    public ItemGenericGrenade(Properties properties, int fuse) {
        super(properties, fuse);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 1.7: "random.bow", pitch 0.4 / (rand*0.4 + 0.8).
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            level.addFreshEntity(new EntityGrenadeBouncyGeneric(level, player).setType(this));
            dev.golem.core.world.ItemStacks.consume(stack, 1, player);
        }

        return InteractionResult.SUCCESS;
    }

    public void explode(Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
    }

    public int getMaxTimer() {
        return this.fuse * 20;
    }

    public double getBounceMod() {
        return 0.5D;
    }


    /**
     * Bare sprite name (no item/ prefix - Sheets.ITEMS_MAPPER adds it) of the icon RenderGenericGrenade billboards, standing in for 1.7's getIconFromDamage(0); both shipped grenades declare a 1.7 setTextureName equal to their item name, so the registry id is the sprite.
     */
    public Identifier iconTexture() {
        return BuiltInRegistries.ITEM.getKey(this);
    }
}
