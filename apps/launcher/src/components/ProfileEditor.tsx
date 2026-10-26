import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Box, Check, Layers } from 'lucide-react';
import { profileSchema, type Profile } from '../../../../packages/shared-types/src';
import { api } from '../lib/bridge';
import { catalogThrough } from '../lib/versions';
import { Modal } from './ui';
export function ProfileEditor({ profile, initialVersion, java, close, save }: { profile?:Profile; initialVersion?:string; java:string|null; close:()=>void; save:(p:Profile)=>Promise<void> }) {
 const [draft,setDraft] = useState<Profile>(profile ?? { id:crypto.randomUUID(), name:'Mein Minecraft',version:initialVersion??'26.3',loader:'vanilla',memoryMb:4096,javaPath:java??'java' });
 const versions=useQuery({queryKey:['versions'],queryFn:async()=>catalogThrough(await api.versions()),staleTime:300000});
 const choices=versions.data??[];
 const [error,setError] = useState(''); const [pending,setPending] = useState(false);
 async function submit(e:FormEvent) { e.preventDefault(); setError('');setPending(true); try { if(!profile&&!choices.some(v=>v.id===draft.version))throw Error('Bitte eine Version aus dem offiziellen Katalog auswählen.');await save(profileSchema.parse(draft)); close(); } catch(e) {setError(String(e));} finally {setPending(false);} }
 return <Modal title={profile?'Dein Profil bearbeiten':'Dein nächstes Abenteuer'} close={()=>{if(!pending)close();}}><form onSubmit={submit}>
 <p className="dialog-intro">Ein eigener Platz für deine Welten, Mods und Einstellungen.</p>
 <label>Wie soll dein Profil heißen?<input autoFocus required maxLength={48} value={draft.name} onChange={e=>setDraft({...draft,name:e.target.value})}/></label>
 <label>Minecraft-Version<select disabled={Boolean(profile)||versions.isPending} value={draft.version} onChange={e=>setDraft({...draft,version:e.target.value,loader:e.target.value==='1.21.1'?draft.loader:'vanilla'})}>{!choices.some(v=>v.id===draft.version)&&<option value={draft.version}>{draft.version}</option>}{choices.map(v=><option key={v.id} value={v.id}>{v.id}{v.type==='release'?'':` · ${v.type}`}</option>)}</select></label>
 {versions.error&&!profile&&<p className="error" role="alert">Versionsliste nicht erreichbar. <button type="button" onClick={()=>void versions.refetch()}>Erneut versuchen</button></p>}
 <fieldset className="loader-choices"><legend>Wie möchtest du spielen?</legend>{(['vanilla','fabric','forge'] as const).map(loader=><button type="button" key={loader} className={`loader-choice ${draft.loader===loader?'chosen':''}`} aria-pressed={draft.loader===loader} disabled={Boolean(profile)||(loader!=='vanilla'&&draft.version!=='1.21.1')} onClick={()=>setDraft({...draft,loader})}>{loader==='vanilla'?<Box size={20}/>:<Layers size={20}/>}<span><strong>{loader==='vanilla'?'Vanilla':loader==='fabric'?'Fabric':'Forge'}</strong><small>{loader==='vanilla'?'Das Original':'Für deine Mods · 1.21.1'}</small></span>{draft.loader===loader&&<Check size={17}/>}</button>)}</fieldset>
 {profile&&<p className="hint">Für eine andere Version oder einen anderen Loader ein neues Profil erstellen. So bleiben deine Welten getrennt.</p>}
 <label>Arbeitsspeicher <span>{(draft.memoryMb/1024).toFixed(1)} GB</span><input aria-label="Arbeitsspeicher" type="range" min="2048" max="16384" step="512" value={draft.memoryMb} onChange={e=>setDraft({...draft,memoryMb:Number(e.target.value)})}/></label>
 <details className="profile-advanced"><summary>Erweiterte Einstellungen</summary><label>Java-Datei<input required value={draft.javaPath} onChange={e=>setDraft({...draft,javaPath:e.target.value})}/></label><p className="hint">Hera prüft beim Start die Java-Version anhand der offiziellen Anforderungen. 26.3 benötigt Java 25; ältere Versionen benötigen eine passende ältere Runtime. Java wird noch nicht automatisch installiert.</p></details>
 {error&&<p role="alert" className="error">{error}</p>}<div className="dialog-actions"><button type="button" className="secondary" disabled={pending} onClick={close}>Abbrechen</button><button disabled={pending||(!profile&&(versions.isPending||Boolean(versions.error)))} className="primary">{pending?'Wird gespeichert …':profile?'Änderungen speichern':'Profil erstellen'}</button></div>
 </form></Modal>;
}
