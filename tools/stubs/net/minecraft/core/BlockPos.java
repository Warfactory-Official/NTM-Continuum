package net.minecraft.core;
import net.minecraft.network.codec.StreamCodec; import io.netty.buffer.ByteBuf;
public class BlockPos extends net.minecraft.core.Vec3i {
 public static final StreamCodec<ByteBuf, BlockPos> STREAM_CODEC = null;
 public BlockPos relative(Direction d,int n){return this;} public BlockPos relative(Direction d){return this;}
 public BlockPos offset(int a,int b,int c){return this;} public BlockPos above(){return this;} public BlockPos below(){return this;}
 public BlockPos immutable(){return this;} public BlockPos subtract(Vec3i v){return this;}
 public static Iterable<BlockPos> betweenClosed(BlockPos a,BlockPos b){return null;} }
