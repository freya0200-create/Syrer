"""Original deterministic pixel sprites and wearable armor textures for Scyer equipment."""
from pathlib import Path
import json, random
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
ASSET=ROOT/'assets/scyer'
def write_json(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
lang=json.loads((ASSET/'lang/en_us.json').read_text())
armor_names=[]
for metal,base,light in [('adamantium',(80,107,96,255),(179,210,191,255)),('uru',(36,61,91,255),(117,232,247,255))]:
 for folder in ['humanoid','humanoid_leggings']:
  im=Image.new('RGBA',(64,32),base);d=ImageDraw.Draw(im)
  for x in range(0,64,8):
   for y in range(0,32,8):
    d.rectangle((x,y,x+7,y+7),outline=light,width=1)
    d.line((x+2,y+2,x+5,y+2),fill=light)
    if metal=='uru':d.line((x+3,y+2,x+3,y+5),fill=light)
  path=ASSET/f'textures/entity/equipment/{folder}/{metal}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
 write_json(ASSET/f'equipment/{metal}.json',{'layers':{kind:[{'texture':f'scyer:{metal}'}] for kind in ['humanoid','humanoid_leggings']}})
 for piece in ['helmet','chestplate','leggings','boots']:
  name=f'{metal}_{piece}';armor_names.append(name)
  im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
  if piece=='helmet':
   d.polygon([(6,8),(10,4),(22,4),(26,8),(26,23),(21,23),(21,15),(11,15),(11,23),(6,23)],fill=base,outline=light,width=2)
  elif piece=='chestplate':
   d.polygon([(3,7),(10,4),(12,9),(20,9),(22,4),(29,7),(26,15),(24,15),(24,28),(8,28),(8,15),(6,15)],fill=base,outline=light,width=2)
  elif piece=='leggings':
   d.polygon([(7,4),(25,4),(25,28),(18,28),(18,16),(14,16),(14,28),(7,28)],fill=base,outline=light,width=2)
  else:
   for x in [4,18]:d.polygon([(x,10),(x+8,10),(x+8,23),(x+11,23),(x+11,28),(x,28)],fill=base,outline=light,width=2)
  if metal=='uru':d.line((15,10,15,14),fill=light,width=2)
  path=ASSET/f'textures/item/{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
  write_json(ASSET/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'scyer:item/{name}'}})
  write_json(ASSET/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'scyer:item/{name}'}})
  lang[f'item.scyer.{name}']=metal.title()+' '+piece.title()
for tool in ['pickaxe','sword','axe','mace']:
 name='uru_'+tool;im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.line((6,27,22,11),fill=(57,39,31,255),width=5);d.line((6,26,22,10),fill=(130,97,67,255),width=2)
 dark=(36,61,91,255);light=(117,232,247,255)
 if tool=='pickaxe':d.polygon([(9,5),(18,3),(25,6),(29,14),(25,11),(20,8),(12,8)],fill=dark,outline=light,width=2)
 elif tool=='sword':
  d.polygon([(10,20),(23,3),(29,2),(29,8),(16,23)],fill=dark,outline=light,width=2);d.line((9,15,20,26),fill=light,width=3)
 elif tool=='axe':d.polygon([(13,4),(24,3),(28,8),(25,18),(19,17),(14,12)],fill=dark,outline=light,width=2)
 else:d.polygon([(15,3),(24,2),(30,8),(28,17),(19,19),(12,11)],fill=dark,outline=light,width=2)
 im.save(ASSET/f'textures/item/{name}.png')
 write_json(ASSET/f'models/item/{name}.json',{'parent':'minecraft:item/handheld','textures':{'layer0':f'scyer:item/{name}'}})
 write_json(ASSET/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'scyer:item/{name}'}})
 lang[f'item.scyer.{name}']='Uru '+tool.title()
for stage,color in [(1,(185,181,164,255)),(2,(92,127,108,255)),(3,(51,110,151,255)),(4,(103,67,158,255))]:
 name='scyer_spawn_egg' if stage==1 else f'scyer_level_{stage}_spawn_egg'
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);d.ellipse((7,3,25,29),fill=color,outline=(25,28,35,255),width=2)
 r=random.Random(stage)
 for i in range(18):
  x,y=r.randint(11,21),r.randint(8,24);d.rectangle((x,y,x+1,y+1),fill=(30,35,41,255))
 # Visible tier tally, distinct even without color perception.
 for i in range(stage):d.line((10+i*3,19,10+i*3,23),fill=(214,246,245,255),width=2)
 im.save(ASSET/f'textures/item/{name}.png')
 write_json(ASSET/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'scyer:item/{name}'}})
 write_json(ASSET/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'scyer:item/{name}'}})
 lang[f'item.scyer.{name}']=f'Level {stage} Scyer Spawn Egg'
 lang['entity.scyer.'+('scyer' if stage==1 else f'scyer_level_{stage}')]=f'Level {stage} Scyer'
write_json(ASSET/'lang/en_us.json',lang)
# Vanilla enchantment and tool tags make these real usable equipment, including Breach on the mace.
tags={'swords':['uru_sword'],'axes':['uru_axe'],'pickaxes':['uru_pickaxe'],
 'enchantable/sword':['uru_sword'],'enchantable/mace':['uru_mace'],
 'enchantable/sharp_weapon':['uru_sword','uru_axe'],'enchantable/weapon':['uru_sword','uru_axe','uru_mace'],
 'enchantable/mining':['uru_pickaxe','uru_axe'],'enchantable/mining_loot':['uru_pickaxe','uru_axe'],
 'enchantable/durability':armor_names+['uru_sword','uru_axe','uru_pickaxe','uru_mace'],
 'enchantable/armor':armor_names,
 'enchantable/head_armor':[n for n in armor_names if n.endswith('helmet')],
 'enchantable/chest_armor':[n for n in armor_names if n.endswith('chestplate')],
 'enchantable/leg_armor':[n for n in armor_names if n.endswith('leggings')],
 'enchantable/foot_armor':[n for n in armor_names if n.endswith('boots')]}
for tag,names in tags.items():write_json(ROOT/f'data/minecraft/tags/item/{tag}.json',{'replace':False,'values':['scyer:'+n for n in names]})
print('Generated 16 item sprites, equipment layers, translations and enchantment tags.')
