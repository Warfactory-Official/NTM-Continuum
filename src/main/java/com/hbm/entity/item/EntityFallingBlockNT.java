package com.hbm.entity.item;

import dev.golem.core.world.Compat;
import dev.golem.core.data.CompatFallingEntity;
import com.hbm.blocks.machine.ISpotlight;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 1.7's falling-block variant, rebased onto FallingBlockEntity.
 */
public class EntityFallingBlockNT extends CompatFallingEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public EntityFallingBlockNT(EntityType<? extends EntityFallingBlockNT> type, Level level) {
        super(type, level);
    }

    // 26.2: FallingBlockEntity's state constructor hard-codes EntityTypes.FALLING_BLOCK.
    public static EntityFallingBlockNT fall(ServerLevel level, BlockPos pos, BlockState state) {
        EntityFallingBlockNT entity = new EntityFallingBlockNT(com.hbm.entity.ModNtmEntities.FALLING_BLOCK_NT.get(), level);
        entity.setCarriedBlockState(state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, false)
                : state);
        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(pos);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        // 26.2: FallingBlockEntity.tick body retained
        if (this.getBlockState().isAir() || this.getBlockState().getBlock() instanceof ISpotlight) {
            // 1.7: spotlight sources are never valid falling entities.
            this.discard();
            return;
        }

        Block block = this.getBlockState().getBlock();
        this.time++;
        compatApplyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        compatApplyEffectsFromBlocks();
        compatHandlePortal();
        if (this.level() instanceof ServerLevel serverLevel && (this.isAlive() || compatForceTickAfterTeleportToDuplicate())) {
            BlockPos pos = this.blockPosition();
            boolean isConcrete = this.getBlockState().getBlock() instanceof ConcretePowderBlock;
            boolean isStuckInWater = isConcrete && this.level().getFluidState(pos)
                                                       .is(FluidTags.WATER);
            double moveVec = this.getDeltaMovement().lengthSqr();
            if (isConcrete && moveVec > 1.0) {
                BlockHitResult clip = this.level().clip(new ClipContext(
                        new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.SOURCE_ONLY, this));
                if (clip.getType() != HitResult.Type.MISS && this.level().getFluidState(clip.getBlockPos())
                                                                 .is(FluidTags.WATER)) {
                    pos = clip.getBlockPos();
                    isStuckInWater = true;
                }
            }

            // 1.7: delayed source removal + identity guard preserved
            if (this.time == 1) {
                if (!this.level().getBlockState(pos).is(block)) {
                    this.discard();
                    return;
                }
                this.level().removeBlock(pos, false);
            }

            if (!this.onGround() && !isStuckInWater) {
                if ((this.time > 100 && (pos.getY() <= this.level().getMinY() || pos.getY() > this.level()
                                                                                                  .getMaxY())) || this.time > 600) {
                    if (this.dropItem && Compat.entityDrops(serverLevel)) {
                        this.spawnAtLocation(serverLevel, block);
                    }
                    this.discard();
                }
            } else {
                BlockState currentState = this.level().getBlockState(pos);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
                if (!currentState.is(Blocks.MOVING_PISTON)) {
                    if (!this.cancelDropCompat()) {
                        boolean mayReplace = currentState.canBeReplaced(
                                new DirectionalPlaceContext(this.level(), pos, Direction.DOWN, ItemStack.EMPTY,
                                        Direction.UP));
                        // 1.7: omits vanilla's isFree continuation test
                        boolean wouldSurvive = this.getBlockState().canSurvive(this.level(), pos);
                        if (mayReplace && wouldSurvive) {
                            if (this.getBlockState().hasProperty(BlockStateProperties.WATERLOGGED)
                                    && this.level().getFluidState(pos).is(Fluids.WATER)) {
                                this.setCarriedBlockState(this.getBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
                            }

                            if (this.level().setBlock(pos, this.getBlockState(), 3)) {
                                dev.golem.core.world.Compat.sendToTrackingPlayers(this,
                                        new ClientboundBlockUpdatePacket(pos, this.level().getBlockState(pos)));
                                this.discard();
                                if (block instanceof Fallable fallable) {
                                    fallable.onLand(this.level(), pos, this.getBlockState(), currentState, this);
                                }

                                if (this.blockData != null && this.getBlockState().hasBlockEntity()) {
                                    BlockEntity blockEntity = this.level().getBlockEntity(pos);
                                    if (blockEntity != null) {
                                        try {
                                            RegistryAccess registryAccess = this.level().registryAccess();
                                            CompoundTag merged = dev.golem.core.registry.BlockEntities
                                                    .saveWithoutMetadata(blockEntity, registryAccess);
                                            dev.golem.core.data.Tags.forEach(this.blockData, (name, tag) -> merged.put(name, tag.copy()));
                                            dev.golem.core.registry.BlockEntities.loadWithComponents(blockEntity,
                                                    merged, registryAccess);
                                        } catch (Exception exception) {
                                            LOGGER.error("Failed to load block entity from falling block", exception);
                                        }
                                        blockEntity.setChanged();
                                    }
                                }
                            } else if (this.dropItem && Compat.entityDrops(serverLevel)) {
                                this.discard();
                                this.callOnBrokenAfterFall(block, pos);
                                this.spawnAtLocation(serverLevel, block);
                            }
                        } else {
                            this.discard();
                            if (this.dropItem && Compat.entityDrops(serverLevel)) {
                                this.callOnBrokenAfterFall(block, pos);
                                this.spawnAtLocation(serverLevel, block);
                            }
                        }
                    } else {
                        this.discard();
                        this.callOnBrokenAfterFall(block, pos);
                    }
                }
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D) /* FallingBlockEntity air drag */);
    }
}
