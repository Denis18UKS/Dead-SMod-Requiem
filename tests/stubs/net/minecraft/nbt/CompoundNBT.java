package net.minecraft.nbt;
import java.util.HashMap;
import java.util.Map;
public class CompoundNBT {
  private final Map<String,Object> tags=new HashMap<>();
  public void func_74778_a(String key,String value){tags.put(key,value);}
  public String func_74779_i(String key){return (String)tags.getOrDefault(key,"");}
  public void func_74757_a(String key,boolean value){tags.put(key,value);}
  public boolean func_74767_n(String key){return Boolean.TRUE.equals(tags.get(key));}
  public void func_74768_a(String key,int value){tags.put(key,value);}
  public void func_218657_a(String key, CompoundNBT value){tags.put(key,value);}
  public void func_82580_o(String key){tags.remove(key);}
  public Object get(String key){return tags.get(key);}
}
