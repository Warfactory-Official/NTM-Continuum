package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemForceFieldUpgrade;
import com.hbm.items.special.ItemTeleLink;
import com.hbm.items.tool.ItemAnchorRemote;
import com.hbm.items.tool.ItemRadarLinker;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * The radar and utility machines' items, as 1.7 {@code ModItems} declared them: the radar linker, the telelinker
 * ({@code linker}), the recall device ({@code anchor_remote}) and the forcefield's two upgrades.
 */
public final class ModRadarUtilityItems {

    private ModRadarUtilityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** 1.7 {@code ItemCoordinateBase}'s {@code posX/posY/posZ}. */
    public static final GolemComponent<BlockPos> COORDINATE_POSITION =
            GolemComponent.of(Constants.MOD_ID, "coordinate_position", BlockPos.CODEC);
    /** 1.7 {@code ItemTeleLink}'s {@code x/y/z/dim}. */
    public static final GolemComponent<CompoundTag> TELE_LINK = GolemComponent.compound(Constants.MOD_ID, "tele_link");
    /** 1.7 {@code ItemAnchorRemote}'s {@code x/y/z}. */
    public static final GolemComponent<BlockPos> ANCHOR_POSITION =
            GolemComponent.of(Constants.MOD_ID, "anchor_position", BlockPos.CODEC);

    // 1.7: setMaxStackSize(1), consumableTab
    public static final RegistryObject<ItemRadarLinker> radar_linker = REG.item(
            "radar_linker", ItemRadarLinker::new, p -> p.stacksTo(1))
            .transform(Presets.sprite())
            .tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemTeleLink> linker = REG.item(
            "linker", ItemTeleLink::new, p -> p.stacksTo(1))
            .transform(Presets.sprite())
            .tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemAnchorRemote> anchor_remote = REG.item(
            "anchor_remote", ItemAnchorRemote::new, p -> p.stacksTo(1))
            .transform(Presets.handheld())
            .tab(HbmTab.CONSUMABLES);

    // 1.7: setMaxStackSize(16), controlTab
    public static final RegistryObject<ItemForceFieldUpgrade> upgrade_radius = REG.item(
            "upgrade_radius", p -> new ItemForceFieldUpgrade(true, p), p -> p.stacksTo(16))
            .transform(Presets.sprite())
            .tab(HbmTab.CONTROL);
    public static final RegistryObject<ItemForceFieldUpgrade> upgrade_health = REG.item(
            "upgrade_health", p -> new ItemForceFieldUpgrade(false, p), p -> p.stacksTo(16))
            .transform(Presets.sprite())
            .tab(HbmTab.CONTROL);

    public static void init() {}
}
