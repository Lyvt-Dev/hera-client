import { invoke, isTauri } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { z } from 'zod';
import { profileSchema, settingsSchema, defaultSettings, type Profile, type Settings, type Account, type Runtime, type Transfer, type Version } from '../../../../packages/shared-types/src';
export const native = isTauri();
function read<T>(key: string, schema: z.ZodType<T>, fallback: T): T {
  try { return schema.parse(JSON.parse(localStorage.getItem(key) ?? 'null')); } catch { return fallback; }
}
export const api = {
  gameRunning:():Promise<boolean>=>native?invoke('game_running'):Promise.resolve(false),
  localCape:(id:string)=>invoke<{version:number;owner:string;enabled:boolean;pngBase64:string}|null>('local_cape',{id}),
  saveLocalCape:(id:string,accountId:string,pngBase64:string)=>invoke<void>('save_local_cape',{id,accountId,pngBase64}),
  setLocalCapeEnabled:(id:string,enabled:boolean)=>invoke<void>('set_local_cape_enabled',{id,enabled}),
  removeLocalCape:(id:string)=>invoke<void>('remove_local_cape',{id}),
  loaderOptions:async(version:string):Promise<{fabric:string|null;forge:string|null}>=>{
    if(native)return invoke('loader_options',{version});
    const response=await fetch(`/api/loader-options?${new URLSearchParams({version})}`,{signal:AbortSignal.timeout(20000)});
    if(!response.ok)throw Error('Loader-Katalog nicht verfügbar. Bitte erneut versuchen.');
    return z.object({fabric:z.string().nullable(),forge:z.string().nullable()}).parse(await response.json());
  },
  profiles: (): Promise<Profile[]> => native ? invoke('profiles') : Promise.resolve(read('hera.profiles', z.array(profileSchema), [])),
  saveProfile: async (input: Profile) => {
    const profile = profileSchema.parse(input);
    if (native) return invoke<void>('save_profile', { profile });
    const all = await api.profiles();
    localStorage.setItem('hera.profiles', JSON.stringify([...all.filter(p => p.id !== profile.id), profile]));
  },
  deleteProfile: async (id: string) => {
    if (native) return invoke<void>('delete_profile', { id });
    localStorage.setItem('hera.profiles', JSON.stringify((await api.profiles()).filter(p => p.id !== id)));
  },
  settings: (): Promise<Settings> => native ? invoke('settings') : Promise.resolve(read('hera.settings', settingsSchema, defaultSettings)),
  saveSettings: async (settings: Settings) => {
    settingsSchema.parse(settings);
    if (native) return invoke<void>('save_settings', { settings });
    localStorage.setItem('hera.settings', JSON.stringify(settings));
  },
  runtime: (): Promise<Runtime> => native ? invoke('runtime') : Promise.resolve({ java: null, platform: 'Browser-Vorschau', authConfigured: false }),
  versions: async (): Promise<Version[]> => {
    if (native) return invoke('versions');
    const r = await fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json');
    if (!r.ok) throw new Error(`Versionsdienst: HTTP ${r.status}`);
    return z.object({ versions: z.array(z.object({ id:z.string(), type:z.string(), releaseTime:z.string() })) }).parse(await r.json()).versions;
  },
  accounts: (): Promise<Account[]> => native ? invoke('accounts') : Promise.resolve([]),
  signIn: (): Promise<void> => invoke('begin_login'),
  cancelLogin: ():Promise<void> => invoke('cancel_login'),
  completeLogin: (): Promise<Account> => invoke('complete_login'),
  logout: (id: string): Promise<void> => invoke('logout', { id }),
  install: (id: string): Promise<void> => invoke('install', { id }),
  launch: (id: string, accountId: string): Promise<void> => invoke('launch', { id, accountId }),
  stop: (): Promise<void> => invoke('stop'),
  clientId: ():Promise<string> => native?invoke('client_id'):Promise.resolve(localStorage.getItem('hera.microsoftClientId')??''),
  saveClientId: async(clientId:string) => {const id=clientId.trim();if(id&&!z.uuid().safeParse(id).success)throw Error('Bitte die öffentliche Anwendungs-ID im UUID-Format eingeben.');if(native)return invoke<void>('save_client_id',{clientId:id});localStorage.setItem('hera.microsoftClientId',id);},
  onTransfer: (fn: (value: Transfer) => void) => listen<Transfer>('transfer', e => fn(e.payload)),
  onExit: (fn: (value: string) => void) => listen<string>('game-exit', e => fn(e.payload)),
};
