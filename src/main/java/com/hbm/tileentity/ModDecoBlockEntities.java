package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.tileentity.deco.TileEntityLantern;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.machine.DemonLamp;
import com.hbm.blocks.machine.FloodlightBeam;
import com.hbm.tileentity.deco.TileEntityDecoModel;
import com.hbm.tileentity.machine.TileEntityFloodlight;

/** Feature-local block-entity types of the decorative family. */
public final class ModDecoBlockEntities {
    private ModDecoBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    /** The one stateless type every OBJ-drawn decoration of this family carries so its renderer runs. */
    public static final RegistryObject<BlockEntityType<TileEntityDecoModel>> DECO_MODEL =
            REG.blockEntity("deco_model", () -> BlockEntities.type(TileEntityDecoModel::new,
                    ModDecoContent.spotlight_incandescent.get(), ModDecoContent.spotlight_incandescent_off.get(),
                    ModDecoContent.spotlight_fluoro.get(), ModDecoContent.spotlight_fluoro_off.get(),
                    ModDecoContent.spotlight_halogen.get(), ModDecoContent.spotlight_halogen_off.get(),
                    ModDecoContent.steel_poles.get(), ModDecoContent.pole_top.get(),
                    ModDecoContent.pole_satellite_receiver.get(), ModDecoContent.tape_recorder.get(),
                    ModDecoContent.deco_toaster_iron.get(), ModDecoContent.deco_toaster_steel.get(), ModDecoContent.deco_toaster_wood.get(),
                    ModDecoContent.deco_crt_clean.get(), ModDecoContent.deco_crt_broken.get(), ModDecoContent.deco_crt_blinking.get(),
                    ModDecoContent.deco_crt_bsod.get(), ModDecoContent.deco_computer_ibm_300pl.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFloodlight>> FLOODLIGHT =
            REG.blockEntity("floodlight", () -> BlockEntities.serverTicking(TileEntityFloodlight::new,
                    (level, pos, state, be) -> be.serverTick(), ModDecoContent.floodlight.get()));
    public static final RegistryObject<BlockEntityType<FloodlightBeam.TileEntityFloodlightBeam>> FLOODLIGHT_BEAM =
            REG.blockEntity("floodlight_beam", () -> BlockEntities.serverTicking(FloodlightBeam.TileEntityFloodlightBeam::new,
                    (level, pos, state, be) -> be.serverTick(), ModDecoContent.floodlight_beam.get()));

    public static final RegistryObject<BlockEntityType<DemonLamp.TileEntityDemonLamp>> DEMON_LAMP =
            REG.blockEntity("lamp_demon", () -> BlockEntities.serverTicking(DemonLamp.TileEntityDemonLamp::new,
                    (level, pos, state, be) -> be.serverTick(), ModDecoContent.lamp_demon.get()));

    /** 1.7 {@code TileEntityLantern}: server-ticking so the glyphid sweep runs; the core cell only. */
    public static final RegistryObject<BlockEntityType<TileEntityLantern>> LANTERN =
            REG.blockEntity("lantern", () -> BlockEntities.serverTicking(
                    TileEntityLantern::new,
                    (level, pos, state, be) -> be.serverTick(), ModDecoContent.lantern.get()));

    public static void init() {}
}
