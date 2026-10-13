package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.explosion.ExplosionChaos;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemDrop}, the parts that are still their own items: {@code beta} ("Beta Features") and
 * {@code crystal_xen} (Artificial Xen Crystal). Like every {@code ItemDrop} both are labelled a dangerous drop. The rest
 * of 1.7's {@code ItemDrop} chain (detonators, antimatter, singularities) lives with those items.
 *
 * <p>Dropped: a beta vanishes at once. A xen crystal, once on the ground, lifts every block in a sphere around it 75
 * blocks up ({@code ExplosionChaos.floater}, radius 25), shoves the entities there up by the same amount
 * ({@code ExplosionChaos.move}) when {@code 10.04_dropCrys} is on, and vanishes either way - 1.7 killed every grounded
 * {@code ItemDrop} after its effect, even with the effect switched off.
 *
 * <p>Carrying a beta pins the food level at 10, turning any surplus into health - see
 * {@code ModArmorSetBItems.tickPlayer}.
 */
public class ItemDrop extends CompatItem {

    public enum Kind { BETA, XEN_CRYSTAL }

    public final Kind kind;

    public ItemDrop(Properties properties) {
        this(properties, Kind.BETA);
    }

    public ItemDrop(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    /** 1.7 {@code onEntityItemUpdate}: {@code beta} setDead at once; {@code crystal_xen} on the ground. */
    public static void tickDropped(ItemEntity entity) {
        if (!(entity.getItem().getItem() instanceof ItemDrop drop)) return;
        if (drop.kind == Kind.BETA) {
            entity.discard();
            return;
        }
        if (!entity.onGround()) return;
        if (WeaponConfig.dropCrys && !entity.level().isClientSide()) {
            // 1.7 casts the position with (int): truncation toward zero, not floor
            BlockPos at = new BlockPos((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
            ExplosionChaos.floater(entity.level(), at, 25, 75);
            ExplosionChaos.move(entity.level(), at, 25, 0, 75, 0);
        }
        entity.discard();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]")
                .withStyle(ChatFormatting.RED));
    }
}
