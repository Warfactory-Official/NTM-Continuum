package net.minecraft.world.level.block.entity;
public interface BlockEntityTicker<T extends BlockEntity> { void tick(net.minecraft.world.level.Level l,net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState s,T be); }
