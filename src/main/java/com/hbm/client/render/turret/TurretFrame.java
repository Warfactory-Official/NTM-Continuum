package com.hbm.client.render.turret;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/** A reusable list of posed model parts, refilled every frame so drawing a turret allocates nothing. */
public final class TurretFrame {

    /**
     * One {@link Geometry} and one {@link Surface} per (model, group) and per texture, kept for the life
     * of the client. A frame is refilled every tick; minting a fresh lump each time would make the
     * instanced backend re-upload the same triangles on every one of them.
     */
    private static final Map<WavefrontObj, Map<String, Geometry>> MESHES = new IdentityHashMap<>();
    private static final Map<Identifier, Surface> SKINS = new HashMap<>();

    private final ArrayList<Part> parts = new ArrayList<>();
    private int size;

    public Part push() {
        while (parts.size() <= size) parts.add(new Part());
        Part part = parts.get(size++);
        part.reset();
        return part;
    }

    public void rewind() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public Part get(int index) {
        return parts.get(index);
    }

    private static synchronized Geometry meshOf(WavefrontObj model, String group) {
        return MESHES.computeIfAbsent(model, key -> new HashMap<>())
                .computeIfAbsent(group, model::part);
    }

    private static synchronized Surface skinOf(Identifier texture) {
        return SKINS.computeIfAbsent(texture, id -> Surface.cutout(id.toString()));
    }

    public static final class Part {

        public final Matrix4f pose = new Matrix4f();
        public @Nullable WavefrontObj model;
        public @Nullable String group;
        public @Nullable Identifier texture;
        private @Nullable Geometry geometry;
        private @Nullable Surface surface;

        void reset() {
            model = null;
            group = null;
            texture = null;
            geometry = null;
            surface = null;
            pose.identity();
        }

        public Part obj(WavefrontObj model, String group) {
            this.model = model;
            this.group = group;
            this.geometry = meshOf(model, group);
            return this;
        }

        public Part texture(Identifier texture) {
            this.texture = texture;
            this.surface = skinOf(texture);
            return this;
        }

        public @Nullable Geometry geometry() {
            return geometry;
        }

        public @Nullable Surface surface() {
            return surface;
        }
    }
}
