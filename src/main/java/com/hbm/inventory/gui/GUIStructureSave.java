package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockWandStructure.TileEntityWandStructure;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code BlockWandStructure.GuiStructureSave}: the file name (300 wide at y 50), the three sizes (50 wide at
 * y 100) and a SAVE button (y 150) over the dimmed world, no labels. Closing sends the name and sizes; the button
 * closes it with {@code save} set, which saves the box on the server.
 */
public final class GUIStructureSave extends CompatScreen {

    private final TileEntityWandStructure tile;

    private EditBox textName;

    private EditBox textSizeX;
    private EditBox textSizeY;
    private EditBox textSizeZ;

    private GUIStructureWandButton performAction;

    private boolean saveOnClose = false;

    public GUIStructureSave(TileEntityWandStructure tile) {
        super(Component.translatable("block.hbm.wand_structure.save"));
        this.tile = tile;
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    @Override
    protected void init() {
        textName = field(width / 2 - 150, 50, 300, tile.name);

        textSizeX = field(width / 2 - 150, 100, 50, "" + tile.sizeX);
        textSizeY = field(width / 2 - 100, 100, 50, "" + tile.sizeY);
        textSizeZ = field(width / 2 - 50, 100, 50, "" + tile.sizeZ);

        performAction = new GUIStructureWandButton(width / 2 - 150, 150, 300, 20, "SAVE");
    }

    /** 1.7 {@code drawDefaultBackground} in a world: the dark gradient over the scene. */
    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fillGradient(g, 0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        performAction.draw(g, font, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedAfterWidgets(MouseInput event, boolean doubleClick) {
        if (performAction.isOver(event.x(), event.y())) {
            GUIStructureWandButton.click();
            saveOnClose = true;
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();

        data.putString("name", textName.getValue());

        try { data.putInt("sizeX", Integer.parseInt(textSizeX.getValue())); } catch (Exception ex) {}
        try { data.putInt("sizeY", Integer.parseInt(textSizeY.getValue())); } catch (Exception ex) {}
        try { data.putInt("sizeZ", Integer.parseInt(textSizeZ.getValue())); } catch (Exception ex) {}

        if (saveOnClose) data.putBoolean("save", true);

        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(tile.getBlockPos(), data));
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
