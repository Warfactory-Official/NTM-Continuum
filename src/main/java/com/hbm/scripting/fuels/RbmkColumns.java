package com.hbm.scripting.fuels;

import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKAbsorber;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKControl;
import com.hbm.blocks.machine.rbmk.RBMKModerator;
import com.hbm.blocks.machine.rbmk.RBMKReflector;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.scripting.ScriptedContent;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ItemModelJson;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * RBMK columns of the parameterised families. Each reuses its family's block entity, renderer and lid skins.
 *
 * <pre>
 * e.rbmkColumns.controlRod('kjs_control', 'KJS Control Rod', c => c
 *     .speed(2)                                       x insertion speed
 *     .moderated()                                    as rbmk_control_mod
 *     .textures('kubejs:block/ctl_side', 'kubejs:block/ctl_top')         default rbmk_control[_mod]'s
 *     .pipes('kubejs:block/ctl_pipe_side', 'kubejs:block/ctl_pipe_top')
 *     .lid('kubejs:block/ctl_lid'))                   item preview lid; placed lids keep rbmk_control's skins
 * e.rbmkColumns.moderator('kjs_mod', 'KJS Moderator', c => c.moderation(1.5))   x moderator dial per pass
 * e.rbmkColumns.reflector('kjs_ref', 'KJS Reflector', c => c.reflection(0.8))   x reflected flux; dial != 1 => pass-through
 * e.rbmkColumns.absorber('kjs_abs', 'KJS Absorber', c => c.heat(2))             x absorbed heat
 * </pre>
 * Block item on the machines tab; drops itself even when blown up.
 * Not parameterisable: fuel/boiler/heater/cooler/outgasser/storage/blank columns (no numeric column parameter).
 */
public final class RbmkColumns {

    private static final String RBMK = Constants.MOD_ID + ":blocks/rbmk/";

    public void controlRod(String id, String name, Consumer<ControlSpec> spec) {
        ControlSpec column = new ControlSpec();
        spec.accept(column);
        String base = column.moderated ? "rbmk_control_mod" : "rbmk_control";
        column.defaults(base);
        if (column.pipeSide == null) column.pipes(RBMK + base + "_pipe_side", RBMK + base + "_pipe_top");
        if (column.lid == null) column.lid = RBMK + base;
        RegistryObject<RBMKControl> block = register(id, p -> new RBMKControl(p, column.moderated, column.speed),
                ModRBMKBlockEntities.RBMK_CONTROL::get);
        Map<String, String> textures = column.textureMap();
        Map<String, String> top = new LinkedHashMap<>(textures);
        top.put("pipe_top", column.pipeTop);
        top.put("pipe_side", column.pipeSide);
        JsonObject preview = preview(column);
        preview.addProperty("pipe_side", column.pipeSide);
        preview.addProperty("pipe_top", column.pipeTop);
        preview.addProperty("lid_texture", column.lid);
        assets(id, "rbmk_control", textures, top, preview);
        MintedBlocks.declare(id, name, true);
        ModTabs.addTo(HbmTab.MACHINES, blockItem(id, block));
    }

    public void moderator(String id, String name, Consumer<ModeratorSpec> spec) {
        ModeratorSpec column = new ModeratorSpec();
        spec.accept(column);
        plain(id, name, "rbmk_moderator", column, p -> new RBMKModerator(p, column.moderation), ModRBMKBlockEntities.RBMK_MODERATOR::get);
    }

    public void reflector(String id, String name, Consumer<ReflectorSpec> spec) {
        ReflectorSpec column = new ReflectorSpec();
        spec.accept(column);
        plain(id, name, "rbmk_reflector", column, p -> new RBMKReflector(p, column.reflection), ModRBMKBlockEntities.RBMK_REFLECTOR::get);
    }

    public void absorber(String id, String name, Consumer<AbsorberSpec> spec) {
        AbsorberSpec column = new AbsorberSpec();
        spec.accept(column);
        plain(id, name, "rbmk_absorber", column, p -> new RBMKAbsorber(p, column.heat), ModRBMKBlockEntities.RBMK_ABSORBER::get);
    }

    private static <B extends RBMKBase> void plain(String id, String name, String base, ColumnSpec<?> column,
                                                   Function<BlockBehaviour.Properties, B> factory,
                                                   Supplier<? extends BlockEntityType<?>> type) {
        column.defaults(base);
        RegistryObject<B> block = register(id, factory, type);
        Map<String, String> textures = column.textureMap();
        assets(id, base, textures, textures, preview(column));
        MintedBlocks.declare(id, name, true);
        ModTabs.addTo(HbmTab.MACHINES, blockItem(id, block));
    }

