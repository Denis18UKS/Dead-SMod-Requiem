import jdk.internal.org.objectweb.asm.*;
import jdk.internal.org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Offline bytecode-level patcher: keeps every original class and resource byte-for-byte except explicitly modified classes. */
public final class PatchJar implements Opcodes {
    private static final String HOOK = "net/mcreator/horrormod/unified/DeadHooks";
    private static final String GEN = "net/mcreator/horrormod/unified/WorldgenPolicy";
    private static final Map<String,String> GENERATORS = new HashMap<>();
    static {
        GENERATORS.put("DeathSTRStructure", "key");
        GENERATORS.put("ExitStructuredeadStructure", "exit");
        GENERATORS.put("NewDangeuonStructure", "overworld_dungeon");
        GENERATORS.put("SphereOsvoboditelStructure", "sphere");
        GENERATORS.put("PumpkinplatformStructure", "pumpkin");
    }

    private static boolean modify(String path, ClassNode node) {
        String c = "net/mcreator/horrormod/";
        boolean modified = false;
        for (Map.Entry<String,String> entry : GENERATORS.entrySet()) {
            if (path.equals(c+"world/structure/"+entry.getKey()+"$FeatureRegisterHandler$1.class")) {
                boolean hit = false;
                for (MethodNode method : node.methods) {
                    if (!method.name.equals("generate") || !method.desc.contains("NoFeatureConfig")) continue;
                    InsnList added = new InsnList();
                    LabelNode accepted = new LabelNode();
                    added.add(new VarInsnNode(ALOAD, 1)); // ISeedReader
                    added.add(new VarInsnNode(ALOAD, 4)); // BlockPos
                    added.add(new LdcInsnNode(entry.getValue()));
                    added.add(new MethodInsnNode(INVOKESTATIC, GEN, "shouldPlace", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Z", false));
                    added.add(new JumpInsnNode(IFNE, accepted));
                    added.add(new InsnNode(ICONST_0));
                    added.add(new InsnNode(IRETURN));
                    added.add(accepted);
                    added.add(new FrameNode(F_SAME, 0, null, 0, null));
                    method.instructions.insert(added);
                    if (entry.getValue().equals("key") || entry.getValue().equals("exit")) {
                        int surface = 0, grounds = 0;
                        for (AbstractInsnNode ins : method.instructions.toArray()) {
                            if (!(ins instanceof MethodInsnNode)) continue;
                            MethodInsnNode call = (MethodInsnNode) ins;
                            if (call.owner.equals("net/minecraft/util/math/MathHelper")
                                    && call.name.equals("func_76136_a") && call.desc.equals("(Ljava/util/Random;II)I")) {
                                call.owner = GEN;
                                call.name = entry.getValue().equals("exit") ? "chooseExitStructureY" : "chooseStructureY";
                                surface++;
                            }
                            if ((call.owner.equals("net/mcreator/horrormod/procedures/TestProcedure") || call.owner.equals("net/mcreator/horrormod/procedures/TestOlegProcedure"))
                                    && call.name.equals("executeProcedure")
                                    && call.desc.equals("(Ljava/util/Map;)Z")) {
                                call.owner = GEN;
                                call.name = "allowDeathDungeon";
                                call.desc = "(Ljava/lang/Object;)Z";
                                grounds++;
                            }
                        }
                        if (surface != 1 || grounds != 1)
                            throw new IllegalStateException("Expected one height choice and terrain gate in " + path + ", got " + surface + "/" + grounds);


                        int calls = 0;
                        for (AbstractInsnNode ins : method.instructions.toArray()) {
                            if (!(ins instanceof MethodInsnNode)) continue;
                            MethodInsnNode call = (MethodInsnNode) ins;
                            if (call.owner.equals("net/minecraft/world/gen/feature/template/Template")
                                    && call.name.equals("func_237144_a_")) {
                                InsnList recorded = new InsnList();
                                recorded.add(new VarInsnNode(ALOAD, 1));
                                recorded.add(new VarInsnNode(ALOAD, 17)); // Actual NBT placement origin
                                recorded.add(new LdcInsnNode(entry.getValue()));
                                recorded.add(new MethodInsnNode(INVOKESTATIC,
                                        "net/mcreator/horrormod/unified/StructureRecords", "placed",
                                        "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)V", false));
                                method.instructions.insert(ins, recorded);
                                calls++;
                            }
                        }
                        if (calls != 1) throw new IllegalStateException("Expected one actual template placement in " + path + ", got " + calls);
                    }
                    if (!entry.getValue().equals("pumpkin")) {
                        int chance = 0;
                        for (AbstractInsnNode ins : method.instructions.toArray()) {
                            if (ins instanceof LdcInsnNode && Integer.valueOf(1000000).equals(((LdcInsnNode)ins).cst)) {
                                method.instructions.set(ins, new InsnNode(ICONST_1));
                                chance++;
                            }
                        }
                        if (chance != 1) throw new IllegalStateException("Expected one original chance gate in " + path + ", got " + chance);
                    }
                    hit=true; modified=true;
                }
                if (!hit) throw new IllegalStateException("Missing worldgen method: "+path);
            }
        }
        // In the original mod, key and exit dungeons were registered for two
        // mutually exclusive biomes. Allow both in either dead-dimension biome.
        if (path.equals(c+"world/structure/DeathSTRStructure.class") ||
            path.equals(c+"world/structure/ExitStructuredeadStructure.class")) {
            int count = 0;
            for (MethodNode method : node.methods) if (method.name.equals("addFeatureToBiomes")) {
                for (AbstractInsnNode ins : method.instructions.toArray()) {
                    if (!(ins instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)ins;
                    if (call.owner.equals("net/minecraft/util/ResourceLocation") && call.name.equals("equals")
                            && call.desc.equals("(Ljava/lang/Object;)Z")) {
                        call.setOpcode(INVOKESTATIC);
                        call.owner=GEN;call.name="biomeMatches";
                        call.desc="(Ljava/lang/Object;Ljava/lang/Object;)Z";
                        call.itf=false;
                        count++;
                    }
                }
            }
            if (count != 1) throw new IllegalStateException("Cannot patch Dead Dimension biome registration for " + path);
            modified = true;
        }
        if (path.equals(c+"HorrormodMod.class")) {
            boolean installed=false;
            for (MethodNode m:node.methods) if (m.name.equals("<init>") && m.desc.equals("()V")) {
                for (AbstractInsnNode ins:m.instructions.toArray()) if (ins.getOpcode()==RETURN) {
                    InsnList hook=new InsnList();
                    hook.add(new MethodInsnNode(INVOKESTATIC,HOOK,"bootstrap","()V",false));
                    m.instructions.insertBefore(ins,hook);
                    installed=true;
                }
            }
            if(!installed)throw new IllegalStateException("Cannot install Requiem event subscriber bootstrap");
            modified=true;
        }
        if (path.equals(c+"world/dimension/SDDimension.class")) {
            insertAfterOriginal(node, "onPlayerChangedDimensionEvent", HOOK, "onSecretDimensionEntry");
            modified = true;
        }
        if (path.equals(c+"world/dimension/DeathDIMDimension.class")) {
            insertAfterOriginal(node, "onPlayerChangedDimensionEvent", HOOK, "onDeathDimensionEntry");
            modified = true;
        }
        if (path.equals(c+"procedures/InOverWordlMusicBiomeProcedure$GlobalTrigger.class")) {
            boolean found = false;
            for (MethodNode method:node.methods) if (method.name.equals("onPlayerTick")) {
                InsnList instructions = new InsnList();
                instructions.add(new VarInsnNode(ALOAD, 0));
                instructions.add(new MethodInsnNode(INVOKESTATIC, HOOK, "onPlayerTick", "(Ljava/lang/Object;)V", false));
                method.instructions.insert(instructions);
                found = true;
            }
            if (!found) throw new IllegalStateException("Missing PlayerTick callback");
            modified = true;
        }
        // Keep progression/time/achievement/teleport logic of original procedure. Only
        // suppress the original template placement at player's variable coordinates.
        // DeadHooks places this same NBT template at a stable spawn once instead.
        if (path.equals(c+"procedures/SDIesliIghrokVkhoditVIzmierieniieProcedure.class")) {
            int replaced = 0;
            for (MethodNode method:node.methods) if (method.name.equals("executeProcedure") && method.desc.equals("(Ljava/util/Map;)V")) {
                for (AbstractInsnNode ins : method.instructions.toArray()) {
                    if (ins instanceof MethodInsnNode) {
                        MethodInsnNode m = (MethodInsnNode)ins;
                        if (m.owner.equals("net/minecraft/world/gen/feature/template/Template") && m.name.equals("func_237144_a_")) {
                            InsnList dropArgs = new InsnList();
                            for (int i=0;i<5;i++) dropArgs.add(new InsnNode(POP));
                            method.instructions.insertBefore(ins,dropArgs);
                            method.instructions.remove(ins);
                            replaced++;
                        }
                    }
                }
            }
            if (replaced != 1) throw new IllegalStateException("Expected one template placement in secret procedure, got "+replaced);
            modified=true;
        }
        return modified;
    }

    private static void insertAfterOriginal(ClassNode node, String methodName, String hookOwner, String hookMethod) {
        boolean found=false;
        for (MethodNode method:node.methods) if (method.name.equals(methodName)) {
            for (AbstractInsnNode ins : method.instructions.toArray()) if (ins.getOpcode()==RETURN) {
                InsnList list=new InsnList();
                list.add(new VarInsnNode(ALOAD, 1));
                list.add(new MethodInsnNode(INVOKESTATIC, hookOwner, hookMethod, "(Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(ins,list);
                found=true;
            }
        }
        if (!found) throw new IllegalStateException("Missing dimension event: "+node.name+"."+methodName);
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: PatchJar <original.jar> <classes_dir> <out.jar>");
        Path original=Paths.get(args[0]), classFolder=Paths.get(args[1]), result=Paths.get(args[2]);
        Set<String> altered=new TreeSet<>();
        Set<String> overwritten=new HashSet<>();
        Path resources=Paths.get("src/main/resources");
        if(Files.isDirectory(resources))try(java.util.stream.Stream<Path> files=Files.walk(resources)){
            files.filter(Files::isRegularFile).forEach(file -> overwritten.add(resources.relativize(file).toString().replace('\\','/')));
        }
        Files.createDirectories(result.toAbsolutePath().getParent());
        try (JarFile input=new JarFile(original.toFile()); JarOutputStream output=new JarOutputStream(Files.newOutputStream(result))) {
            Enumeration<JarEntry> all=input.entries();
            while (all.hasMoreElements()) {
                JarEntry entry=all.nextElement();
                String path=entry.getName();
                if (overwritten.contains(path))continue;
                if (path.startsWith("META-INF/") && (path.endsWith(".SF") || path.endsWith(".RSA") || path.endsWith(".DSA"))) continue;
                byte[] data;
                try (InputStream in=input.getInputStream(entry)) {data=readAll(in);}
                if (path.endsWith(".class")) {
                    ClassNode node=new ClassNode();
                    new ClassReader(data).accept(node, 0);
                    if (modify(path,node)) {
                        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        node.accept(writer);
                        data=writer.toByteArray();
                        altered.add(path);
                    }
                }
                JarEntry newEntry = new JarEntry(path);
                newEntry.setTime(0L);
                output.putNextEntry(newEntry);
                output.write(data);
                output.closeEntry();
            }
            if(Files.isDirectory(resources))try(java.util.stream.Stream<Path> files=Files.walk(resources)){
                for(Path file:(Iterable<Path>)files.filter(Files::isRegularFile)::iterator){
                    String name=resources.relativize(file).toString().replace('\\','/');
                    JarEntry newEntry=new JarEntry(name);newEntry.setTime(0L);
                    output.putNextEntry(newEntry);output.write(Files.readAllBytes(file));output.closeEntry();
                    altered.add(name);
                }
            }
            Path packagePath=classFolder.resolve("net/mcreator/horrormod/unified");
            try (DirectoryStream<Path> files=Files.newDirectoryStream(packagePath,"*.class")) {
                for (Path file:files) {
                    String name="net/mcreator/horrormod/unified/"+file.getFileName().toString();
                    JarEntry entry=new JarEntry(name);entry.setTime(0L);
                    output.putNextEntry(entry);
                    output.write(Files.readAllBytes(file));
                    output.closeEntry();altered.add(name);
                }
            }
        }
        System.out.println("Created " + result + " with patches/additions:");
        for (String entry:altered) System.out.println("  " + entry);
        if (altered.size()<145) throw new IllegalStateException("Expected v3 hooks and texture assets, got only "+altered.size()+" patched entries");
    }
    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[65536];int size;
        while ((size=input.read(buf))>=0) out.write(buf,0,size);
        return out.toByteArray();
    }
}
