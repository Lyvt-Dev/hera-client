import type { Plugin } from 'vite';
import { createCatalog, CatalogError } from '../backend/src/catalog';
export function catalogPlugin(key:string|undefined):Plugin {
 const catalog=createCatalog(key);
 return {name:'hera-local-catalog',configureServer(server){
  server.middlewares.use('/api/catalog',async(req,res,next)=>{
   if(req.method!=='GET'){res.statusCode=405;res.end();return;}
   try{
    const url=new URL(req.url??'/', 'http://localhost');const action=url.pathname.slice(1);
    if(!['status','search','project','versions','version','download'].includes(action)){next();return;}
    const result=await catalog.handle(action,Object.fromEntries(url.searchParams));
    res.setHeader('Content-Type',result instanceof Uint8Array?'application/java-archive':'application/json');res.setHeader('Cache-Control','no-store');res.end(result instanceof Uint8Array?Buffer.from(result):JSON.stringify(result));
   }catch(e){res.statusCode=e instanceof CatalogError?e.status:502;res.setHeader('Content-Type','application/json');res.end(JSON.stringify({error:e instanceof CatalogError?e.message:'Katalogdienst derzeit nicht verfügbar.'}));}
  });
 }};
}
