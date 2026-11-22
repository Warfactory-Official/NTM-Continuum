package net.minecraft.world.level.block.entity;
import net.minecraft.core.*; import net.minecraft.nbt.CompoundTag; import net.minecraft.world.level.Level; import net.minecraft.world.level.block.state.BlockState;
public class BlockEntity { protected Level level; protected BlockPos worldPosition;
 public BlockEntity(BlockEntityType<?> t,BlockPos p,BlockState s){} public BlockState getBlockState(){return null;} public void setChanged(){}
 protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){} protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){}
 public CompoundTag getUpdateTag(HolderLookup.Provider r){return null;} public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return null;}
 public final CompoundTag saveWithoutMetadata(HolderLookup.Provider r){return null;} }
