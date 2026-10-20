"""Generate the SCP-096-inspired model, pixel atlas, and geometry used by the preview.
All geometry and pixels are original; no third-party game assets are copied.
"""
from pathlib import Path
import json, random, struct, zlib
ROOT = Path(__file__).resolve().parents[1]
parts = []
boxes = []
def part(name, parent, pivot, rotation=(0, 0, 0)):
    parts.append(dict(name=name,parent=parent,pivot=pivot,rotation=rotation))
def box(parent,name,palette,x,y,z,w,h,d):
    boxes.append(dict(parent=parent,name=name,palette=palette,box=[x,y,z,w,h,d]))
part('torso','root',(0,1,1),(.16,0,0))
box('torso','shoulders',0,-4,-6,-2,8,3,4)
box('torso','ribcage',5,-3.5,-3,-1.7,7,7,3.5)
box('torso','waist',0,-1.8,4,-1.1,3.6,5,2.4)
box('torso','pelvis',0,-2.7,8,-1.5,5.4,3,3)
box('torso','sternum',0,-.35,-3.3,-2.05,.7,6.5,.65)
for i in range(5):
    width=3.1-i*.16
    box('torso',f'left_rib_{i}',0,-3.2,-2.6+i*1.2,-2.15,width,.38,.7)
    box('torso',f'right_rib_{i}',0,.3,-2.6+i*1.2,-2.15,width,.38,.7)
box('torso','left_clavicle',0,-3.8,-4.3,-2.4,3.4,.5,.7)
box('torso','right_clavicle',0,.4,-4.3,-2.4,3.4,.5,.7)
box('torso','spine',5,-.4,-4,1.6,.8,13,.7)
part('neck','torso',(0,-6,-.1),(.12,0,0))
box('neck','neck_tendon',5,-.85,-2,-.65,1.7,2.5,1.7)
part('head','neck',(0,-1.7,-.45),(.12,0,0))
box('head','cranium',0,-2.75,-6.7,-1.6,5.5,6.2,4)
box('head','crown',0,-2.2,-7.5,-1.2,4.4,1,3.3)
box('head','brow',0,-2.7,-5.8,-2.65,5.4,1.1,1.5)
box('head','left_temple',5,-3,-4.8,-2.1,.8,3.4,2.6)
box('head','right_temple',5,2.2,-4.8,-2.1,.8,3.4,2.6)
box('head','left_socket',1,-2.2,-4.6,-2.72,1.5,1,.28)
box('head','right_socket',1,.7,-4.6,-2.72,1.5,1,.28)
box('head','nose_bridge',0,-.45,-4.8,-2.95,.9,2.3,.75)
box('head','left_cheekbone',0,-2.7,-3.4,-3,.9,1.2,1)
box('head','right_cheekbone',0,1.8,-3.4,-3,.9,1.2,1)
box('head','left_tear',7,-1.9,-3.6,-2.85,.35,2.1,.2)
box('head','right_tear',7,1.5,-3.6,-2.85,.3,1.5,.2)
box('head','mouth_depth',1,-1.75,-2.3,-2.4,3.5,3.7,.5)
box('head','left_mouth_edge',2,-1.9,-2.2,-2.8,.35,3,.3)
box('head','right_mouth_edge',2,1.55,-2.2,-2.8,.35,3,.3)
for i in range(5):
    box('head',f'upper_tooth_{i+1}',3,-1.45+i*.61,-2.1,-2.95,.3,.55+(i%2)*.25,.4)
part('jaw','head',(0,-.1,-.3),(.3,0,0))
box('jaw','left_mandible',5,-2,-.7,-2.2,.5,2.5,2.3)
box('jaw','right_mandible',5,1.5,-.7,-2.2,.5,2.5,2.3)
box('jaw','chin',0,-1.8,1.2,-2.3,3.6,.65,1.1)
box('jaw','lower_gum',2,-1.5,1,-2.4,3,.3,.5)
for i in range(4):
    box('jaw',f'lower_tooth_{i+1}',3,-1.35+i*.75,.45-(i%2)*.15,-2.45,.25,.75,.4)
