import { useRef, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload, Trash2 } from 'lucide-react';
import { api, native } from '../lib/bridge';
import { includesHera } from '../lib/hera-profile';
import { Toggle } from './ui';
import type { Account, Profile } from '../../../../packages/shared-types/src';

export function CapePage({profiles,active,account,select}:{profiles:Profile[];active?:Profile;account?:Account;select:(id:string)=>void}){
 const picker=useRef<HTMLInputElement>(null),qc=useQueryClient();
 const [busy,setBusy]=useState(false),[error,setError]=useState('');
 const supported=includesHera(active),id=active?.id??'';
 const cape=useQuery({queryKey:['cape',id],queryFn:()=>api.localCape(id),enabled:native&&supported});
 const image=cape.data?`data:image/png;base64,${cape.data.pngBase64}`:undefined;
 async function change(action:()=>Promise<unknown>){const target=id;setBusy(true);setError('');try{await action();await qc.invalidateQueries({queryKey:['cape',target]});}catch(e){setError(String(e));}finally{setBusy(false);}}
 async function upload(file:File){
  if(!account)return;
  if(file.size>1024*1024){setError('Bitte eine PNG-Datei mit höchstens 1 MB auswählen.');return;}
  const bytes=new Uint8Array(await file.arrayBuffer());let binary='';for(const byte of bytes)binary+=String.fromCharCode(byte);
  await change(()=>api.saveLocalCape(id,account.id,btoa(binary)));
 }
 return <><div className="page-heading"><div><span className="eyebrow">DEIN LOOK / DEIN HERA</span><h1>Dein Cape<span>.</span></h1><p>Dein eigenes Design. Direkt im Spiel.</p></div></div>
 <section className="panel"><div className="setting-row"><div><strong>Spielprofil</strong><p>Das Cape wird nur in diesem Profil verwendet.</p></div><select aria-label="Cape-Profil" value={id} disabled={busy} onChange={e=>{select(e.target.value);setError('');}}>{!profiles.length&&<option value="">Kein Profil vorhanden</option>}{profiles.map(p=><option key={p.id} value={p.id}>{p.name} · {p.loader} {p.version}</option>)}</select></div>
 {!supported&&<p className="hint">Wähle ein Fabric-1.21.11-Profil mit der Hera-Mod.</p>}
 {!native&&<p className="hint">Eigene Capes lassen sich in der installierten Hera-App hinzufügen.</p>}
 {!account&&<p className="hint">Verbinde zuerst dein Minecraft-Konto unter Account.</p>}
 <div className="cape-layout"><div className="cape-stage" aria-label="Cape-Texturvorschau">{image?<div className="cape-fabric" style={{backgroundImage:`url("${image}")`}}/>:<div className="cape-placeholder">Dein Design<br/><small>wartet auf dich</small></div>}<span>Lokale Texturvorschau</span></div>
 <div className="cape-details"><h2>Trag deinen eigenen Stil.</h2><p>Wähle eine Minecraft-Cape-PNG. Dein Cape bleibt lokal und ist nur für dich sichtbar.</p><p className="hint">64 × 32, 128 × 64, 256 × 128 oder 512 × 256 Pixel · maximal 1 MB · keine Animation</p>
 <input ref={picker} hidden type="file" accept="image/png,.png" onChange={e=>{const file=e.target.files?.[0];if(file)void upload(file).catch(e=>setError(String(e)));e.target.value='';}}/>
 <button className="primary" disabled={!native||!supported||!account||busy||cape.isFetching} onClick={()=>picker.current?.click()}><Upload size={17}/>{busy?'Wird gespeichert …':image?'Cape ersetzen':'Cape auswählen'}</button>
 {cape.data&&<><div className="setting-row"><div><strong>Cape verwenden</strong><p>{cape.data.owner.replaceAll('-','')===account?.id.replaceAll('-','')?`Für ${account.name}`:'Für das beim Hochladen gewählte Minecraft-Konto'}</p></div><Toggle label="Cape verwenden" checked={cape.data.enabled} onChange={value=>{if(!busy)void change(()=>api.setLocalCapeEnabled(id,value));}}/></div><button className="secondary" disabled={busy} onClick={()=>void change(()=>api.removeLocalCape(id))}><Trash2 size={16}/>Cape entfernen</button><p className="hint">In der dritten Person sichtbar (F5). Minecrafts Einstellung „Cape“ unter Skin-Anpassung muss eingeschaltet sein. Änderungen werden nach wenigen Sekunden übernommen.</p></>}
 </div></div>{cape.isLoading&&<p role="status">Cape wird geladen …</p>}{(error||cape.error)&&<p className="error" role="alert">{error||String(cape.error)}</p>}</section></>;
}
