import net.mcreator.horrormod.unified.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.FakeSeedReader;
import net.minecraftforge.client.event.RenderWorldLastEvent;
public class OutlineV32SmokeTest {
  private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
  public static void main(String[] args) throws Exception {
    DeadHooks.bootstrap();
    check(net.minecraftforge.common.MinecraftForge.EVENT_BUS.staticSubscribers.contains(CompassEvents.class), "Missing click-event registration");
    check(net.minecraftforge.common.MinecraftForge.EVENT_BUS.staticSubscribers.contains(ClientOutline.class), "Missing client render-event registration");
    ServerWorld world=new ServerWorld();
    PlayerEntity p=new PlayerEntity();p.field_70170_p=world; p.field_70173_aa=80;
    StructureRecords.placed(new FakeSeedReader(world),new BlockPos(15,60,24),"key");
    StructureRecords.flush(world);
    DeadHooks.onPlayerTick(new Object(){public final PlayerEntity player=p;});
    check(p.field_71071_by.field_70462_a.size()==1,"Compass not granted");
    Minecraft.func_71410_x().field_71439_g=p;
    ClientOutline.render(new RenderWorldLastEvent());
    check(Tessellator.draws==2,"Expected two tessellated outline passes");
    check(BufferBuilder.totalVertices>10,"No contour vertices emitted");
    int before=BufferBuilder.totalVertices;
    p.field_71071_by.field_70462_a.clear();
    ClientOutline.render(new RenderWorldLastEvent());
    check(BufferBuilder.totalVertices==before,"Outline visible with no compass in hand");
    System.out.println("PASS: client pose matrix, tessellated outline, compass gate and bus registration");
  }
}
