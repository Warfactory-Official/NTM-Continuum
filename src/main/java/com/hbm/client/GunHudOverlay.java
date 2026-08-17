package com.hbm.client;

import com.hbm.Constants;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The gun HUD: how many rounds are left, of what, and how worn the weapon is.
 *
 * <p>Ports 1.12.2's {@code HUDComponentAmmoCounter} and {@code HUDComponentDurabilityBar}. Upstream
 * builds these per gun as an {@code IHUDComponent[]} on the gun config and stacks them by height; with
 * only two components and no gun yet declaring a different set, they are drawn together here and the
 * per-gun component array becomes a follow-up when a weapon actually needs a different HUD.
 *
 * <p>The geometry is upstream's, including the {@code 62 + 36} offset from screen centre that puts the
 * readout just right of the hotbar, and the shift that makes room for an offhand item.
 */
public final class GunHudOverlay {

    private GunHudOverlay() {}

    private static final Identifier MISC =
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    /** Width of the durability bar's track, in pixels — upstream's 50 plus a 1px border each side. */
    private static final int BAR_WIDTH = 52;

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;

        Player player = mc.player;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        int centre = mc.getWindow().getGuiScaledWidth() / 2;
        int bottom = mc.getWindow().getGuiScaledHeight();
        int posX = centre + 62 + 36;

        for (int i = 0; i < gun.getConfigCount(); i++) {
            GunConfig config = gun.getConfig(stack, i);
            com.hbm.items.weapon.sedna.Receiver[] receivers = config.getReceivers(stack);
            if (receivers.length == 0) continue;

            IMagazine<BulletConfig> mag = receivers[0].getMagazine(stack);
            int rowBottom = bottom - i * 24;

            drawAmmoCounter(graphics, mc, player, stack, mag, posX, rowBottom - 23);
            drawDurabilityBar(graphics, stack, config, i, posX, rowBottom - 21);
        }
    }

    /** The loaded round's icon plus a "loaded / capacity" readout. */
    private static void drawAmmoCounter(GuiGraphicsExtractor graphics, Minecraft mc, Player player,
            ItemStack stack, IMagazine<BulletConfig> mag, int x, int y) {
        dev.golem.core.gui.GuiDraw.text(graphics, mc.font,
                mag.reportAmmoStateForHUD(stack, player), x + 17, y + 6, 0xFFFFFFFF, false);

        ItemStack icon = mag.getIconForHUD(stack, player);
        if (!icon.isEmpty()) dev.golem.core.gui.GuiDraw.item(graphics, icon, x, y);
    }

    /**
     * A bar that <em>empties</em> as the gun wears out. Upstream draws the full track and then covers
     * the remaining condition with a second blit whose width shrinks with wear, which is why the fill
     * is drawn at {@code 50 - wear} rather than at the wear itself.
     */
    private static void drawDurabilityBar(GuiGraphicsExtractor graphics, ItemStack stack,
            GunConfig config, int index, int x, int y) {
        float durability = config.getDurability(stack);
        if (durability <= 0F) return;

        int worn = (int) (50 * ItemGunBaseNT.getWear(stack, index) / durability);
        worn = Math.max(0, Math.min(50, worn));

        dev.golem.core.gui.GuiDraw.blit(graphics, MISC, x, y + 16, 94, 0, BAR_WIDTH, 3, 256, 256);
        dev.golem.core.gui.GuiDraw.blit(graphics, MISC, x + 1, y + 16, 95, 3, 50 - worn, 3, 256, 256);
    }
}
