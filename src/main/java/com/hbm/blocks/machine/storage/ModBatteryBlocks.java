package com.hbm.blocks.machine.storage;

import com.hbm.registration.BlockTag;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemBatteryREDD;
import com.hbm.items.machine.ItemFENSU;
import dev.golem.core.world.Compat;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.SoundType;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;

/**
 * Hand-registered energy-storage machine blocks (the battery socket; future: capacitor banks, FENSU,
 * etc.). Separate from the generated {@link com.hbm.blocks.ModBlocks} and the general
 * {@link com.hbm.blocks.machine.ModMachineBlocks} so codegen never clobbers them.
 */
public final class ModBatteryBlocks {

    private ModBatteryBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);
    /** Folded multiblocks drawn by their renderer: every core/cell state is the shared empty placeholder. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    public static final RegistryObject<MachineBatterySocket> machine_battery_socket = REG.<MachineBatterySocket>block(
            "machine_battery_socket", MachineBatterySocket::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineFENSU> machine_fensu = REG.<MachineFENSU>block(
            "machine_fensu", MachineFENSU::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false))
            .transform(FOLDED);

    public static final RegistryObject<MachineBatteryREDD> machine_battery_redd = REG.<MachineBatteryREDD>block(
            "machine_battery_redd", MachineBatteryREDD::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false))
            .transform(FOLDED)
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    static {
        REG.blockItem(machine_battery_socket).tab(HbmTab.MACHINES);
        REG.<MachineFENSU, ItemFENSU>blockItem(machine_fensu, ItemFENSU::new, Compat::blockDescription)
                .tab(HbmTab.MACHINES);
        REG.<MachineBatteryREDD, ItemBatteryREDD>blockItem(machine_battery_redd, ItemBatteryREDD::new,
                        Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }

    public static void init() {}
}
