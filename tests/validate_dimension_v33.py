#!/usr/bin/env python3
"""Assert v3.3 worldgen datapack remains a compatible open-top version."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
new=json.loads((root/'src/main/resources/data/horrormod/dimension/death_dim.json').read_text())
kind=json.loads((root/'src/main/resources/data/horrormod/dimension_type/death_dim.json').read_text())
assert new['type']=='horrormod:death_dim'
assert new['generator']['type']=='minecraft:noise'
settings=new['generator']['settings']
assert settings['bedrock_roof_position']==-10
assert settings['noise']['top_slide']['target']==-10
assert settings['noise']['height']==128
assert [b['biome'] for b in new['generator']['biome_source']['biomes']]==[
 'horrormod:dead_woods_biome','horrormod:death']
assert not kind['has_ceiling'] and not kind['has_skylight']
assert kind['effects']=='horrormod:death_dim'
print('PASS: death dimension is open-top, dark, same biomes/settings')
