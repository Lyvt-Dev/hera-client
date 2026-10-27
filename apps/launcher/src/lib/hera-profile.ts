import type { Profile, LoaderOptions } from '../../../../packages/shared-types/src';
export const HERA_MINECRAFT = '1.21.11';
export const HERA_MOD_VERSION = '0.11.0';
export function includesHera(profile?:Pick<Profile,'loader'|'version'>){return profile?.loader==='fabric'&&profile.version===HERA_MINECRAFT;}
export function loaderAvailable(loader:Profile['loader'],options?:LoaderOptions){return loader==='vanilla'||Boolean(options?.[loader]);}
