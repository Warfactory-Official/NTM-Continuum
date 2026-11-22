package net.minecraft.world.item;
import java.util.function.Supplier;
public class CreativeModeTab { public static Builder builder(){return null;}
 public interface DisplayItemsGenerator { void accept(Object params, Output out); }
 public interface Output { void accept(ItemLike i); }
 public static class Builder { public Builder title(net.minecraft.network.chat.Component c){return this;} public Builder icon(Supplier<ItemStack> s){return this;}
  public Builder displayItems(DisplayItemsGenerator g){return this;} public CreativeModeTab build(){return null;} } }
