package com.hbm.inventory.gui;

import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.packet.ItemControlPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIScreenPager}: {@code gui_rtty_pager.png}, 184x42, a green ten-character channel field
 * ({@code setupTextFieldStandard}) and a save button at (137, 17) that sends the channel as an item control tag.
 */
public class GUIScreenPager extends CompatScreen {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_rtty_pager.png");
    public final String startingChannel;
    protected int xSize = 184;
    protected int ySize = 42;
    protected int guiLeft;
    protected int guiTop;

    protected EditBox chan;

    public GUIScreenPager(ItemStack pager) {
        super(Component.translatable("container.rttyPager"));
        String start = pager == null ? null : ItemRTTYPager.channel(pager);
        this.startingChannel = start == null ? "" : start;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        int oX = 4;
        int oY = 4;

        chan = new EditBox(font, guiLeft + 27 + oX, guiTop + 19 + oY, 90 - oX * 2, 14, Component.empty());
        chan.setBordered(false);
        chan.setTextColor(0xFF00FF00);
        chan.setTextColorUneditable(0xFF00FF00);
        chan.setMaxLength(10);
        chan.setValue(startingChannel);
        addRenderableWidget(chan);
        focusedEditBoxForInput = chan;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, xSize, ySize, 256, 256);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
        String name = title.getString();
        GuiDraw.text(graphics, font, name, this.guiLeft + this.xSize / 2 - font.width(name) / 2, this.guiTop + 6,
                0xFF404040, false);
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        double x = event.x(), y = event.y();
        if (guiLeft + 137 <= x && guiLeft + 137 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            CompoundTag data = new CompoundTag();
            data.putString(ItemRTTYPager.KEY_CHANNEL, this.chan.getValue());
            GolemNetwork.sendToServer(ItemControlPayload.CODEC, new ItemControlPayload(data));
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (chan != null && chan.isFocused() && event.key() != 256) return false;
        if (event.key() == 256 || event.matches(mc.options.keyInventory)) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override public boolean isPauseScreen() { return false; }
}
