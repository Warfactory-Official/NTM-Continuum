package net.minecraft.world.level.block.entity;
public class BlockEntityType<T extends BlockEntity> {
 public interface BlockEntitySupplier<T extends BlockEntity> { T create(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s); }
 public static class Builder<T extends BlockEntity> { public static <T extends BlockEntity> Builder<T> of(BlockEntitySupplier<? extends T> s,net.minecraft.world.level.block.Block... b){return null;}
  public BlockEntityType<T> build(Object o){return null;} } }
