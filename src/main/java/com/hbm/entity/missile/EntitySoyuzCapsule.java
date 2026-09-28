package com.hbm.entity.missile;

import dev.golem.core.data.Tags;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatThrowableProjectile;
import com.hbm.interfaces.StoredItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 entity/missile/EntitySoyuzCapsule - the soyuz lander's return stage: spawned at y=600 over the target column,
 * free-falls on hand-rolled integration (1.7 replaced the throwable tick wholesale, so no super.tick()), and once its
 * own block position is occupied it discards and places a soyuz_capsule block above, loading payload + missile into it.
 * 1.7's empty {@code onImpact} override has no 26.2 equivalent - this hand-rolled tick never routes impacts.
 * 1.7's ignoreFrustumCheck is a renderer concern in 26.2 (RenderSoyuzCapsule's job).
 */
public class EntitySoyuzCapsule extends CompatThrowableProjectile implements StoredItems {

    public int soyuz;
    // 1.7: null-filled ItemStack[18]; null and EMPTY both mean "no stack" here (EntitySoyuz assigns its own array)
    public ItemStack[] payload = new ItemStack[18];

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            ItemStack stack = payload[i];
            if (stack != null && visitor.visit(stack) && stack.isEmpty()) payload[i] = ItemStack.EMPTY;
        }
    }

    public EntitySoyuzCapsule(EntityType<? extends EntitySoyuzCapsule> type, Level level) {
        super(type, level);
    }

    // 1.7: entityInit/dataWatcher stored nothing
    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    public boolean fireImmune() {
        return true; // 1.7: isImmuneToFire = true
    }

    @Override
    public void tick() {
        // 1.7 replaces EntityThrowable.onUpdate wholesale - no super.tick(), no baseTick side effects

        Vec3 motion = getDeltaMovement();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2D)
            this.setDeltaMovement(motion.x, motion.y - 0.02D, motion.z);

        if (getY() > 600)
            this.setPos(getX(), 600, getZ());

        // 1.7: (int) casts kept - truncation, not floor
        BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());

        if (!level().getBlockState(pos).isAir()) {
            this.discard();

            if (!level().isClientSide()) {
                BlockPos above = pos.above();
                // 1.7: the soyuz_capsule block above the landing spot, the payload in slots 0-17 and the spent
                // rocket (skin = this.soyuz) in slot 18
                level().setBlockAndUpdate(above, com.hbm.blocks.ModRadarUtilityBlocks.soyuz_capsule.get().defaultBlockState());
                if (level().getBlockEntity(above) instanceof com.hbm.tileentity.machine.storage.TileEntitySoyuzCapsule capsule) {
                    for (int i = 0; i < payload.length; i++) {
                        capsule.setItem(i, payload[i] == null ? ItemStack.EMPTY : payload[i]);
                    }
                    com.hbm.items.ItemEnums.SoyuzSkinType[] skins = com.hbm.items.ItemEnums.SoyuzSkinType.VALUES;
                    capsule.setItem(18, com.hbm.blocks.machine.soyuz.ModSoyuzBlocks.missile_soyuz.get()
                            .of(skins[Math.floorMod(soyuz, skins.length)]));
                }
            }
        }
    }

    // 1.7: isInRangeToRenderDist(distance < 500000); parameter was squared distance
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);

        soyuz = input.getIntOr("soyuz", soyuz);

        input.read("items", SlotEntry.CODEC.listOf()).ifPresent(list -> {
            for (SlotEntry entry : list) {
                byte b0 = entry.slot();
                if (b0 >= 0 && b0 < payload.length) {
                    payload[b0] = entry.stack();
                }
            }
        });
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);

        output.putInt("soyuz", soyuz);

        List<SlotEntry> list = new ArrayList<>();
        for (int i = 0; i < payload.length; i++) {
            if (payload[i] != null && !payload[i].isEmpty()) {
                list.add(new SlotEntry((byte) i, payload[i]));
            }
        }
        output.store("items", SlotEntry.CODEC.listOf(), list);
    }

    /**
     * One non-empty payload slot - keeps 1.7's sparse "items" list of "slot"-tagged stacks intact, gaps included.
     */
    private record SlotEntry(byte slot, ItemStack stack) {
        private static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BYTE.fieldOf("slot").forGetter(SlotEntry::slot),
                dev.golem.core.world.ItemStacks.optionalCodec().fieldOf("item").forGetter(SlotEntry::stack)
        ).apply(instance, SlotEntry::new));
    }
}
