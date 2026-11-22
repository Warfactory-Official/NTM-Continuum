package net.minecraft.world.item;
public class ItemStack { public static final ItemStack EMPTY=null; public ItemStack(ItemLike i){}
 public boolean is(Item i){return false;} public <T> boolean is(net.minecraft.tags.TagKey<T> t){return false;} public void shrink(int n){}
 public Item getItem(){return null;} public int getDamageValue(){return 0;} public void setDamageValue(int n){} public boolean isEmpty(){return false;} }
