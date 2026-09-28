package com.hbm.inventory.gui;

import com.hbm.items.tool.ItemSatInterface;
import com.hbm.packet.ItemControlPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code GUIScreenSatCoord}: {@code gui_satellites/gui_sat_coord.png} at 176x126 with three borderless white
 * seven-character fields (X at 66,21, Y at 66,56, Z at 66,92). Only while the held designator reports a
 * connection can the fields be focused and the button at (133,52) pressed; the button sends X, Z and (when it is
 * a number) Y as an item control tag and closes the screen. Two lamps at (120,17)/(120,25) show the connection.
 */
public class GUIScreenSatCoord extends CompatScreen {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/satellites/gui_sat_coord.png");
    protected int xSize = 176;
    protected int ySize = 126;
    protected int guiLeft;
    protected int guiTop;
    private final Player player;

    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public GUIScreenSatCoord(Player player) {
        super(Component.empty());
        this.player = player;
    }

    /** 1.7 kept the stack it was opened with; the held stack is re-read here since component updates replace it. */
    private ItemStack device() {
        return player.getMainHandItem();
    }

    private boolean connected() {
        ItemStack device = device();
        return device.getItem() instanceof ItemSatInterface && ItemSatInterface.isConnected(device);
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        this.xField = field(guiLeft + 66, guiTop + 21);
        this.yField = field(guiLeft + 66, guiTop + 56);
        this.zField = field(guiLeft + 66, guiTop + 92);
    }

    private EditBox field(int x, int y) {
        EditBox box = new EditBox(font, x, y, 48, 12, Component.empty());
        box.setTextColor(0xFFFFFFFF);
        box.setTextColorUneditable(0xFFFFFFFF);
        box.setBordered(false);
        box.setMaxLength(7);
        addRenderableWidget(box);
        return box;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, xSize, ySize, 256, 256);

        if (xField.isFocused()) GuiDraw.blit(graphics, TEXTURE, guiLeft + 61, guiTop + 16, 0F, 126F, 54, 18, 256, 256);
        if (yField.isFocused()) GuiDraw.blit(graphics, TEXTURE, guiLeft + 61, guiTop + 52, 0F, 126F, 54, 18, 256, 256);
        if (zField.isFocused()) GuiDraw.blit(graphics, TEXTURE, guiLeft + 61, guiTop + 88, 0F, 126F, 54, 18, 256, 256);

        if (connected()) {
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 120, guiTop + 17, 194F, 0F, 7, 7, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 120, guiTop + 25, 194F, 0F, 7, 7, 256, 256);
        }
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        // 1.7: without a connection the click goes nowhere, not even into a field
        if (!connected()) return true;

        double i = event.x(), j = event.y();
        if (i >= this.guiLeft + 133 && i < this.guiLeft + 133 + 18 && j >= this.guiTop + 52 && j < this.guiTop + 52 + 18) {

            if (isNumber(xField.getValue()) && isNumber(zField.getValue())) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                CompoundTag data = new CompoundTag();
                data.putInt("x", (int) Double.parseDouble(xField.getValue()));
                data.putInt("z", (int) Double.parseDouble(zField.getValue()));
                if (isNumber(yField.getValue())) data.putInt("y", (int) Double.parseDouble(yField.getValue()));
                GolemNetwork.sendToServer(ItemControlPayload.CODEC, new ItemControlPayload(data));

                Minecraft.getInstance().setScreen(null);
                return true;
            }
        }
        return false;
    }

    /** 1.7 {@code NumberUtils.isNumber}, as far as {@code Double.parseDouble} (what the click then calls) agrees. */
    private static boolean isNumber(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
