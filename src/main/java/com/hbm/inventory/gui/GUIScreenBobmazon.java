package com.hbm.inventory.gui;

import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.handler.BobmazonOfferFactory.Offer;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.packet.ItemBobmazonPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIScreenBobmazon}: the 217x229 catalogue, three offers a page. Each offer is an 18x18 folder
 * button with the item on it; beside it the name (half scale), the rating bar, the price in caps, the comment
 * and the requirement's icon. Clicking an offer sends its index; the arrows page through.
 */
public class GUIScreenBobmazon extends CompatScreen {

    protected static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_bobmazon.png");
    protected final int xSize = 176 + 41;
    protected final int ySize = 229;
    protected int guiLeft;
    protected int guiTop;
    int currentPage = 0;
    final List<Offer> offers;
    final List<FolderButton> buttons = new ArrayList<>();

    public GUIScreenBobmazon(List<Offer> offers) {
        super(Component.empty());
        this.offers = offers;
    }

    int getPageCount() {
        // 1.7: Math.ceil of an integer division, i.e. (size - 1) / 3 rounded down
        return (offers.size() - 1) / 3;
    }

    @Override
    public void tick() {
        super.tick();
        if (currentPage < 0) currentPage = 0;
        if (currentPage > getPageCount()) currentPage = getPageCount();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() == ModMeleeKitItems.bobmazon_hidden.get()
                && player.getName().getString().equals("SolsticeUnlimitd")) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        updateButtons();
    }

    protected void updateButtons() {
        buttons.clear();

        for (int i = currentPage * 3; i < Math.min(currentPage * 3 + 3, offers.size()); i++) {
            buttons.add(new FolderButton(guiLeft + 34, guiTop + 35 + (54 * i) - currentPage * 3 * 54, offers.get(i)));
        }

        if (currentPage != 0)
            buttons.add(new FolderButton(guiLeft + 25 - 18, guiTop + 26 + (27 * 3), 1, "Previous"));
        if (currentPage != getPageCount())
            buttons.add(new FolderButton(guiLeft + 25 + (27 * 4) + 18 + 41, guiTop + 26 + (27 * 3), 2, "Next"));
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        int i = (int) event.x();
        int j = (int) event.y();
        for (FolderButton b : new ArrayList<>(buttons)) {
            if (b.isMouseOnButton(i, j)) {
                b.executeAction();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, xSize, ySize, 256, 256);

        for (FolderButton b : buttons) b.drawButton(graphics, b.isMouseOnButton(mouseX, mouseY));
        for (FolderButton b : buttons) b.drawIcon(graphics);

        for (int d = currentPage * 3; d < Math.min(currentPage * 3 + 3, offers.size()); d++) {
            drawRequirement(graphics, offers.get(d), guiLeft + 34, guiTop + 53 + (54 * d) - currentPage * 3 * 54);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String page = (currentPage + 1) + "/" + (getPageCount() + 1);
        GuiDraw.text(graphics, font, page, guiLeft + this.xSize / 2 - font.width(page) / 2, guiTop + 205, 0xFF404040, false);

        for (FolderButton b : buttons) {
            if (b.isMouseOnButton(mouseX, mouseY) && b.info != null && !b.info.isEmpty()) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal(b.info)), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /** 1.7 {@code Offer.drawRequirement}. */
    private void drawRequirement(GuiGraphicsExtractor graphics, Offer offer, int x, int y) {
        GuiDraw.blit(graphics, TEXTURE, x + 19, y - 4, 176 + 41, 62, 39, 8, 256, 256);
        if (offer.rating > 0) GuiDraw.blit(graphics, TEXTURE, x + 19, y - 4, 176 + 41, 54, offer.rating, 8, 256, 256);

        String count = offer.offer.getCount() > 1 ? " x" + offer.offer.getCount() : "";

        GuiDraw.pushMatrix(graphics);
        float scale = 0.5F;
        GuiDraw.scale(graphics, scale, scale);
        GuiDraw.text(graphics, font, Component.empty().append(offer.offer.getHoverName()).append(count),
                (int) ((x + 20) / scale), (int) ((y - 12) / scale), 0xFF404040, false);
        GuiDraw.popMatrix(graphics);

        String price = offer.cost + " Cap";
        if (offer.cost != 1) price += "s";
        GuiDraw.text(graphics, font, price, x + 62, y - 3, 0xFF404040, false);

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        if (!offer.author.isEmpty())
            GuiDraw.text(graphics, font, "- " + offer.author, (x + 20) * 2, (y + 18) * 2, 0xFF222222, false);
        GuiDraw.text(graphics, font, offer.comment, (x + 20) * 2, (y + 8) * 2, 0xFF222222, false);
        GuiDraw.popMatrix(graphics);

        GuiDraw.item(graphics, offer.requirement.icon(), x + 1, y + 1);
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    final class FolderButton {

        final int xPos;
        final int yPos;
        /** 0: regular, 1: prev, 2: next */
        final int type;
        final String info;
        final Offer offer;

        FolderButton(int x, int y, int t, String i) {
            xPos = x;
            yPos = y;
            type = t;
            info = i;
            offer = null;
        }

        FolderButton(int x, int y, Offer offer) {
            xPos = x;
            yPos = y;
            type = 0;
            info = null;
            this.offer = offer;
        }

        boolean isMouseOnButton(int mouseX, int mouseY) {
            return xPos <= mouseX && xPos + 18 > mouseX && yPos < mouseY && yPos + 18 >= mouseY;
        }

        void drawButton(GuiGraphicsExtractor graphics, boolean b) {
            GuiDraw.blit(graphics, TEXTURE, xPos, yPos, b ? 176 + 41 + 18 : 176 + 41, type == 1 ? 18 : (type == 2 ? 36 : 0),
                    18, 18, 256, 256);
        }

        void drawIcon(GuiGraphicsExtractor graphics) {
            if (offer != null) GuiDraw.item(graphics, offer.offer, xPos + 1, yPos + 1);
        }

        void executeAction() {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (type == 0) {
                GolemNetwork.sendToServer(ItemBobmazonPayload.CODEC, new ItemBobmazonPayload(offers.indexOf(offer)));
            } else if (type == 1) {
                if (currentPage > 0) currentPage--;
                updateButtons();
            } else if (type == 2) {
                if (currentPage < getPageCount()) currentPage++;
                updateButtons();
            }
        }
    }

    /** Opened by {@code ItemCatalog} on the client with the held catalogue's offers. */
    public static void open(net.minecraft.world.item.ItemStack held) {
        List<Offer> offers = BobmazonOfferFactory.getOffers(held);
        if (offers != null) Minecraft.getInstance().setScreen(new GUIScreenBobmazon(offers));
    }
}
