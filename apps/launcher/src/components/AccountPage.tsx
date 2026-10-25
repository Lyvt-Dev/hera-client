import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { LogIn, ShieldCheck } from 'lucide-react';
import { api, native } from '../lib/bridge';
import type { Account } from '../../../../packages/shared-types/src';
export function AccountPage({accounts,selected,select,connected}:{accounts:Account[];selected?:string;select:(id:string)=>void;connected:()=>void}) {
 const qc=useQueryClient();const [busy,setBusy]=useState(false),[error,setError]=useState(''),[info,setInfo]=useState('');
 const run=useRef(0);
 useEffect(()=>()=>{run.current++;if(native)void api.cancelLogin().catch(()=>{});},[]);
 async function signIn(){const attempt=++run.current;setBusy(true);setError('');setInfo('Anmeldung wird vorbereitet …');try{
   await api.signIn();if(attempt!==run.current)return;setInfo('Melde dich im Microsoft-Fenster an. Dein Konto wird automatisch verbunden.');
   const account=await api.completeLogin();if(attempt!==run.current)return;select(account.id);setError('');setInfo(`Willkommen, ${account.name}. Dein Konto ist verbunden.`);await qc.invalidateQueries({queryKey:['accounts']});connected();
 }catch(e){if(attempt===run.current){setError(String(e));setInfo('');}}finally{if(attempt===run.current)setBusy(false)}}
 async function cancel(){run.current++;await api.cancelLogin();setBusy(false);setInfo('Anmeldung abgebrochen.');setError('')}
 return <><div className="page-heading"><div><h1>Dein Account<span>.</span></h1><p>Verbinde dein Microsoft-Konto und starte Minecraft Java.</p></div></div><section className="panel"><div className="panel-title"><ShieldCheck size={24}/><h3>Mit Microsoft verbinden</h3></div><p>Die Anmeldung erfolgt direkt im Microsoft-Fenster. Hera speichert dein Passwort nicht.</p>{accounts.map(a=><div className="setting-row" key={a.id}><label><input type="radio" name="account" checked={selected===a.id} onChange={()=>select(a.id)}/>{a.name}</label><button className="secondary" disabled={busy} onClick={()=>void api.logout(a.id).then(()=>qc.invalidateQueries({queryKey:['accounts']})).catch(e=>setError(String(e)))}>Abmelden</button></div>)}<button className="primary" disabled={!native||busy} onClick={()=>void signIn()}><LogIn size={17}/>{busy?'Anmeldung läuft …':accounts.length?'Weiteres Konto hinzufügen':'Mit Microsoft anmelden'}</button>{!native&&<p className="hint">Öffne die installierte Hera-App, um dein Konto zu verbinden.</p>}{info&&<p role="status">{info}</p>}{error&&<p className="error" role="alert">{error}</p>}{busy&&<button className="secondary" style={{marginLeft:12}} onClick={()=>void cancel().catch(e=>setError(String(e)))}>Anmeldung abbrechen</button>}</section></>;
}
