package com.hbm.items.tool;

import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.network.TileEntityPipeAnchor;
import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 1.7 {@code ItemWrench}, the pipe wrench: a steel sword ({@code 4 + 2} attack) that nudges what it hits half a
 * block along the wielder's look and clanks like a landing anvil, and never wears on a hit (1.7's
 * {@code hitEntity} returned {@code false} without calling the sword's).
 *
 * <p>Right-click (not sneaking) on a pipe anchor links it: the first click stores the anchor ("Pipe start", the 1.7
 * NBT {@code x/y/z} as the shared start-position component the wiring spool uses), the second connects the two
 * ({@link TileEntityPipeAnchor#canConnect}: same type, not the same anchor, within 10 m, same fluid - a fluid-less
 * anchor takes the other's) and clears it. While a start is stored the HUD shows the distance to it
 * ({@code WrenchClientDisplay}, 1.7's client {@code onUpdate}).
 */
public class ItemWrench extends CompatSwordItem {

    public ItemWrench(Properties properties, ToolTier tier) {
        super(properties, tier, 4.0F + tier.attackDamageBonus(), Tools.SWORD_SPEED, 0.0D, false);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null) pos = core;

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof TileEntityPipeAnchor second)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
        if (start == null) {
            ModEnergyNetContent.WIRE_START.set(stack, pos.immutable());
            if (!level.isClientSide()) player.sendSystemMessage(Component.literal("Pipe start"));
        } else if (!level.isClientSide()) {
            if (level.getBlockEntity(start) instanceof TileEntityPipeAnchor first) {
                switch (TileEntityPipeAnchor.canConnect(first, second)) {
                    case 0 -> {
                        first.addConnection(pos);
                        second.addConnection(start);
                        player.sendSystemMessage(Component.literal("Pipe end"));
                    }
                    case 1 -> player.sendSystemMessage(Component.literal("Pipe error - Pipes are not the same type"));
                    case 2 -> player.sendSystemMessage(Component.literal("Pipe error - Cannot connect to the same pipe anchor"));
                    case 3 -> player.sendSystemMessage(Component.literal("Pipe error - Pipe anchor is too far away"));
                    case 4 -> player.sendSystemMessage(Component.literal("Pipe error - Pipe anchor fluid types do not match"));
                    default -> { }
                }
            } else {
                player.sendSystemMessage(Component.literal("Pipe error"));
            }
            ModEnergyNetContent.WIRE_START.remove(stack);
        }

        player.swing(context.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity entity, LivingEntity attacker) {
        WeaponSpecial.push(entity, attacker.getLookAngle().scale(0.5));
        if (entity.level() instanceof ServerLevel world) {
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ANVIL_LAND,
                    SoundSource.PLAYERS, 3.0F, 0.75F);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
        if (start != null) {
            list.add(Component.literal("Pipe start x: " + start.getX()));
            list.add(Component.literal("Pipe start y: " + start.getY()));
            list.add(Component.literal("Pipe start z: " + start.getZ()));
        } else {
            list.add(Component.literal("Right-click anchor to connect"));
        }
    }
}
