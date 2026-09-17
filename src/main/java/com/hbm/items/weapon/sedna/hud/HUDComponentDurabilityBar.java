package com.hbm.items.weapon.sedna.hud;

import com.hbm.Constants;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The source durability rail, including its optional mirrored placement. */
public class HUDComponentDurabilityBar implements IHUDComponent {

    private static final Identifier MISC = Ids.of(Constants.MOD_ID, "textures/misc/overlay_misc.png");

    protected boolean mirrored;

    public HUDComponentDurabilityBar() {
        this(false);
    }

    public HUDComponentDurabilityBar(boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 5;
    }

    @Override
    public void renderHUDComponent(GuiGraphicsExtractor graphics, Player player, ItemStack stack, int bottomOffset,
                                   int configIndex) {
        // Hud.addLast is the modern equivalent of the source HOTBAR pass.
        boolean offhandOccupied = !player.getOffhandItem().isEmpty();
        int x = graphics.guiWidth() / 2 + (mirrored
                ? -(62 + 36 + 52 + (offhandOccupied ? 29 : 0))
                : (62 + 36));
        int y = graphics.guiHeight() - 21;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int wear = (int) (50 * ItemGunBaseNT.getWear(stack, configIndex)
                / gun.getConfig(stack, configIndex).getDurability(stack));

        GuiDraw.blit(graphics, MISC, x, y + 16, 94F, 0F, 52, 3, 256, 256);
        GuiDraw.blit(graphics, MISC, x + 1, y + 16, 95F, 3F, 50 - wear, 3, 256, 256);
    }
}
