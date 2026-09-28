package com.hbm.items.tool;

import com.hbm.util.FertilizerUtil;
import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemFertilizer} (industrial fertilizer): bone meal over the 3x3x3 cube around the clicked block.
 * The clicked block itself is forced to grow, its neighbours roll their own chance. One powder is spent if
 * anything accepted it. 1.7 answered {@code false} from {@code onItemUse} in every case, which passes.
 *
 * <p>Not reproduced: the dispenser behaviour ({@code DispenserBehaviorHandler}); the port has no dispenser
 * registration seam yet.
 */
public class ItemFertilizer extends CompatItem {

    public ItemFertilizer(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos center = context.getClickedPos();

        if (player != null && !player.mayUseItemAt(center, context.getClickedFace(), stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        boolean didSomething = false;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos pos = center.offset(i, j, k);
                    boolean success = FertilizerUtil.fertilize(level, pos, player, stack, i == 0 && j == 0 && k == 0);
                    didSomething = didSomething || success;
                    if (success && !level.isClientSide()) FertilizerUtil.growthParticles(level, pos);
                }
            }
        }

        if (didSomething && (player == null || !player.getAbilities().instabuild)) stack.shrink(1);
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
