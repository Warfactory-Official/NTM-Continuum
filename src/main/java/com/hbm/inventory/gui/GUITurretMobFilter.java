package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.gui.TextSearch;
import dev.golem.core.net.GolemNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/** The old 242x124 searchable living-mob filter screen. */
public final class GUITurretMobFilter extends CompatScreen {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_turret_mob_filter.png");
    private static final int GUI_WIDTH = 242;
    private static final int GUI_HEIGHT = 124;
    private static final int LIST_ROWS = 8;
    private static final int ROW_HEIGHT = 10;
    private static final float TEXT_SCALE = 0.7F;

    private final TileEntityTurretBaseNT turret;
    private final Map<String, EntityType<?>> entityTypes = new HashMap<>();
    private final List<String> mobList = new ArrayList<>();
    private final List<String> filteredMobList = new ArrayList<>();

    private int guiLeft;
    private int guiTop;
    private int mobScroll;
    private int filterScroll;
    private int mobSelected = -1;
    private int filterSelected = -1;
    private EditBox search;
    private long lastListClick;
    private int lastList;
    private int lastRow;

    public GUITurretMobFilter(TileEntityTurretBaseNT turret) {
        super(Component.empty());
        this.turret = turret;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null) continue;
            String key = id.toString();
            if (!TileEntityTurretBaseNT.isFilterableMob(key, type)) continue;
            entityTypes.put(key, type);
            mobList.add(key);
        }
        mobList.sort(String::compareTo);
        filteredMobList.addAll(mobList);
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.search = new EditBox(this.font, guiLeft + 8, guiTop + 14, 84, 14, Component.empty());
        this.search.setBordered(false);
        this.search.setTextColor(-1);
        this.search.setMaxLength(20);
        this.search.setFocused(true);
        this.search.setResponder(this::updateMobList);
        addRenderableWidget(this.search);
        setFocused(this.search);
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0F, 0F, GUI_WIDTH, GUI_HEIGHT, 256, 256);
        drawList(graphics, filteredMobList, mobScroll, mobSelected, 6, 31);
        drawList(graphics, turret.mobFilter, filterScroll, filterSelected, 132, 31);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick) {
        int textureX = turret.isBlacklistMobFilter ? 0 : 10;
        GuiDraw.blit(graphics, TEXTURE, guiLeft + 227, guiTop + 5,
                textureX, 124F, 10, 12, 256, 256);
    }

    private void drawList(GuiGraphicsExtractor graphics, List<String> values, int scroll, int selected,
                          int x, int y) {
        int first = Math.min(scroll, Math.max(0, values.size() - LIST_ROWS));
        for (int row = 0; row < LIST_ROWS && first + row < values.size(); row++) {
            int index = first + row;
            String value = fittingName(getMobName(values.get(index)));
            GuiDraw.pushMatrix(graphics);
            GuiDraw.translate(graphics, guiLeft + x + 2, guiTop + y + row * ROW_HEIGHT + 1);
            GuiDraw.scale(graphics, TEXT_SCALE, TEXT_SCALE);
            GuiDraw.text(graphics, font, value, 0, 0, index == selected ? 0xFFFFFFFF : 0xFFE0E0E0, false);
            GuiDraw.popMatrix(graphics);
        }
    }

    private String fittingName(String value) {
        int maxWidth = (int) (84 / TEXT_SCALE) - 2;
        if (font.width(value) <= maxWidth) return value;
        int length = value.length();
        while (length > 0 && font.width(value.substring(0, length)) > maxWidth) length--;
        return value.substring(0, length);
    }

    private String getMobName(String id) {
        EntityType<?> type = entityTypes.get(id);
        if (type == null) return id;
        String name = Component.translatable(type.getDescriptionId()).getString();
        return name.isEmpty() ? id : name;
    }

    private void updateMobList(String value) {
        filteredMobList.clear();
        for (String id : mobList) {
            String name = getMobName(id).toLowerCase(Locale.ROOT);
            if (id.toLowerCase(Locale.ROOT).contains(value) || TextSearch.contains(name, value)) {
                filteredMobList.add(id);
            }
        }
        mobSelected = -1;
        mobScroll = 0;
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();
        boolean mobDoubleClick = clickList(event, doubleClick, filteredMobList, 0, 6, 31);
        boolean filterDoubleClick = clickList(event, doubleClick, turret.mobFilter, 1, 132, 31);

        if (isOver(x, y, 227, 5, 10, 12)) {
            click();
            sendToggle(6);
            return true;
        }

        if (isOver(x, y, 112, 31, 18, 18) || mobDoubleClick) {
            click();
            if (mobSelected < 0 || mobSelected >= filteredMobList.size()) return true;
            CompoundTag data = new CompoundTag();
            data.putString("addMobFilter", filteredMobList.get(mobSelected));
            send(data);
            return true;
        }

        if (isOver(x, y, 112, 52, 18, 18) || filterDoubleClick) {
            click();
            if (filterSelected < 0 || filterSelected >= turret.mobFilter.size()) return true;
            CompoundTag data = new CompoundTag();
            data.putString("removeMobFilter", turret.mobFilter.get(filterSelected));
            send(data);
            filterSelected = turret.mobFilter.isEmpty() ? -1 : 0;
            filterScroll = 0;
            return true;
        }

        return isOver(x, y, 6, 31, 86, 83) || isOver(x, y, 132, 31, 86, 83);
    }

    private boolean clickList(MouseInput event, boolean doubleClick, List<String> values, int list,
                              int x, int y) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (!isOver(mouseX, mouseY, x, y, 86, 83)) return false;

        int row = (mouseY - (guiTop + y)) / ROW_HEIGHT;
        int scroll = list == 0 ? mobScroll : filterScroll;
        int selected = scroll + row;
        if (row < 0 || row >= LIST_ROWS || selected >= values.size()) return false;

        long now = System.currentTimeMillis();
        boolean repeated = list == lastList && row == lastRow && now - lastListClick < 250L;
        lastListClick = now;
        lastList = list;
        lastRow = row;
        if (list == 0) mobSelected = selected;
        else filterSelected = selected;
        return doubleClick || repeated;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOver((int) mouseX, (int) mouseY, 6, 31, 86, 83)) {
            mobScroll = scroll(mobScroll, filteredMobList.size(), scrollY);
            return true;
        }
        if (isOver((int) mouseX, (int) mouseY, 132, 31, 86, 83)) {
            filterScroll = scroll(filterScroll, turret.mobFilter.size(), scrollY);
            return true;
        }
        return false;
    }

    private int scroll(int current, int size, double amount) {
        int max = Math.max(0, size - LIST_ROWS);
        return Math.max(0, Math.min(max, current + (amount < 0 ? 1 : -1)));
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (search != null && search.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(search);
            return true;
        }
        if (event.key() != 256 && this.minecraft != null && event.matches(this.minecraft.options.keyInventory)) {
            onClose();
            return true;
        }
        return false;
    }

    private void sendToggle(int button) {
        CompoundTag data = new CompoundTag();
        data.putInt("toggle", button);
        send(data);
    }

    private void send(CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(turret.getBlockPos(), data));
    }

    private void click() {
        GuiUtil.playClickSound();
    }

    private boolean isOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= guiLeft + x && mouseX < guiLeft + x + width
                && mouseY >= guiTop + y && mouseY < guiTop + y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
