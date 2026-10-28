package com.hbm.inventory.gui;

import com.hbm.lib.Library;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import com.hbm.inventory.container.ContainerPneumoStorageAccess.ClickOp;
import com.hbm.inventory.container.ContainerPneumoStorageAccess.SlotPneumo;
import com.hbm.packet.PneumoAccessInputPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 1.7 {@code GUIPneumoStorageAccess}: the storage network's terminal - a scrolled, searched and sorted listing of
 * everything the network can reach, with the sort/search toggles on the side panel.
 */
public class GUIPneumoStorageAccess extends PneumaticScreenBase<ContainerPneumoStorageAccess> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_access.png");
    private static final int PANEL_X = 34;
    private static final int SCROLL_BAR_X = 154;
    private static final int SCROLL_TRAVEL = 106 - 15; // bar height minus the scroll knob's height
    private static final int DRAG_TRAVEL = 92; // 106 scroll bar size, -7 pixels on top and bottom

    // 1.7 keeps all three across openings
    private static int sorting = 0;
    private static boolean startFocussed = false;
    private static boolean detailedSearch = false;

    private EditBox search;
    private int scrollBounds = 1;
    private boolean draggingScroll;

    public GUIPneumoStorageAccess(ContainerPneumoStorageAccess menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176 + PANEL_X, 251);
    }

    @Override
    protected void init() {
        super.init();

        // a re-init (resize, a recipe viewer closing) keeps the search and asks the server for the whole listing
        boolean reinit = this.search != null;
        String text = reinit ? this.search.getValue() : "";
        if (reinit) send(ClickOp.RESYNC, ContainerPneumoStorageAccess.NO_ENTRY);

        // 1.7 draws the field two pixels below its own y
        this.search = new EditBox(this.font, leftPos + PANEL_X + 45, topPos + 129, 86, 12, Component.empty());
        this.search.setBordered(false);
        this.search.setTextColor(0xFFFFFFFF);
        this.search.setMaxLength(50);
        this.search.setValue(text);
        this.search.setResponder(term -> menu.setFilter(searchFilter(term)));
        addRenderableWidget(this.search);

        if (startFocussed) setFocused(this.search);

        menu.setSorter(sorterFor(sorting));
        menu.setFilter(searchFilter(text));
    }

    private Predicate<ItemStack> searchFilter(String term) {
        if (term.isEmpty()) return stack -> true;
        String needle = term.toLowerCase(Locale.US);

        return stack -> {
            if (stack.getHoverName().getString().toLowerCase(Locale.US).contains(needle)) return true;
            if (!detailedSearch) return false;
            for (Component line : getTooltipFromContainerItem(stack)) {
                if (line.getString().toLowerCase(Locale.US).contains(needle)) return true;
            }
            return false;
        };
    }

    private static Comparator<ContainerPneumoStorageAccess.Entry> sorterFor(int index) {
        return switch (index) {
            case 1 -> ContainerPneumoStorageAccess.SORT_BY_ID;
            case 2 -> ContainerPneumoStorageAccess.SORT_BY_LOCALIZED;
            case 3 -> ContainerPneumoStorageAccess.SORT_BY_INTERNAL;
            default -> ContainerPneumoStorageAccess.SORT_BY_STACK_SIZE;
        };
    }

    private static String onOff(boolean on) {
        return on ? ChatFormatting.GREEN + "ON" : ChatFormatting.RED + "OFF";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.scrollBounds = Math.max(1, (int) Math.ceil(menu.listingSize() / 8.0D - 6.0D));
        if (menu.listingStart() > scrollBounds) setScroll(scrollBounds);

        // 1.7 polled the mouse button while drawing; the drag is tracked between press and release here
        if (draggingScroll) {
            int sY = Mth.clamp(mouseY - topPos - 24, 0, DRAG_TRAVEL);
            double scrollFrac = (double) sY / (double) DRAG_TRAVEL;
            setScroll((int) Math.round(scrollBounds * scrollFrac));
        }

        GuiDraw.blit(graphics, TEXTURE, leftPos + PANEL_X, topPos, 0, 0, 176, imageHeight, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 176, 15, 32, 122, 256, 256);

        GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 7 + sorting * 18, 208, 0, 18, 18, 256, 256);
        if (startFocussed) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 79, 208, 18, 18, 18, 256, 256);
        if (detailedSearch) GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 97, 208, 18, 18, 18, 256, 256);

        GuiDraw.blit(graphics, TEXTURE, leftPos + PANEL_X + SCROLL_BAR_X, topPos + scrollKnobY(),
                draggingScroll ? 188 : 176, 0, 12, 15, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, PANEL_X + 176 / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, PANEL_X + 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    /** The totals ride over the stacks at half scale in each slot's corner; 26.1 draws the label hook before the slots. */
    private void extractTotals(GuiGraphicsExtractor graphics) {
        GuiDraw.pushMatrix(graphics);
        GuiDraw.aboveSlots(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        for (Slot slot : menu.slots) {
            if (!(slot instanceof SlotPneumo pneumo) || !pneumo.hasItem()) continue;
            String label = Library.getShortNumber(pneumo.amount);
            int ix = (leftPos + pneumo.x + 16) * 2 - font.width(label);
            int iy = (topPos + pneumo.y + 16) * 2 - font.lineHeight;
            GuiDraw.text(graphics, font, label, ix, iy, 0xFFFFFFFF, true);
        }
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        extractTotals(graphics);
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 7, 18, 18, "Sorting: " + ChatFormatting.YELLOW + "Amount");
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 25, 18, 18, "Sorting: " + ChatFormatting.YELLOW + "Item ID");
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 43, 18, 18, "Sorting: " + ChatFormatting.YELLOW + "Name");
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 61, 18, 18, "Sorting: " + ChatFormatting.YELLOW + "Internal Name");
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 79, 18, 18, "Focus search by default: " + onOff(startFocussed));
        drawCustomInfoStat(graphics, mouseX, mouseY, 7, 97, 18, 18, "Inlude tooltips in search: " + onOff(detailedSearch));
    }

    private int scrollKnobY() {
        double progress = (double) menu.listingStart() / scrollBounds;
        return 17 + (int) (Math.min(1.0D, progress) * SCROLL_TRAVEL);
    }

    private void setScroll(int scroll) {
        int clamped = Mth.clamp(scroll, 0, scrollBounds);
        if (clamped != menu.listingStart()) menu.setListingStart(clamped);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        double x = event.x(), y = event.y();

        if (hover(PANEL_X + 153, 16, 14, 108, x, y)) draggingScroll = true;

        if (hover(7, 7, 18, 18, x, y)) { sort(0); return true; }
        if (hover(7, 25, 18, 18, x, y)) { sort(1); return true; }
        if (hover(7, 43, 18, 18, x, y)) { sort(2); return true; }
        if (hover(7, 61, 18, 18, x, y)) { sort(3); return true; }

        if (hover(7, 79, 18, 18, x, y)) {
            GuiUtil.playClickSound();
            startFocussed = !startFocussed;
            return true;
        }
        if (hover(7, 97, 18, 18, x, y)) {
            GuiUtil.playClickSound();
            detailedSearch = !detailedSearch;
            menu.setFilter(searchFilter(search.getValue()));
            return true;
        }

        // a grid slot is not an inventory slot: the click names a type to the terminal
        if (event.button() <= 1) {
            for (int i = 0; i < ContainerPneumoStorageAccess.GRID_SIZE; i++) {
                Slot slot = menu.slots.get(i);
                if (!(slot instanceof SlotPneumo pneumo) || !hover(slot.x, slot.y, 16, 16, x, y)) continue;
                gridClick(pneumo, event.button() == 1, event.hasShiftDown());
                return true;
            }
        }

        return false;
    }

    /** 1.7 {@code ContainerPneumoStorageAccess.slotClick}'s client arm: predict the cursor, then tell the server. */
    private void gridClick(SlotPneumo pneumo, boolean rightClick, boolean shift) {
        int entry = pneumo.entryId;

        // 1.7 sends only a shift-left click; a shift-right click on the grid does nothing
        if (shift) {
            if (!rightClick) send(ClickOp.SHIFT_CLICK, entry);
            return;
        }

        ItemStack carried = menu.getCarried();
        boolean sameType = pneumo.hasItem() && !carried.isEmpty() && ItemStacks.sameItemAndData(pneumo.getItem(), carried);

        if (!carried.isEmpty() && !sameType) {
            menu.setCarried(rightClick ? carried.copyWithCount(carried.getCount() - 1) : ItemStack.EMPTY);
        } else if (!carried.isEmpty()) {
            long grab = rightClick ? 1 : pneumo.amount;
            menu.setCarried(carried.copyWithCount(
                    (int) Math.min(carried.getMaxStackSize(), carried.getCount() + grab)));
        } else if (pneumo.hasItem()) {
            ItemStack stack = pneumo.getItem();
            menu.setCarried(stack.copyWithCount(
                    rightClick ? 1 : (int) Math.min(pneumo.amount, stack.getMaxStackSize())));
        }

        ItemStack after = menu.getCarried();
        if (!after.isEmpty() && after.getCount() <= 0) menu.setCarried(ItemStack.EMPTY);

        send(rightClick ? ClickOp.RIGHT_CLICK : ClickOp.LEFT_CLICK, entry);
    }

    private void send(ClickOp op, int entryId) {
        GolemNetwork.sendToServer(PneumoAccessInputPayload.CODEC,
                new PneumoAccessInputPayload(menu.containerId, op.ordinal(), entryId));
    }

    private void sort(int index) {
        GuiUtil.playClickSound();
        sorting = index;
        menu.setSorter(sorterFor(index));
    }

    @Override
    protected boolean mouseReleased(MouseInput event) {
        draggingScroll = false;
        return false;
    }

    @Override
    protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && hover(0, 0, imageWidth, imageHeight, mouseX, mouseY)) {
            setScroll(menu.listingStart() - (int) Math.signum(scrollY));
            return true;
        }
        return false;
    }
}
