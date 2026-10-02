package com.hbm.items.special;

import com.hbm.blocks.rail.BlockRailNTM;
import com.hbm.blocks.rail.IRailNTM.RailCheckType;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.train.EntityRailCarBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * 1.7 {@code items/special/ItemTrain}: the rail car in item form, one item per {@link EnumTrainType}.
 */
public class ItemTrain extends CompatItem {


    public final EnumTrainType type;

    public ItemTrain(Properties props, EnumTrainType type) {
        super(props);
        this.type = type;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        EnumTrainType train = type;

        if (train.engine != null) tooltip.add(Component.translatable("desc.item.train.engine", train.engine));
        tooltip.add(Component.translatable("desc.item.train.gauge", train.gauge));
        if (train.maxSpeed != null) tooltip.add(Component.translatable("desc.item.train.maxSpeed", train.maxSpeed));
        if (train.acceleration != null) {
            tooltip.add(Component.translatable("desc.item.train.acceleration", train.acceleration));
        }
        if (train.brakeThreshold != null) {
            tooltip.add(Component.translatable("desc.item.train.engineBrakeThreshold", train.brakeThreshold));
        }
        if (train.parkingBrake != null) {
            tooltip.add(Component.translatable("desc.item.train.parkingBrake", train.parkingBrake));
        }
    }

    /**
     * 1.7 asked the clicked block whether it was an IRailNTM; here only the folded core is, so the click is
     * resolved through the core index the same way the cars themselves resolve the track under them.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockRailNTM.Owner owner = new BlockRailNTM.Owner();

        if (!BlockRailNTM.railAt(level, pos, owner)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ItemStack stack = context.getItemInHand();
        EntityRailCarBase train = dev.golem.core.registry.Entities.create(type.train.get(), level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (train == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (train.getGauge() != owner.rail.getGauge(level, pos.getX(), pos.getY(), pos.getZ())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        train.setPos(context.getClickLocation());
        train.setYRot(context.getPlayer() != null ? context.getPlayer().getYRot() : 0F);

        BlockPos anchor = train.getCurrentAnchorPos();
        if (!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), 0, RailCheckType.CORE, 0)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        train.setPos(train.railResult().x, train.railResult().y, train.railResult().z);

        if (!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), train.getLengthSpan(),
                RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        double frontX = train.railResult().x, frontZ = train.railResult().z;

        if (!train.walkRail(anchor.getX(), anchor.getY(), anchor.getZ(), -train.getLengthSpan(),
                RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!level.isClientSide()) {
            train.setYRot(EntityRailCarBase.generateYaw(frontX, frontZ, train.railResult().x, train.railResult().z));
            level.addFreshEntity(train);
        }
        stack.shrink(1);

        return InteractionResult.SUCCESS;
    }

    /**
     * The tooltip lines are built here rather than read from statics of the enclosing item class: this enum names
     * the train items' registry ids, so it must initialise without the item class (and Minecraft) behind it.
     */
    public enum EnumTrainType {

        //   Engine       Gauge              Max Speed  Accel.     Eng. Brake  Parking Brake
        CARGO_TRAM(() -> ModNtmEntities.CARGO_TRAM.get(),
                Component.translatable("desc.item.train.electric"), Component.translatable("desc.item.train.standardGauge"),
                Component.translatable("desc.item.train.metresPerSecond", 10),
                Component.translatable("desc.item.train.metresPerSecondSquared", 0.2D),
                Component.translatable("desc.item.train.belowMetresPerSecond", 1), Component.translatable("desc.item.train.yes")),
        CARGO_TRAM_TRAILER(() -> ModNtmEntities.CARGO_TRAM_TRAILER.get(),
                null, Component.translatable("desc.item.train.standardGauge"), Component.translatable("desc.item.train.yes"),
                null, null, Component.translatable("desc.item.train.no"));

        public static final EnumTrainType[] VALUES = values();

        public final Supplier<EntityType<? extends EntityRailCarBase>> train;
        public final @Nullable Component engine;
        public final @Nullable Component maxSpeed;
        public final @Nullable Component acceleration;
        public final @Nullable Component brakeThreshold;
        public final @Nullable Component parkingBrake;
        public final Component gauge;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumTrainType(Supplier<EntityType<? extends EntityRailCarBase>> train, @Nullable Component engine,
                      Component gauge, @Nullable Component maxSpeed, @Nullable Component acceleration,
                      @Nullable Component brakeThreshold, @Nullable Component parkingBrake) {
            this.train = train;
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }
}
