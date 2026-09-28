package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockWandJigsaw.TileEntityWandJigsaw;
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
 * 1.7 {@code BlockWandJigsaw.GuiWandJigsaw}: target pool, name / target name, selection / placement priority and
 * the joint-type toggle over the dimmed world, at 1.7's coordinates. Closing it (any way) sends the fields.
 */
public final class GUIWandJigsaw extends CompatScreen {

    private final TileEntityWandJigsaw jigsaw;

    private EditBox textPool;
    private EditBox textName;
    private EditBox textTarget;

    private EditBox textSelectionPriority;
    private EditBox textPlacementPriority;

    private GUIStructureWandButton jointToggle;

    public GUIWandJigsaw(TileEntityWandJigsaw jigsaw) {
        super(Component.translatable("block.hbm.wand_jigsaw"));
        this.jigsaw = jigsaw;
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    @Override
    protected void init() {
        textPool = field(this.width / 2 - 150, 50, 300, jigsaw.pool);
        textName = field(this.width / 2 - 150, 100, 140, jigsaw.name);
        textTarget = field(this.width / 2 + 10, 100, 140, jigsaw.target);
        textSelectionPriority = field(this.width / 2 - 150, 150, 90, "" + jigsaw.selectionPriority);
        textPlacementPriority = field(this.width / 2 - 40, 150, 90, "" + jigsaw.placementPriority);

        jointToggle = new GUIStructureWandButton(this.width / 2 + 60, 150, 90, 20, jigsaw.isRollable ? "Rollable" : "Aligned");
    }

    /** 1.7 {@code drawDefaultBackground} in a world: the dark gradient over the scene. */
    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fillGradient(g, 0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.text(g, font, "Target pool:", this.width / 2 - 150, 37, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Name:", this.width / 2 - 150, 87, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Target name:", this.width / 2 + 10, 87, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Selection priority:", this.width / 2 - 150, 137, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Placement priority:", this.width / 2 - 40, 137, 0xFFA0A0A0, true);
        GuiDraw.text(g, font, "Joint type:", this.width / 2 + 60, 137, 0xFFA0A0A0, true);
        jointToggle.draw(g, font, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedAfterWidgets(MouseInput event, boolean doubleClick) {
        if (jointToggle.isOver(event.x(), event.y())) {
            GUIStructureWandButton.click();
            jointToggle.label = jointToggle.label.equals("Rollable") ? "Aligned" : "Rollable";
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();

        data.putString("pool", textPool.getValue());
        data.putString("name", textName.getValue());
        data.putString("target", textTarget.getValue());

        try { data.putInt("selection", Integer.parseInt(textSelectionPriority.getValue())); } catch (Exception ex) {}
        try { data.putInt("placement", Integer.parseInt(textPlacementPriority.getValue())); } catch (Exception ex) {}

        data.putBoolean("roll", jointToggle.label.equals("Rollable"));

        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(jigsaw.getBlockPos(), data));
        super.removed();
    }
}
