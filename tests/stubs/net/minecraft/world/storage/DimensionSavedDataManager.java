package net.minecraft.world.storage;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Supplier;
public class DimensionSavedDataManager {
  private final Map<String,WorldSavedData> saved = new HashMap<>();
  public <T extends WorldSavedData> T computeIfAbsent(Supplier<T> supplier, String name){return (T)saved.computeIfAbsent(name,x->supplier.get());}
}
