package com.hbm.client.render.turret;

import com.hbm.client.model.WavefrontObj;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/** A reusable list of posed model parts, refilled every frame so drawing a turret allocates nothing. */
public final class TurretFrame {

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

    public static final class Part {

        public final Matrix4f pose = new Matrix4f();
        public @Nullable WavefrontObj model;
        public @Nullable String group;
        public @Nullable Identifier texture;

        void reset() {
            model = null;
            group = null;
            texture = null;
            pose.identity();
        }

        public Part obj(WavefrontObj model, String group) {
            this.model = model;
            this.group = group;
            return this;
        }

        public Part texture(Identifier texture) {
            this.texture = texture;
            return this;
        }
    }
}
