package com.hbm.entity.train;

import com.hbm.blocks.rail.IRailNTM.TrackGauge;
import com.hbm.inventory.container.MenuTrainCargoTramTrailer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * <pre>
 *                         &lt;--
 *
 * _O\____________________________________________/O_
 * |____|                                      |____|
 *    \__________________________________________/
 *        '( + )'                      '( + )'
 * </pre>
 */
public class TrainCargoTramTrailer extends EntityRailCarCargo {

    public static final int SLOT_COUNT = 45;

    // constants, not per-tick garbage: the base walks getDummies() every tick
    private static final DummyConfig[] DUMMIES = {
            new DummyConfig(2F, 1F, 0, 0, 1.5),
            new DummyConfig(2F, 1F, 0, 0, 0),
            new DummyConfig(2F, 1F, 0, 0, -1.5)
    };

    public TrainCargoTramTrailer(EntityType<? extends TrainCargoTramTrailer> type, Level level) {
        super(type, level);
    }

    @Override public double getMaxRailSpeed() { return 1; }
    @Override public TrackGauge getGauge() { return TrackGauge.STANDARD; }
    @Override public double getLengthSpan() { return 1.5; }
    @Override public double getCollisionSpan() { return 2.5; }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public DummyConfig[] getDummies() { return DUMMIES; }
    @Override public double getCouplingDist(TrainCoupling coupling) { return coupling != null ? 2.75 : 0; }
    @Override public double getCurrentSpeed() { return 0; }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if(!this.isRemoved()) this.discard();
        return true;
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        InteractionResult coupled = super.onInteract(player, hand);
        if(coupled != InteractionResult.PASS) return coupled;

        if(player instanceof ServerPlayer server) server.openMenu(this);

        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return this.hasCustomName() ? this.getCustomName() : Component.translatable("container.trainTramTrailer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new MenuTrainCargoTramTrailer(containerId, playerInv, this);
    }
}
