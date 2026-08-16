package com.hbm.tileentity.machine.storage;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * The three ported HBM storage crates and all their per-crate layout data (slot count, GUI grid + size,
 * label colours, background texture). One {@link com.hbm.tileentity.machine.storage.TileEntityCrate}
 * class is parameterised by this enum, so the iron / steel / desh crates share all their code and differ
 * only by these values (mirroring the 1.12.2 {@code TileEntityCrateIron/Steel/Desh} subclass constants).
 */
public enum CrateType {

    //          id            name                   slots cols rows cX cY  invX invY hotY  gW   gH   lblX titleColor invColor
    IRON("crate_iron",   "container.crateIron",   36,   9,   4,   8, 18,  8, 104, 162, 176, 186,  8, 0x404040, 0x404040),
    STEEL("crate_steel", "container.crateSteel",  54,   9,   6,   8, 18,  8, 140, 198, 176, 222,  8, 0x1C1C1C, 0x1C1C1C),
    DESH("crate_desh",   "container.crateDesh",  104,  13,   8,   8, 18, 44, 174, 232, 248, 256, 44, 0x3F1515, 0x3F1515);

    public final String id;
    public final String defaultName;
    public final int slots;
    public final int columns;
    public final int rows;
    public final int crateX;
    public final int crateY;
    public final int playerInventoryX;
    public final int playerInventoryY;
    public final int hotbarY;
    public final int guiWidth;
    public final int guiHeight;
    public final int inventoryLabelX;
    public final int titleColor;
    public final int inventoryLabelColor;
    public final Identifier texture;
    /** The crate's BlockEntityType, wired lazily by {@code ModBlockEntities} (avoids a registration cycle). */
    private Supplier<BlockEntityType<TileEntityCrate>> typeSupplier;

    CrateType(String id, String defaultName, int slots, int columns, int rows, int crateX, int crateY,
              int playerInventoryX, int playerInventoryY, int hotbarY, int guiWidth, int guiHeight,
              int inventoryLabelX, int titleColor, int inventoryLabelColor) {
        this.id = id;
        this.defaultName = defaultName;
        this.slots = slots;
        this.columns = columns;
        this.rows = rows;
        this.crateX = crateX;
        this.crateY = crateY;
        this.playerInventoryX = playerInventoryX;
        this.playerInventoryY = playerInventoryY;
        this.hotbarY = hotbarY;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.inventoryLabelX = inventoryLabelX;
        this.titleColor = titleColor;
        this.inventoryLabelColor = inventoryLabelColor;
        this.texture = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_" + id + ".png");
    }

    public void setTypeSupplier(Supplier<BlockEntityType<TileEntityCrate>> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    public BlockEntityType<TileEntityCrate> blockEntityType() {
        return typeSupplier.get();
    }
}
