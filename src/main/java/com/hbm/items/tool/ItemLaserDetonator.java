package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.api.event.HbmEvents;
import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.SubLevels;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ItemLaserDetonator}: aim at a bomb within 500 blocks and click. The server fires it and
 * reports the return code; the client draws a line of redstone dust toward the target. (The 1.7
 * {@code IHoldableWeapon} L_ARROWS crosshair has no seam in this port's gun HUD yet.)
 */
public class ItemLaserDetonator extends CompatItem {

    private static final double RANGE = 500D;

    public ItemLaserDetonator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Aim & click to detonate!"));
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        // 1.7 Library.rayTrace always yields block coordinates (the miss position along the ray otherwise).
        BlockPos pos = hit instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(hit.getLocation());

        if (!level.isClientSide()) {
            if (level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                BombReturnCode ret = HbmEvents.detonate(bomb, level, pos, player);
                if (GeneralConfig.enableExtendedLogging) {
                    Constants.LOG.info("[DET] Tried to detonate block at {} / {} / {} by {}!", pos.getX(), pos.getY(),
                            pos.getZ(), player.getName().getString());
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.translatable(ret.getUnlocalizedMessage())
                        .withStyle(ret.wasSuccessful() ? ChatFormatting.YELLOW : ChatFormatting.RED));
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.translatable(BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            // A block on a Sable sub-level has its grid position far from where it is seen.
            Vec3 vec = SubLevels.toWorld(level, pos).subtract(player.position());
            double len = Math.min(vec.length(), 15D);
            vec = vec.normalize();
            for (int i = 0; i < len; i++) {
                double rand = level.getRandom().nextDouble() * len + 3;
                level.addParticle(DustParticleOptions.REDSTONE, player.getX() + vec.x * rand,
                        player.getY() + vec.y * rand, player.getZ() + vec.z * rand, 0, 0, 0);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
