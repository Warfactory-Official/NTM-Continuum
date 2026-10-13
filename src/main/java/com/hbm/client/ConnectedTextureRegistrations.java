package com.hbm.client;

import com.google.common.base.Suppliers;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.machine.BlockHadronCoil;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import dev.golem.core.render.ConnectedTextures;
import dev.golem.core.registry.RegistryObject;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code IBlockCT} blocks: each face in quarters from its {@code _ct} sheet (Core {@link ConnectedTextures}).
 * Connection rules are 1.7's {@code canConnect}: hadron coils to any coil; coated wire to itself (the default); the ICF
 * shell to shell cells and the ICF controller; the PWR shell to shell cells and the PWR controller. A converted port
 * cell (1.7 metadata 1) draws the port sheet.
 *
 * <p>Also the rest of 1.7's set: {@code BlockDecoCT} (deco metals; steel and rusty steel join each
 * other), {@code BlockNTMGlassCT} (eight glasses, reinforced glass and laminate), {@code BlockEnumMultiCT}
 * platemetal and {@code BlockPile}. The 1.12 port's extra CTM blocks ({@code block_actinium} and so on) are not
 * here: 1.7 drew them plain.
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

        // 1.7 BlockDecoCT.canConnect: itself, and "i don't care" steel <-> rusty steel
        for (RegistryObject<? extends Block> deco : List.of(ModBlocks.deco_red_copper, ModBlocks.deco_tungsten,
                ModBlocks.deco_aluminium, ModBlocks.deco_lead, ModBlocks.deco_beryllium)) {
            String[] decoSheets = sheets(deco.getId().getPath());
            ConnectedTextures.register(deco::get, state -> decoSheets, ConnectedTextureRegistrations::self);
        }
        String[] steel = sheets("deco_steel");
        String[] rusty = sheets("deco_rusty_steel");
        ConnectedTextures.register(ModBlocks.deco_steel::get, state -> steel,
                (level, pos, self, neighbour) -> neighbour.getBlock() == ModBlocks.deco_steel.get()
                        || neighbour.getBlock() == ModBlocks.deco_rusty_steel.get());
        ConnectedTextures.register(ModBlocks.deco_rusty_steel::get, state -> rusty,
                (level, pos, self, neighbour) -> neighbour.getBlock() == ModBlocks.deco_rusty_steel.get()
                        || neighbour.getBlock() == ModBlocks.deco_steel.get());

        // 1.7 BlockNTMGlassCT: the IBlockCT default, itself
        for (RegistryObject<? extends Block> glass : List.of(ModGlassBlocks.glass_boron, ModGlassBlocks.glass_lead,
                ModGlassBlocks.glass_uranium, ModGlassBlocks.glass_trinitite, ModGlassBlocks.glass_polonium,
                ModGlassBlocks.glass_ash, ModGlassBlocks.glass_quartz, ModGlassBlocks.glass_polarized,
                ModGlassBlocks.reinforced_glass, ModGlassBlocks.reinforced_laminate)) {
            String[] glassSheets = sheets(glass.getId().getPath());
            ConnectedTextures.register(glass::get, state -> glassSheets, ConnectedTextureRegistrations::self);
        }

        // 1.7 platemetal was one BlockEnumMultiCT: each colour (metadata) draws its own sheet, and canConnect is
        // `this == block`, so every colour joins every other. The port flattened the colours into blocks.
        Supplier<Set<Block>> platemetal = Suppliers.memoize(() -> {
            Set<Block> blocks = Collections.newSetFromMap(new IdentityHashMap<>());
            ModDecoContent.platemetal.values().forEach(r -> blocks.add(r.get()));
            return blocks;
        })::get;
        for (var entry : ModDecoContent.platemetal.entrySet()) {
            String[] plateSheets = sheets("platemetal." + entry.getKey());
            ConnectedTextures.register(entry.getValue()::get, state -> plateSheets,
                    (level, pos, self, neighbour) -> platemetal.get().contains(neighbour.getBlock()));
        }

        // 1.7 BlockPile.getFragments: DOWN/UP the top sheet (control channel: control top), sides by metadata. The
        // input/output receivers keep the plain pile side as their unconnected quarters, and the control/core ones the
        // pile top - 1.7's primeReceiver arguments, odd but ported as they are.
        String[] pileTop = {"hbm:blocks/pile_block_top", "hbm:blocks/pile_block_top_ct"};
        String[] pileControl = {"hbm:blocks/pile_block_top", "hbm:blocks/pile_block_control_top_ct"};
        String[] pileSide = sheets("pile_block");
        String[] pileIn = {"hbm:blocks/pile_block", "hbm:blocks/pile_block_input_ct"};
        String[] pileOut = {"hbm:blocks/pile_block", "hbm:blocks/pile_block_output_ct"};
        String[] pileCore = {"hbm:blocks/pile_block_top", "hbm:blocks/pile_block_core_ct"};
        ConnectedTextures.register(ModPileBlocks.pile_block::get, (ConnectedTextures.FaceSheets) (state, face) -> {
            int meta = state.getValue(BlockPile.META);
            if (face.getAxis() == Direction.Axis.Y) return meta == BlockPile.META_CONTROL ? pileControl : pileTop;
            return switch (meta) {
                case BlockPile.META_FUEL_IN, BlockPile.META_AIR_IN -> pileIn;
                case BlockPile.META_FUEL_OUT, BlockPile.META_AIR_OUT -> pileOut;
                case BlockPile.META_CORE -> pileCore;
                default -> pileSide;
            };
        }, ConnectedTextureRegistrations::self);
    }

    /** 1.7 {@code IBlockCT.canConnect} default: {@code this == block}. */
    private static boolean self(BlockGetter level, BlockPos pos,
                                BlockState self,
                                BlockState neighbour) {
        return neighbour.getBlock() == self.getBlock();
    }

    private static String[] sheets(String name) {
        return new String[]{"hbm:blocks/" + name, "hbm:blocks/" + name + "_ct"};
    }
}
