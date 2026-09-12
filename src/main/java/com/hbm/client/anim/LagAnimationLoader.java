package com.hbm.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.core.Ids;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationKeyframe;
import dev.golem.core.render.anim.BusAnimationSequence;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Loads the upstream Blender-exported LAG bus animations at resource-load time. */
public final class LagAnimationLoader {

    private static final Identifier FILE = Ids.of("hbm", "models/weapons/animations/lag.json");
    private static Map<String, BusAnimation> cached;

    private LagAnimationLoader() {}

    public static Map<String, BusAnimation> load() {
        if (cached != null) return cached;
        try (BufferedReader reader = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(FILE).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, double[]> offsets = vectors(root, "offset");
            Map<String, BusAnimation> animations = new HashMap<>();
            for (Map.Entry<String, JsonElement> animationEntry : root.getAsJsonObject("anim").entrySet()) {
                BusAnimation animation = new BusAnimation();
                for (Map.Entry<String, JsonElement> busEntry : animationEntry.getValue().getAsJsonObject().entrySet()) {
                    animation.addBus(busEntry.getKey(), loadSequence(busEntry.getValue().getAsJsonObject(),
                            offsets.getOrDefault(busEntry.getKey(), new double[] {0, 0, 0})));
                }
                animations.put(animationEntry.getKey(), animation);
            }
            cached = Map.copyOf(animations);
            return cached;
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed to load LAG animation " + FILE, e);
        }
    }

    /** Clears the resource-pack-backed cache; the next animation lookup reparses the current pack. */
    public static void invalidate() {
        cached = null;
    }

    private static Map<String, double[]> vectors(JsonObject root, String key) {
        Map<String, double[]> out = new HashMap<>();
        JsonObject object = root.has(key) ? root.getAsJsonObject(key) : null;
        if (object == null) return out;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonArray values = entry.getValue().getAsJsonArray();
            out.put(entry.getKey(), new double[] {values.get(0).getAsDouble(), values.get(1).getAsDouble(),
                    values.get(2).getAsDouble()});
        }
        return out;
    }

    private static BusAnimationSequence loadSequence(JsonObject json, double[] offset) {
        BusAnimationSequence sequence = new BusAnimationSequence();
        addCurve(sequence, json, "location", "x", BusAnimationSequence.Dimension.TX);
        addCurve(sequence, json, "location", "y", BusAnimationSequence.Dimension.TY);
        addCurve(sequence, json, "location", "z", BusAnimationSequence.Dimension.TZ);
        addCurve(sequence, json, "rotation_euler", "x", BusAnimationSequence.Dimension.RX);
        addCurve(sequence, json, "rotation_euler", "y", BusAnimationSequence.Dimension.RY);
        addCurve(sequence, json, "rotation_euler", "z", BusAnimationSequence.Dimension.RZ);
        addCurve(sequence, json, "scale", "x", BusAnimationSequence.Dimension.SX);
        addCurve(sequence, json, "scale", "y", BusAnimationSequence.Dimension.SY);
        addCurve(sequence, json, "scale", "z", BusAnimationSequence.Dimension.SZ);
        sequence.offset = offset;
        // Upstream computes rotmode but never inserts it into the sequence; the observable default is XYZ.
        return sequence;
    }

    private static void addCurve(BusAnimationSequence sequence, JsonObject json, String group,
            String axis, BusAnimationSequence.Dimension dimension) {
        if (!json.has(group) || !json.getAsJsonObject(group).has(axis)) return;
        IType previous = null;
        for (JsonElement element : json.getAsJsonObject(group).getAsJsonArray(axis)) {
            BusAnimationKeyframe frame = loadKeyframe(element, previous);
            sequence.addKeyframe(dimension, frame);
            previous = frame.interpolationType;
        }
    }

    private static BusAnimationKeyframe loadKeyframe(JsonElement element, IType previous) {
        JsonArray values = element.getAsJsonArray();
        double value = values.get(0).getAsDouble();
        int duration = values.get(1).getAsInt();
        IType interpolation = values.size() >= 3 ? type(values.get(2).getAsString()) : IType.LINEAR;
        BusAnimationKeyframe frame = new BusAnimationKeyframe(value, duration, interpolation);
        int index = 4;
        if (previous == IType.BEZIER && values.size() >= index + 3) {
            frame.leftX = values.get(index++).getAsDouble();
            frame.leftY = values.get(index++).getAsDouble();
            index++;
        }
        if (interpolation == IType.BEZIER && values.size() >= index + 3) {
            frame.rightX = values.get(index++).getAsDouble();
            frame.rightY = values.get(index++).getAsDouble();
        }
        return frame;
    }

    private static IType type(String name) {
        try {
            return IType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return IType.LINEAR;
        }
    }
}
