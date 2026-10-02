package com.hbm.blocks.machine.solar;

import com.hbm.registration.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;

/** Solar-boiler content register; central bootstrap wiring is intentionally kept separate. */
public final class ModSolarBlocks {

    private ModSolarBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);
    /** Folded multiblocks drawn by their renderer: every core/cell state is the shared empty placeholder. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));
    /** A renderer-drawn block with one state. */
    private static final Preset RENDERED_SIMPLE = Presets.rendered(Variants.simple());

    public static final RegistryObject<MachineSolarBoiler> machine_solar_boiler = REG.<MachineSolarBoiler>block(
            "machine_solar_boiler", MachineSolarBoiler::new,
            p -> nonOccluding(p.strength(5.0F, 10.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<SolarMirror> solar_mirror = REG.<SolarMirror>block(
            "solar_mirror", SolarMirror::new,
            p -> nonOccluding(p.strength(5.0F, 10.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion()))
            .transform(RENDERED_SIMPLE)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    static {
        RegistryObject<BlockItem> boilerItem = REG.blockItem(machine_solar_boiler);
        RegistryObject<BlockItem> mirrorItem = REG.blockItem(solar_mirror);
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
