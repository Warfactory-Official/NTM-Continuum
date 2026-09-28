package com.hbm.items.tool;

import com.hbm.handler.ToolKeybinds;
import com.hbm.handler.ToolKeybinds.EnumToolKey;
import com.hbm.interfaces.ICopiable;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.PlayerInformPayload.Translated;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ItemSettingsTool}: shift-right-click an {@link ICopiable} machine to copy its settings onto the
 * tool, right-click another to paste. The copied tag is the stack's whole tag in 1.7 and the
 * {@code hbm:settings_tool} component here, with the same keys ({@code tileName}, {@code copyIndex},
 * {@code inputDelay}, {@code displayInfo}). While held, the copy tool's Alt key cycles which entry pastes and
 * every 5 ticks the entries go to the info HUD (ids 897+), the selected one in aqua.
 *
 * <p>1.7 ran this from {@code onItemUseFirst}, before the machine could open its menu; here that is Core's
 * {@link dev.golem.core.world.ItemFirstUse}, registered by {@link ModUtilityToolItems}.
 */
public class ItemSettingsTool extends CompatItem {

    public ItemSettingsTool(Properties properties) {
        super(properties);
    }

    /** The input-delay counter rewrites the stack every tick; that must not re-raise the held item. */
    @Override
    public boolean compatIsSameHeldItem(ItemStack shown, ItemStack held) {
        return shown.getItem() == held.getItem();
    }

    private static CompoundTag tag(ItemStack stack) {
        return ModUtilityToolItems.SETTINGS.getOr(stack, null);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (!(entity instanceof ServerPlayer player)) return;
        CompoundTag nbt = tag(stack);

        if (player.getMainHandItem() == stack && nbt != null) {
            nbt = nbt.copy();
            int delay = Tags.getIntOr(nbt, "inputDelay", 0);
            delay++;
            ListTag displayInfo = Tags.getListOrEmpty(nbt, "displayInfo");

            if (ToolKeybinds.getKeyPressed(player, EnumToolKey.TOOL_ALT) && delay > 4) {
                int index = Tags.getIntOr(nbt, "copyIndex", 0) + 1;
                if (index > displayInfo.size() - 1) index = 0;
                nbt.putInt("copyIndex", index);
                delay = 0;
            }

            nbt.putInt("inputDelay", delay);
            ModUtilityToolItems.SETTINGS.set(stack, nbt);
            if (world.getGameTime() % 5 != 0) return;

            for (int j = 0; j < displayInfo.size(); j++) {
                CompoundTag info = Tags.getCompoundOrEmpty(displayInfo, j);
                ChatFormatting format = Tags.getIntOr(nbt, "copyIndex", 0) == j ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
                GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(897 + j, 4000,
                        format, new Translated(Tags.getStringOr(info, "info", ""))));
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Can copy the settings (filters, fluid ID, etc) of machines"));
        list.add(Component.literal("Shift right-click to copy, right click to paste"));
        list.add(Component.literal("Ctrl click on pipes to paste settings to multiple pipes"));
        CompoundTag nbt = tag(stack);
        if (nbt != null) {
            if (Tags.contains(nbt, "tileName")) {
                list.add(Component.translatable(Tags.getStringOr(nbt, "tileName", "")).withStyle(ChatFormatting.BLUE));
            } else {
                list.add(Component.literal(" None ").withStyle(ChatFormatting.RED));
            }
        }
    }

    /** 1.7 {@code onItemUseFirst}. */
    public static InteractionResult useFirst(ItemStack stack, UseOnContext context) {
        if (!(stack.getItem() instanceof ItemSettingsTool)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ICopiable copiable = getCopyInfoSource(world, pos);
        if (copiable == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (player.isShiftKeyDown()) {
            CompoundTag settings = copiable.getSettings(world, pos);
            if (settings != null) {
                settings = settings.copy();
                settings.putString("tileName", copiable.getSettingsSourceID(world, pos));
                settings.putInt("copyIndex", 0);
                settings.putInt("inputDelay", 0);
                String[] info = copiable.infoForDisplay(world, pos);
                if (info != null) {
                    ListTag displayInfo = new ListTag();
                    for (String str : info) {
                        CompoundTag nbt = new CompoundTag();
                        nbt.putString("info", str);
                        displayInfo.add(nbt);
                    }
                    settings.put("displayInfo", displayInfo);
                }
                if (!world.isClientSide()) ModUtilityToolItems.SETTINGS.set(stack, settings);

                if (world.isClientSide()) {
                    player.sendSystemMessage(prefix().append(Component.literal("Copied settings of ")
                            .append(copiable.getSettingsSourceDisplay(world, pos)).withStyle(ChatFormatting.AQUA)));
                }
            } else {
                // 1.7 printed this on both sides, so the player reads it twice in singleplayer.
                player.sendSystemMessage(prefix().append(Component.literal(
                        "Copy failed, machine has no settings tool support: ")
                        .append(copiable.getSettingsSourceDisplay(world, pos)).withStyle(ChatFormatting.RED)));
            }

        } else if (tag(stack) != null) {
            if (!world.isClientSide()) {
                CompoundTag nbt = tag(stack);
                copiable.pasteSettings(nbt.copy(), Tags.getIntOr(nbt, "copyIndex", 0), world, player, pos);
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null) be.setChanged();
            }
        }

        // 1.7 answered !isRemote, which in 1.7 still stopped the block; both sides end the click here.
        return InteractionResult.SUCCESS;
    }

    private static net.minecraft.network.chat.MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("item.hbm.settings_tool").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    private static @Nullable ICopiable getCopyInfoSource(Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof ICopiable copiable) return copiable;
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof ICopiable copiable) return copiable;
        return null;
    }
}