for side,sign in [('left',1),('right',-1)]:
    part(side+'Arm','torso',(sign*4.2,-4.5,.2),(-.08,0,-sign*.10))
    box(side+'Arm','upper_arm',0,-.95,-.5,-1,1.9,10.5,2)
    box(side+'Arm','tendon',5,-.5,1,-1.1,1,8,.3)
    part(side+'Forearm',side+'Arm',(0,9.5,0),(-.2,0,sign*.03))
    box(side+'Forearm','elbow',5,-1,-.4,-.95,2,1.6,2)
    box(side+'Forearm','forearm',0,-.7,.7,-.7,1.4,10.5,1.5)
    part(side+'Hand',side+'Forearm',(0,10.8,0),(.1,0,0))
    box(side+'Hand','palm',5,-1,0,-.5,2,2.4,1.1)
    for i in range(3):
        box(side+'Hand',f'finger_{i}',0,-.9+i*.72,2,-.6,.38,2.1+(i%2)*.5,.45)
        box(side+'Hand',f'claw_{i}',6,-.9+i*.72,3.7+(i%2)*.5,-1.05,.32,.9,.85)
    box(side+'Hand','thumb',0,-sign*1.3,1,-1,.4,1.8,.65)
    part(side+'Leg','root',(sign*1.7,11,1),(-.10,0,sign*.025))
    box(side+'Leg','thigh',0,-1.05,0,-1,2.1,7.4,2.1)
    part(side+'Shin',side+'Leg',(0,6.8,0),(.18,0,0))
    box(side+'Shin','knee',5,-1.05,-.3,-1.3,2.1,1.8,2)
    box(side+'Shin','shin',0,-.75,1,-.6,1.5,5.4,1.6)
    box(side+'Shin','foot',5,-.9,5.4,-2.2,1.8,.9,3.3)
    for i in range(3):
        box(side+'Shin',f'toe_{i}',6,-.8+i*.55,5.7,-2.7,.32,.5,1)


# Armor follows the same articulated body as the skin; no humanoid-renderer dependency.
armor_parts = {2: [], 3: []}
for tier, palette, prefix in [(2,8,'adam'), (3,9,'uru')]:
    for suffix,parent,shape in [
        ('Chest','torso',(-4.15,-6.15,-2.35,8.3,9,4.8)),
        ('Helm','head',(-3.15,-7.8,-1.85,6.3,3.1,4.8)),
        ('LeftShoulder','leftArm',(-1.6,-.8,-1.55,3.2,3.5,3.2)),
        ('RightShoulder','rightArm',(-1.6,-.8,-1.55,3.2,3.5,3.2)),
        ('LeftGreave','leftLeg',(-1.4,-.2,-1.3,2.8,6.4,2.8)),
        ('RightGreave','rightLeg',(-1.4,-.2,-1.3,2.8,6.4,2.8)),
        ('LeftBoot','leftShin',(-1.25,3,-2.5,2.5,3.6,4)),
        ('RightBoot','rightShin',(-1.25,3,-2.5,2.5,3.6,4))]:
        name=prefix+suffix
        part(name,parent,(0,0,0)); box(name,'plate',palette,*shape)
        if tier==3:
            x,y,z,w,h,d=shape
            box(name,'rune',10,x+w*.45,y+.3,z-.08,.3,min(2,h-.4),.12)
        armor_parts[tier].append((parent,name))
part('weaponLeft','leftHand',(0,1,-1))
part('weaponRight','rightHand',(0,1,-1))
# Miniature sword, axe, mace and pickaxe held together on the left palm.
box('weaponLeft','sword_blade',9,-2,-6,-.3,.5,7,.5)
box('weaponLeft','sword_guard',10,-2.7,.2,-.4,2,.4,.7)
box('weaponLeft','axe_shaft',5,-.7,-4,-.2,.35,6,.4)
box('weaponLeft','axe_head',9,-1.4,-4,-.4,2.1,1.5,.8)
box('weaponLeft','mace_handle',5,1,-2,-.1,.4,4,.4)
box('weaponLeft','mace_head',9,.3,-3,-.6,1.8,1.5,1.4)
box('weaponLeft','pick_handle',5,2.5,-3,-.1,.3,5,.4)
box('weaponLeft','pick_head',9,1.5,-3.5,-.3,2.6,.45,.7)
# Miniature spear, trident, bow and crossbow on the right palm.
box('weaponRight','spear_shaft',5,-2,-7,0,.3,9,.35)
box('weaponRight','spear_tip',9,-2.2,-8,-.1,.65,1.5,.6)
box('weaponRight','trident_shaft',9,0,-5,0,.35,7,.4)
box('weaponRight','trident_bar',9,-.8,-5,-.1,2,.4,.6)
for i in range(3): box('weaponRight',f'trident_tip_{i}',10,-.8+i*.8,-6.5,-.1,.3,1.7,.5)
box('weaponRight','bow_limb_top',9,1.7,-4,-.1,.4,3,.5)
box('weaponRight','bow_limb_bottom',9,1.7,-.5,-.1,.4,3,.5)
box('weaponRight','bow_string',3,2,-4,.15,.08,6.5,.08)
box('weaponRight','crossbow_stock',5,3,-1,-.1,.5,3,.6)
box('weaponRight','crossbow_limb',9,1.8,-1,-.1,3,.4,.6)

def f(v): return str(float(v))+'F'
lines=[]
for p in parts:
    vals=', '.join(f(v) for v in p['pivot']+p['rotation'])
    lines.append(f'        PartDefinition {p["name"]} = {p["parent"]}.addOrReplaceChild("{p["name"]}", CubeListBuilder.create(), PartPose.offsetAndRotation({vals}));')
for b in boxes:
    lines.append(f'        box({b["parent"]}, "{b["name"]}", {b["palette"]}, '+', '.join(f(v) for v in b['box'])+');')
