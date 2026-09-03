package com.hbm.client;

import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.gui.GuiDraw;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The power-armour charge bars, stacked above the vanilla armour row.
 *
 * <p>Ports 1.12.2's {@code ModEventHandlerClient.getBars} + {@code onHUDRenderBar}. One row per worn
 * piece that has something to report, drawn only while a full set is worn — {@code hasFSBArmorIgnoreCharge},
 * so a flat suit still shows four empty bars rather than vanishing.
 *
 * <p>Geometry is upstream's: 81 pixels wide starting at {@code width/2 - 91}, a one-pixel dark track
 * around a one-pixel fill, and each drawn row pushing the next up by 4.
 *
 * <p><b>Where the rows sit vertically</b> is the one thing that could not be ported literally — see
 * {@link #FIRST_ROW}. Upstream's {@code -10} when the player has no armour value is kept for the same
 * reason it exists: no armour row was drawn, so nothing reserved that space.
 *
 * <p><b>Hidden in creative</b>, unlike upstream. The drain skips creative entirely, so the bars there
 * would be four permanently full readouts of a number that never moves.
 *
 * <p><b>Deferred:</b> bars contributed by installed armour mods, and the fuelled-jetpack fuel bar. Both
 * belong to systems that are not ported, and both would appear as extra bars sharing a row — which is
 * what the {@code bars.size() > 1} splitting in the old code is for.
 */
public final class ArmorHudOverlay {

    private ArmorHudOverlay() {}

    /** Total width of one row's track, upstream's 81. */
    private static final int WIDTH = 81;

    /**
     * How far above the screen bottom the first bar row sits.
     *
     * <p>Vanilla stacks the left-hand column upwards: the hotbar, then the health row at
     * {@code height - 39}, then the armour row at {@code height - 49} occupying roughly ten pixels. The
     * bars belong <b>above</b> all of that, so the first row starts one row clear of the armour icons.
     *
     * <p>This is the piece that could not be ported literally. Upstream reads and advances Forge's
     * {@code GuiIngameForge.left_height}, which no other loader has; reconstructing it as "39 + 10" put
     * the bars <i>on top of</i> the armour icons, which is what the first playtest found.
     */
    private static final int FIRST_ROW = 53;
    /** Vertical pitch between rows, upstream's {@code left_height += 4}. */
    private static final int ROW_PITCH = 4;
    /** Upstream's dark track colour, 0.25 grey, with alpha. */
    private static final int TRACK = 0xFF404040;

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        ArmorFSB helmet = player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorFSB fsb ? fsb : null;
        if (helmet != null && helmet.overlay() != null) {
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            GuiDraw.blit(graphics, helmet.overlay(), 0, 0, 0F, 0F, width, height, 256, 256);
        }
        // Creative never spends charge — ArmorFSBPowered.armorTick returns early there — so the bars
        // would sit permanently full and tell the player nothing. Hiding them is a deliberate departure
        // from upstream, which draws them regardless; do not "restore" it for faithfulness.
        if (player.isCreative()) return;
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player)) return;

        List<Row> rows = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            Row row = barFor(player.getItemBySlot(slot));
            if (row != null) rows.add(row);
        }
        if (rows.isEmpty()) return;

        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int height = mc.getWindow().getGuiScaledHeight();

        // No armour value means vanilla drew no armour row, so nothing reserved that space and the bars
        // drop down into it. Upstream's own -10 adjustment, for the same reason.
        int first = FIRST_ROW - (player.getArmorValue() == 0 ? 10 : 0);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int top = height - first - i * ROW_PITCH;

            // The track, then the fill inset by one pixel on every side.
            GuiDraw.fill(graphics, left, top - 1, left + WIDTH, top + 2, TRACK);
            int end = left + 1 + Math.round((WIDTH - 1) * row.fill);
            if (end > left + 1) GuiDraw.fill(graphics, left + 1, top, end, top + 1, row.color);
        }

        // The suit's own HUD, if it has one. Empty for T-51b; HEV's readout plugs in here.
        ArmorFSB chestplate = ArmorFSB.chestplate(player);
        if (chestplate != null) chestplate.handleOverlay(graphics, player);
    }

    /** One bar's fill fraction and colour, or null if this piece reports nothing. */
    private static Row barFor(ItemStack stack) {
        if (stack.getItem() instanceof ArmorFSBPowered powered) {
            long max = powered.getMaxCharge(stack);
            float fill = max <= 0 ? 0F : (float) powered.getCharge(stack) / max;
            return new Row(Math.max(0F, Math.min(1F, fill)), 0xFF00FF00);
        }
        return null;
    }

    private record Row(float fill, int color) {}
}
