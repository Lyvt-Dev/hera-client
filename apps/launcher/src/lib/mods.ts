import { invoke } from '@tauri-apps/api/core';
import { z } from 'zod';
import { installedModSchema, modProjectSchema, modVersionSchema, profileSchema, type InstalledMod, type ModProject, type ModSource, type ModVersion, type Profile } from '../../../../packages/shared-types/src';
import { native } from './bridge';
export interface ModTarget {source:ModSource;profile:Profile}
export async function catalog<T>(action:string,target:ModTarget,fields:Record<string,string>,schema:z.ZodType<T>):Promise<T>{
 const params={source:target.source,loader:target.profile.loader,version:target.profile.version,...fields};
 if(native)return schema.parse(await invoke('catalog',{action,params}));
 const response=await fetch(`/api/catalog/${action}?${new URLSearchParams(params)}`,{signal:AbortSignal.timeout(25000)});
 if(!response.headers.get('content-type')?.includes('application/json'))throw Error('Lokaler Mod-Katalog ist nicht gestartet. Bitte den Hera-Entwicklungsserver neu starten.');
 const data:unknown=await response.json();if(!response.ok)throw Error(typeof data==='object'&&data!==null&&'error'in data?String(data.error):`Katalog: HTTP ${response.status}`);return schema.parse(data);
}
export const searchMods=(target:ModTarget,query:string,offset:number)=>catalog('search',target,{query,offset:String(offset)},z.object({projects:z.array(modProjectSchema),hasMore:z.boolean()}));
export const modVersions=(target:ModTarget,id:string)=>catalog('versions',target,{id},z.array(modVersionSchema));
export interface Resolver { versions:(projectId:string)=>Promise<ModVersion[]>; version:(id:string,projectId:string)=>Promise<ModVersion>; project:(id:string)=>Promise<ModProject> }
export async function resolveMods(target:ModTarget,root:ModProject,version:ModVersion,existing:InstalledMod[],custom?:Resolver):Promise<{record:InstalledMod;file:NonNullable<ModVersion['file']>}[]> {
 if(version.projectId!==root.id)throw Error('Mod-Version gehört zum falschen Projekt.');
 const resolver=custom??{versions:(id:string)=>modVersions(target,id),version:(id:string,projectId:string)=>catalog('version',target,{id,projectId},modVersionSchema),project:(id:string)=>catalog('project',target,{id},modProjectSchema)};
 const chosen=new Map<string,ModVersion>(),names=new Map<string,string>([[root.id,root.title]]),plan:{record:InstalledMod;file:NonNullable<ModVersion['file']>}[]=[];
 async function visit(v:ModVersion){
  if(chosen.size>=64)throw Error('Zu viele Abhängigkeiten (maximal 64).');
  const prior=chosen.get(v.projectId);if(prior){if(prior.id!==v.id)throw Error(`Widersprüchliche Abhängigkeit: ${v.projectId}`);return;}
  if(!v.gameVersions.includes(target.profile.version)||!v.loaders.includes(target.profile.loader))throw Error(`Version ${v.number} passt nicht zu diesem Profil.`);
  if(!v.file)throw Error(`Für ${v.name} ist kein erlaubter Download verfügbar.`);
  assertModUrl(v.file.url,target.source);
  chosen.set(v.projectId,v);
  const installed=existing.find(m=>m.source===target.source&&m.projectId===v.projectId);
  if(installed&&installed.versionId!==v.id)throw Error(`${installed.title} ist bereits in einer anderen Version installiert. Verwende ein separates Profil.`);
  for(const d of v.dependencies.filter(d=>d.kind==='required')){
   let dep:ModVersion;
   if(d.versionId)dep=await resolver.version(d.versionId,d.projectId??'');
   else if(d.projectId){const list=await resolver.versions(d.projectId);dep=list.find(v=>v.release==='release')??list[0]!;if(!dep)throw Error(`Keine passende Version für Abhängigkeit ${d.projectId}.`);}
   else throw Error('Eine erforderliche Abhängigkeit hat keine Projekt- oder Versions-ID.');
   if(d.projectId&&dep.projectId!==d.projectId)throw Error('Abhängigkeit gehört zum falschen Projekt.');
   if(!names.has(dep.projectId))names.set(dep.projectId,(await resolver.project(dep.projectId)).title);
   await visit(dep);
  }
  if(!installed)plan.push({record:{source:target.source,projectId:v.projectId,versionId:v.id,title:names.get(v.projectId)??v.name,version:v.number,filename:v.file.filename,sha1:v.file.sha1,size:v.file.size},file:v.file});
 }
 await visit(version);
 for(const v of chosen.values())for(const d of v.dependencies.filter(d=>d.kind==='incompatible')){
  const records=[...existing.filter(m=>m.source===target.source),...plan.map(p=>p.record)];
  if(records.some(m=>(d.projectId?m.projectId===d.projectId:true)&&(d.versionId?m.versionId===d.versionId:true)))throw Error(`Inkompatible Mod erkannt: ${d.projectId??d.versionId}`);
 }
 const all=[...existing,...plan.map(p=>p.record)];if(new Set(all.map(m=>m.filename.toLowerCase())).size!==all.length)throw Error('Zwei Mods verwenden denselben Dateinamen.');
 return plan;
}
export function assertModUrl(raw:string,source:ModSource){const u=new URL(raw);const hosts=source==='modrinth'?['cdn.modrinth.com']:['edge.forgecdn.net','mediafilez.forgecdn.net','media.forgecdn.net'];if(u.protocol!=='https:'||u.port||u.username||u.password||!hosts.includes(u.hostname))throw Error('Nicht freigegebene Mod-Downloadquelle.');}
async function folder(id:string){profileSchema.shape.id.parse(id);const root=await navigator.storage.getDirectory();return (await (await root.getDirectoryHandle('hera-profiles',{create:true})).getDirectoryHandle(id,{create:true})).getDirectoryHandle('mods',{create:true});}
async function readManifest(dir:FileSystemDirectoryHandle):Promise<InstalledMod[]>{try{const file=await (await dir.getFileHandle('hera-mods.json')).getFile();return z.array(installedModSchema).parse(JSON.parse(await file.text()));}catch(e){if(e instanceof DOMException&&e.name==='NotFoundError')return [];throw e;}}
export async function installedMods(id:string):Promise<InstalledMod[]>{return native?z.array(installedModSchema).parse(await invoke('installed_mods',{id})):readManifest(await folder(id));}
async function writeFile(dir:FileSystemDirectoryHandle,name:string,blob:Blob){const handle=await dir.getFileHandle(name,{create:true});const writer=await handle.createWritable();try{await writer.write(blob);await writer.close();}catch(e){await writer.abort();throw e;}}
export async function installMod(target:ModTarget,project:ModProject,version:ModVersion,onProgress:(message:string)=>void):Promise<void>{
 if(target.profile.loader==='vanilla')throw Error('Mods benötigen ein Fabric- oder Forge-Profil.');
 if(native){await invoke('install_mod',{id:target.profile.id,source:target.source,projectId:project.id,versionId:version.id});return;}
 if(!navigator.locks)throw Error('Dieser Browser unterstützt keine sicheren Profilsperren. Bitte Desktop-App verwenden.');
 await navigator.locks.request(`hera-mods-${target.profile.id}`,async()=>{
  const dir=await folder(target.profile.id),old=await readManifest(dir),plan=await resolveMods(target,project,version,old);const blobs:{name:string;blob:Blob}[]=[];
  if(plan.reduce((total,item)=>total+item.file.size,0)>256*1024*1024)throw Error('Dieses Mod-Set ist für einen Browser-Download zu groß. Bitte die Desktop-App verwenden.');
  for(const [index,item] of plan.entries()){
   onProgress(`${item.record.title} herunterladen und prüfen (${index+1}/${plan.length})`);
   assertModUrl(item.file.url,target.source);
   const downloadUrl=target.source==='curseforge'?`/api/catalog/download?${new URLSearchParams({source:target.source,loader:target.profile.loader,version:target.profile.version,id:item.record.versionId,projectId:item.record.projectId})}`:item.file.url;
   const response=await fetch(downloadUrl,{redirect:'error',signal:AbortSignal.timeout(120000)});if(!response.ok)throw Error(`Mod-Download: HTTP ${response.status}`);
   if(!response.body)throw Error('Leere Mod-Datei.');const reader=response.body.getReader();const chunks:Uint8Array<ArrayBuffer>[]=[];let size=0;
   try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>item.file.size)throw Error('Mod-Datei überschreitet die erwartete Größe.');chunks.push(new Uint8Array(value));}}finally{await reader.cancel();}
   const blob=new Blob(chunks,{type:'application/java-archive'}),bytes=await blob.arrayBuffer();const hash=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-1',bytes)),b=>b.toString(16).padStart(2,'0')).join('');
   if(size!==item.file.size||hash!==item.file.sha1)throw Error('Mod-Integritätsprüfung fehlgeschlagen.');blobs.push({name:item.file.filename,blob});
  }
  const written:string[]=[];try{
   for(const file of blobs){try{await dir.getFileHandle(file.name);throw Error(`Datei ${file.name} existiert bereits.`);}catch(e){if(!(e instanceof DOMException&&e.name==='NotFoundError'))throw e;}await writeFile(dir,file.name,file.blob);written.push(file.name);}
   await writeFile(dir,'hera-mods.json',new Blob([JSON.stringify([...old,...plan.map(p=>p.record)],null,2)],{type:'application/json'}));
  }catch(e){for(const name of written)await dir.removeEntry(name).catch(()=>{});throw e;}
 });
}
export async function downloadBrowserMod(id:string,mod:InstalledMod){const dir=await folder(id);const file=await (await dir.getFileHandle(mod.filename)).getFile();const url=URL.createObjectURL(file);const a=document.createElement('a');a.href=url;a.download=mod.filename;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
