package com.hbm.client;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.machine.BlockHadronCoil;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockPWR;
import dev.golem.core.render.ConnectedTextures;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import net.minecraft.world.level.block.Block;

/**
 * The 1.7 {@code IBlockCT} blocks (G-C): each face in quarters from its {@code _ct} sheet (Core {@link ConnectedTextures}).
 * Connection rules are 1.7's {@code canConnect}: hadron coils to any coil; coated wire to itself (the default); the ICF
 * shell to shell cells and the ICF controller; the PWR shell to shell cells and the PWR controller. A converted port
 * cell (1.7 metadata 1) draws the port sheet.
 */
public final class ConnectedTextureRegistrations {

    private ConnectedTextureRegistrations() {}

    public static void register() {
        for (RegistryObject<? extends Block> coil : List.of(ModDecoCrateBlocks.hadron_coil_alloy, ModDecoCrateBlocks.hadron_coil_gold,
                ModDecoCrateBlocks.hadron_coil_neodymium, ModDecoCrateBlocks.hadron_coil_magtung,
                ModDecoCrateBlocks.hadron_coil_schrabidium, ModDecoCrateBlocks.hadron_coil_schrabidate,
                ModDecoCrateBlocks.hadron_coil_starmetal, ModDecoCrateBlocks.hadron_coil_chlorophyte,
                ModDecoCrateBlocks.hadron_coil_mese)) {
            String[] sheets = sheets(coil.getId().getPath());
            ConnectedTextures.register(coil::get, state -> sheets,
                    (level, pos, self, neighbour) -> neighbour.getBlock() instanceof BlockHadronCoil);
        }

        String[] wireSheets = sheets("red_wire_coated");
        ConnectedTextures.register(ModEnergyBedrockBlocks.red_wire_coated::get, state -> wireSheets,
                (level, pos, self, neighbour) -> neighbour.getBlock() == self.getBlock());

        String[] icf = sheets("icf_block");
        String[] icfPort = sheets("icf_block_port");
        ConnectedTextures.register(ModIcfBlocks.icf_block::get, state -> state.getValue(BlockICF.PORT) ? icfPort : icf,
                (level, pos, self, neighbour) -> neighbour.getBlock() == ModIcfBlocks.icf_block.get()
                        || neighbour.getBlock() == ModIcfBlocks.icf_controller.get());

        String[] pwr = sheets("pwr_block");
        String[] pwrPort = sheets("pwr_casing_port");
        ConnectedTextures.register(ModPwrBlocks.PWR_BLOCK::get, state -> state.getValue(BlockPWR.PORT) ? pwrPort : pwr,
                (level, pos, self, neighbour) -> neighbour.getBlock() == ModPwrBlocks.PWR_BLOCK.get()
                        || neighbour.getBlock() == ModPwrBlocks.PWR_CONTROLLER.get());
    }

    private static String[] sheets(String name) {
        return new String[]{"hbm:blocks/" + name, "hbm:blocks/" + name + "_ct"};
    }
}
