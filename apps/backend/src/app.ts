import Fastify from 'fastify';
import helmet from '@fastify/helmet';
import rateLimit from '@fastify/rate-limit';
import type { Redis } from 'ioredis';
import { z } from 'zod';
import { settingsSchema } from '@hera/shared-types';
import { createCatalog, CatalogError } from './catalog.js';

const syncSchema=z.object({ revision:z.number().int().min(0), settings:settingsSchema }).strict();
export type SyncInput=z.infer<typeof syncSchema>;
export interface SyncRecord {revision:number;settings:SyncInput['settings']}
export interface Dependencies {
 authenticate:(token:string)=>Promise<string|null>;
 read:(uuid:string)=>Promise<SyncRecord|null>;
 write:(uuid:string,input:SyncInput)=>Promise<SyncRecord|null>;
 ready:()=>Promise<boolean>;
 redis?:Redis;
 curseforgeKey?:string;
}
export async function createApp(deps:Dependencies) {
 const app=Fastify({bodyLimit:16_384,logger:{redact:['req.headers.authorization','req.headers.cookie']},trustProxy:false});
 await app.register(helmet);
 await app.register(rateLimit,{max:60,timeWindow:60_000,...(deps.redis?{redis:deps.redis}:{}),skipOnError:false});
 const catalog=createCatalog(deps.curseforgeKey);
 app.get<{Params:{action:string};Querystring:Record<string,string>}>('/api/catalog/:action',async(req,reply)=>{
  try{const result=await catalog.handle(req.params.action,req.query);return result instanceof Uint8Array?reply.type('application/java-archive').send(Buffer.from(result)):result;}catch(e){if(e instanceof CatalogError)return reply.code(e.status).send({error:e.message});throw e;}
 });
 app.get('/health/live',async()=>({status:'alive'}));
 app.get('/health/ready',async(_req,reply)=>{const ready=await deps.ready().catch(()=>false);return reply.code(ready?200:503).send({status:ready?'ready':'unavailable'});});
 app.register(async secure=>{
   secure.decorateRequest('minecraftId','');
   secure.addHook('preHandler',async(req,reply)=>{
     const header=req.headers.authorization;
     if(!header?.startsWith('Bearer ')||header.length>8192)return reply.code(401).send({error:'Authentication required'});
     const id=await deps.authenticate(header.slice(7)).catch(()=>null);
     if(!id||!/^[a-f0-9]{32}$/i.test(id))return reply.code(401).send({error:'Invalid Minecraft session'});
     req.minecraftId=id;
   });
   secure.get('/v1/me/settings',async(req,reply)=>{const record=await deps.read(req.minecraftId);return record??reply.code(404).send({error:'No synced settings'});});
   secure.put('/v1/me/settings',async(req,reply)=>{
     const parsed=syncSchema.safeParse(req.body);if(!parsed.success)return reply.code(400).send({error:'Invalid settings'});
     const record=await deps.write(req.minecraftId,parsed.data);
     return record??reply.code(409).send({error:'Revision conflict; fetch the latest settings first'});
   });
 });
 app.setErrorHandler((error,req,reply)=>{const code=typeof error==='object'&&error!==null&&'statusCode'in error&&typeof error.statusCode==='number'?error.statusCode:503;if(code<500)return reply.code(code).send({error:'Request rejected'});req.log.error({requestId:req.id},'Backend operation failed');return reply.code(503).send({error:'Service unavailable'});});
 return app;
}
declare module 'fastify' { interface FastifyRequest { minecraftId:string } }
