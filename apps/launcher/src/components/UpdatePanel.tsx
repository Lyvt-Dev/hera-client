import { useEffect, useState } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { listen } from '@tauri-apps/api/event';
import { native } from '../lib/bridge';
type Status={configured:boolean;current:string;version:string|null;notes:string|null};
type Progress={downloaded:number;total:number|null;stage:string};
export function UpdatePanel(){
 const [status,setStatus]=useState<Status|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[progress,setProgress]=useState<Progress|null>(null);
 useEffect(()=>{if(!native)return;let disposed=false;let off:(()=>void)|undefined;void listen<Progress>('update-progress',e=>{if(!disposed)setProgress(e.payload)}).then(fn=>{if(disposed)fn();else off=fn});return()=>{disposed=true;off?.()};},[]);
 async function check(){setBusy(true);setError('');setProgress(null);try{setStatus(await invoke<Status>('check_update'));}catch(e){setError(String(e));setStatus(null);}finally{setBusy(false)}}
 async function install(){setBusy(true);setError('');setProgress({downloaded:0,total:null,stage:'Download vorbereiten'});try{await invoke('install_update');}catch(e){setError(String(e));setProgress(null);setStatus(null);}finally{setBusy(false)}}
 return <section className="panel"><span className="eyebrow">HERA UPDATER</span><h3>Launcher aktualisieren</h3><p>Halte Hera aktuell. Das Update wird sicher heruntergeladen und Hera anschließend neu gestartet.</p>{!native?<p className="hint">Updates werden in der installierten Desktop-App ausgeführt.</p>:<><button className="secondary" disabled={busy} onClick={()=>void check()}>{busy?'Vorgang läuft …':'Nach Updates suchen'}</button>{status&&<p role="status">{!status.configured?'Die Veröffentlichungsadresse ist noch nicht eingerichtet.':status.version?`Hera ${status.version} ist verfügbar (installiert: ${status.current}).`:`Hera ${status.current} ist aktuell.`}</p>}{status?.notes&&<p style={{whiteSpace:'pre-wrap'}}>{status.notes}</p>}{status?.version&&<button className="primary" disabled={busy} onClick={()=>void install()}>Update herunterladen & installieren</button>}{progress&&<div role="status"><p>{progress.stage}</p>{progress.total?<progress max={progress.total} value={progress.downloaded}/>:<progress/>}{progress.downloaded>0&&<small>{(progress.downloaded/1048576).toFixed(1)} MB{progress.total?` / ${(progress.total/1048576).toFixed(1)} MB`:''}</small>}</div>}</>}{error&&<p className="error" role="alert">{error}</p>}</section>;
}
