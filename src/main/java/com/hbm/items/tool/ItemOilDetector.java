package com.hbm.items.tool;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.sound.ModSounds;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


/**
 * Oil Reservoir Detector. Right-click scans fixed columns around the player for an oil deposit: the player's own
 * column (bullseye), then a ring 5 blocks out (cross and diagonals) and a cross 10 blocks out. Heights are counted
 * from the world floor, where 1.7 had y = 0.
 */
public class ItemOilDetector extends CompatItem {

    /** 1.7 {@code ServerProxy.ID_DETONATOR}. */
    private static final int ID_DETONATOR = 8;

    public ItemOilDetector(Properties props) {
        super(props);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.oil_detector.desc1"));
        tooltip.add(Component.translatable("item.hbm.oil_detector.desc2"));
    }

    /** 1.7: {@code ore_oil}; any deposit a plain drill reaches ({@code hbm:fluid_deposit}, not fracking-only). */
    public static boolean isOil(Level level, BlockPos pos) {
        var deposit = DepositDataMaps.deposit(level.getBlockState(pos));
        return deposit != null && !deposit.frackingOnly();
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return hbm$use(level, player, hand);
    }

    private InteractionResult hbm$use(Level level, Player player, InteractionHand hand) {
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        int floor = level.getMinY();
        MutableBlockPos mPos = new MutableBlockPos();

        boolean direct = column(level, mPos, x, y, z, floor + 5);
        boolean oil = direct;
        for (int[] o : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {5, 5}, {-5, 5}, {5, -5}, {-5, -5}}) {
            oil |= column(level, mPos, x + o[0], y, z + o[1], floor + 5);
        }
        for (int[] o : new int[][]{{10, 0}, {-10, 0}, {0, 10}, {0, -10}}) {
            oil |= column(level, mPos, x + o[0], y, z + o[1], floor + 10);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PlayerInformPayload message;
            if (direct) {
                message = PlayerInformPayload.of(ID_DETONATOR, 0, ChatFormatting.DARK_GREEN,
                        new PlayerInformPayload.Translated("item.hbm.oil_detector.bullseye"));
            } else if (oil) {
                message = PlayerInformPayload.of(ID_DETONATOR, 0, ChatFormatting.GOLD,
                        new PlayerInformPayload.Translated("item.hbm.oil_detector.detected"));
            } else {
                message = PlayerInformPayload.of(ID_DETONATOR, 0, ChatFormatting.RED,
                        new PlayerInformPayload.Translated("item.hbm.oil_detector.noOil"));
            }
            GolemNetwork.sendToPlayer(serverPlayer, PlayerInformPayload.CODEC, message);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);

        return InteractionResult.SUCCESS;
    }

    /** Any oil from 15 above {@code y} down to, but not including, {@code bottom}. */
    private static boolean column(Level level, MutableBlockPos pos, int x, int y, int z, int bottom) {
        for (int ly = y + 15; ly > bottom; ly--) {
            if (isOil(level, pos.set(x, ly, z))) return true;
        }
        return false;
    }
}
