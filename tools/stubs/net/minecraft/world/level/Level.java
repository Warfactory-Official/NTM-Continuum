package net.minecraft.world.level;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.level.block.entity.BlockEntity;
public class Level { public boolean isClientSide; public enum ExplosionInteraction { BLOCK }
 public BlockState getBlockState(BlockPos p){return null;} public boolean setBlock(BlockPos p,BlockState s,int f){return true;}
 public BlockEntity getBlockEntity(BlockPos p){return null;}
 public void playSound(net.minecraft.world.entity.player.Player pl,BlockPos p,net.minecraft.sounds.SoundEvent e,net.minecraft.sounds.SoundSource s,float v,float pi){}
 public boolean removeBlock(BlockPos p,boolean b){return true;}
 public Object explode(Object e,double x,double y,double z,float r,ExplosionInteraction i){return null;}
 public void sendBlockUpdated(BlockPos p,BlockState a,BlockState b,int f){}
 public <T,C> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T,C> c,BlockPos p,C ctx){return null;} }
