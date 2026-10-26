import type { Version } from '../../../../packages/shared-types/src';
/** Use the official release timestamp, never lexicographic version ordering. */
export function catalogThrough(versions: Version[], last = '26.3'): Version[] {
 const cutoff = versions.find(v=>v.id===last)?.releaseTime;
 if (!cutoff) throw new Error(`Die offizielle Versionsliste enthält ${last} noch nicht. Bitte später erneut laden.`);
 const time=Date.parse(cutoff);
 return versions.filter(v=>Number.isFinite(Date.parse(v.releaseTime))&&Date.parse(v.releaseTime)<=time);
}
export function versionKind(type:string) {return ({release:'Release',snapshot:'Snapshot',old_beta:'Beta',old_alpha:'Alpha'} as Record<string,string>)[type]??type;}
