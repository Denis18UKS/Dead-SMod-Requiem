package net.minecraftforge.registries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
public class ForgeRegistries {
  public static final ItemRegistry ITEMS = new ItemRegistry();
  public static final BlockRegistry BLOCKS = new BlockRegistry();
  public static class ItemRegistry {
    public Item getValue(ResourceLocation id){return new Item();}
  }
  public static class BlockRegistry {
    public Block getValue(ResourceLocation id){return new Block(id.id);}
    public ResourceLocation getKey(Block block){return new ResourceLocation(block.toString());}
  }
}
