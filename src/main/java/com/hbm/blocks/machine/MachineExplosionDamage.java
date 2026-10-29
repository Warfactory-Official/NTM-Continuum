package com.hbm.blocks.machine;

import com.hbm.interfaces.IRepairable;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code MachineFluidTank.onBlockExploded} / {@code MachineRefinery.onBlockExploded} and their
 * {@code IRepairable} blowtorch repair, shared by the two blocks. Any explosion that reaches a cell hits the machine
 * once ({@code lastExplosion}): an intact machine is blown open ({@code explode}), one already blown open is removed.
 * The cells never drop anything and are never broken by the blast itself.
 *
 * <p>The napalm route to {@code inferno}: when the exploder is a bomber's bomblet ({@code EntityBombletZeta}) every
 * player within 100 blocks gets the achievement - for the tank only if its fluid is flammable. (1.7 created the
 * bomblet's {@code ExplosionVNT} without an exploder, so the check could never pass there; the port's bomblet names
 * itself as the exploder so the route exists, as the achievement's text describes.)
 */
public final class MachineExplosionDamage {

    private MachineExplosionDamage() {
    }

    /**
     * Returns true when {@code pos} belongs to a tank or refinery, which then owns the hit.
     */
    public static boolean hit(ServerLevel level, BlockPos pos, Object explosion, @Nullable Entity exploder) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return false;
        BlockEntity be = level.getBlockEntity(core);

        if (be instanceof TileEntityMachineFluidTank tank) {
            if (tank.lastExplosion == explosion) return true;
            tank.lastExplosion = explosion;
            if (!tank.hasExploded) {
                tank.explode();
                if (exploder instanceof EntityBombletZeta && FluidCombustion.isFlammable(tank.tank.getTankType())) {
                    inferno(level, pos, DetonationTrigger.Kind.FLUID_TANK);
                }
            } else {
                level.removeBlock(core, false);
            }
            return true;
        }

        if (be instanceof TileEntityMachineRefinery refinery) {
            if (refinery.lastExplosion == explosion) return true;
            refinery.lastExplosion = explosion;
            if (!refinery.hasExploded) {
                refinery.explode();
                if (exploder instanceof EntityBombletZeta) inferno(level, pos, DetonationTrigger.Kind.REFINERY);
            } else {
                level.removeBlock(core, false);
            }
            return true;
        }
        return false;
    }

    /**
     * 1.7: {@code triggerAchievement(achInferno)} for every player in the hit cell's box grown by 100.
     */
    private static void inferno(ServerLevel level, BlockPos pos, DetonationTrigger.Kind kind) {
        AABB box = new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(100);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            HbmCriteria.detonation(player, kind);
        }
    }

    /**
     * 1.7 {@code IRepairable.tryRepairMultiblock} for the two machines; true when something was repaired.
     */
    public static boolean repair(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return false;
        return IRepairable.tryRepairMultiblock(level, core, player);
    }

    /**
     * hbm-space's repair overlay rule: a damaged machine always lists its repair, with a
     * "Tool: Blowtorch" line while no blowtorch is held. False (1.7): the list shows only with a blowtorch in hand.
     * NTM: Space sets it.
     */
    public static boolean alwaysShowRepair = false;

    /**
     * 1.7 {@code IRepairable.addGenericOverlay}: with a blowtorch in hand, a damaged machine lists its repair.
     */
    public static ILookOverlay.@Nullable Data overlay(Level level, BlockPos pos, Player player, String title) {
        ItemStack held = player.getMainHandItem();
        boolean torch = ToolType.getType(held) == ToolType.TORCH;
        if (!torch && !alwaysShowRepair) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null) return null;
        BlockEntity be = level.getBlockEntity(core);
        List<AStack> materials;
        if (be instanceof TileEntityMachineFluidTank tank && tank.isDamaged()) materials = tank.getRepairMaterials();
        else if (be instanceof TileEntityMachineRefinery refinery && refinery.isDamaged())
            materials = refinery.getRepairMaterials();
        else return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GOLD + Component.translatable("desc.repair.with").getString());
        if (!torch) text.add(Component.translatable("desc.repair.tool").getString());
        for (AStack stack : materials) {
            try {
                ItemStack display = stack.getCyclingDisplayStack();
                text.add("- " + display.getHoverName().getString() + " x" + stack.stacksize);
            } catch (Exception ex) {
                text.add(ChatFormatting.RED + "- ERROR");
            }
        }
        return new ILookOverlay.Data(title, 0xffff00, 0x404000, text);
    }

    /**
     * 1.7 {@code onBlockActivated}: a blown-open machine opens no GUI.
     */
    public static boolean blownOpen(Level level, BlockPos core) {
        BlockEntity be = level.getBlockEntity(core);
        return be instanceof TileEntityMachineFluidTank tank && tank.hasExploded
                || be instanceof TileEntityMachineRefinery refinery && refinery.hasExploded;
    }
}