java='''package dev.scyer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;


/** Generated by tools/generate_scyer_model.py. See VISUAL_CREDITS.md. */
public final class ScyerModel extends EntityModel<ScyerRenderState> {
    private final ModelPart torso, head, jaw, leftArm, rightArm, leftLeg, rightLeg;
    private final java.util.List<ModelPart> adamArmor, uruArmor;
    private final ModelPart weaponLeft, weaponRight;
    public ScyerModel(ModelPart root) {
        super(root);
        torso = root.getChild("torso");
        head = torso.getChild("neck").getChild("head");
        jaw = head.getChild("jaw");
        leftArm = torso.getChild("leftArm");
        rightArm = torso.getChild("rightArm");
        leftLeg = root.getChild("leftLeg");
        rightLeg = root.getChild("rightLeg");
        adamArmor = armorParts(root, "adam"); uruArmor = armorParts(root, "uru");
        weaponLeft = leftArm.getChild("leftForearm").getChild("leftHand").getChild("weaponLeft");
        weaponRight = rightArm.getChild("rightForearm").getChild("rightHand").getChild("weaponRight");
    }
    private java.util.List<ModelPart> armorParts(ModelPart root, String prefix) {
        return java.util.List.of(torso.getChild(prefix + "Chest"), head.getChild(prefix + "Helm"),
            leftArm.getChild(prefix + "LeftShoulder"), rightArm.getChild(prefix + "RightShoulder"),
            leftLeg.getChild(prefix + "LeftGreave"), rightLeg.getChild(prefix + "RightGreave"),
            leftLeg.getChild("leftShin").getChild(prefix + "LeftBoot"),
            rightLeg.getChild("rightShin").getChild(prefix + "RightBoot"));
    }
    private static void box(PartDefinition parent, String name, int palette,
                            float x, float y, float z, float w, float h, float d) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(palette * 64, 0)
            .addBox(x, y, z, w, h, d), PartPose.ZERO);
    }
    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
'''+ '\n'.join(lines)+'''
        return LayerDefinition.create(mesh, 704, 64);
    }
    @Override public void setupAnim(ScyerRenderState state) {
        super.setupAnim(state);
        adamArmor.forEach(part -> part.visible = state.stage == 2);
        uruArmor.forEach(part -> part.visible = state.stage >= 3);
        weaponLeft.visible = weaponRight.visible = state.stage == 4;
        float moving = Math.min(1, state.walkAnimationSpeed);
        float stride = state.walkAnimationPos * .58F;
        float breath = (float) Math.sin(state.ageInTicks * .085F);
        torso.xRot += breath * .012F;
        head.xRot += state.xRot * ((float) Math.PI / 180) * .6F;
        head.yRot += state.yRot * ((float) Math.PI / 180) * .7F;
        head.zRot += .055F + (float) Math.sin(state.ageInTicks * .037F) * .035F;
        jaw.xRot += moving * .28F + breath * .04F;
        leftLeg.xRot += (float) Math.cos(stride) * .48F * moving;
        rightLeg.xRot += (float) Math.cos(stride + Math.PI) * .48F * moving;
        leftArm.xRot += (float) Math.cos(stride + Math.PI) * .22F * moving;
        rightArm.xRot += (float) Math.cos(stride) * .22F * moving;
        leftArm.zRot += (float) Math.sin(state.ageInTicks * .61F) * .012F;
        rightArm.zRot -= (float) Math.sin(state.ageInTicks * .53F) * .012F;
        if (state.roaring) {
            jaw.xRot += .65F;
            head.xRot -= .3F;
            leftArm.xRot -= 1F;
            rightArm.xRot -= 1F;
        }
        float claw = (float)Math.sin(state.clawSwing * Math.PI);
        leftArm.xRot -= claw * 1.8F;
        rightArm.xRot -= claw * 1.5F;
    }
}
'''
ROOT.joinpath('src/client/java/dev/scyer/ScyerModel.java').write_text(java)
ROOT.joinpath('tools/scyer_geometry.json').write_text(json.dumps(dict(parts=parts,boxes=boxes),indent=2))
colors=[(184,181,165),(29,27,28),(68,32,35),(177,169,140),(100,97,87),(145,141,126),(50,44,39),(87,58,56),(97,121,112),(55,81,110),(108,229,243)]
r=random.Random(96)
noise=[[r.choice([-10,-6,-3,0,2,4,7]) for x in range(176)] for y in range(16)]
rows=[]
for y in range(64):
    row=bytearray([0])
    for x in range(704):
        n=noise[y//4][x//4]
        row.extend([max(0,min(255,c+n)) for c in colors[x//64]]+[255])
    rows.append(row)
def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d)&0xffffffff)
ROOT.joinpath('src/main/resources/assets/scyer/textures/entity/scyer_pale.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',704,64,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b''))
print('Generated',len(parts),'articulated parts and',len(boxes),'cubes.')
