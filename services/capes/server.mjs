import http from 'node:http';
import { randomBytes, createHash } from 'node:crypto';
import { inflateSync } from 'node:zlib';
import { mkdir, readFile, writeFile, rename, unlink, lstat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const MAX_PNG=1048576, MAX_BODY=1500000, UUID=/^[a-f0-9]{32}$/, HASH=/^[a-f0-9]{64}$/;
const sha=b=>createHash('sha256').update(b).digest('hex');
const table=new Uint32Array(256);for(let n=0;n<256;n++){let c=n;for(let i=0;i<8;i++)c=c&1?0xedb88320^(c>>>1):c>>>1;table[n]=c>>>0;}
function crc(b){let c=0xffffffff;for(const v of b)c=table[(c^v)&255]^(c>>>8);return (c^0xffffffff)>>>0;}
/** Decode size is bounded before inflation. Only static RGB/RGBA 8-bit atlases. */
export function validatePng(b){
 if(!Buffer.isBuffer(b)||b.length<45||b.length>MAX_PNG||b.subarray(0,8).toString('hex')!=='89504e470d0a1a0a')throw Error('Invalid PNG');
 let offset=8,width=0,height=0,channels=0,ended=false,chunks=0;const data=[];
 while(offset+12<=b.length){if(++chunks>128)throw Error("Too many chunks");const size=b.readUInt32BE(offset),type=b.toString('ascii',offset+4,offset+8);if(size>MAX_PNG||offset+12+size>b.length)throw Error('Invalid chunk');
 const chunk=b.subarray(offset+4,offset+8+size);if(crc(chunk)!==b.readUInt32BE(offset+8+size))throw Error('Invalid CRC');
 const payload=b.subarray(offset+8,offset+8+size);
 if(offset===8&&type!=='IHDR')throw Error('Missing header');
 if(type==='IHDR'){if(width||size!==13)throw Error('Invalid header');width=payload.readUInt32BE(0);height=payload.readUInt32BE(4);channels=payload[9]===6?4:payload[9]===2?3:0;if(![64,128,256,512].includes(width)||height!==width/2||payload[8]!==8||!channels||payload[10]||payload[11]||payload[12])throw Error('Invalid atlas');}
 else if(type==='IDAT')data.push(payload);
 else if(type==='IEND'){if(size||offset+12!==b.length)throw Error('Invalid end');ended=true;break;}
 else if(type==='acTL'||type==='fcTL'||type==='fdAT'||type[0]===type[0].toUpperCase())throw Error('Unsupported PNG');
 offset+=size+12;
 }
 if(!ended||!data.length)throw Error('Incomplete PNG');const expected=(width*channels+1)*height;
 const pixels=inflateSync(Buffer.concat(data),{maxOutputLength:expected});if(pixels.length!==expected)throw Error('Invalid pixels');for(let row=0;row<height;row++)if(pixels[row*(width*channels+1)]>4)throw Error('Invalid filter');return b;
}
async function verifyMojang(challenge){
 const url=new URL('https://sessionserver.mojang.com/session/minecraft/hasJoined');url.searchParams.set('username',challenge.playerName);url.searchParams.set('serverId',challenge.serverId);
 const r=await fetch(url,{redirect:'error',signal:AbortSignal.timeout(10000)});if(r.status!==200)throw Error('Ownership not confirmed');const text=await r.text();if(text.length>65536)throw Error('Invalid proof');return JSON.parse(text);
}
export function createCapeServer({dataDir=path.resolve('cape-data'),verifyJoin=verifyMojang,now=Date.now}={}){
 const challenges=new Map(),rates=new Map(),queues=new Map();
 function limited(ip){const time=now();for(const [k,v]of rates)if(v.until<time)rates.delete(k);let r=rates.get(ip);if(!r){if(rates.size>=2048)return true;r={count:0,until:time+60000};rates.set(ip,r);}return ++r.count>180;}
 function send(res,status,value){const bytes=Buffer.from(JSON.stringify(value));res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Content-Length':bytes.length});res.end(bytes);}
 async function body(req){const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>MAX_BODY)throw Error('Request too large');chunks.push(chunk);}return JSON.parse(Buffer.concat(chunks).toString('utf8'));}
 async function stored(id){const file=path.join(dataDir,id+'.png');try{const stat=await lstat(file);if(!stat.isFile()||stat.isSymbolicLink()||stat.size>MAX_PNG)return null;const png=await readFile(file);validatePng(png);return {sha256:sha(png),pngBase64:png.toString('base64')};}catch(e){if(e.code==='ENOENT')return null;return null;}}
 const server=http.createServer(async(req,res)=>{try{
  if(limited(req.socket.remoteAddress??''))return send(res,429,{error:'Too many requests'});
  const url=new URL(req.url,'http://localhost');if(req.headers.origin)return send(res,403,{error:'Native clients only'});
  if(req.method==='GET'&&url.pathname==='/health')return send(res,200,{service:'hera-capes',version:1});
  if(req.method==='GET'&&url.pathname==='/v1/capes'){
   const ids=[...new Set((url.searchParams.get('ids')??'').split(','))];if(ids.length>8||ids.some(id=>!UUID.test(id)))return send(res,400,{error:'Invalid IDs'});
   const known=new Map((url.searchParams.get('known')??'').split(',').slice(0,8).map(part=>part.split(':')));const capes={};for(const id of ids){const value=await stored(id);capes[id]=value&&value.sha256===known.get(id)?{sha256:value.sha256,pngBase64:null}:value;}return send(res,200,{version:1,capes});
  }
  if(req.method==='POST'&&url.pathname==='/v1/challenges'){
   const b=await body(req);for(const [id,c] of challenges)if(c.expires<now())challenges.delete(id);
   if(!UUID.test(b.owner)||!/^\w{1,16}$/.test(b.playerName)||!['publish','remove'].includes(b.action)||!HASH.test(b.sha256)||challenges.size>=512)return send(res,400,{error:'Invalid challenge'});
   const id=randomBytes(24).toString('hex'),serverId=randomBytes(20).toString('hex');challenges.set(id,{...b,serverId,expires:now()+60000});return send(res,200,{id,serverId});
  }
  if(req.method==='POST'&&url.pathname==='/v1/capes'){
   const b=await body(req),c=challenges.get(b.challengeId);challenges.delete(b.challengeId);if(!c||c.expires<now())return send(res,401,{error:'Expired challenge'});
   let png;if(c.action==='publish'){if(typeof b.pngBase64!=='string'||b.pngBase64.length>1400000)return send(res,400,{error:'Invalid PNG'});png=validatePng(Buffer.from(b.pngBase64,'base64'));if(sha(png)!==c.sha256)return send(res,400,{error:'Image changed'});}else if(c.sha256!==sha(Buffer.alloc(0)))return send(res,400,{error:'Invalid removal'});
   let proof;try{proof=await verifyJoin(c);}catch{return send(res,401,{error:'Minecraft ownership not confirmed'});}if(c.expires<now())return send(res,401,{error:'Expired challenge'});if(proof?.id?.toLowerCase()!==c.owner)return send(res,403,{error:'Wrong Minecraft account'});
   const previous=queues.get(c.owner)??Promise.resolve();const write=previous.catch(()=>{}).then(async()=>{await mkdir(dataDir,{recursive:true});const file=path.join(dataDir,c.owner+'.png');if(c.action==='remove'){try{await unlink(file);}catch(e){if(e.code!=='ENOENT')throw e;}}else{const temporary=path.join(dataDir,randomBytes(16).toString('hex')+'.tmp');try{await writeFile(temporary,png,{flag:'wx',mode:0o600});await rename(temporary,file);}finally{await unlink(temporary).catch(()=>{});}}});queues.set(c.owner,write);try{await write;}finally{if(queues.get(c.owner)===write)queues.delete(c.owner);}return send(res,200,{ok:true});
  }
  send(res,404,{error:'Not found'});
 }catch{if(!res.headersSent)send(res,400,{error:'Request could not be processed'});else res.destroy();}});
 server.requestTimeout=15000;server.headersTimeout=10000;server.maxHeadersCount=32;server.keepAliveTimeout=2000;return server;
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const host=process.argv.includes('--lan')?'0.0.0.0':'127.0.0.1',port=Number(process.env.HERA_CAPE_PORT??3847);
 if(!Number.isInteger(port)||port<1024||port>65535)throw Error('Invalid port');
 const dataDir=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'cape-data');
 createCapeServer({dataDir}).listen(port,host,()=>console.log(`Hera Cape Service: http://${host}:${port} (${dataDir})`));
}
