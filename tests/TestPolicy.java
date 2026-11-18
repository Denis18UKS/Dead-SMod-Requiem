/**
 * Standalone Java 8 smoke tests for the deterministic worldgen policy.
 * These do not replace a Forge client or dedicated-server launch test.
 */
public final class TestPolicy {
    public static final class FakeWorld {
        public long getSeed() { return 918273645L; }
    }
    public static final class FakeReader {
        private final FakeWorld world = new FakeWorld();
        public FakeWorld getWorld() { return world; }
    }
    public static final class FakePos {
        private final int x, z;
        public FakePos(int x, int z) { this.x = x; this.z = z; }
        public int getX() { return x; }
        public int getZ() { return z; }
    }
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String[] ids = {"key", "exit", "overworld_dungeon", "sphere"};
        FakeReader reader = new FakeReader();
        long seed = reader.getWorld().getSeed();
        for (String id : ids) {
            int step = net.mcreator.horrormod.unified.WorldgenPolicy.spacing(id);
            require(step >= 48, "Invalid spacing: "+id);
            for (int rx = -3; rx <= 3; rx++) for (int rz = -3; rz <= 3; rz++) {
                int cx = net.mcreator.horrormod.unified.WorldgenPolicy.candidate(seed,id,rx,rz,false);
                int cz = net.mcreator.horrormod.unified.WorldgenPolicy.candidate(seed,id,rx,rz,true);
                require(Math.floorDiv(cx,step)==rx && Math.floorDiv(cz,step)==rz, "Candidate out of region: "+id);
                require(net.mcreator.horrormod.unified.WorldgenPolicy.shouldPlace(reader, new FakePos(cx*16,cz*16),id), "Candidate rejected: "+id);
                require(!net.mcreator.horrormod.unified.WorldgenPolicy.shouldPlace(reader,new FakePos((cx+step)*16,cz*16),id), "False positive: "+id);
            }
        }
        require(!net.mcreator.horrormod.unified.WorldgenPolicy.shouldPlace(reader,new FakePos(0,0),"pumpkin"),"Pumpkin structure should not generate randomly");
        int[] near=net.mcreator.horrormod.unified.WorldgenPolicy.nearest(seed,0,0);
        require(near.length==3 && near[1]==70,"Invalid nearest target");
        System.out.println("Worldgen policy smoke tests passed.");
    }
}
