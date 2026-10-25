import Fastify from 'fastify';
import helmet from '@fastify/helmet';
import rateLimit from '@fastify/rate-limit';
import { createCatalog, CatalogError } from './catalog.js';
// A separate local gateway for desktop development. No DB or online accounts required.
const app=Fastify({logger:true,bodyLimit:4096,trustProxy:false});
await app.register(helmet);await app.register(rateLimit,{max:120,timeWindow:60_000});
const catalog=createCatalog(process.env.CURSEFORGE_API_KEY);
app.get<{Params:{action:string};Querystring:Record<string,string>}>('/api/catalog/:action',async(req,reply)=>{
 try{const result=await catalog.handle(req.params.action,req.query);return result instanceof Uint8Array?reply.type('application/java-archive').send(Buffer.from(result)):result;}
 catch(e){return reply.code(e instanceof CatalogError?e.status:502).send({error:e instanceof CatalogError?e.message:'Mod-Katalog nicht verfügbar.'});}
});
await app.listen({host:'127.0.0.1',port:Number(process.env.CATALOG_PORT??3101)});
process.once('SIGINT',()=>void app.close());process.once('SIGTERM',()=>void app.close());
