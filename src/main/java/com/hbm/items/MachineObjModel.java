package com.hbm.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.ItemModel;
import dev.golem.core.dejson.ItemModels;

/**
 * The declaration vocabulary of the {@code hbm:machine_obj} item model: an OBJ mesh drawn by
 * {@code com.hbm.client.render.MachineItemModelRenderer} with the old {@code ItemRenderBase}
 * transforms. Touches no Minecraft class, so Dejson can run it at build time.
 *
 * <p>Each method is one field {@code com.hbm.client.render.MachineItemModels} reads, spelled the way
 * that reader spells it, so a declaration reads straight against the renderer:
 *
 * <pre>{@code
 * MachineObjModel.of("hbm:models/machines/chimney_brick.obj", "hbm:textures/models/machines/chimney_brick.png")
 *         .scale(0.5).ty(0.0).yaw(90.0).rawLegacyTransform()
 *         .guiTranslate(0, -2.5, 0).guiScale(1.25)
 * }</pre>
 *
 * <p>Numbers keep the Java type they are written with ({@code 90} stays an integer), matching what
 * the item definitions shipped with.
 */
public final class MachineObjModel implements ItemModel {

    private final ItemModels.Custom model = ItemModels.custom("hbm:machine_obj");

    private MachineObjModel(String obj, String texture) {
        model.with("obj", obj);
        if (texture != null) model.with("texture", texture);
    }

    /** An OBJ drawn with one texture. */
    public static MachineObjModel of(String obj, String texture) {
        return new MachineObjModel(obj, texture);
    }

    /** An OBJ drawn with the materials of its own MTL file, or with per-part textures only. */
    public static MachineObjModel of(String obj) {
        return new MachineObjModel(obj, null);
    }

    public MachineObjModel scale(Number scale) { model.with("scale", scale); return this; }
    public MachineObjModel ty(Number ty) { model.with("ty", ty); return this; }
    public MachineObjModel yaw(Number yaw) { model.with("yaw", yaw); return this; }
    public MachineObjModel pitch(Number pitch) { model.with("pitch", pitch); return this; }
    public MachineObjModel guiScale(Number guiScale) { model.with("guiScale", guiScale); return this; }
    public MachineObjModel guiSpinSpeed(Number speed) { model.with("guiSpinSpeed", speed); return this; }
    public MachineObjModel guiYaw(Number guiYaw) { model.with("guiYaw", guiYaw); return this; }
    public MachineObjModel rotateX(Number degrees) { model.with("rotateX", degrees); return this; }
    public MachineObjModel rotateZ(Number degrees) { model.with("rotateZ", degrees); return this; }

    /** The 1.7 item transform is applied as-is rather than re-framed for the modern display contexts. */
    public MachineObjModel rawLegacyTransform() { model.with("rawLegacyTransform", true); return this; }
    public MachineObjModel noCull() { model.with("noCull", true); return this; }
    public MachineObjModel mtl() { model.with("mtl", true); return this; }
    public MachineObjModel spin() { model.with("spin", true); return this; }

    /** An explicit {@code false}, where the definition states the default rather than omitting it. */
    public MachineObjModel flag(String field, boolean value) { model.with(field, value); return this; }

    public MachineObjModel itemTranslate(Number x, Number y, Number z) { model.numbers("itemTranslate", x, y, z); return this; }
    public MachineObjModel itemPreTranslate(Number x, Number y, Number z) { model.numbers("itemPreTranslate", x, y, z); return this; }
    public MachineObjModel guiTranslate(Number x, Number y, Number z) { model.numbers("guiTranslate", x, y, z); return this; }
    public MachineObjModel translate(Number... xyz) { model.numbers("translate", xyz); return this; }
    public MachineObjModel rotatePivot(Number x, Number y, Number z) { model.numbers("rotatePivot", x, y, z); return this; }
    public MachineObjModel spinPivot(Number x, Number y, Number z) { model.numbers("spinPivot", x, y, z); return this; }

    /**
     * GUI-only steps after {@link #guiTranslate}/{@link #guiScale}/{@link #guiYaw}, in order: the rest of a 1.7
     * {@code renderInventory} that turned the model after scaling it. {@code guiRotateStep} takes Euler degrees
     * (X, then Y, then Z), {@code guiSpinStep} turns the GUI spin angle ({@link #guiSpinSpeed}) about an axis.
     */
    public MachineObjModel guiRotateStep(Number x, Number y, Number z) { return guiStep("rotate", x, y, z); }
    public MachineObjModel guiTranslateStep(Number x, Number y, Number z) { return guiStep("translate", x, y, z); }
    public MachineObjModel guiScaleStep(Number x, Number y, Number z) { return guiStep("scale", x, y, z); }
    public MachineObjModel guiSpinStep(Number x, Number y, Number z) { return guiStep("spin", x, y, z); }

    private JsonArray guiSteps;

    private MachineObjModel guiStep(String kind, Number x, Number y, Number z) {
        if (guiSteps == null) guiSteps = new JsonArray();
        JsonObject step = new JsonObject();
        step.add(kind, numbers(x, y, z));
        guiSteps.add(step); // written once, by toJson(): Custom refuses a field set twice
        return this;
    }

