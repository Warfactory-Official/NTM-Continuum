package com.hbm.items.weapon;

import com.hbm.items.LegacyMeta;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.data.Inventories;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Source ntm-next ammo container: supplies default ammunition for up to three carried Sedna guns. 1.7 metadata 1 (the
 * constrained box that skips expensive ammo) is its own item, {@code ammo_container_alt}, as in ntm-next.
 */
public final class ItemAmmoContainer extends CompatItem implements LegacyMeta {

    private final boolean constrained;

    public ItemAmmoContainer(Properties properties, boolean constrained) {
        super(properties.stacksTo(1));
        this.constrained = constrained;
    }

    @Override
    public int legacyMeta() {
        return constrained ? 1 : 0;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        ItemStack container = player.getItemInHand(hand);
        List<ItemGunBaseNT> guns = new ArrayList<>();
        for (ItemStack carried : Inventories.main(player.getInventory())) {
            if (!(carried.getItem() instanceof ItemGunBaseNT gun)
                    || gun.defaultAmmo == null
                    || constrained && gun.isDefaultExpensive) continue;
            guns.add(gun);
        }
        if (guns.isEmpty()) return InteractionResult.SUCCESS;

        Collections.shuffle(guns);
        for (int index = 0; index < Math.min(3, guns.size()); index++) {
            ItemGunBaseNT gun = guns.get(index);
            ItemStack ammo = new ItemStack(gun.defaultAmmo.get(), gun.defaultAmmoAmount);
            if (constrained) ammo.setCount((int) Math.ceil(ammo.getCount() / 2D));
            if (!player.getInventory().add(ammo)) player.drop(ammo, false);
        }
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.ITEM_UNPACK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        container.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String key = constrained ? "item.hbm.ammo_container_1.desc.0" : "item.hbm.ammo_container.desc.0";
        for (String line : Component.translatable(key).getString().split("\\$", -1)) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
