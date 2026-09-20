package com.hbm.tileentity.machine.pile;

import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.neutron.PileNeutronHandler;
import com.hbm.handler.neutron.PileNeutronHandler.PileNeutronNode;
import com.hbm.handler.neutron.PileNeutronHandler.PileNeutronStream;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The legacy graphite-pile tile base: a node in the neutron graph that casts random-direction rays. */
@Deprecated
public abstract class TileEntityPileBase extends CompatBlockEntity {

    protected TileEntityPileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) NeutronNodeWorld.removeNode(level, worldPosition);
    }

    /** The old {@code writeToNBT} contents, used to shove a tile along when a rod chain is pushed. */
    public abstract void writeSnapshot(CompoundTag tag);

    public abstract void readSnapshot(CompoundTag tag);

    protected void castRay(int flux) {
        BlockPos pos = worldPosition;

        if (flux == 0) {
            // simple way to remove the node from the cache when no flux is going into it!
            NeutronNodeWorld.removeNode(level, pos);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        PileNeutronNode node = streamWorld.getNode(pos) instanceof PileNeutronNode cached ? cached : null;

        if (node == null) {
            node = PileNeutronHandler.makeNode(streamWorld, this);
            streamWorld.addNode(node);
        }

        // 1.7 Vec3.rotateAroundZ / Y / X on (1, 0, 0), in that order.
        Vec3 neutronVector = new Vec3(1, 0, 0);
        neutronVector = rotateAroundZ(neutronVector, (float) (Math.PI * 2D * level.getRandom().nextDouble()));
        neutronVector = rotateAroundY(neutronVector, (float) (Math.PI * 2D * level.getRandom().nextDouble()));
        neutronVector = rotateAroundX(neutronVector, (float) (Math.PI * 2D * level.getRandom().nextDouble()));

        new PileNeutronStream(node, neutronVector, flux);
    }

    private static Vec3 rotateAroundX(Vec3 v, float angle) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
        return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s);
    }

    private static Vec3 rotateAroundY(Vec3 v, float angle) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
        return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
    }

    private static Vec3 rotateAroundZ(Vec3 v, float angle) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle);
        return new Vec3(v.x * c + v.y * s, v.y * c - v.x * s, v.z);
    }
}
