package net.minecraft.world.level.block;
import net.minecraft.world.level.block.entity.*; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.core.BlockPos;
public interface EntityBlock { BlockEntity newBlockEntity(BlockPos p,BlockState s);
 default <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level l,BlockState s,BlockEntityType<T> t){return null;} }
