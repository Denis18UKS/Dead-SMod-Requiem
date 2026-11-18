package net.minecraft.entity.player;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
public class PlayerEntity extends Entity {
  private final net.minecraft.nbt.CompoundNBT persisted = new net.minecraft.nbt.CompoundNBT();
  public net.minecraft.nbt.CompoundNBT getPersistentData(){return persisted;}
  public Inventory field_71071_by = new Inventory();
  public static class Inventory {
    public List<ItemStack> field_70462_a = new ArrayList<>();
    public boolean func_70441_a(ItemStack stack){return field_70462_a.add(stack);}
  }
}
