package net.minecraft.entity.player;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayList;
import java.util.List;
public class PlayerEntity extends Entity {
  private final net.minecraft.nbt.CompoundNBT persisted = new net.minecraft.nbt.CompoundNBT();
  public net.minecraft.nbt.CompoundNBT getPersistentData(){return persisted;}
  public Inventory field_71071_by = new Inventory();
  public double x=0,y=81,z=0;
  public String message;
  public ItemStack func_184614_ca(){return field_71071_by.field_70462_a.isEmpty()?null:field_71071_by.field_70462_a.get(0);}
  public ItemStack func_184592_cb(){return null;}
  public double func_226277_ct_(){return x;}
  public double func_226278_cu_(){return y;}
  public double func_226281_cx_(){return z;}
  public void func_200619_a(ServerWorld world, double px,double py,double pz,float yaw,float pitch){x=px;y=py;z=pz;}
  public void func_146105_b(net.minecraft.util.text.StringTextComponent text, boolean actionBar){message=text.toString();}
  public static class Inventory {
    public List<ItemStack> field_70462_a = new ArrayList<>();
    public boolean func_70441_a(ItemStack stack){return field_70462_a.add(stack);}
  }
}
