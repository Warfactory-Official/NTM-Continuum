package com.hbm.tileentity.deco;

import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemKitCustom;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code TileEntityLanternBehemoth}: the old lantern's core. A broken lantern ({@code isBroken}, set by the
 * structures that place it) waits for its blowtorch repair; repairing it starts a 400-tick call for help - the
 * near horn at 360, the far horn at 280, the double near horn at 220, the double far horn at 100 - and at 0 a
 * Bobmazon shuttle falls from y 300 somewhere around it (gaussian, sigma 10) with a "Supplies" kit: 4-7 basic
 * and 4-5 advanced circuits, one vending token per point of the repairer's reputation (at least one), 6-11
 * diamonds and a poppy, or one alexandrite instead of the diamonds when that reputation was at least 10.
 */
public class TileEntityLanternBehemoth extends TileEntityLoadedBase implements IRepairable {

    public boolean isBroken = false;
    public int comTimer = -1;
    /** The repairing player's reputation, taken at the repair. */
    public int reputation = 0;

    public TileEntityLanternBehemoth(BlockPos pos, BlockState state) {
        super(ModDungeonBlockEntities.LANTERN_BEHEMOTH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLanternBehemoth lantern) {
        int t = lantern.comTimer;
        if (t == 360) lantern.horn(ModSounds.HORN_NEAR_SINGLE.get(), 10F);
        if (t == 280) lantern.horn(ModSounds.HORN_FAR_SINGLE.get(), 10000F);
        if (t == 220) lantern.horn(ModSounds.HORN_NEAR_DUAL.get(), 10F);
        if (t == 100) lantern.horn(ModSounds.HORN_FAR_DUAL.get(), 10000F);

        if (t == 0) {
            boolean bonus = lantern.reputation >= 10;
            EntityBobmazon shuttle = new EntityBobmazon(level);
            shuttle.setPos(pos.getX() + 0.5 + level.getRandom().nextGaussian() * 10, 300,
                    pos.getZ() + 0.5 + level.getRandom().nextGaussian() * 10);
            shuttle.payload = ItemKitCustom.create("Supplies", null, 0xffffff, 0x008000,
                    ModIntermediateItems.circuit(EnumCircuitType.BASIC, 4 + level.getRandom().nextInt(4)),
                    ModIntermediateItems.circuit(EnumCircuitType.ADVANCED, 4 + level.getRandom().nextInt(2)),
                    new ItemStack(ModItems.coin_token.get(), Math.max(1, lantern.reputation)),
                    bonus ? new ItemStack(ModItems.gem_alexandrite.get()) : new ItemStack(Items.DIAMOND, 6 + level.getRandom().nextInt(6)),
                    new ItemStack(Items.POPPY));
            level.addFreshEntity(shuttle);
        }

        if (lantern.comTimer >= 0) lantern.comTimer--;

        lantern.networkPackNT();
    }

    private void horn(SoundEvent sound, float volume) {
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), sound,
                SoundSource.BLOCKS, volume, 1F);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isBroken);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.isBroken = buf.readBoolean();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isBroken = input.getBooleanOr("isBroken", false);
        comTimer = input.getIntOr("comTimer", -1);
        reputation = input.getIntOr("reputation", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isBroken", isBroken);
        output.putInt("comTimer", comTimer);
        output.putInt("reputation", reputation);
    }

    public boolean isDamaged() {
        return isBroken;
    }

    private final List<AStack> repair = new ArrayList<>();

    public List<AStack> getRepairMaterials() {
        if (!repair.isEmpty()) return repair;
        repair.add(new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "plates/steel")), 2));
        repair.add(new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.BASIC), 1));
        return repair;
    }

    public void repair(Player player) {
        this.isBroken = false;
        this.comTimer = 400;
        this.reputation = HbmPlayerProps.getReputation(player);
        this.setChanged();
    }

    @Override
    public void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type) { }
}
