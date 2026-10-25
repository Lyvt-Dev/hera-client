import { useState, type FormEvent } from 'react';
import { profileSchema, type Profile } from '../../../../packages/shared-types/src';
import { Modal } from './ui';
export function ProfileEditor({ profile, java, close, save }: { profile?:Profile; java:string|null; close:()=>void; save:(p:Profile)=>Promise<void> }) {
 const [draft,setDraft] = useState<Profile>(profile ?? { id:crypto.randomUUID(), name:'Mein Mod-Profil',version:'1.21.1',loader:'fabric',memoryMb:4096,javaPath:java??'java' });
 const [error,setError] = useState(''); const [pending,setPending] = useState(false);
 async function submit(e:FormEvent) { e.preventDefault(); setPending(true); try { await save(profileSchema.parse(draft)); close(); } catch(e) {setError(String(e));} finally {setPending(false);} }
 return <Modal title={profile?'Profil bearbeiten':'Neues Profil'} close={close}><form onSubmit={submit}>
  <label>Profilname<input autoFocus required maxLength={48} value={draft.name} onChange={e=>setDraft({...draft,name:e.target.value})}/></label>
  <div className="form-columns"><label>Minecraft-Version<select value={draft.version} onChange={()=>{}}><option>1.21.1</option></select></label><label>Loader<select value={draft.loader} disabled={Boolean(profile)} onChange={e=>setDraft({...draft,loader:e.target.value as Profile['loader']})}><option value="vanilla">Vanilla</option><option value="fabric">Fabric</option><option value="forge">Forge</option></select></label></div>
  <label>Arbeitsspeicher <span>{(draft.memoryMb/1024).toFixed(1)} GB</span><input type="range" min="2048" max="16384" step="512" value={draft.memoryMb} onChange={e=>setDraft({...draft,memoryMb:Number(e.target.value)})}/></label>
  <label>Java 21 · ausführbare Datei<input required value={draft.javaPath} onChange={e=>setDraft({...draft,javaPath:e.target.value})}/></label>
  <p className="hint">Jedes Profil hat eigene Welten, Mods und Einstellungen. Wähle Fabric oder Forge passend zu deinen Mods.</p>
  {error&&<p role="alert" className="error">{error}</p>}<button disabled={pending} className="primary wide">{pending?'Speichern …':'Profil speichern'}</button>
 </form></Modal>;
}
