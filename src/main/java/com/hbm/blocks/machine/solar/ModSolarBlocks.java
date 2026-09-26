package com.hbm.blocks.machine.solar;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Solar-boiler content register; central bootstrap wiring is intentionally kept separate. */
public final class ModSolarBlocks {

    private ModSolarBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<MachineSolarBoiler> machine_solar_boiler = BLOCKS.registerBlock(
            "machine_solar_boiler", MachineSolarBoiler::new,
            p -> nonOccluding(p.strength(5.0F, 10.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()));

    public static final RegistryObject<SolarMirror> solar_mirror = BLOCKS.registerBlock(
            "solar_mirror", SolarMirror::new,
            p -> nonOccluding(p.strength(5.0F, 10.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()));

    static {
        RegistryObject<BlockItem> boilerItem = ITEMS.registerBlockItem("machine_solar_boiler", machine_solar_boiler);
        RegistryObject<BlockItem> mirrorItem = ITEMS.registerBlockItem("solar_mirror", solar_mirror);
        ModTabs.addTo(HbmTab.MACHINES, boilerItem);
        ModTabs.addTo(HbmTab.MACHINES, mirrorItem);
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties nonOccluding(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
        return properties.isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    public static void init() {}
}
