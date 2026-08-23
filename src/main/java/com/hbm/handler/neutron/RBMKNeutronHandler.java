package com.hbm.handler.neutron;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.tileentity.machine.rbmk.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Current RBMK cell-by-cell neutron propagation. */
public final class RBMKNeutronHandler {
    static double moderatorEfficiency, reflectorEfficiency, absorberEfficiency;
    static int columnHeight, fluxRange;
    private RBMKNeutronHandler() {}
    private static BlockEntity blockPosToTE(Level world, BlockPos pos) { return world.getBlockEntity(pos); }
    public static RBMKNeutronNode makeNode(StreamWorld world, BlockEntityRBMKBase tile) { NeutronNode node = world.getNode(tile.getBlockPos()); return node != null ? (RBMKNeutronNode) node : new RBMKNeutronNode(tile, tile.getRBMKType(), tile.hasLid()); }
    public enum RBMKType { ROD, MODERATOR, CONTROL_ROD, REFLECTOR, ABSORBER, OUTGASSER, OTHER }
    public static class RBMKNeutronNode extends NeutronNode {
        public static final Key<Boolean> HAS_LID = new Key<>("hasLid", Boolean.class);
        public static final Key<RBMKType> COLUMN_TYPE = new Key<>("type", RBMKType.class);
        public RBMKNeutronNode(BlockEntityRBMKBase tile, RBMKType type, boolean lid) { super(tile, NeutronStream.NeutronType.RBMK); set(HAS_LID, lid); set(COLUMN_TYPE, type); }
        public void addLid() { set(HAS_LID, true); }
        public void removeLid() { set(HAS_LID, false); }
        public List<BlockPos> checkNode(StreamWorld world) {
            List<BlockPos> list = new ArrayList<>();
            RBMKNeutronStream[] streams = new RBMKNeutronStream[BlockEntityRBMKRod.fluxDirs.length];
            for (int i = 0; i < streams.length; i++) streams[i] = new RBMKNeutronStream(this, BlockEntityRBMKRod.fluxDirs[i]);
            if (tile instanceof BlockEntityRBMKRod rod && (!rod.hasRod || rod.lastFluxQuantity == 0D)) { for (RBMKNeutronStream stream : streams) for (NeutronNode node : stream.getNodes(world, false)) if (node != null) list.add(node.tile.getBlockPos()); return list; }
            for (RBMKNeutronStream stream : streams) for (NeutronNode node : stream.getNodes(world, false)) if (node != null && node.tile instanceof BlockEntityRBMKRod) return list;
            list.add(tile.getBlockPos()); return list;
        }
    }
    public static class RBMKNeutronStream extends NeutronStream {
        public RBMKNeutronStream(NeutronNode origin, Vec3 vector) { super(origin, vector); }
        public RBMKNeutronStream(NeutronNode origin, Vec3 vector, double flux, double ratio) { super(origin, vector, flux, ratio, NeutronType.RBMK); }
        private static boolean neutronOpaque(BlockState state) { return state.canOcclude(); }
        public NeutronNode[] getNodes(StreamWorld streamWorld, boolean addNode) {
            NeutronNode[] positions = new RBMKNeutronNode[fluxRange]; BlockPos originPos = origin.tile.getBlockPos(); Level world = origin.tile.getLevel();
            for (int i = 1; i <= fluxRange; i++) { int x = (int)Math.floor(.5D + vector.x * i), z = (int)Math.floor(.5D + vector.z * i); BlockPos pos = new BlockPos(originPos.getX()+x, originPos.getY(), originPos.getZ()+z); NeutronNode node = streamWorld.getNode(pos); if (node instanceof RBMKNeutronNode) positions[i-1] = node; else if (origin.tile.getBlockState().getBlock() instanceof RBMKBase && blockPosToTE(world,pos) instanceof BlockEntityRBMKBase cell) { node = makeNode(streamWorld,cell); positions[i-1] = node; if(addNode) streamWorld.addNode(node); } }
            return positions;
        }
        @Override public void runStreamInteraction(Level world, StreamWorld streamWorld) {
            if (fluxQuantity == 0D) return; BlockPos originPos = origin.tile.getBlockPos();
            BlockEntityRBMKBase originTE;
            NeutronNode cached = streamWorld.getNode(originPos);
            if (cached != null) originTE = (BlockEntityRBMKBase) cached.tile;
            else { if (!(blockPosToTE(world,originPos) instanceof BlockEntityRBMKBase cell)) return; originTE = cell; streamWorld.addNode(new RBMKNeutronNode(cell,cell.getRBMKType(),cell.hasLid())); }
            int moderated = 0; Iterator<BlockPos> iterator = getBlocks(fluxRange);
            while(iterator.hasNext()) {
                BlockPos targetPos=iterator.next(); if(fluxQuantity==0D)return; NeutronNode target=streamWorld.getNode(targetPos);
                if(target==null) { if(blockPosToTE(world,targetPos) instanceof BlockEntityRBMKBase cell) { target=makeNode(streamWorld,cell); streamWorld.addNode(target); } else { int hits=getHits(targetPos); if(hits==columnHeight)return; if(hits>0){irradiateFromFlux(originPos,hits);fluxQuantity*=1D-(double)hits/columnHeight;}else irradiateFromFlux(originPos,0); continue; } }
                RBMKType type=target.get(RBMKNeutronNode.COLUMN_TYPE); if(type==RBMKType.OTHER||type==null)continue; BlockEntityRBMKBase targetTE=(BlockEntityRBMKBase)target.tile;
                if(Boolean.FALSE.equals(target.get(RBMKNeutronNode.HAS_LID))) RadiationSystemNT.incrementRad((ServerLevel)world,targetPos,fluxQuantity*.05D,fluxQuantity*.05D*1024D);
                if(type==RBMKType.MODERATOR||targetTE.isModerated()){moderated++;moderateStream();}
                if(targetTE instanceof IRBMKFluxReceiver receiver) { if(type==RBMKType.ROD){BlockEntityRBMKRod rod=(BlockEntityRBMKRod)receiver;if(rod.hasRod){rod.receiveFlux(this);return;}} else if(type==RBMKType.OUTGASSER){BlockEntityRBMKOutgasser out=(BlockEntityRBMKOutgasser)receiver;if(out.canProcess()){out.receiveFlux(this);return;}} }
                else if(type==RBMKType.CONTROL_ROD){BlockEntityRBMKControl rod=(BlockEntityRBMKControl)targetTE;if(rod.level>0D){fluxQuantity*=rod.getMult();continue;}return;}
                else if(type==RBMKType.REFLECTOR){if(originTE.isModerated())moderated++;if(fluxRatio>0D&&moderated>0)for(int i=0;i<moderated;i++)moderateStream();if(reflectorEfficiency!=1D){fluxQuantity*=reflectorEfficiency;continue;}if(originTE instanceof BlockEntityRBMKRod rod)rod.receiveFlux(this);return;}
                else if(type==RBMKType.ABSORBER){targetTE.heat+=RBMKDials.getAbsorberHeatConversion(world)*fluxQuantity;if(absorberEfficiency==1D)return;fluxQuantity*=absorberEfficiency;}
            }
            NeutronNode[] nodes=getNodes(streamWorld,true);NeutronNode last=nodes[nodes.length-1];if(last==null){irradiateFromFlux(new BlockPos((int)(originPos.getX()+vector.x),originPos.getY(),(int)(originPos.getZ()+vector.z)));return;}
            if(last.get(RBMKNeutronNode.COLUMN_TYPE)==RBMKType.CONTROL_ROD){BlockEntityRBMKControl rod=(BlockEntityRBMKControl)last.tile;if(rod.getMult()>0D){fluxQuantity*=rod.getMult();BlockPos after=new BlockPos((int)(last.tile.getBlockPos().getX()+vector.x),last.tile.getBlockPos().getY(),(int)(last.tile.getBlockPos().getZ()+vector.z));if(NeutronNodeWorld.getNode(world,originPos)==null){if(blockPosToTE(world,after) instanceof BlockEntityRBMKBase cell)NeutronNodeWorld.getOrAddWorld(world).addNode(makeNode(NeutronNodeWorld.getOrAddWorld(world),cell));else irradiateFromFlux(after);}}}
        }
        public int getHits(BlockPos pos){int hits=0;Level world=origin.tile.getLevel();for(int h=0;h<columnHeight;h++)if(neutronOpaque(world.getBlockState(pos.above(h))))hits++;return hits;}
        public void irradiateFromFlux(BlockPos pos){double amount=fluxQuantity*.05D*(1D-(double)getHits(pos)/columnHeight);RadiationSystemNT.incrementRad((ServerLevel)origin.tile.getLevel(),pos,amount,Math.abs(amount)*1024D+1D);}
        public void irradiateFromFlux(BlockPos pos,int hits){double amount=fluxQuantity*.05D*(1D-(double)hits/columnHeight);RadiationSystemNT.incrementRad((ServerLevel)origin.tile.getLevel(),pos,amount,Math.abs(amount)*1024D+1D);}
        public void moderateStream(){fluxRatio*=1D-moderatorEfficiency;}
    }
}
