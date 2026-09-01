package com.hbm.tileentity.machine.storage;

import dev.golem.core.Ids;
import com.hbm.inventory.container.ContainerCrateTemplate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** The old ordinary template-crate layout, kept separate until a central CrateType row is added. */
public final class TemplateCrateLayout {

    public static final int SLOTS = 27;
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int CRATE_X = 8;
    public static final int CRATE_Y = 18;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 86;
    public static final int HOTBAR_Y = 144;
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 168;
    public static final int LABEL_X = 8;
    public static final int LABEL_COLOR = 0x404040;
    public static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_crate_template.png");
    private static Supplier<BlockEntityType<TileEntityCrateTemplate>> blockEntityType;
    private static Supplier<MenuType<ContainerCrateTemplate>> menuType;

    private TemplateCrateLayout() {}

    /** Central registration hooks; the leaf itself does not depend on any registry class. */
    public static void setBlockEntityTypeSupplier(Supplier<BlockEntityType<TileEntityCrateTemplate>> supplier) {
        blockEntityType = supplier;
    }

    public static BlockEntityType<TileEntityCrateTemplate> blockEntityType() {
        return blockEntityType.get();
    }

    public static void setMenuTypeSupplier(Supplier<MenuType<ContainerCrateTemplate>> supplier) {
        menuType = supplier;
    }

    public static MenuType<ContainerCrateTemplate> menuType() {
        return menuType.get();
    }
}
