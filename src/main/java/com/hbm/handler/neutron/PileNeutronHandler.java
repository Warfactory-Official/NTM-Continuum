package com.hbm.handler.neutron;

import com.hbm.api.block.IPileNeutronReceiver;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.tileentity.machine.pile.TileEntityPileBase;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The legacy (Chicago pile MK1) random-direction neutron rays, 1:1 from the 1.7 {@code PileNeutronHandler}. */
public final class PileNeutronHandler {

    public static int range = 5;

    private PileNeutronHandler() {}

    public static class PileNeutronNode extends NeutronNode {
        public PileNeutronNode(TileEntityPileBase tile) {
            super(tile, NeutronStream.NeutronType.PILE);
        }
    }

    public static PileNeutronNode makeNode(StreamWorld streamWorld, TileEntityPileBase tile) {
        NeutronNode node = streamWorld.getNode(tile.getBlockPos());
        return node instanceof PileNeutronNode pile ? pile : new PileNeutronNode(tile);
    }

    public static class PileNeutronStream extends NeutronStream {

        public PileNeutronStream(NeutronNode origin, Vec3 vector, double flux) {
            super(origin, vector, flux, 0D, NeutronType.PILE);
        }

        @Override
        public void runStreamInteraction(Level level, StreamWorld streamWorld) {
            BlockPos origin = this.origin.tile.getBlockPos();
            BlockPos pos = origin;

            for (float i = 1; i <= range; i += 0.5F) {
                BlockPos nodePos = new BlockPos(
                        (int) Math.floor(origin.getX() + 0.5 + vector.x * i),
                        (int) Math.floor(origin.getY() + 0.5 + vector.y * i),
                        (int) Math.floor(origin.getZ() + 0.5 + vector.z * i));

                if (nodePos.equals(pos)) continue; // don't interact with itself (or the previous cell)!
                pos = nodePos;

                BlockEntity tile;
                NeutronNode node = streamWorld.getNode(nodePos);
                if (node instanceof PileNeutronNode) {
                    tile = node.tile;
                } else {
                    tile = level.getBlockEntity(nodePos);
                    // Odd but load-bearing (1.7 and 1.12 both do it): a cell without a block entity, such as air,
                    // plain drilled graphite, concrete, ends the ray outright. The shielding checks below
                    // therefore only ever see blocks that happen to carry a foreign block entity.
                    if (tile == null) return;
                    if (tile instanceof TileEntityPileBase pile) streamWorld.addNode(new PileNeutronNode(pile));
                }

                BlockState state = tile.getBlockState();
                Block block = state.getBlock();
                boolean out = state.hasProperty(BlockGraphiteDrilledBase.OUT) && state.getValue(BlockGraphiteDrilledBase.OUT);
                if (!(tile instanceof TileEntityPileBase)) {
                    if (block == ModHazardBlocks.block_boron.get()) return;
                    else if (block == ModBlocks.concrete.get() || block == ModBlocks.concrete_smooth.get()
                            || block == ModBlocks.concrete_asbestos.get()
                            || block == ModDecoBlocks.CONCRETE_COLORED.get()
                            || block == ModHazardBlocks.brick_concrete.get())
                        fluxQuantity *= 0.25;
                    if (block == ModNuclearMiscBlocks.BLOCK_GRAPHITE_ROD.get() && !out) return;
                }

                if (tile instanceof IPileNeutronReceiver rec) {
                    rec.receiveNeutrons((int) Math.floor(fluxQuantity));
                    if (block != ModNuclearMiscBlocks.BLOCK_GRAPHITE_DETECTOR.get() || !out) return;
                }

                int x = (int) (nodePos.getX() + 0.5);
                int y = (int) (nodePos.getY() + 0.5);
                int z = (int) (nodePos.getZ() + 0.5);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z));
                for (LivingEntity e : entities) {
                    ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE, (float) (fluxQuantity / 4D));
                }
            }
        }
    }
}
