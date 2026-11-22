package net.minecraft.world.entity.player;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.InteractionHand;
public class Player { public void displayClientMessage(net.minecraft.network.chat.Component c,boolean b){} public ItemStack getMainHandItem(){return null;}
 public boolean isCreative(){return false;} public Inventory getInventory(){return null;} public void drop(ItemStack s,boolean b){}
 public void setItemInHand(InteractionHand h,ItemStack s){} public net.minecraft.world.level.Level level(){return null;} public double distanceToSqr(net.minecraft.world.phys.Vec3 v){return 0;} }

