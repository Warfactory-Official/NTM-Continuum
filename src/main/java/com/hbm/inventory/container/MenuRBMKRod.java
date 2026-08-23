package com.hbm.inventory.container;
import com.hbm.inventory.ModRBMKMenus;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import net.minecraft.core.BlockPos; import net.minecraft.world.entity.player.*; import net.minecraft.world.inventory.Slot; import net.minecraft.world.item.ItemStack;
public final class MenuRBMKRod extends RBMKMenu<BlockEntityRBMKRod> {
 public MenuRBMKRod(int id, Inventory inv, BlockPos pos){super(ModRBMKMenus.RBMK_ROD.get(),id,inv,pos,BlockEntityRBMKRod.class,1); add();} public MenuRBMKRod(int id,Inventory inv,BlockEntityRBMKRod be){super(ModRBMKMenus.RBMK_ROD.get(),id,inv,be,1);add();}
 private void add(){addSlot(new Slot(container,0,80,45){@Override public boolean mayPlace(ItemStack s){return s.getItem() instanceof ItemRBMKRod;}});addPlayer();} @Override public ItemStack quickMoveStack(Player p,int i){return move(p,i,1,false);}
}
