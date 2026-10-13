package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockWandStructure.TileEntityWandStructure;
import com.hbm.packet.ControlEventPayload;
import com.hbm.world.gen.nbt.NBTStructure;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 1.7 {@code BlockWandStructure.GuiStructureLoad}: the file name field (y 50), a list of the {@code .nbt} files in
 * {@code <game dir>/structures} whose names contain the typed text ({@code GuiFileList}: 16 px rows from y 70 to
 * 90 above the bottom, names at +20, the matching file boxed), and a LOAD button 70 above the bottom. Clicking a
 * row fills the name; closing sends it, the button with {@code load} set.
 *
 * <p>1.7 drew the list with {@code GuiSlot}'s dirt-texture panels, which are not available in every supported
 * version; the list area is a dark panel instead.
 */
public final class GUIStructureLoad extends CompatScreen {

    private static final int SLOT_HEIGHT = 16;
    private static final int LIST_WIDTH = 220;

    private static String nameFilter = "";

    private final TileEntityWandStructure tile;

    private EditBox textName;
    private final List<String> files = new ArrayList<>();
    private int selectedId = -1;
    private double scroll;

    private GUIStructureWandButton performAction;

    private boolean loadOnClose = false;

    public GUIStructureLoad(TileEntityWandStructure tile) {
        super(Component.translatable("block.hbm.wand_structure.load"));
        this.tile = tile;
    }

    @Override
    protected void init() {
        textName = new EditBox(font, width / 2 - 150, 50, 300, 20, Component.empty());
        textName.setValue(tile.name);
        addRenderableWidget(textName);
        nameFilter = tile.name;

        refreshList();

        performAction = new GUIStructureWandButton(width / 2 - 150, height - 70, 300, 20, "LOAD");
    }

    private int top() { return 70; }
    private int bottom() { return height - 90; }

    private void refreshList() {
        files.clear();
        selectedId = -1;
        Path directory = NBTStructure.structureDirectory();
        try {
            Files.createDirectories(directory);
            try (Stream<Path> stream = Files.list(directory)) {
                stream.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".nbt") && (nameFilter.isEmpty() || n.contains(nameFilter)))
                        .forEach(files::add);
            }
        } catch (Exception ignored) {
            // an unreadable folder lists nothing, as File.listFiles returned null in 1.7
        }
        select(nameFilter);
    }

    private void select(String name) {
        for (int i = 0; i < files.size(); i++) {
            if (files.get(i).equals(name + ".nbt")) {
                selectedId = i;
                return;
            }
        }
    }

    private int rowY(int index) {
        return top() + 4 - (int) scroll + index * SLOT_HEIGHT;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fill(g, 0, top(), width, bottom(), 0xC0101010);

        int left = width / 2 - LIST_WIDTH / 2;
        for (int i = 0; i < files.size(); i++) {
            int y = rowY(i);
            if (y + SLOT_HEIGHT < top() || y > bottom() - 8) continue;
            if (i == selectedId) {
                GuiDraw.fill(g, left, y - 2, left + LIST_WIDTH, y + SLOT_HEIGHT - 2, 0xFF808080);
                GuiDraw.fill(g, left + 1, y - 1, left + LIST_WIDTH - 1, y + SLOT_HEIGHT - 3, 0xFF000000);
            }
            GuiDraw.text(g, font, files.get(i), left + 2 + 20, y + 1, 0xFFFFFFFF, true);
        }
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        performAction.draw(g, font, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedAfterWidgets(MouseInput event, boolean doubleClick) {
        if (performAction.isOver(event.x(), event.y())) {
            GUIStructureWandButton.click();
            loadOnClose = true;
            onClose();
            return true;
        }

        int left = width / 2 - LIST_WIDTH / 2;
        if (event.y() >= top() && event.y() < bottom() && event.x() >= left && event.x() < left + LIST_WIDTH) {
            for (int i = 0; i < files.size(); i++) {
                int y = rowY(i);
                if (event.y() >= y - 2 && event.y() < y + SLOT_HEIGHT - 2) {
                    String fileName = files.get(i);
                    textName.setValue(fileName.substring(0, fileName.length() - 4));
                    select(textName.getValue());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, files.size() * SLOT_HEIGHT - (bottom() - top() - 4));
        scroll = Mth.clamp(scroll - scrollY * SLOT_HEIGHT / 2.0, 0, max);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // 1.7 rebuilt the list whenever a key changed the name
        if (!nameFilter.equals(textName.getValue())) {
            nameFilter = textName.getValue();
            scroll = 0;
            refreshList();
        }
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        return false;
    }

    @Override
    public void removed() {
        CompoundTag data = new CompoundTag();
        data.putString("name", textName.getValue());

        if (loadOnClose) data.putBoolean("load", true);

        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(tile.getBlockPos(), data));
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
