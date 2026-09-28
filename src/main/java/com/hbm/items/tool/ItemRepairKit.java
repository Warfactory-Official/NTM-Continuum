package com.hbm.items.tool;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.ItemStacks;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemRepairKit} (gun oil / gun repair kit): repairs a quarter of each configuration's maximum
 * durability on every worn gun in the hotbar, and wears by one use if anything was repaired. Gun oil has 10 uses
 * and sprays, the repair kit 100 and clanks.
 */
public class ItemRepairKit extends CompatItem {

    private final Supplier<SoundEvent> sound;

    public ItemRepairKit(Properties properties, Supplier<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        boolean didSomething = false;
        for (int i = 0; i < 9; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!(item.getItem() instanceof ItemGunBaseNT gun)) continue;
            int configs = gun.getConfigCount();
            for (int j = 0; j < configs; j++) {
                GunConfig cfg = gun.getConfig(item, j);
                float maxDura = cfg.getDurability(item);
                float wear = Math.min(ItemGunBaseNT.getWear(item, j), maxDura);
                if (wear > 0) {
                    ItemGunBaseNT.setWear(item, j, Math.max(0F, ItemGunBaseNT.getWear(item, j) - maxDura * 0.25F));
                    didSomething = true;
                }
            }
        }

        if (didSomething) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.get(), SoundSource.PLAYERS,
                    1.0F, 1.0F);
            ItemStacks.hurtAndBreak(kit, 1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
