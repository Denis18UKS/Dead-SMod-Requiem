"""Build 32-frame x 2-mode vanilla-compatible custom compass sprite sets (16x16)."""
from PIL import Image, ImageDraw
from pathlib import Path
import json,math,sys

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
tex=ROOT/'assets/horrormod/textures/item'
models=ROOT/'assets/horrormod/models/item'
base=ROOT/'assets/minecraft/models/item'
for p in [tex,models,base]:p.mkdir(parents=True,exist_ok=True)
size=16

def make(frame,mode):
    im=Image.new('RGBA',(size,size)); p=im.load()
    ring=[(0,1,1,1,0,0,0,0,0,0,0,1,1,1,0,0)]
    for y in range(16):
      for x in range(16):
        dx=x-7.5;dy=y-7.5;dist=math.hypot(dx,dy)
        # Black iron and unpolished steel mounting
        if dist<=7.6:
          if dist>=6.25:
            c=(14,16,23,255) if (x+y)%3 else (21,26,34,255)
          elif dist>=5.3:
            c=(84,91,105,255) if y<7 else (49,57,74,255)
          else:
            v=int(20+4*math.sin(x*1.31+y*0.72))
            c=(v,v+3,v+10,255)
          p[x,y]=c
    d=ImageDraw.Draw(im)
    # Gothic screws and runic hour indicators
    for x,y in [(7,1),(7,14),(1,7),(14,7)]:
      d.point((x,y),fill=(174,175,182,255))
    for x,y in [(3,3),(12,3),(3,12),(12,12)]:
      d.point((x,y),fill=(80,79,98,255))
    d.ellipse((5,5,10,10),outline=(54,61,74,255))
    # frame 16 is the vertical compass reference; positive angle rotates clockwise.
    angle=(frame-16)*2*math.pi/32
    nx=math.sin(angle);ny=-math.cos(angle)
    # Directional needle with shadow/hilt
    cx=7.5;cy=7.5
    head=(cx+nx*5,cy+ny*5)
    tail=(cx-nx*3,cy-ny*3)
    shade=(46,39,56,255)
    d.line((tail,(cx,cy)),fill=(97,93,117,255),width=2)
    accent=(230,243,248,255) if mode=='key' else (172,211,237,255)
    d.line(((cx,cy),head),fill=accent,width=2)
    d.point((round(head[0]),round(head[1])),fill=(255,255,255,255))
    d.ellipse((6,6,9,9),fill=(21,18,33,255),outline=(162,166,180,255))
    d.point((7,7),fill=(244,249,255,255) if mode=='key' else (173,204,237,255))
    return im

for mode in ['key','exit']:
 for frame in range(32):
  name=f'requiem_compass_{mode}_{frame:02d}'
  make(frame,mode).save(tex/(name+'.png'))
  (models/(name+'.json')).write_text(json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":f"horrormod:item/{name}"}},ensure_ascii=False),encoding='utf8')

def angle_frame(step):return (16 + step//2)%32
# vanilla uses 64 angle steps, first at 0 is compass_16 and step advances by 1 frame at each 0.03125.
# Preserve the vanilla 32-angle animation. Each custom variant has full 64-step overrides.
entries=[]
for step in range(64):
  entries.append({"predicate":{"angle":step/64},"model":f"minecraft:item/compass_{angle_frame(step):02d}"})
for mode,custom in [('key',314159),('exit',314160)]:
 for step in range(64):
  entries.append({"predicate":{"angle":step/64,"custom_model_data":custom},"model":f"horrormod:item/requiem_compass_{mode}_{angle_frame(step):02d}"})
(base/'compass.json').write_text(json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/compass_16"},"overrides":entries},ensure_ascii=False,indent=1),encoding='utf8')
print('Generated',64,'textures,',64,'models and custom-model-data compass override')
