import net.mcreator.horrormod.unified.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.FakeSeedReader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class CompassV3SmokeTest {
  public static class DimensionEvent {
    private final PlayerEntity player;
    public DimensionEvent(PlayerEntity p){this.player=p;}
    public Object getTo(){return "ResourceKey[minecraft:dimension / horrormod:death_dim]";}
    public PlayerEntity getPlayer(){return player;}
  }
  private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  public static void main(String[] args) throws Exception {
    ServerWorld world=new ServerWorld();
    PlayerEntity player=new PlayerEntity();player.field_70170_p=world;
    StructureRecords.placed(new FakeSeedReader(world),new BlockPos(15,60,24),"key");
    StructureRecords.placed(new FakeSeedReader(world),new BlockPos(240,72,100),"exit");
    StructureRecords.flush(world);
    // The original spawn point is inside a wall: safe entrance must move it to x=1.
    DeadHooks.onDeathDimensionEntry(new DimensionEvent(player));
    System.out.println("Spawn after event="+player.x+","+player.y+","+player.z); check(player.x==1.5 && player.y==81.0,"Safe spawn did not relocate blocked player");
    check(player.field_71071_by.field_70462_a.size()==1,"Automatic compass was not granted");
    ItemStack stack=player.field_71071_by.field_70462_a.get(0);
    CompoundNBT tag=stack.func_77978_p();
    check("key".equals(tag.func_74779_i("RequiemMode")),"Default compass mode must be key");
    check(tag.func_74762_e("CustomModelData")==314159,"Missing key texture selector");
    CompoundNBT key=(CompoundNBT)tag.get("LodestonePos");
    check(key!=null && key.func_74762_e("X")==15,"Key compass target differs from actual structure");
    player.field_70173_aa=123;
    PlayerInteractEvent.RightClickItem event=new PlayerInteractEvent.RightClickItem(player,stack,world);
    DeadHooks.onCompassInteract(event);
    check(event.isCanceled(),"Compass click was not consumed");
    check("exit".equals(tag.func_74779_i("RequiemMode")),"Click did not switch to exit mode");
    check(tag.func_74762_e("CustomModelData")==314160,"Missing exit texture selector");
    CompoundNBT exit=(CompoundNBT)tag.get("LodestonePos");
    check(exit!=null && exit.func_74762_e("X")==240,"Exit compass target differs from actual structure");
    // Double event in same tick (RightClickBlock + RightClickItem) cannot toggle back.
    DeadHooks.onCompassInteract(new PlayerInteractEvent.RightClickBlock(player,stack,world));
    check("exit".equals(tag.func_74779_i("RequiemMode")),"Click processed twice in same tick");
    player.field_70173_aa=124;
    DeadHooks.onCompassInteract(new PlayerInteractEvent.RightClickItem(player,stack,world));
    check("key".equals(tag.func_74779_i("RequiemMode")),"Second actual click did not switch back");
    System.out.println("PASS: blocked spawn relocated; compass granted; key/exit RMB target + 2 textures + double-event guard");
  }
}
