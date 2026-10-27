import { useRef, useState, useMemo, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload, Trash2 } from 'lucide-react';
import { api, native } from '../lib/bridge';
import { includesHera } from '../lib/hera-profile';
import { Toggle, PageHeader } from './ui';
import type { Account, Profile } from '../../../../packages/shared-types/src';

export function CapePage({profiles,active,account,select}:{profiles:Profile[];active?:Profile;account?:Account;select:(id:string)=>void}){
 const picker=useRef<HTMLInputElement>(null),qc=useQueryClient();
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[serviceUrl,setServiceUrl]=useState('http://127.0.0.1:3847'),[sharing,setSharing]=useState(false),[notice,setNotice]=useState('');
 const supported=includesHera(active),id=active?.id??'';
 const cape=useQuery({queryKey:['cape',id],queryFn:()=>api.localCape(id),enabled:native&&supported});
 const service=useQuery({queryKey:['cape-service',id],queryFn:()=>api.capeService(id),enabled:native&&supported});
 useEffect(()=>{setServiceUrl(service.data?.baseUrl??'http://127.0.0.1:3847');setSharing(service.data?.enabled??false);},[id,service.data]);
 useEffect(()=>{setNotice('');setError('');},[id]);
 const config={version:1,enabled:sharing,baseUrl:serviceUrl};
 const png=cape.data?.pngBase64;const image=useMemo(()=>png?`data:image/png;base64,${png}`:undefined,[png]);
 async function change(action:()=>Promise<unknown>){const target=id;setBusy(true);setError('');try{await action();await qc.invalidateQueries({queryKey:['cape',target]});await qc.invalidateQueries({queryKey:['cape-service',target]});}catch(e){setError(String(e));}finally{setBusy(false);}}
 async function upload(file:File){
  if(!account)return;
  if(file.size>1024*1024){setError('Bitte eine PNG-Datei mit höchstens 1 MB auswählen.');return;}
  const bytes=new Uint8Array(await file.arrayBuffer());let binary='';for(const byte of bytes)binary+=String.fromCharCode(byte);
  await change(()=>api.saveLocalCape(id,account.id,btoa(binary)));
 }
 return <><PageHeader title="Capes" description="Dein Cape – lokal oder gemeinsam über deinen Hera-Cape-Dienst."/>
 <section className="panel"><div className="setting-row"><div><strong>Spielprofil</strong><p>Das Cape wird nur in diesem Profil verwendet.</p></div><select aria-label="Cape-Profil" value={id} disabled={busy} onChange={e=>{select(e.target.value);setError('');}}>{!profiles.length&&<option value="">Kein Profil vorhanden</option>}{profiles.map(p=><option key={p.id} value={p.id}>{p.name} · {p.loader} {p.version}</option>)}</select></div>
 {(!native||!supported||!account)&&<p className="hint">{!native?"Eigene Capes sind in der installierten Hera-App verfügbar.":!supported?"Wähle ein Fabric-1.21.11-Profil mit der Hera-Mod.":"Verbinde zuerst dein Minecraft-Konto unter Accounts."}</p>}
 <div className="cape-layout"><div className="cape-stage" aria-label="Cape-Texturvorschau">{image?<div className="cape-fabric" style={{backgroundImage:`url("${image}")`}}/>:<div className="cape-placeholder">Kein Cape ausgewählt<br/><small>Deine PNG erscheint hier</small></div>}<span>Lokale Texturvorschau</span></div>
 <div className="cape-details"><h2>Eigenes Cape verwenden</h2><p>Wähle eine Cape-PNG für dein Minecraft-Konto. Ohne Cape-Dienst bleibt die Anzeige lokal auf diesem Gerät.</p><p className="hint">64 × 32, 128 × 64, 256 × 128 oder 512 × 256 Pixel · maximal 1 MB · keine Animation</p>
 <input ref={picker} hidden type="file" accept="image/png,.png" onChange={e=>{const file=e.target.files?.[0];if(file)void upload(file).catch(e=>setError(String(e)));e.target.value='';}}/>
 <button className="primary" disabled={!native||!supported||!account||busy||cape.isFetching} onClick={()=>picker.current?.click()}><Upload size={17}/>{busy?'Wird gespeichert …':image?'Cape ersetzen':'Cape auswählen'}</button>
 {cape.data&&<><div className="setting-row"><div><strong>Cape verwenden</strong><p>{cape.data.owner.replaceAll('-','')===account?.id.replaceAll('-','')?`Für ${account.name}`:'Für das beim Hochladen gewählte Minecraft-Konto'}</p></div><Toggle disabled={busy} label="Cape verwenden" checked={cape.data.enabled} onChange={value=>{if(!busy)void change(()=>api.setLocalCapeEnabled(id,value));}}/></div><button className="secondary" disabled={busy} onClick={()=>void change(()=>api.removeLocalCape(id))}><Trash2 size={16}/>Cape entfernen</button><p className="hint">In der dritten Person sichtbar (F5). Minecrafts Einstellung „Cape“ unter Skin-Anpassung muss eingeschaltet sein. Änderungen werden nach wenigen Sekunden übernommen.</p></>}
 </div></div>{cape.isLoading&&<p role="status">Cape wird geladen …</p>}{(error||cape.error)&&<p className="error" role="alert">{error||String(cape.error)}</p>}</section>
 <section className="panel"><h2>Gemeinsamer Cape-Dienst</h2><p>Alle Mitspieler benötigen die aktuelle Hera-Mod und dieselbe Dienst-Adresse. Bei Ausfall bleibt nur dein eigenes lokales Cape sichtbar.</p>
 <label className="cape-service-field">Dienst-Adresse<input aria-label="Cape-Dienst-Adresse" maxLength={512} value={serviceUrl} disabled={busy||!native||!supported} placeholder="http://192.168.1.20:3847" onChange={e=>{setServiceUrl(e.target.value);setNotice('');}}/></label>
 <div className="setting-row"><div><strong>Geteilte Capes anzeigen</strong><p>Die Mod fragt nur Capes geladener Spieler ab.</p></div><Toggle label="Geteilte Capes anzeigen" checked={sharing} disabled={busy||!native||!supported} onChange={setSharing}/></div>
 <div className="button-row"><button className="primary" disabled={busy||!native||!supported} onClick={()=>void change(async()=>{await api.saveCapeService(id,config);setNotice('Cape-Dienst gespeichert.');})}>Speichern</button>
 <button className="secondary" disabled={busy||!native||!supported} onClick={()=>void change(async()=>setNotice(await api.probeCapeService(config)))}>Verbindung testen</button>
 <button className="secondary" disabled={busy||!native||!supported||!account||!cape.data?.enabled||!sharing} onClick={()=>void change(async()=>{await api.saveCapeService(id,config);await api.publishCape(id,account!.id);setNotice('Dein Cape ist veröffentlicht. Andere Hera-Nutzer können es jetzt sehen.');})}>Eigenes Cape veröffentlichen</button>
 <button className="secondary" disabled={busy||!native||!supported||!account||!sharing} onClick={()=>void change(async()=>{await api.saveCapeService(id,config);await api.publishCape(id,account!.id,true);setNotice('Dein Cape wurde vom Dienst entfernt. Lokal bleibt es erhalten.');})}>Vom Dienst entfernen</button></div>
 <p className="hint">Zum Testen auf diesem PC: http://127.0.0.1:3847. Auf anderen PCs die LAN-IP des Dienst-PCs verwenden. HTTP ist nur im lokalen Netz erlaubt. Nach dem Ändern eines Capes erneut veröffentlichen.</p>
 {notice&&<p role="status">{notice}</p>}{service.error&&<p className="error" role="alert">{String(service.error)}</p>}</section></>;
}
