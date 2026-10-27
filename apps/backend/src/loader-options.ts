import { profileSchema } from '@hera/shared-types';
type Fetch=typeof fetch;
/** Development renderer proxy for official public metadata; no arbitrary URLs. */
export async function loaderOptions(raw:string,request:Fetch=fetch):Promise<{fabric:string|null;forge:string|null}>{
 const version=profileSchema.shape.version.parse(raw);
 async function read(url:string,empty404=false){const response=await request(url,{signal:AbortSignal.timeout(15000),redirect:'error'});if(empty404&&response.status===404)return [];if(!response.ok)throw Error('Loader-Katalog nicht verfügbar. Bitte erneut versuchen.');if(!response.body)throw Error('Loader-Katalog ist leer.');const reader=response.body.getReader(),chunks:Uint8Array[]=[];let size=0;try{while(true){const chunk=await reader.read();if(chunk.done)break;size+=chunk.value.byteLength;if(size>4*1024*1024)throw Error('Loader-Katalog zu groß.');chunks.push(chunk.value);}}finally{await reader.cancel();}const bytes=new Uint8Array(size);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}return JSON.parse(new TextDecoder().decode(bytes)) as unknown;}
 const [rawFabric,rawForge]=await Promise.all([read(`https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(version)}`,true),read('https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json')]);
 const fabric=rawFabric as {loader:{version:string;stable:boolean}}[];
 const forge=rawForge as {promos:Record<string,string>};
 if(!Array.isArray(fabric)||!forge.promos||typeof forge.promos!=='object')throw Error('Loader-Katalog beschädigt.');
 const coordinate=/^[a-zA-Z0-9._+-]{1,119}$/;
 const stable=fabric.find(v=>v?.loader?.stable&&typeof v.loader.version==='string'&&coordinate.test(v.loader.version))?.loader.version??null;
 const build=forge.promos[`${version}-recommended`]??forge.promos[`${version}-latest`];
 return {fabric:stable,forge:typeof build==='string'&&coordinate.test(build)?`${version}-${build}`:null};
}
