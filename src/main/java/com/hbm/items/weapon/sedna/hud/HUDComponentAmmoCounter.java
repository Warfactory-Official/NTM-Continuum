package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The source ammo icon/readout rail, including mirror and icon-only modes. */
public class HUDComponentAmmoCounter implements IHUDComponent {

    protected int receiver;
    protected boolean mirrored;
    protected boolean noCounter;

    public HUDComponentAmmoCounter(int receiver) {
        this.receiver = receiver;
    }

    public HUDComponentAmmoCounter mirror() {
        this.mirrored = true;
        return this;
    }

    public HUDComponentAmmoCounter noCounter() {
        this.noCounter = true;
        return this;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 24;
    }

    @Override
    public void renderHUDComponent(GuiGraphicsExtractor graphics, Player player, ItemStack stack, int bottomOffset,
                                   int configIndex) {
        boolean offhandOccupied = !player.getOffhandItem().isEmpty();
        int x = graphics.guiWidth() / 2 + (mirrored
                ? -(62 + 36 + 52 + (offhandOccupied ? 29 : 0))
                : (62 + 36)) + (noCounter ? 14 : 0);
        int y = graphics.guiHeight() - bottomOffset - 23;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> magazine = gun.getConfig(stack, configIndex).getReceivers(stack)[receiver].getMagazine(stack);

        if (!noCounter) {
            GuiDraw.text(graphics, Minecraft.getInstance().font,
                    magazine.reportAmmoStateForHUD(stack, player), x + 17, y + 6, 0xFFFFFFFF, false);
        }

        ItemStack icon = magazine.getIconForHUD(stack, player);
        if (!icon.isEmpty()) GuiDraw.item(graphics, icon, x, y);
    }
}
