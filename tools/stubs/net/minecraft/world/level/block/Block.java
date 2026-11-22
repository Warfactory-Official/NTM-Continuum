package net.minecraft.world.level.block;
import net.minecraft.world.level.block.state.*;
public class Block extends BlockBehaviour { protected final StateDefinition<Block,BlockState> stateDefinition=null;
 public Block(BlockBehaviour.Properties p){} protected final void registerDefaultState(BlockState s){} public final BlockState defaultBlockState(){return null;}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){} public net.minecraft.network.chat.MutableComponent getName(){return null;}
 public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return null;} }
