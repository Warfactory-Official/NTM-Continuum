package com.hbm.blocks.machine;

import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine block backed by a {@link TileEntityMachineBattery}. Plain {@link Block} + {@link EntityBlock}
 * (not BaseEntityBlock) so it renders with its normal block model. Opens its menu on right-click via
 * the cross-loader platform service. Implements {@link ILookOverlay} to show its charge/throughput when
 * looked at.
 */
public class BlockMachineBattery extends CompatBlock implements TickingBlock, ILookOverlay {

    public BlockMachineBattery(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBattery(pos, state);
    }

    /** 1.7 {@code MachineBattery.maxPower}: this block's capacity; the tiers override it. */
    public long maxPower() {
        return TileEntityMachineBattery.MAX_POWER;
    }

    // --- the charge travels with the item (1.7 IPersistentNBT; the capacitor's component/loot approach) ---

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof TileEntityMachineBattery battery) {
            battery.setPower(Math.min(ModEnergyNetContent.BATTERY_POWER.getOr(stack, 0L), battery.getMaxPower()));
            battery.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        // A player break removes the block (and its block entity) before the drops are rolled, so the tile is
        // the one the loot context carries; the level lookup only answers for the explosion/other paths.
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be == null) {
            BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
            be = params.getLevel().getBlockEntity(pos);
        }
        if (be instanceof TileEntityMachineBattery battery && battery.getPower() > 0) {
            ModEnergyNetContent.BATTERY_POWER.set(stack, battery.getPower());
        }
        return List.of(stack);
    }

    /** 1.7 {@code MachineBattery.addInformation}: three gold lines and the yellow stored/max line. */
    public List<Component> persistentInfo(ItemStack stack) {
        long max = maxPower();
        List<Component> list = new ArrayList<>();
        list.add(EnergyNetBlockItem.gold("Stores up to " + Library.getShortNumber(max) + "HE"));
        list.add(EnergyNetBlockItem.gold("Charge speed: " + Library.getShortNumber(max / 200) + "HE"));
        list.add(EnergyNetBlockItem.gold("Discharge speed: " + Library.getShortNumber(max / 600) + "HE"));
        long stored = ModEnergyNetContent.BATTERY_POWER.getOr(stack, 0L);
        list.add(Component.literal(Library.getShortNumber(stored) + "/"
                + Library.getShortNumber(max) + "HE").withStyle(ChatFormatting.YELLOW));
        return list;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                Menus.openWithPos(serverPlayer, provider, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Look-overlay: charge / max, throughput (±HE/s), and a colour-coded fill percentage. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineBattery battery)) return null;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(battery.power) + "/" + Library.getShortNumber(battery.getMaxPower()) + " HE");
        if (battery.delta == 0) text.add("§e-- §r0HE/s");
        else if (battery.delta > 0) text.add("§a-> §r" + Library.getShortNumber(battery.delta) + "HE/s");
        else text.add("§c<- §r" + Library.getShortNumber(-battery.delta) + "HE/s");
        double frac = (double) battery.power / (double) battery.getMaxPower();
        text.add("&[" + Library.getColorProgress(frac) + "&]    " + Library.getPercentage(frac) + "%");

        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
