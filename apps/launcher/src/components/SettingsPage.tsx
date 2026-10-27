import { useState } from 'react';
import { ArrowRight, Monitor, SlidersHorizontal } from 'lucide-react';
import type { Settings } from '../../../../packages/shared-types/src';
import { AccountSetup } from './AccountSetup';
import { PageHeader, Toggle } from './ui';
export function SettingsPage({settings,save,profiles,updates}:{settings:Settings;save:(s:Settings)=>Promise<void>;profiles:()=>void;updates:()=>void}){
 const [pending,setPending]=useState(false),[error,setError]=useState(''),[scale,setScale]=useState<number|null>(null);
 async function commit(s:Settings){setPending(true);setError('');try{await save(s);setScale(null);}catch(e){setError(String(e));}finally{setPending(false);}}
 return <><PageHeader title="Einstellungen" description="Oberfläche und Launcher verwalten."/>
 <section className="settings-section"><div className="settings-heading"><Monitor size={19}/><div><h2>Darstellung</h2><p>Wird auf diesem Gerät gespeichert.</p></div></div><div className="panel settings-panel">
  <div className="setting-row"><div><strong>Animationen reduzieren</strong><p>Kurze Übergänge und Bewegungen ausschalten.</p></div><div aria-disabled={pending}><Toggle disabled={pending} label="Animationen reduzieren" checked={settings.reducedMotion} onChange={v=>{if(!pending)void commit({...settings,reducedMotion:v});}}/></div></div>
  <div className="setting-row"><div><strong>Akzentfarbe</strong><p>Für Auswahl, Fortschritt und Hauptaktionen.</p></div><select aria-label="Akzentfarbe" disabled={pending} value={settings.accent==='violet'?'orange':settings.accent} onChange={e=>void commit({...settings,accent:e.target.value as Settings['accent']})}><option value="orange">Hera Orange</option><option value="cyan">Cyan</option></select></div>
  <div className="setting-row"><div><strong>Interface-Skalierung</strong><p>{Math.round((scale??settings.scale)*100)} %</p></div><input aria-label="Interface-Skalierung" type="range" disabled={pending} min="0.85" max="1.2" step="0.05" value={scale??settings.scale} onChange={e=>setScale(Number(e.target.value))} onPointerUp={e=>void commit({...settings,scale:Number(e.currentTarget.value)})} onKeyUp={e=>{if(['ArrowLeft','ArrowRight','Home','End'].includes(e.key))void commit({...settings,scale:Number(e.currentTarget.value)});}}/></div>
 </div></section>
 <section className="settings-section"><div className="settings-heading"><SlidersHorizontal size={19}/><div><h2>Spiel & Launcher</h2><p>Versionen, Java und Speicher bleiben pro Profil getrennt.</p></div></div><div className="panel settings-links"><button onClick={profiles}><div><strong>Java & Arbeitsspeicher</strong><small>In den Einstellungen deines Spielprofils ändern</small></div><ArrowRight size={17}/></button><button onClick={updates}><div><strong>Launcher-Updates</strong><small>Neue Versionen prüfen und herunterladen</small></div><ArrowRight size={17}/></button></div></section>
 <details className="panel advanced-settings"><summary>Erweiterte Kontoeinstellungen</summary><AccountSetup/></details>
 {pending&&<p role="status" className="hint">Einstellungen werden gespeichert …</p>}{error&&<p role="alert" className="error">{error}</p>}
 </>;
}
