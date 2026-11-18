import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.mcreator.horrormod.unified.SurfacePlacement;

/** Pure simulated terrain test; real Minecraft validation is still required. */
public final class SurfaceV33Test {
    static class Terrain {
        boolean roof, steep;
        int writes, stoneFill, clear;
        Map<String,String> edited=new HashMap<>();
        Terrain(boolean roof,boolean steep){this.roof=roof;this.steep=steep;}
        String xyz(int x,int y,int z){return x+","+y+","+z;}
        int height(int x,int z){return steep && x>=7 ? 94 : 60+Math.floorMod(x*17+z*31,3);}
        public int func_201676_a(Object type,int x,int z){return height(x,z);}
        public BlockState func_180495_p(BlockPos p){
            int x=p.func_177958_n(),y=p.func_177956_o(),z=p.func_177952_p();
            String v=edited.get(xyz(x,y,z));
            if(v!=null)return new BlockState(new Block(v));
            if(roof && y==126) return new BlockState(new Block("minecraft:bedrock"));
            if(y<=5)return new BlockState(new Block("minecraft:bedrock"));
            return new BlockState(new Block(y < height(x,z) ? "horrormod:deathblock" : "minecraft:air"));
        }
        public boolean func_180501_a(BlockPos p,BlockState state,int flags){
            writes++;
            String id=state.func_177230_c().toString();
            if(id.equals("minecraft:air"))clear++;else stoneFill++;
            edited.put(xyz(p.func_177958_n(),p.func_177956_o(),p.func_177952_p()),id);
            return true;
        }
    }
    static void check(boolean test,String err){if(!test)throw new AssertionError(err);}
    static BlockPos pos(int x,int y,int z){return new BlockPos(x,y,z);}
    public static void main(String[] args) throws Exception {
        Terrain world=new Terrain(false,false);
        int y=SurfacePlacement.findOpenSurface(world,"heightmap",0,0,"key");
        check(y>=60 && y<=62,"wrong exposed terrain Y: "+y);
        check(SurfacePlacement.prepare(world,pos(0,y,0),"key"),"surface preparation failed");
        check(world.stoneFill>0,"missing support/foundation");
        check(world.writes>0,"no terrain edits performed");
        // The exit origin is 2 blocks lower because its source generator adds +2.
        Terrain exit=new Terrain(false,false);
        int ey=SurfacePlacement.findOpenSurface(exit,"heightmap",0,0,"exit");
        check(ey==y-2,"exit's +2 placement wasn't compensated");
        check(SurfacePlacement.prepare(exit,pos(0,ey+2,0),"exit"),"exit preparation failed");
        check(SurfacePlacement.findOpenSurface(new Terrain(true,false),"heightmap",0,0,"key")==-1,
            "old bedrock roof was accepted as terrain");
        check(SurfacePlacement.findOpenSurface(new Terrain(false,true),"heightmap",0,0,"key")==-1,
            "unsafe cliff was accepted");
        System.out.println("PASS: exposed terrain, roof exclusion, slope, 2-block exit offset, foundation");
    }
}
