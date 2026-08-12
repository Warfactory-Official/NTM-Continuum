package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Oil Reservoir Detector — 1:1 port of the 1.12.2 {@code ItemOilDetector}. Right-click: scans the
 * player's own column (y+15 down to the world floor) for {@code ore_oil} → green "bullseye" chat line
 * plus the world-floor cell for {@code ore_bedrock_oil} → dark-green "bullseye bedrock"; then takes 50
 * gaussian samples (range 25, σ = range/2, clamped) around the player → yellow "detected" / gold
 * "detected bedrock" / red "no oil".
 */
public class ItemOilDetector extends Item {

    public ItemOilDetector(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.oil_detector.desc1"));
        adder.accept(Component.translatable("item.hbm.oil_detector.desc2"));
    }

    public static boolean isOil(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == ModOilBlocks.ore_oil.get();
    }

    public static boolean isBedrockOil(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == ModBlocks.ore_bedrock_oil.get();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        boolean oil = false;
        boolean bedrockoil = false;
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        MutableBlockPos mPos = new MutableBlockPos();

        boolean directoil = false;
        for (int ly = y + 15; ly > level.getMinY(); ly--) {
            directoil |= isOil(level, mPos.set(x, ly, z));
            if (directoil) break;
        }
        boolean directBedrock = isBedrockOil(level, mPos.set(x, level.getMinY(), z));

        int range = 25;
        int samples = 50;

        for (int i = 0; i < samples; i++) {
            if (oil || bedrockoil) break;
            int lx = (int) Mth.clamp(level.getRandom().nextGaussian() * range / 2F, -range, range);
            int lz = (int) Mth.clamp(level.getRandom().nextGaussian() * range / 2F, -range, range);
            for (int ly = y + 15; ly > level.getMinY(); ly--) {
                oil |= isOil(level, mPos.set(x + lx, ly, z + lz));
                if (oil) break;
            }
            bedrockoil |= isBedrockOil(level, mPos.set(x + lx, level.getMinY(), z + lz));
        }

        if (!level.isClientSide()) {
            if (directBedrock) {
                player.sendSystemMessage(Component.translatable("item.hbm.oil_detector.bullseyeBedrock").withStyle(ChatFormatting.DARK_GREEN));
            } else if (directoil) {
                player.sendSystemMessage(Component.translatable("item.hbm.oil_detector.bullseye").withStyle(ChatFormatting.GREEN));
            } else if (bedrockoil) {
                player.sendSystemMessage(Component.translatable("item.hbm.oil_detector.detectedBedrock").withStyle(ChatFormatting.GOLD));
            } else if (oil) {
                player.sendSystemMessage(Component.translatable("item.hbm.oil_detector.detected").withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.translatable("item.hbm.oil_detector.noOil").withStyle(ChatFormatting.RED));
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);

        return InteractionResult.SUCCESS;
    }
}
