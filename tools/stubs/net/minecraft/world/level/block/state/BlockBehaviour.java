package net.minecraft.world.level.block.state;
import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.level.Level; import net.minecraft.core.BlockPos;
public class BlockBehaviour {
 public static class Properties { public static Properties of(){return null;} public Properties strength(float a,float b){return this;}
  public Properties sound(net.minecraft.world.level.block.SoundType s){return this;} public Properties lightLevel(java.util.function.ToIntFunction<BlockState> f){return this;} }
 protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack s,BlockState st,Level l,BlockPos p,net.minecraft.world.entity.player.Player pl,net.minecraft.world.InteractionHand h,net.minecraft.world.phys.BlockHitResult r){return null;}
 protected net.minecraft.world.InteractionResult useWithoutItem(BlockState st,Level l,BlockPos p,net.minecraft.world.entity.player.Player pl,net.minecraft.world.phys.BlockHitResult r){return null;}
 protected void onRemove(BlockState st,Level l,BlockPos p,BlockState n,boolean m){} }
