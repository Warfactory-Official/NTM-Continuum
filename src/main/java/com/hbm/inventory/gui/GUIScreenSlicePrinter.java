package com.hbm.inventory.gui;

import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

/**
 * The construction-diagram printer (1.7 {@code GUIScreenSlicePrinter}): draws one horizontal slice of the
 * synced box per pass, saves it as an image, and closes after the top slice.
 *
 * <p><b>Deviation:</b> 1.7 rendered each slice as an isometric block render through {@code RenderBlocks} and
 * chroma-keyed the magenta background out of the saved PNG. A block-model render inside a GUI is a
 * per-version renderer, so the slice is drawn as a top-down grid of the blocks' item icons over the same
 * magenta, and captured with vanilla's {@link Screenshot#grab} (the only screenshot entry point with the same
 * shape on every target) under {@code .minecraft/printer/<timestamp>/screenshots/}. The controller's facing
 * swizzle is kept so the diagram reads with the controller at the bottom.
 */
public class GUIScreenSlicePrinter extends CompatScreen {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    private static final int CELL = 16;
    /** Frames a slice stays on screen before it is captured; the capture reads the last completed frame. */
    private static final int FRAMES_PER_SLICE = 3;

    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final int sizeX, sizeY, sizeZ;
    private final ForgeDirection dir;
    private final Set<Block> whitelist;
    private final String dirname;

    private int yIndex;
    private int framesOnSlice;
    private boolean finished;

    public GUIScreenSlicePrinter(int x1, int y1, int z1, int x2, int y2, int z2, ForgeDirection dir, Set<Block> whitelist) {
        super(Component.literal("PWR Printer"));
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.dir = dir;
        this.whitelist = whitelist;
        this.sizeX = this.x2 - this.x1 + 1;
        this.sizeY = this.y2 - this.y1 + 1;
        this.sizeZ = this.z2 - this.z1 + 1;
        this.dirname = DATE_FORMAT.format(new Date());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.fill(graphics, 0, 0, width, height, 0xFFFF00FF);
        if (finished || minecraft == null || minecraft.level == null) return;

        boolean sideways = dir == ForgeDirection.WEST || dir == ForgeDirection.EAST;
        int gridW = (sideways ? sizeZ : sizeX) * CELL;
        int gridH = (sideways ? sizeX : sizeZ) * CELL;
        int originX = width / 2 - gridW / 2;
        int originY = height / 2 - gridH / 2;

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                BlockPos pos = new BlockPos(x1 + x, y1 + yIndex, z1 + z);
                Block block = minecraft.level.getBlockState(pos).getBlock();
                if (whitelist != null && !whitelist.contains(block)) continue;

                // Revert PWR blocks to originals for slice rendering
                if (block instanceof BlockPWR) {
                    BlockEntity tile = minecraft.level.getBlockEntity(pos);
                    if (tile instanceof TileEntityBlockPWR pwr && pwr.block != null) block = pwr.block;
                }

                int dx = x;
                int dz = z;
                // swizzle instead of rotating, so the PWR controller faces the correct rotation
                if (dir == ForgeDirection.WEST) {
                    dx = sizeZ - 1 - z;
                    dz = x;
                } else if (dir == ForgeDirection.SOUTH) {
                    dx = sizeX - 1 - x;
                    dz = sizeZ - 1 - z;
                } else if (dir == ForgeDirection.EAST) {
                    dx = z;
                    dz = sizeX - 1 - x;
                }

                GuiDraw.item(graphics, new ItemStack(block), originX + dx * CELL, originY + dz * CELL);
            }
        }
        framesOnSlice++;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) return;

        // Once we've reached the top slice, close the GUI
        if (finished || yIndex >= sizeY) {
            minecraft.player.sendSystemMessage(Component.literal("Slices saved to: .minecraft/printer/" + dirname));
            minecraft.setScreen(null);
            return;
        }

        if (framesOnSlice < FRAMES_PER_SLICE) return;

        File printerDir = new File(new File(minecraft.gameDirectory, "printer"), dirname);
        if (!printerDir.isDirectory() && !printerDir.mkdirs()) {
            minecraft.player.sendSystemMessage(Component.literal("Could not create " + printerDir).withStyle(ChatFormatting.RED));
            finished = true;
            return;
        }
        Screenshot.grab(printerDir, minecraft.getMainRenderTarget(), message -> {});

        yIndex++;
        framesOnSlice = 0;
    }
}
