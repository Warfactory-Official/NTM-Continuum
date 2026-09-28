package com.hbm.entity.missile;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModMissileItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The stealth missile — a tier-2 airframe with a modest warhead whose value is that radar cannot see
 * it, and that an {@link EntityMissileAntiBallistic} refuses to lock onto it.
 *
 * <p>Its rare drop upstream is ash ({@code powder_ash} with the MISC subtype), which is part of the
 * un-ported {@code ItemEnums} ash family; it drops nothing rare until that lands.
 */
public class EntityMissileStealth extends EntityMissileBaseNT {

    public EntityMissileStealth(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMissileStealth(Level world, float x, float y, float z, int a, int b) {
        super(ModMissileEntities.MISSILE_STEALTH.get(), world, x, y, z, a, b);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        // Upstream drops steel bolts; the autogen family keys them by (shape, material).
        list.add(new ItemStack(ModAutogenItems.get(MaterialShapes.BOLT, Mats.MAT_STEEL), 4));
        return list;
    }

    @Override
    public void onMissileImpact(HitResult hit) {
        this.explodeStandard(20F, 24, false);
    }

    @Override
    public @Nullable ItemStack getDebrisRareDrop() {
        return null;
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(ModMissileItems.missile_stealth.get());
    }

    /** 1.7: the whole point of the airframe. */
    @Override
    public boolean canBeSeenBy(Object radar) {
        return false;
    }
}
