package com.hbm.entity.item;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.interfaces.StoredItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code entity/item/EntityMovingPackage}: a bundle of stacks riding a conveyor belt as one object.
 *
 * <p>The contents are server-only, exactly as in 1.7 - the package renders as an opaque crate either way, so
 * nothing needs them client-side.
 */
public class EntityMovingPackage extends EntityMovingConveyorObject implements IConveyorPackage, StoredItems {

    protected List<ItemStack> contents = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : contents) changed |= visitor.visit(stack);
        if (changed) contents.removeIf(ItemStack::isEmpty);
    }

    public EntityMovingPackage(EntityType<? extends EntityMovingPackage> type, Level level) {
        super(type, level);
    }

    public EntityMovingPackage(Level level) {
        super(ModNtmEntities.MOVING_PACKAGE.get(), level);
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    /** 1.7 ItemStackUtil.carefulCopyArray: the package owns its own copies. */
    public void setItemStacks(List<ItemStack> stacks) {
        this.contents = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) this.contents.add(stack.copy());
    }

    @Override
    public List<ItemStack> getItemStacks() {
        return this.contents;
    }

    /** 1.7 interactFirst: right-clicking unpacks into the player, spilling whatever will not fit. */
    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || isRemoved()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        for (ItemStack stack : this.contents) {
            player.getInventory().placeItemBackInInventory(stack);
        }

        discard();
        return InteractionResult.SUCCESS;
    }

    /** 1.7 hitByEntity: punching it bursts the package where it stands. */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            discard();
            for (ItemStack stack : this.contents) {
                server.addFreshEntity(new ItemEntity(server, getX(), getY() + 0.125, getZ(), stack));
            }
        }
        return false;
    }

    @Override
    public void enterBlock(IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir) {
        if (isRemoved()) return;

        if (enterable.canPackageEnter(level(), pos, dir, this)) {
            enterable.onPackageEnter(level(), pos, dir, this);
            discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        discard();

        Vec3 motion = getDeltaMovement();
        for (ItemStack stack : this.contents) {
            ItemEntity item = new ItemEntity(level(),
                    getX() + motion.x * 2, getY() + motion.y * 2, getZ() + motion.z * 2, stack);
            item.setDeltaMovement(motion.x * 2, 0.1, motion.z * 2);
            level().addFreshEntity(item);
        }

        return true;
    }

    @Override
    protected void loadData(DataIn input) {
        // 1.7: kept a slot index per entry so null holes survived; 26.1 has no null stacks, so order is enough
        this.contents = new ArrayList<>(input.read("contents", ItemStack.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveData(DataOut output) {
        output.store("contents", ItemStack.CODEC.listOf(), List.copyOf(this.contents));
    }
}
