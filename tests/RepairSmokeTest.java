import net.mcreator.horrormod.unified.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.FakeSeedReader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

public class RepairSmokeTest {
  public static final class TickEvent {
    public final PlayerEntity player;
    public TickEvent(PlayerEntity p){player=p;}
  }
  private static void check(boolean value, String info){if(!value)throw new AssertionError(info);}
  public static void main(String[] args) throws Exception {
    ServerWorld world = new ServerWorld();
    PlayerEntity player = new PlayerEntity(); player.field_70170_p=world;player.field_70173_aa=80;
    DeadHooks.onPlayerTick(new TickEvent(player));
    check(player.field_71071_by.field_70462_a.size()==1,"Automatic compass grant failed");
    ItemStack compass=player.field_71071_by.field_70462_a.get(0);
    CompoundNBT tag=compass.func_77978_p();
    check(tag.func_74767_n("DeadsModStructureCompass"),"Compass NBT marker missing");
    check(tag.get("LodestonePos")==null,"Phantom destination should be absent");
    StructureRecords.placed(new FakeSeedReader(world),new BlockPos(193,65,-487),"key");
    StructureRecords.flush(world);
    player.field_70173_aa=160;
    DeadHooks.onPlayerTick(new TickEvent(player));
    check(player.field_71071_by.field_70462_a.size()==1,"Duplicate compass after another tick");
    check(compass.func_77978_p().get("LodestonePos") instanceof CompoundNBT,"Real lodestone target absent");
    StructureRecords.Site actual=StructureRecords.nearest(world,0,0);
    check(actual != null && actual.x==193 && actual.z==-487 && actual.y==65,"Actual template coordinates are not preserved");
    // If the compass is discarded, do not grant infinitely many replacements.
    player.field_71071_by.field_70462_a.clear();
    player.field_70173_aa=240;
    DeadHooks.onPlayerTick(new TickEvent(player));
    check(player.field_71071_by.field_70462_a.isEmpty(),"Repeated grant after compass discarded");
    // Test save/load round trip through the 1.16.5 SRG methods.
    StructureRecords original=world.getSavedData().computeIfAbsent(StructureRecords::new,"horrormod_requiem_structures_v2");
    CompoundNBT disk=original.func_189551_b(new CompoundNBT());
    StructureRecords restored=new StructureRecords();
    restored.func_76184_a(disk);
    java.lang.reflect.Field f=StructureRecords.class.getDeclaredField("sites");
    f.setAccessible(true);
    java.util.List<?> restoredSites=(java.util.List<?>)f.get(restored);
    check(restoredSites.size()==1,"Saved structure was not restored");
    System.out.println("PASS: auto grant, no phantom target, actual target, persistent save/load, no duplicate");
  }
}
