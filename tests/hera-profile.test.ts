import { describe,it,expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { HERA_MOD_VERSION,includesHera,loaderAvailable } from '../apps/launcher/src/lib/hera-profile';
describe('Hera profile setup',()=>{
 it('includes the mod only for its actual loader and game version',()=>{expect(includesHera({loader:'fabric',version:'1.21.11'})).toBe(true);expect(includesHera({loader:'forge',version:'1.21.11'})).toBe(false);expect(includesHera({loader:'fabric',version:'1.21.1'})).toBe(false);expect(includesHera(undefined)).toBe(false);});
 it('uses official availability instead of fixed game versions',()=>{expect(loaderAvailable('fabric',{fabric:'0.18.1',forge:null})).toBe(true);expect(loaderAvailable('forge',{fabric:null,forge:'1.21.11-61.2.4'})).toBe(true);});
 it('does not offer absent loaders',()=>expect(loaderAvailable('fabric',{fabric:null,forge:'legacy-build'})).toBe(false));
 it('waits for catalog data before offering mod loaders',()=>expect(loaderAvailable('forge')).toBe(false));
 it('always keeps Vanilla available',()=>expect(loaderAvailable('vanilla')).toBe(true));
 it('matches the actual native bundle version',()=>{const payloads=JSON.parse(readFileSync(new URL('../apps/launcher/src-tauri/resources/bundled-mods.json',import.meta.url),'utf8')) as {filename:string}[];expect(payloads.some(p=>p.filename===`hera-fabric-1.21.11-${HERA_MOD_VERSION}.jar`)).toBe(true);});
});
