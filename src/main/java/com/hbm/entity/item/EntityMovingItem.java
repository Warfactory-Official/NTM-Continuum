package com.hbm.entity.item;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.interfaces.StoredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityMovingItem extends EntityMovingConveyorObject implements IConveyorItem, StoredItems {

    // 1.7 dataWatcher id 10
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(EntityMovingItem.class, EntityDataSerializers.ITEM_STACK);

    public EntityMovingItem(EntityType<? extends EntityMovingItem> type, Level level) {
        super(type, level);
    }

    public EntityMovingItem(Level level) {
        super(ModNtmEntities.MOVING_ITEM.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(STACK, ItemStack.EMPTY);
    }

    public void setItemStack(ItemStack stack) {
        entityData.set(STACK, stack);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        ItemStack stack = getItemStack();
        if (visitor.visit(stack)) visitor.afterChanges(() -> {
            if (!isRemoved() && getItemStack() == stack) setItemStack(stack.copy());
        });
    }

    /**
     * 1.7 substituted a stone stack for a null one so its renderer always had something to draw; 26.2 draws an
     * empty stack as nothing, so the placeholder is dropped.
     */
    @Override
    public ItemStack getItemStack() {
        return entityData.get(STACK);
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || isRemoved()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        player.getInventory().placeItemBackInInventory(getItemStack().copy());
        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            discard();
            server.addFreshEntity(new ItemEntity(server, getX(), getY(), getZ(), getItemStack()));
        }
        return false;
    }

    @Override
    public void enterBlock(IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir) {
        if (isRemoved()) return;

        if (enterable.canItemEnter(level(), pos, dir, this)) {
            enterable.onItemEnter(level(), pos, dir, this);
            discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        if (isRemoved()) return true;

        discard();

        Vec3 motion = getDeltaMovement();
        ItemEntity item = new ItemEntity(level(),
                getX() + motion.x * 2, getY() + motion.y * 2, getZ() + motion.z * 2, getItemStack());
        // TODO: 1.7 EntityMovingItem:125 sets the drop's lifespan to 60*20. NeoForge patches ItemEntity.lifespan
        //  and Fabric has no counterpart, so it needs a service seam rather than a common-side field.
        item.setDeltaMovement(motion.x * 2, 0.1, motion.z * 2);
        level().addFreshEntity(item);

        return true;
    }

    @Override
    protected void loadData(DataIn input) {
        setItemStack(input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        if (getItemStack().isEmpty()) discard();
    }

    @Override
    protected void saveData(DataOut output) {
        if (!getItemStack().isEmpty()) output.store("Item", ItemStack.CODEC, getItemStack());
    }
}
