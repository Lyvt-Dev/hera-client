import { describe,it,expect } from 'vitest';
import { createApp, type Dependencies } from '../apps/backend/src/app';
const id='f18547018b0d4ebaa288c080932a5667';
const deps:Dependencies={authenticate:async token=>token==='valid'?id:null,read:async()=>null,write:async()=>null,ready:async()=>true};
describe('API authentication and validation',()=>{
 it('does not expose settings anonymously',async()=>{const app=await createApp(deps);try{const r=await app.inject({url:'/v1/me/settings'});expect(r.statusCode).toBe(401);}finally{await app.close();}});
 it('rejects invalid bearer sessions',async()=>{const app=await createApp(deps);try{expect((await app.inject({url:'/v1/me/settings',headers:{authorization:'Bearer wrong'}})).statusCode).toBe(401);}finally{await app.close();}});
 it('does not allow overwriting another account',async()=>{const app=await createApp(deps);try{expect((await app.inject({method:'PUT',url:'/v1/me/settings',headers:{authorization:'Bearer valid'},payload:{revision:0,uuid:id,settings:{reducedMotion:false,accent:'violet',scale:1}}})).statusCode).toBe(400);}finally{await app.close();}});
 it('reports write conflicts',async()=>{const app=await createApp(deps);try{expect((await app.inject({method:'PUT',url:'/v1/me/settings',headers:{authorization:'Bearer valid'},payload:{revision:1,settings:{reducedMotion:false,accent:'violet',scale:1}}})).statusCode).toBe(409);}finally{await app.close();}});
 it('maps dependency failure to readiness failure',async()=>{const app=await createApp({...deps,ready:async()=>{throw Error('unavailable');}});try{expect((await app.inject({url:'/health/ready'})).statusCode).toBe(503);}finally{await app.close();}});
 it('validates account identity with the verifier',async()=>{let seen='';const app=await createApp({...deps,read:async uuid=>{seen=uuid;return {revision:1,settings:{reducedMotion:true,accent:'cyan',scale:1}};}});try{expect((await app.inject({url:'/v1/me/settings',headers:{authorization:'Bearer valid'}})).statusCode).toBe(200);expect(seen).toBe(id);}finally{await app.close();}});
});
