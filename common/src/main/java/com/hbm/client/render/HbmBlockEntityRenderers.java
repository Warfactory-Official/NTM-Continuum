package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class HbmBlockEntityRenderers {

    private HbmBlockEntityRenderers() {}

    public interface Sink {
        <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, ?> provider);
    }

    private static final int FRAME_HEIGHT = 3;
    private static final Set<String> FRAME = Set.of("Frame");

    public static void registerAll(Sink sink) {

        regCustom(sink, ModBlockEntities.MACHINE_ASSEMBLY_MACHINE.get(), ctx -> new AssemblyMachineRenderer());

        regCustom(sink, ModBlockEntities.MACHINE_CHEMICAL_PLANT.get(), ctx -> new ChemicalPlantRenderer());

        regCustom(sink, ModBlockEntities.MACHINE_EPRESS.get(), ctx -> new PressRenderer<>(
                obj("epress_body.obj"), tex("machines/epress_body.png"),
                obj("epress_head.obj"), tex("machines/epress_head.png"),
                90F, true, 1.875F, 1.0F, 200, te -> te.progress));
        regCustom(sink, ModBlockEntities.MACHINE_PRESS.get(), ctx -> new PressRenderer<>(
                obj("press_body.obj"), tex("machines/press_body.png"),
                obj("press_head.obj"), tex("machines/press_head.png"),
                180F, false, 0.875F, 0.95F, 200, te -> te.progress));
        reg(sink, ModBlockEntities.LAUNCH_PAD.get(), 90F, 1F,
                new ModelDef(obj("weapons/launch_pad_silo.obj"), tex("launchpad/silo.png")));

        regCustom(sink, ModBlockEntities.NUKE_MAN.get(),
                ctx -> new BombObjRenderer<>(obj("bombs/fatman.obj"), tex("bombs/fatman.png"), 1F));

        regCustom(sink, ModBlockEntities.RED_CABLE.get(), ctx -> new CableObjRenderer<>());

        regCustom(sink, ModBlockEntities.FLUID_DUCT.get(), ctx -> new FluidDuctObjRenderer());

        regCustom(sink, ModBlockEntities.FLUID_TANK.get(), ctx -> new FluidTankRenderer());

        regCustom(sink, ModBlockEntities.BATTERY_SOCKET.get(), ctx -> new BatterySocketRenderer());
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale, ModelDef... defs) {
        reg(sink, type, yaw, scale, Set.of(), 0, defs);
    }

    private static <T extends BlockEntity, S extends BlockEntityRenderState> void regCustom(
            Sink sink, BlockEntityType<T> type, BlockEntityRendererProvider<T, S> provider) {
        sink.register(type, provider);
    }

    private static <T extends BlockEntity> void reg(Sink sink, BlockEntityType<T> type, float yaw, float scale,
                                                    Set<String> frameParts, int frameHeight, ModelDef... defs) {
        BlockEntityRendererProvider<T, MachineRenderState> provider =
                ctx -> new MachineObjRenderer<>(yaw, scale, frameParts, frameHeight, defs);
        sink.register(type, provider);
    }

    private static Identifier obj(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/" + path);
    }

    private static Identifier tex(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/" + path);
    }
}
