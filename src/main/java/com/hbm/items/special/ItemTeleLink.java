package com.hbm.items.special;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 1.7 {@code ItemTeleLink}, the "Telelinker": right-click any block to remember it as a teleporter exit,
 * then right-click a teleporter to hand it that exit. The 1.7 NBT keys {@code x/y/z/dim} are kept inside one
 * compound component; the dimension is the level id rather than 1.7's integer.
 */
public class ItemTeleLink extends CompatItem {

    public ItemTeleLink(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // 1.7 returns false on the client and swings the arm from the server
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        BlockEntity te = level.getBlockEntity(pos);

        if (!(te instanceof TileEntityMachineTeleporter tele)) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.putString("dim", Ids.keyId(level.dimension()).toString());
            ModRadarUtilityItems.TELE_LINK.set(stack, tag);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("[TeleLink] Set teleporter exit to "
                    + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ".").withStyle(ChatFormatting.AQUA));
            player.swing(context.getHand(), true);
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = ModRadarUtilityItems.TELE_LINK.get(stack).orElse(null);
        if (tag == null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("[TeleLink] No destination set!").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        tele.setTarget(GlobalPos.of(ResourceKey.create(
                Registries.DIMENSION, Ids.parse(Tags.getStringOr(tag, "dim", "minecraft:overworld"))),
                new BlockPos(Tags.getIntOr(tag, "x", 0), Tags.getIntOr(tag, "y", 0), Tags.getIntOr(tag, "z", 0))));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("[TeleLink] Teleporters destination has been set!")
                .withStyle(ChatFormatting.AQUA));
        player.swing(context.getHand(), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        CompoundTag tag = ModRadarUtilityItems.TELE_LINK.get(stack).orElse(null);
        if (tag != null) {
            tooltip.add(Component.literal("X: " + Tags.getIntOr(tag, "x", 0)));
            tooltip.add(Component.literal("Y: " + Tags.getIntOr(tag, "y", 0)));
            tooltip.add(Component.literal("Z: " + Tags.getIntOr(tag, "z", 0)));
            tooltip.add(Component.literal("D: " + Tags.getStringOr(tag, "dim", "")));
        } else {
            tooltip.add(Component.literal("Select exit location first!").withStyle(ChatFormatting.RED));
        }
    }
}
