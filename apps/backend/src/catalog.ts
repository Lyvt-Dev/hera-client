import { z } from 'zod';
import { createHash } from 'node:crypto';
import { modProjectSchema, modVersionSchema, type ModProject, type ModVersion } from '@hera/shared-types';

const querySchema=z.object({source:z.enum(['modrinth','curseforge']),loader:z.enum(['fabric','forge']),version:z.literal('1.21.1'),query:z.string().max(100).default(''),id:z.string().regex(/^[a-zA-Z0-9_-]{1,100}$/).optional(),projectId:z.string().regex(/^[a-zA-Z0-9_-]{1,100}$/).optional(),offset:z.coerce.number().int().min(0).max(10000).default(0)});
export class CatalogError extends Error {constructor(public status:number,message:string){super(message);}}
type Fetch=typeof fetch;
export function createCatalog(key:string|undefined, request:Fetch=fetch) {
 async function upstream(url:URL,cf:boolean):Promise<unknown>{
  if(cf&&!key)throw new CatalogError(503,'CurseForge ist noch nicht eingerichtet. Der Betreiber muss CURSEFORGE_API_KEY serverseitig hinterlegen.');
  const response=await request(url,{headers:cf?{'x-api-key':key!,Accept:'application/json'}:{'User-Agent':'HeraClient/0.1.0 (local mod catalog)',Accept:'application/json'},signal:AbortSignal.timeout(20000),redirect:'error'});
  if(!response.ok)throw new CatalogError(response.status===429?429:502,`Mod-Dienst antwortet mit HTTP ${response.status}.`);
  const bytes=await bounded(response,8*1024*1024);
  try{return JSON.parse(new TextDecoder().decode(bytes));}catch{throw new CatalogError(502,'Ungültige Mod-Metadaten.');}
 }
 return {
  async handle(action:string,raw:Record<string,string>):Promise<unknown>{
   if(action==='status')return {curseforgeConfigured:Boolean(key)};
   const parsed=querySchema.safeParse(raw);if(!parsed.success)throw new CatalogError(400,'Ungültige Kataloganfrage.');
   const q=parsed.data,cf=q.source==='curseforge';
   const url=new URL(cf?'https://api.curseforge.com/v1/':'https://api.modrinth.com/v2/');
   if(action==='search'){
    if(cf){url.pathname+='mods/search';url.search=new URLSearchParams({gameId:'432',classId:'6',searchFilter:q.query,gameVersion:q.version,modLoaderType:q.loader==='forge'?'1':'4',pageSize:'20',index:String(q.offset),sortField:'6',sortOrder:'desc'}).toString();}
    else{url.pathname+='search';url.search=new URLSearchParams({query:q.query,facets:JSON.stringify([['project_type:mod'],[`versions:${q.version}`],[`categories:${q.loader}`],['client_side:optional','client_side:required']]),limit:'20',offset:String(q.offset),index:'downloads'}).toString();}
    const body=await upstream(url,cf);
    const result=z.object(cf?{data:z.array(z.unknown())}:{hits:z.array(z.unknown()),total_hits:z.number()}).parse(body) as {data?:unknown[];hits?:unknown[];total_hits?:number};
    return {projects:(result.data??result.hits??[]).map(v=>project(v,cf)),hasMore:cf?(result.data?.length===20):(q.offset+20<(result.total_hits??0))};
   }
   if(!q.id)throw new CatalogError(400,'Projekt- oder Versions-ID fehlt.');
   if(cf&&!/^\d+$/.test(q.id))throw new CatalogError(400,'Ungültige CurseForge-ID.');
   if(cf&&['versions','version','download'].includes(action)){
    const projectId=action==='versions'?q.id:q.projectId;
    if(!projectId||!/^\d+$/.test(projectId))throw new CatalogError(400,'CurseForge-Projekt-ID fehlt.');
    const meta=z.object({data:z.object({allowModDistribution:z.boolean().nullable().optional()})}).parse(await upstream(new URL(`https://api.curseforge.com/v1/mods/${projectId}`),true));
    if(meta.data.allowModDistribution===false)throw new CatalogError(403,'Der Autor erlaubt keine Verteilung über Drittanbieter-Launcher.');
   }
   if(action==='project'){
    url.pathname+=cf?`mods/${q.id}`:`project/${q.id}`;const body=await upstream(url,cf);
    return project(cf?z.object({data:z.unknown()}).parse(body).data:body,cf);
   }
   if(action==='versions'){
    url.pathname+=cf?`mods/${q.id}/files`:`project/${q.id}/version`;
    url.search=cf?new URLSearchParams({gameVersion:q.version,modLoaderType:q.loader==='forge'?'1':'4',pageSize:'50',index:'0'}).toString():new URLSearchParams({game_versions:JSON.stringify([q.version]),loaders:JSON.stringify([q.loader])}).toString();
    const body=await upstream(url,cf);const list=cf?z.object({data:z.array(z.unknown())}).parse(body).data:z.array(z.unknown()).parse(body);
    return list.map(v=>version(v,cf,q.id!));
   }
   if(action==='version'||action==='download'){
    if(cf&&(!q.projectId||!/^\d+$/.test(q.projectId)))throw new CatalogError(400,'CurseForge-Projekt-ID fehlt.');
    url.pathname+=cf?`mods/${q.projectId}/files/${q.id}`:`version/${q.id}`;const body=await upstream(url,cf);
    const release=version(cf?z.object({data:z.unknown()}).parse(body).data:body,cf,q.projectId??'');
    if(action==='download'){
     if(!release.file||!release.loaders.includes(q.loader)||!release.gameVersions.includes(q.version))throw new CatalogError(403,'Kein erlaubter kompatibler Download.');
     const file=release.file,link=new URL(file.url);const hosts=cf?['edge.forgecdn.net','mediafilez.forgecdn.net','media.forgecdn.net']:['cdn.modrinth.com'];
     if(link.protocol!=='https:'||link.port||link.username||link.password||!hosts.includes(link.hostname))throw new CatalogError(502,'Nicht freigegebene Mod-Quelle.');
     const response=await request(link,{signal:AbortSignal.timeout(120000),redirect:'error'});if(!response.ok)throw new CatalogError(502,'Mod-Download nicht erreichbar.');
     const bytes=await bounded(response,file.size);if(bytes.length!==file.size||createHash('sha1').update(bytes).digest('hex')!==file.sha1)throw new CatalogError(502,'Mod-Integritätsprüfung fehlgeschlagen.');return bytes;
    }
    return release;
   }
   throw new CatalogError(404,'Unbekannte Katalogfunktion.');
  },
 };
}
export async function bounded(response:Response,max:number):Promise<Uint8Array>{
 if(!response.body)throw new CatalogError(502,'Leere Download-Antwort.');
 const reader=response.body.getReader(),chunks:Uint8Array[]=[];let size=0;
 try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>max)throw new CatalogError(502,'Download überschreitet das Größenlimit.');chunks.push(value);}}finally{await reader.cancel();}
 const bytes=new Uint8Array(size);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}return bytes;
}
export function project(raw:unknown,cf:boolean):ModProject{
 const data=z.record(z.string(),z.unknown()).parse(raw);
 if(cf){const d=z.object({id:z.number(),slug:z.string(),name:z.string(),summary:z.string(),authors:z.array(z.object({name:z.string()})),downloadCount:z.number(),logo:z.object({thumbnailUrl:z.string()}).nullable(),links:z.object({websiteUrl:z.string()})}).parse(data);return modProjectSchema.parse({id:String(d.id),slug:d.slug,title:d.name,description:d.summary,author:d.authors.map(a=>a.name).join(', '),downloads:d.downloadCount,iconUrl:d.logo?.thumbnailUrl??null,url:d.links.websiteUrl});}
 const d=z.object({project_id:z.string().optional(),id:z.string().optional(),slug:z.string(),title:z.string(),description:z.string(),author:z.string().optional(),downloads:z.number(),icon_url:z.string().nullable()}).parse(data);
 return modProjectSchema.parse({id:d.project_id??d.id,slug:d.slug,title:d.title,description:d.description,author:d.author??'',downloads:d.downloads,iconUrl:d.icon_url,url:`https://modrinth.com/mod/${d.slug}`});
}
export function version(raw:unknown,cf:boolean,projectId:string):ModVersion{
 if(cf){
  const d=z.object({id:z.number(),modId:z.number(),displayName:z.string(),fileName:z.string(),gameVersions:z.array(z.string()),releaseType:z.number(),downloadUrl:z.string().nullable(),fileLength:z.number(),hashes:z.array(z.object({value:z.string(),algo:z.number()})),dependencies:z.array(z.object({modId:z.number(),relationType:z.number()})),isAvailable:z.boolean()}).parse(raw);
  const hash=d.hashes.find(h=>h.algo===1)?.value;
  return modVersionSchema.parse({id:String(d.id),projectId:String(d.modId),name:d.displayName,number:d.displayName,gameVersions:d.gameVersions,loaders:d.gameVersions.filter(v=>['Forge','Fabric'].includes(v)).map(v=>v.toLowerCase()),release:d.releaseType===1?'release':d.releaseType===2?'beta':'alpha',file:d.isAvailable&&d.downloadUrl&&hash?{filename:d.fileName,url:d.downloadUrl,size:d.fileLength,sha1:hash}:null,dependencies:d.dependencies.filter(d=>[2,3,5,6].includes(d.relationType)).map(d=>({projectId:String(d.modId),versionId:null,kind:d.relationType===3?'required':d.relationType===5?'incompatible':d.relationType===6?'embedded':'optional'}))});
 }
 const d=z.object({id:z.string(),project_id:z.string(),name:z.string(),version_number:z.string(),game_versions:z.array(z.string()),loaders:z.array(z.string()),version_type:z.enum(['release','beta','alpha']),files:z.array(z.object({filename:z.string(),url:z.string(),size:z.number(),primary:z.boolean(),hashes:z.object({sha1:z.string()})})),dependencies:z.array(z.object({project_id:z.string().nullable(),version_id:z.string().nullable(),dependency_type:z.enum(['required','optional','incompatible','embedded'])}))}).parse(raw);
 const f=d.files.find(f=>f.primary)??d.files[0];
 void projectId;
 return modVersionSchema.parse({id:d.id,projectId:d.project_id,name:d.name,number:d.version_number,gameVersions:d.game_versions,loaders:d.loaders,release:d.version_type,file:f?{filename:f.filename,url:f.url,size:f.size,sha1:f.hashes.sha1}:null,dependencies:d.dependencies.map(v=>({projectId:v.project_id,versionId:v.version_id,kind:v.dependency_type}))});
}
