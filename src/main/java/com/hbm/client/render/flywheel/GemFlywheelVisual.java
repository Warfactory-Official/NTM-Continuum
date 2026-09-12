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
    private final List<Slot> slots = new ArrayList<>();
    private final Matrix4f scratch = new Matrix4f();
    private volatile boolean healthy = true;
    private volatile boolean written;

    GemFlywheelVisual(VisualizationContext ctx, T blockEntity, float partialTick, GemBER<T, S> renderer) {
        super(ctx, blockEntity, partialTick);
        this.renderer = renderer;
        this.model = renderer.gem();
        this.state = renderer.createRenderState();
        LIVE.put(pos, this);
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) {
        if (!healthy) return;
        try {
            // A model with no state-driven part is written once and then left alone until its light
            // changes, which is what makes a bolted-down machine cost nothing per frame.
            if (written && model.rigid()) return;

            state.blockPos = pos;
            state.lightCoords = Renderers.lightAt(level, pos);
            renderer.extract(blockEntity, state, ctx.partialTick(), ctx.camera().getPosition());

            scratch.identity().translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            int next = 0;
            for (Gem.Part<S> root : model.roots()) next = write(root, scratch, next, true);
            while (slots.size() > next) slots.remove(slots.size() - 1).instance().delete();

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
    private int write(Gem.Part<S> part, Matrix4f parent, int slot, boolean parentVisible) {
        boolean visible = parentVisible && part.visible(state);
        Matrix4f here = new Matrix4f(parent);
        part.applyTo(here, state);

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
            TransformedInstance instance = target.instance();
            instance.setTransform(here);
            instance.light(part.lightOf(state, model.lightOf(state, state.lightCoords)));
            instance.overlay(OverlayTexture.NO_OVERLAY);
            instance.colorArgb(part.tintOf(state));
            instance.setVisible(visible);
            instance.setChanged();
            slot++;
        }

        for (Gem.Part<S> child : part.children()) slot = write(child, here, slot, visible);
        return slot;
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
    }

    private record Slot(TransformedInstance instance, Geometry geometry, Surface surface) {}
}
*///?}