    private static <B extends RBMKBase> RegistryObject<B> register(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                   Supplier<? extends BlockEntityType<?>> type) {
        RegistryObject<B> block = ScriptedContent.blocks(ModRBMKBlocks.BLOCKS).registerBlock(id, factory,
                p -> p.strength(3F, 30F).requiresCorrectToolForDrops());
        ScriptedContent.whenRegistered(() -> BlockEntities.addBlocks(type.get(), block.get()));
        return block;
    }

    private static RegistryObject<BlockItem> blockItem(String id, RegistryObject<? extends RBMKBase> block) {
        return ScriptedContent.items(ModRBMKBlocks.ITEMS).registerBlockItem(id, block::get);
    }

    private static JsonObject preview(ColumnSpec<?> column) {
        JsonObject preview = new JsonObject();
        preview.addProperty("type", Constants.MOD_ID + ":rbmk_block_column");
        preview.addProperty("side", column.side);
        preview.addProperty("top", column.top);
        preview.addProperty("scale", 0.35);
        preview.addProperty("yaw", 0);
        preview.addProperty("lift", -0.675);
        return preview;
    }

    /** Bodies: every lid/part state on {@code <id>_column_none}, top of a control column on {@code _column_top}. */
    private static void assets(String id, String base, Map<String, String> textures, Map<String, String> topTextures,
                               JsonObject preview) {
        String none = Constants.MOD_ID + ":block/rbmk/" + id + "_column_none";
        String top = Constants.MOD_ID + ":block/rbmk/" + id + "_column_top";
        boolean control = base.equals("rbmk_control");
        Map<String, String> states = new LinkedHashMap<>();
        for (RBMKBase.Lid lid : RBMKBase.Lid.values()) {
            for (RBMKBase.Part part : RBMKBase.Part.values()) {
                states.put("lid=" + lid.getSerializedName() + ",part=" + part.getSerializedName(),
                        control && part == RBMKBase.Part.TOP ? top : none);
            }
        }
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:special");
        special.addProperty("base", Constants.MOD_ID + ":item/machine_obj_base");
        special.add("model", preview);
        ScriptedContent.assets(out -> {
            out.json(Constants.MOD_ID, "models/block/rbmk/" + id + "_column_none",
                    MintedBlocks.retextured(Constants.MOD_ID + ":block/rbmk/" + base + "_column_none", textures));
            if (control) out.json(Constants.MOD_ID, "models/block/rbmk/" + id + "_column_top",
                    MintedBlocks.retextured(Constants.MOD_ID + ":block/rbmk/" + base + "_column_top", topTextures));
            out.json(Constants.MOD_ID, "blockstates/" + id, MintedBlocks.blockState(states));
            ItemModelJson.definition((path, json) -> out.json(Constants.MOD_ID, path, json), id, special,
                    Constants.MOD_ID + ":item/machine_obj_legacy");
        });
    }

    @SuppressWarnings("unchecked")
    public abstract static class ColumnSpec<S extends ColumnSpec<S>> {

        String side;
        String top;

        public S textures(String side, String top) {
            this.side = side;
            this.top = top;
            return (S) this;
        }

        void defaults(String base) {
            if (side == null) side = RBMK + base + "_side";
            if (top == null) top = RBMK + base + "_top";
        }

        Map<String, String> textureMap() {
            Map<String, String> textures = new LinkedHashMap<>();
            textures.put("top", top);
            textures.put("side", side);
            textures.put("bottom", top);
            textures.put("particle", side);
            return textures;
        }
    }

    public static final class ControlSpec extends ColumnSpec<ControlSpec> {

        private double speed = 1D;
        private boolean moderated;
        private String pipeSide;
        private String pipeTop;
        private String lid;

        public ControlSpec speed(double mult) { this.speed = mult; return this; }
        public ControlSpec moderated() { this.moderated = true; return this; }
        public ControlSpec pipes(String side, String top) { this.pipeSide = side; this.pipeTop = top; return this; }
        public ControlSpec lid(String sprite) { this.lid = sprite; return this; }
    }

    public static final class ModeratorSpec extends ColumnSpec<ModeratorSpec> {
        private double moderation = 1D;
        public ModeratorSpec moderation(double mult) { this.moderation = mult; return this; }
    }

    public static final class ReflectorSpec extends ColumnSpec<ReflectorSpec> {
        private double reflection = 1D;
        public ReflectorSpec reflection(double mult) { this.reflection = mult; return this; }
    }

    public static final class AbsorberSpec extends ColumnSpec<AbsorberSpec> {
        private double heat = 1D;
        public AbsorberSpec heat(double mult) { this.heat = mult; return this; }
    }
}
