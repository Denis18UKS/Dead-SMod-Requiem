public class TestPolicy {
    public static final class FakeWorld {public long getSeed() {return 918273645L;}}
    public static final class FakeReader {public FakeWorld getWorld(){return new FakeWorld();}}
    public static final class FakePos {private final int x,z;FakePos(int x,int z){this.x=x;this.z=z;}public int getX(){return x;}public int getZ(){return z;}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        for(String id:new String[]{"key","exit","overworld_dungeon","sphere"}){
            int spacing=net.mcreator.horrormod.unified.WorldgenPolicy.spacing(id);
            for(int rx=-3;rx<4;rx++)for(int rz=-3;rz<4;rz++){
                int cx=net.mcreator.horrormod.unified.WorldgenPolicy.candidate(918273645L,id,rx,rz,false);
                int cz=net.mcreator.horrormod.unified.WorldgenPolicy.candidate(918273645L,id,rx,rz,true);
                check(Math.floorDiv(cx,spacing)==rx && Math.floorDiv(cz,spacing)==rz,"cell mismatch");
                check(net.mcreator.horrormod.unified.WorldgenPolicy.shouldPlace(new FakeReader(),new FakePos(cx<<4,cz<<4),id),"candidate not accepted");
            }
        }
        check(net.mcreator.horrormod.unified.WorldgenPolicy.spacing("key")==20,"key dungeons are still too rare");
        check(net.mcreator.horrormod.unified.WorldgenPolicy.spacing("exit")==24,"exit dungeons are still too rare");
        for(String biome:new String[]{"horrormod:death","horrormod:dead_woods_biome"}){
            check(net.mcreator.horrormod.unified.WorldgenPolicy.biomeMatches("horrormod:death",biome),"key wrong biome " + biome);
            check(net.mcreator.horrormod.unified.WorldgenPolicy.biomeMatches("horrormod:dead_woods_biome",biome),"exit wrong biome " + biome);
        }
        check(!net.mcreator.horrormod.unified.WorldgenPolicy.biomeMatches("horrormod:death","minecraft:plains"),"dungeon in non-dead biome");
        check(net.mcreator.horrormod.unified.WorldgenPolicy.chooseStructureY(new java.util.Random(1),8,60)==60,"key worldgen must respect selected surface");
        check(net.mcreator.horrormod.unified.WorldgenPolicy.chooseExitStructureY(new java.util.Random(1),8,60)==60,"exit worldgen must respect selected surface");
        check(net.mcreator.horrormod.unified.WorldgenPolicy.chooseStructureYForType(50,14)==50,"surface calculation incorrect");
        check(net.mcreator.horrormod.unified.WorldgenPolicy.chooseStructureY(new java.util.Random(1),8,7)==7,"Y wrapper should not clamp a valid selected site");
        java.util.Map<String,Object> deps=new java.util.HashMap<String,Object>();
        for(String key:new String[]{"world","x","y","z"})deps.put(key,0);
        check(net.mcreator.horrormod.unified.WorldgenPolicy.allowDeathDungeon(deps),"valid structure should not be rejected by random ground check");
        deps.remove("world");
        check(!net.mcreator.horrormod.unified.WorldgenPolicy.allowDeathDungeon(deps),"malformed generation dependencies accepted");
        System.out.println("PASS: density, both dead biomes, exposed-surface Y, safe generation conditions");
    }
}
