package com.hbm.inventory;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import com.hbm.inventory.container.ContainerPneumoStorageClutter;
import com.hbm.inventory.container.ContainerPneumoStorageExporter;
import com.hbm.inventory.container.ContainerPneumoStorageImporter;
import com.hbm.inventory.container.ContainerPneumoStorageMono;
import com.hbm.inventory.container.ContainerPneumoTube;

/** Feature-local position-bound menus for the pneumatic tube storage system. */
public final class ModPneumaticMenus {
    private ModPneumaticMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerPneumoTube>> PNEUMATIC_TUBE =
            MENUS.register("pneumatic_tube", () -> Menus.typeWithPos(ContainerPneumoTube::new));
    public static final RegistryObject<MenuType<ContainerPneumoStorageAccess>> PNEUMATIC_STORAGE_ACCESS =
            MENUS.register("pneumatic_storage_access", () -> Menus.typeWithPos(ContainerPneumoStorageAccess::new));
    public static final RegistryObject<MenuType<ContainerPneumoStorageClutter>> PNEUMATIC_STORAGE_CLUTTER =
            MENUS.register("pneumatic_storage_clutter", () -> Menus.typeWithPos(ContainerPneumoStorageClutter::new));
    public static final RegistryObject<MenuType<ContainerPneumoStorageMono>> PNEUMATIC_STORAGE_MONO =
            MENUS.register("pneumatic_storage_mono", () -> Menus.typeWithPos(ContainerPneumoStorageMono::new));
    public static final RegistryObject<MenuType<ContainerPneumoStorageImporter>> PNEUMATIC_STORAGE_IMPORTER =
            MENUS.register("pneumatic_storage_importer", () -> Menus.typeWithPos(ContainerPneumoStorageImporter::new));
    public static final RegistryObject<MenuType<ContainerPneumoStorageExporter>> PNEUMATIC_STORAGE_EXPORTER =
            MENUS.register("pneumatic_storage_exporter", () -> Menus.typeWithPos(ContainerPneumoStorageExporter::new));

    public static void init() {}
}
