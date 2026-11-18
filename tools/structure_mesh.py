"""Read Minecraft structure NBT and generate a compact, view-dependent contour edge mesh."""
import gzip, struct, io, sys, zipfile, json, collections
from pathlib import Path

def parse_nbt(buf):
    stream=io.BytesIO(buf)
    def read(n):
        x=stream.read(n)
        if len(x)!=n: raise EOFError(f'expected {n} bytes')
        return x
    def u8(): return read(1)[0]
    def i16(): return struct.unpack('>h',read(2))[0]
    def i32(): return struct.unpack('>i',read(4))[0]
    def i64(): return struct.unpack('>q',read(8))[0]
    def text():
        n=struct.unpack('>H',read(2))[0];return read(n).decode('utf8')
    def value(t):
        if t==1:return struct.unpack('>b',read(1))[0]
        if t==2:return i16()
        if t==3:return i32()
        if t==4:return i64()
        if t==5:return struct.unpack('>f',read(4))[0]
        if t==6:return struct.unpack('>d',read(8))[0]
        if t==7:return read(i32())
        if t==8:return text()
        if t==9:
            item=u8();num=i32()
            if num<0 or num>2000000: raise ValueError('invalid list')
            return [value(item) for _ in range(num)]
        if t==10:
            result={}
            while True:
                field=u8()
                if field==0:break
                name=text()
                result[name]=value(field)
            return result
        if t==11:return [i32() for _ in range(i32())]
        if t==12:return [i64() for _ in range(i32())]
        raise ValueError(t)
    tag=u8();name=text();result=value(tag)
    if stream.read(1): raise ValueError('trailing bytes')
    return result

def blocks_in(file):
    raw=file
    if raw[:2]==b'\x1f\x8b': raw=gzip.decompress(raw)
    nbt=parse_nbt(raw)
    size=nbt['size']
    palette=nbt.get('palette',[])
    blocks={}
    for b in nbt['blocks']:
        state=b['state']
        name=palette[state]['Name']
        if name in ('minecraft:air','minecraft:cave_air','minecraft:structure_void','minecraft:void_air'):continue
        blocks[tuple(b['pos'])]=name
    return size,blocks,nbt

if __name__=='__main__' and len(sys.argv)>1 and sys.argv[1]!='--build':
  with zipfile.ZipFile(sys.argv[1]) as jf:
   for name in ['keystructurenewrem','exitdeadssss']:
    size,solid,nbt=blocks_in(jf.read('data/horrormod/structures/'+name+'.nbt'))
    print(name,size,len(solid), 'pal',len(nbt.get('palette',[])), collections.Counter(solid.values()).most_common(15))

# Face corner offsets (x,y,z) for each outward normal; all coordinates are integer voxel corners.
FACES = [
 ((1,0,0),[(1,0,0),(1,1,0),(1,1,1),(1,0,1)]),
 ((-1,0,0),[(0,0,0),(0,0,1),(0,1,1),(0,1,0)]),
 ((0,1,0),[(0,1,0),(0,1,1),(1,1,1),(1,1,0)]),
 ((0,-1,0),[(0,0,0),(1,0,0),(1,0,1),(0,0,1)]),
 ((0,0,1),[(0,0,1),(1,0,1),(1,1,1),(0,1,1)]),
 ((0,0,-1),[(0,0,0),(0,1,0),(1,1,0),(1,0,0)]),
]

def mesh(blocks):
    occupancy=set(blocks)
    edges={}
    for (x,y,z) in occupancy:
        for index,(normal,corners) in enumerate(FACES):
            nx,ny,nz=normal
            if (x+nx,y+ny,z+nz) in occupancy:continue
            points=[(x+a,y+b,z+c) for (a,b,c) in corners]
            for i in range(4):
                v1,v2=points[i],points[(i+1)%4]
                key=tuple(sorted((v1,v2)))
                edges[key]=edges.get(key,0)|(1<<index)
    return edges

def serialize_structure(jar_path, structure, output):
    with zipfile.ZipFile(jar_path) as j:
        size, blocks, _ = blocks_in(j.read(f'data/horrormod/structures/{structure}.nbt'))
    edges=mesh(blocks)
    content=b'R3ME'+struct.pack('>I',len(edges))
    for edge,mask in sorted(edges.items()):
        content+=struct.pack('>hhhhhhB',*edge[0],*edge[1],mask)
    output=Path(output)
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_bytes(content)
    return len(edges),len(content),size

if __name__=='__main__' and len(sys.argv)>1 and sys.argv[1]=='--build':
    jar_path=sys.argv[2]
    assets=Path(sys.argv[3])
    for name,mode in [('keystructurenewrem','key'),('exitdeadssss','exit')]:
        result=serialize_structure(jar_path,name,assets/('mesh_'+mode+'.bin'))
        print(name,'->',result)
