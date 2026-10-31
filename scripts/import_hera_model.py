"""Import owner-supplied Blockbench cubes and bake its limited Molang channels at 60 Hz.
No code from the asset is executed. Supported AST operators/functions are explicit.
"""
import ast,base64,bisect,json,math,sys
from pathlib import Path

def expression(value,t):
 s=str(value).replace('query.anim_time','t').replace('math.','')
 def visit(n):
  if isinstance(n,ast.Constant) and type(n.value) in (int,float):return n.value
  if isinstance(n,ast.Name) and n.id=='t':return t
  if isinstance(n,ast.UnaryOp) and isinstance(n.op,(ast.UAdd,ast.USub)):return visit(n.operand)*(1 if isinstance(n.op,ast.UAdd) else -1)
  if isinstance(n,ast.BinOp):
   a,b=visit(n.left),visit(n.right)
   if isinstance(n.op,ast.Add):return a+b
   if isinstance(n.op,ast.Sub):return a-b
   if isinstance(n.op,ast.Mult):return a*b
   if isinstance(n.op,ast.Div):return a/b
  if isinstance(n,ast.Call) and isinstance(n.func,ast.Name) and not n.keywords:
   args=[visit(v) for v in n.args];name=n.func.id
   if name=='sin':return math.sin(math.radians(args[0]))
   if name=='cos':return math.cos(math.radians(args[0]))
   if name=='clamp':return max(args[1],min(args[2],args[0]))
   if name=='max':return max(args)
   if name=='min':return min(args)
  raise ValueError('Unsupported Molang: '+s)
 return visit(ast.parse(s,mode='eval').body)

def sample(keys,t,axis):
 keys=sorted(keys,key=lambda k:k['time']);times=[k['time'] for k in keys];i=max(0,bisect.bisect_right(times,t)-1);a=keys[i];b=keys[min(i+1,len(keys)-1)]
 def val(k):return expression(k['data_points'][0].get('xyz'[axis],0),t)
 if a is b:return val(a)
 ratio=max(0,min(1,(t-a['time'])/(b['time']-a['time'])))
 if a.get('interpolation')=='bezier':
  x0=a['time'];x3=b['time'];x1=x0+a.get('bezier_right_time',[0,0,0])[axis];x2=x3+b.get('bezier_left_time',[0,0,0])[axis]
  def cubic(p0,p1,p2,p3,u):return (1-u)**3*p0+3*(1-u)**2*u*p1+3*(1-u)*u*u*p2+u**3*p3
  lo,hi=0.,1.
  for _ in range(22):
   u=(lo+hi)/2
   if cubic(x0,x1,x2,x3,u)<t:lo=u
   else:hi=u
  return cubic(val(a),val(a)+a.get('bezier_right_value',[0,0,0])[axis],val(b)+b.get('bezier_left_value',[0,0,0])[axis],val(b),(lo+hi)/2)
 return val(a)*(1-ratio)+val(b)*ratio

def export(source,destination):
 p=json.loads(Path(source).read_text(encoding='utf-8-sig'));out=Path(destination);out.mkdir(parents=True,exist_ok=True)
 groups={g['uuid']:g for g in p['groups']};elements={e['uuid']:e for e in p['elements']}
 def bone(node,parent=(0,0,0)):
  g=groups[node['uuid']];origin=g['origin'];result={'name':g['name'],'id':g['uuid'],'pivot':[origin[i]-parent[i] for i in range(3)],'rotation':g.get('rotation',[0,0,0]),'bones':[],'cubes':[]}
  for child in node['children']:
   if isinstance(child,dict):result['bones'].append(bone(child,origin))
   else:
    e=elements[child]
    if not e.get('visibility',True) or not e.get('export',True):continue
    assert e['type']=='cube'
    pivot=e.get('origin',origin);f=e['from'];to=e['to'];inflate=e.get('inflate',0);f=[v-inflate for v in f];to=[v+inflate for v in to];x,y,z=f;X,Y,Z=to
    faces={'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],'up':[(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],'down':[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]}
    normals={'north':[0,0,-1],'south':[0,0,1],'east':[1,0,0],'west':[-1,0,0],'up':[0,1,0],'down':[0,-1,0]};quads=[]
    for side,points in faces.items():
     face=e['faces'][side]
     if face.get('texture') is None:continue
     assert face.get('rotation',0)==0,'rotated face UV not supported'
     u,v,U,V=face['uv'];uv=[(u,V),(U,V),(U,v),(u,v)]
     quads.append({'normal':normals[side],'vertices':[[*( (point[i]-pivot[i])/16 for i in range(3)),uv[j][0]/128,uv[j][1]/128] for j,point in enumerate(points)]})
    result['cubes'].append({'pivot':[(pivot[i]-origin[i])/16 for i in range(3)],'rotation':e.get('rotation',[0,0,0]),'quads':quads})
  result['pivot']=[v/16 for v in result['pivot']];return result
 clips={}
 for a in p['animations']:
  duration=a['length'];frames=round(duration*60)+1;tracks={}
  for uuid,b in a['animators'].items():
   channels={}
   for channel in ['rotation','position']:
    keys=[k for k in b.get('keyframes',[]) if k['channel']==channel]
    if keys:channels[channel]=[[round(sample(keys,min(i/60,duration),axis),5) for axis in range(3)] for i in range(frames)]
   if channels:tracks[uuid]=channels
  clips[a['name'].split('.')[-1]]={'duration':duration,'loop':a['loop']=='loop','tracks':tracks}
 data={'bones':[bone(n) for n in p['outliner'] if isinstance(n,dict)],'clips':clips}
 (out/'model.json').write_text(json.dumps(data,separators=(',',':')))
 for i,t in enumerate(p['textures']):(out/('skin.png' if i==0 else 'glow.png')).write_bytes(base64.b64decode(t['source'].split(',',1)[1]))
 print('Imported',len(elements),'cubes;',len(clips),'animations; all expressions and curves baked.')
if __name__=='__main__':export(sys.argv[1],sys.argv[2])
