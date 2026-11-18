package net.minecraftforge.registries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
public class ForgeRegistries {
  public static final Registry ITEMS = new Registry();
  public static class Registry {
    public Item getValue(ResourceLocation id){return new Item();}
  }
}