    /** Only these OBJ groups are drawn. */
    public MachineObjModel parts(String... parts) { model.strings("parts", parts); return this; }
    /** Every OBJ group but these is drawn. */
    public MachineObjModel exclude(String... parts) { model.strings("exclude", parts); return this; }
    public MachineObjModel blackParts(String... parts) { model.strings("blackParts", parts); return this; }
    public MachineObjModel rotateParts(String... parts) { model.strings("rotateParts", parts); return this; }
    public MachineObjModel spinParts(String... parts) { model.strings("spinParts", parts); return this; }

    public MachineObjModel overlayTexture(String texture) { model.with("overlayTexture", texture); return this; }
    public MachineObjModel overlayParts(String... parts) { model.strings("overlayParts", parts); return this; }

    public MachineObjModel animation(String animation) { model.with("animation", animation); return this; }

    /** The display frame the renderer composes onto, e.g. {@code missile}. */
    public MachineObjModel frame(String frame) { model.with("frame", frame); return this; }

    /** Per-group textures, as group then texture pairs. */
    public MachineObjModel partTextures(String... groupThenTexture) {
        if (groupThenTexture.length % 2 != 0) {
            throw new IllegalArgumentException("partTextures takes group/texture pairs");
        }
        JsonObject textures = new JsonObject();
        for (int index = 0; index < groupThenTexture.length; index += 2) {
            textures.addProperty(groupThenTexture[index], groupThenTexture[index + 1]);
        }
        model.with("partTextures", textures);
        return this;
    }

    /** The GUI context's own rotation, translation and uniform scale, applied after the frame. */
    public MachineObjModel guiContext(Number[] rotation, Number[] translation, Number scale) {
        return context("gui", rotation, translation, scale);
    }

    /**
     * A 1.7 {@code renderNonInv}: applied after the frame in every non-GUI slot (hands, ground, item frame), where
     * ItemRenderBase called it. The renderer rotates X, Y, Z before it translates, so a 1.7 translate-then-rotate
     * chain is restated with the translation carried through the inverse rotation.
     */
    public MachineObjModel nonInvContext(Number[] rotation, Number[] translation, Number scale) {
        context("non_gui", rotation, translation, scale);
        return context("ground", rotation, translation, scale);
    }

    /** NONE displays the primary OBJ at full size, centered on its loaded bounds, before the common item body. */
    public MachineObjModel rawMeshInNone() {
        var none = new JsonObject();
        none.addProperty("raw_mesh", true);
        if (contexts == null) contexts = new JsonObject();
        contexts.add("none", none);
        return this;
    }

    private JsonObject contexts;

    private MachineObjModel context(String key, Number[] rotation, Number[] translation, Number scale) {
        JsonObject transform = new JsonObject();
        transform.add("rotation", numbers(rotation));
        transform.add("translation", numbers(translation));
        transform.addProperty("scale", scale);
        if (contexts == null) contexts = new JsonObject();
        contexts.add(key, transform); // written once, by toJson(): Custom refuses a field set twice
        return this;
    }

    /** Further draw passes after the main one, each with its own texture and optionally its own mesh. */
    public MachineObjModel passes(Pass... passes) {
        JsonArray list = new JsonArray();
        for (Pass pass : passes) list.add(pass.json.deepCopy());
        model.with("passes", list);
        return this;
    }

    public static Pass pass(String texture) {
        return new Pass(texture);
    }

    /** Three numbers, for {@link #guiContext}. */
    public static Number[] xyz(Number x, Number y, Number z) {
        return new Number[] {x, y, z};
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = model.toJson();
        if (contexts != null) json.add("contextTransforms", contexts.deepCopy());
        if (guiSteps != null) json.add("guiSteps", guiSteps.deepCopy());
        return json;
    }

    private static JsonArray numbers(Number... values) {
        JsonArray array = new JsonArray();
        for (Number value : values) array.add(value);
        return array;
    }

    /** One extra draw pass. */
    public static final class Pass {

        private final JsonObject json = new JsonObject();
        private JsonArray steps;

        private Pass(String texture) {
            json.addProperty("texture", texture);
        }

        public Pass obj(String obj) { json.addProperty("obj", obj); return this; }

        public Pass parts(String... parts) {
            JsonArray list = new JsonArray();
            for (String part : parts) list.add(part);
            json.add("parts", list);
            return this;
        }

        public Pass translate(Number x, Number y, Number z) { json.add("translate", numbers(x, y, z)); return this; }

        /** 1.7's {@code ObjUtil.setColor(rgb)} around this pass: an opaque RGB vertex tint. */
        public Pass tint(int rgb) { json.addProperty("tint", rgb & 0xFFFFFF); return this; }

        /** A uniform scale about the pass origin, applied after its translation and steps. */
        public Pass scale(Number scale) { json.addProperty("scale", scale); return this; }

        /** A rotation step applied before this pass draws; steps apply in order. */
        public Pass rotateStep(Number... angleAxis) { return step("rotate", angleAxis); }

        /** A translation step applied before this pass draws; steps apply in order. */
        public Pass translateStep(Number... xyz) { return step("translate", xyz); }

        private Pass step(String kind, Number... values) {
            if (steps == null) {
                steps = new JsonArray();
                json.add("steps", steps);
            }
            JsonObject step = new JsonObject();
            step.add(kind, numbers(values));
            steps.add(step);
            return this;
        }
    }
}
