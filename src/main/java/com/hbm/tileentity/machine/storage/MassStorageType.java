package com.hbm.tileentity.machine.storage;

import dev.golem.core.Ids;
import com.hbm.inventory.container.ContainerMassStorage;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** The four legacy mass-storage variants and their fixed stockpile capacities. */
public enum MassStorageType {
    WOOD("mass_storage_wood", "container.massStorage", 100), // 1.7 BlockMassStorage.getCapacity: meta 3 (wood) holds 100
    IRON("mass_storage_iron", "container.massStorage", 10_000),
    DESH("mass_storage_desh", "container.massStorage", 100_000),
    MASS("mass_storage", "container.massStorage", 1_000_000);

    public final String id;
    public final String defaultName;
    public final int capacity;
    public final Identifier guiTexture = Ids.of("hbm", "textures/gui/storage/gui_mass_storage.png");
    private Supplier<BlockEntityType<TileEntityMassStorage>> blockEntityType;
    private Supplier<MenuType<ContainerMassStorage>> menuType;

    MassStorageType(String id, String defaultName, int capacity) {
        this.id = id;
        this.defaultName = defaultName;
        this.capacity = capacity;
    }

    /** Wired by the central registration rows after all four blocks and menu types exist. */
    public void setBlockEntityTypeSupplier(Supplier<BlockEntityType<TileEntityMassStorage>> supplier) {
        this.blockEntityType = supplier;
    }

    public BlockEntityType<TileEntityMassStorage> blockEntityType() {
        return blockEntityType.get();
    }

    /** Wired by the shared mass-storage menu registration row. */
    public void setMenuTypeSupplier(Supplier<MenuType<ContainerMassStorage>> supplier) {
        this.menuType = supplier;
    }

    public MenuType<ContainerMassStorage> menuType() {
        return menuType.get();
    }
}
