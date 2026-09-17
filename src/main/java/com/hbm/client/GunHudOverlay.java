package com.hbm.client;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.hud.IHUDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Renders each gun config's source-shaped HUD component rail. */
public final class GunHudOverlay {

    private GunHudOverlay() {}

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || (minecraft.options.hideGui && minecraft.screen == null)) return;

        Player player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        ItemGunBaseNT gun;
        if (stack.getItem() instanceof ItemGunBaseNT mainGun) {
            gun = mainGun;
        } else {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof ItemGunBaseNT offhandGun)) return;
            gun = offhandGun;
        }

        for (int configIndex = 0; configIndex < gun.getConfigCount(); configIndex++) {
            IHUDComponent[] components = gun.getConfig(stack, configIndex).getHUDComponents(stack);
            int bottomOffset = 0;
            if (components == null) continue;
            for (IHUDComponent component : components) {
                component.renderHUDComponent(graphics, player, stack, bottomOffset, configIndex);
                bottomOffset += component.getComponentHeight(player, stack);
            }
        }
    }
}
