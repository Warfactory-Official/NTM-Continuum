package net.minecraft.world.item;
public class Item implements ItemLike { public Item(Properties p){}
 public static class Properties { public Properties durability(int n){return this;} public Properties stacksTo(int n){return this;} }
 public ItemStack getDefaultInstance(){return null;} }
