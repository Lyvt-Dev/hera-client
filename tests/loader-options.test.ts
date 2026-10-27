import { describe,it,expect } from 'vitest';
import { loaderOptions } from '../apps/backend/src/loader-options';
describe('development loader metadata proxy',()=>{
 it('uses the selected version and keeps only stable Fabric coordinates',async()=>{const urls:string[]=[];const request:typeof fetch=async input=>{urls.push(String(input));return new Response(JSON.stringify(String(input).includes('fabricmc.net')?[{loader:{version:'unstable',stable:false}},{loader:{version:'0.18.1',stable:true}}]:{promos:{'1.21.11-recommended':'61.2.4'}}));};expect(await loaderOptions('1.21.11',request)).toEqual({fabric:'0.18.1',forge:'1.21.11-61.2.4'});expect(urls[0].endsWith('/1.21.11')).toBe(true);});
 it('rejects malformed versions before making a request',async()=>{let calls=0;await expect(loaderOptions('../escape',async()=>{calls++;throw Error('unexpected');})).rejects.toThrow();expect(calls).toBe(0);});
 it('keeps a failed catalog distinct from an unavailable loader',async()=>{await expect(loaderOptions('1.21.11',async()=>new Response('',{status:503}))).rejects.toThrow('nicht verfügbar');});
});
