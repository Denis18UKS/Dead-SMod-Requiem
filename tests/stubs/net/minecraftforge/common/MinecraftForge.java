package net.minecraftforge.common;
import java.util.ArrayList;
import java.util.List;
public class MinecraftForge {
 public static final TestEventBus EVENT_BUS=new TestEventBus();
 public static class TestEventBus {
  public final List<Object> staticSubscribers=new ArrayList<>();
  public void register(Object subscriber){staticSubscribers.add(subscriber);}
  public void register(Class<?> subscriber){throw new AssertionError("Wrong overload");}
 }
}
