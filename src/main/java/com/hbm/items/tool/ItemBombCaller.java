package com.hbm.items.tool;

import com.hbm.entity.logic.EntityBomber;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.core.BlockPos;
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

/**
 * 1.7 {@code ItemBombCaller} ("Airstrike Designator"): aim within 500 blocks and click to call an
 * {@link EntityBomber} of the given payload onto that spot; the designator is consumed. The 1.7
 * metadata (0-7) is one item per {@link Type}; only the first five were listed in the creative tab.
 */
public class ItemBombCaller extends CompatItem {

    public enum Type {
        CARPET("Carpet bombing"), NAPALM("Napalm"), CHLORINE("Poison gas"), ORANGE("Agent orange"),
        ABOMB("Atomic bomb"), STINGER("VT stinger rockets"), BOXCAR("PIP OH GOD"), PC("Cloud the cloud oh god the cloud");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    private static final double RANGE = 500D;

    public final Type type;

    public ItemBombCaller(Properties properties, Type type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Aim & click to call an airstrike!"));
        tooltip.add(Component.literal("Type: " + type.label));
    }

    /** 1.7 hasEffect: the atomic and later designators glint. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return type.ordinal() >= 4;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        BlockPos pos = hit instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(hit.getLocation());

        if (!level.isClientSide()) {
            EntityBomber bomber = switch (type) {
                case NAPALM -> EntityBomber.statFacNapalm(level, pos.getX(), pos.getY(), pos.getZ());
                case CHLORINE -> EntityBomber.statFacChlorine(level, pos.getX(), pos.getY(), pos.getZ());
                case ORANGE -> EntityBomber.statFacOrange(level, pos.getX(), pos.getY(), pos.getZ());
                case ABOMB -> EntityBomber.statFacABomb(level, pos.getX(), pos.getY(), pos.getZ());
                case STINGER -> EntityBomber.statFacStinger(level, pos.getX(), pos.getY(), pos.getZ());
                case BOXCAR -> EntityBomber.statFacBoxcar(level, pos.getX(), pos.getY(), pos.getZ());
                case PC -> EntityBomber.statFacPC(level, pos.getX(), pos.getY(), pos.getZ());
                default -> EntityBomber.statFacCarpet(level, pos.getX(), pos.getY(), pos.getZ());
            };
            level.addFreshEntity(bomber); // 1.7: WorldUtil.loadAndSpawnEntityInWorld
            player.sendSystemMessage(Component.literal("Called in airstrike!"));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
