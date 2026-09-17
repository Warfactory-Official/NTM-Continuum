package com.hbm.client.render.flywheel;

//? if flywheel {

/*import com.hbm.Constants;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.Renderers;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.slf4j.LoggerFactory;

/^*
 * Draws a {@linkplain Gem declared model} through the published engine, on the nodes that have one.
 *
 * <p>This is the same adapter Core ships for its vendored engine, written against
 * {@code dev.engine_room} instead. Both read the same declaration and run the same {@code extract},
 * which is the whole point of a renderer declaring rather than drawing: the two backends cannot
 * disagree about what a machine looks like, because there is only one description of it.
 *
 * <p>Instancing is an addition, never a replacement. Any failure marks the visual unhealthy, drops its
 * instances and lets the vanilla renderer have the block back, so the worst case is a machine that
 * draws the slow way rather than a hole in the world.
 ^/
final class GemFlywheelVisual<T extends BlockEntity, S extends CompatRenderState>
        extends AbstractBlockEntityVisual<T> implements SimpleDynamicVisual {

    /^*
     * The live visuals, so {@link dev.golem.core.render.Visuals#owns} can answer for this engine.
     *
     * <p>The vanilla renderer is never skipped outright — its {@code submitUndeclared} geometry has no
     * resident mesh and has to keep being submitted — so what it needs to know is narrower: whether
     * this block's <em>declared</em> parts are already on the GPU. A visual that has failed, or that
     * has not written its instances yet, answers no and the machine draws the slow way.
     ^/
    private static final Map<BlockPos, GemFlywheelVisual<?, ?>> LIVE = new ConcurrentHashMap<>();

    static boolean owns(BlockPos pos) {
        GemFlywheelVisual<?, ?> visual = LIVE.get(pos);
        return visual != null && visual.healthy && visual.written;
    }

    private final GemBER<T, S> renderer;
    private final Gem.Model<S> model;
    private final S state;
    private final boolean channelsFixed;
    private final List<Slot> slots = new ArrayList<>();
    private final Matrix4f scratch = new Matrix4f();

    /^*
     * The world transform this walk last gave each part, in visit order, and whether it drew.
     *
     * <p>Parts are numbered rather than looked up because the tree is sealed, exactly as the slot
     * list already relies on. Joints are numbered too: they carry no instance, but a joint is the
     * most common thing a still subtree hangs from, so proving one unchanged is what lets the walk
     * skip everything under it.
     ^/
    private final Matrix4f[] poses;
    private final boolean[] drawn;

    /^*
     * The slot each part started at last walk.
     *
     * <p>Slots are positional, so a part earlier in the walk that stops drawing — a mesh chooser is
     * allowed to hand back nothing — shifts every slot after it by one. A subtree proved unchanged is
     * still holding the right values, but no longer in the right slots, so the cursor has to agree
     * before stepping over it.
     ^/
    private final int[] slotAt;
    private int walked;
    private volatile boolean healthy = true;
    private volatile boolean written;
    private boolean primed;
    private int writtenLight;
    private boolean touched;

    GemFlywheelVisual(VisualizationContext ctx, T blockEntity, float partialTick, GemBER<T, S> renderer) {
        super(ctx, blockEntity, partialTick);
        this.renderer = renderer;
        this.model = renderer.gem();
        this.state = renderer.createRenderState();
        this.channelsFixed = this.model.channelsFixed();
        int nodes = 0;
        for (Gem.Part<S> root : this.model.roots()) nodes += root.subtreeNodes();
        this.poses = new Matrix4f[nodes];
        this.drawn = new boolean[nodes];
        this.slotAt = new int[nodes];
        for (int i = 0; i < nodes; i++) this.poses[i] = new Matrix4f();
        LIVE.put(pos, this);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!healthy) return;
        try {
            state.blockPos = pos;
            int sampled = Renderers.lightAt(level, pos);
            if (sampled != writtenLight) primed = false;
            state.lightCoords = sampled;

            // A model with no state-driven part is written once and then left alone until its light
            // changes, which is what makes a bolted-down machine cost nothing per frame.
            if (written && primed && model.rigid()) return;

            renderer.extract(blockEntity, state, ctx.partialTick(), ctx.camera().getPosition());

            scratch.identity().translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            boolean comparable = primed;
            touched = false;
            walked = 0;
            int next = 0;
            for (Gem.Part<S> root : model.roots()) next = write(root, scratch, next, true, comparable);
            while (slots.size() > next) slots.remove(slots.size() - 1).instance().delete();

            primed = true;
            writtenLight = sampled;
            written = next > 0;
        } catch (RuntimeException failure) {
            fail(failure);
        }
    }

    /^*
     * Writes one part and its children, depth first, and returns the next free slot.
     *
     * <p>Slots are keyed by position in that walk rather than by identity, because the walk is over a
     * sealed tree: part <i>n</i> is the same part every frame, so its instance can simply be reused.
     ^/
    private int write(Gem.Part<S> part, Matrix4f parent, int slot, boolean parentVisible,
                      boolean parentUnchanged) {
        int node = walked++;

        // Nothing above this part moved, nothing in it or under it reads the render state, and the
        // model-wide channels every part falls back to are fixed too — so this subtree is already on
        // the GPU holding the values it would be handed again. A spinning blade should cost a blade,
        // not the machine it is bolted to.
        if (parentUnchanged && channelsFixed && slot == slotAt[node] && part.rigidTree()) {
            walked = node + part.subtreeNodes();
            return slot + part.subtreeSlots();
        }
        slotAt[node] = slot;

        boolean visible = parentVisible && part.visible(state);
        Matrix4f here = new Matrix4f(parent);
        part.applyTo(here, state);

        Matrix4f cached = poses[node];
        boolean unchanged = parentUnchanged && visible == drawn[node] && here.equals(cached);
        if (!unchanged) {
            cached.set(here);
            drawn[node] = visible;
        }

        Geometry geometry = part.geometryOf(state);
        if (geometry != null) {
            Surface surface = part.surfaceOf(state);
            Slot target;
            if (slot < slots.size()) {
                target = slots.get(slot);
                if (target.geometry() != geometry || !target.surface().equals(surface)) {
                    target.instance().delete();
                    target = allocate(geometry, surface);
                    slots.set(slot, target);
                }
            } else {
                target = allocate(geometry, surface);
                slots.add(target);
            }
            apply(target, here, part.lightOf(state, model.lightOf(state, state.lightCoords)),
                    part.tintOf(state), visible);
            slot++;
        }

        for (Gem.Part<S> child : part.children()) slot = write(child, here, slot, visible, unchanged);
        return slot;
    }

    /^*
     * Sends one part's properties to its instance, and only the ones that differ.
     *
     * <p>{@code setChanged} is what queues an instance for upload, so an unchanged part that still
     * calls it costs an upload to change nothing. Most machines in a loaded world are in exactly that
     * position on most frames: they read their facing from the render state, which is enough to make
     * them animated as far as the declaration is concerned, and then they never turn.
     ^/
    private void apply(Slot target, Matrix4f transform, int light, int tint, boolean visible) {
        TransformedInstance instance = target.instance();
        if (!target.written) {
            target.written = true;
            target.transform.set(transform);
            target.light = light;
            target.color = tint;
            target.visible = visible;
            instance.setTransform(transform);
            instance.light(light);
            instance.overlay(OverlayTexture.NO_OVERLAY);
            instance.colorArgb(tint);
            instance.setVisible(visible);
            instance.setChanged();
            touched = true;
            return;
        }

        boolean changed = false;
        if (!target.transform.equals(transform)) {
            target.transform.set(transform);
            instance.setTransform(transform);
            changed = true;
            touched = true;
        }
        if (target.light != light) {
            target.light = light;
            instance.light(light);
            changed = true;
        }
        if (target.color != tint) {
            target.color = tint;
            instance.colorArgb(tint);
            changed = true;
        }
        if (target.visible != visible) {
            target.visible = visible;
            instance.setVisible(visible);
            changed = true;
        }
        if (changed) instance.setChanged();
    }

    /^*
     * One instance in the bucket for this (mesh, surface) pair. A part that chooses its mesh or its
     * skin from the render state moves between buckets, which is a delete and a fresh allocation
     * rather than a property on the instance — the bucket is what a draw call is.
     ^/
    private Slot allocate(Geometry geometry, Surface surface) {
        Instancer<TransformedInstance> instancer = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, GemModels.of(geometry, surface));
        return new Slot(instancer.createInstance(), geometry, surface);
    }

    /^*
     * Hands every part to the destroy-stage overlay, so a machine cracks as it is mined. Which block
     * owns the visual is a question this mod's multiblocks answer oddly, and the engine asks
     * {@code FlywheelCompat.crumblingCore} rather than assuming the mined position.
     ^/
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (!healthy) return;
        for (Slot slot : slots) consumer.accept(slot.instance());
    }

    @Override
    public void updateLight(float partialTick) {
        written = false;
        primed = false;
    }

    @Override
    protected void _delete() {
        LIVE.remove(pos, this);
        clearInstances();
    }

    private void fail(RuntimeException failure) {
        healthy = false;
        clearInstances();
        LoggerFactory.getLogger(Constants.MOD_ID)
                .error("Gem visual failed for {} at {}; returning ownership to vanilla",
                        blockEntity.getType(), pos, failure);
    }

    private void clearInstances() {
        for (Slot slot : slots) slot.instance().delete();
        slots.clear();
        written = false;
        primed = false;
    }

    /^*
     * One instance, the declaration it was allocated against, and the last values sent to it.
     *
     * <p>The cache is what makes a redundant write detectable at all: an instance holds its own
     * properties but has no way to answer "is this what you already have".
     ^/
    private static final class Slot {

        private final TransformedInstance instance;
        private final Geometry geometry;
        private final Surface surface;
        private final Matrix4f transform = new Matrix4f();
        private boolean written;
        private int light;
        private int color;
        private boolean visible;

        private Slot(TransformedInstance instance, Geometry geometry, Surface surface) {
            this.instance = instance;
            this.geometry = geometry;
            this.surface = surface;
        }

        private TransformedInstance instance() {
            return instance;
        }

        private Geometry geometry() {
            return geometry;
        }

        private Surface surface() {
            return surface;
        }
    }
}
*///?}
