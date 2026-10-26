import { describe, it, expect } from 'vitest';
import { catalogThrough } from '../apps/launcher/src/lib/versions';
const rows=[{id:'26.4-snapshot-1',type:'snapshot',releaseTime:'2026-10-01'},{id:'26.3',type:'release',releaseTime:'2026-09-15'},{id:'1.8.9',type:'release',releaseTime:'2015-12-09'},{id:'b1.7.3',type:'old_beta',releaseTime:'2011-07-08'}];
describe('official version catalog boundary',()=>{
 it('keeps old versions and excludes releases after the requested ceiling',()=>expect(catalogThrough(rows).map(v=>v.id)).toEqual(['26.3','1.8.9','b1.7.3']));
 it('does not silently invent a release if the official ceiling is missing',()=>expect(()=>catalogThrough(rows.filter(v=>v.id!=='26.3'))).toThrow('26.3'));
});
