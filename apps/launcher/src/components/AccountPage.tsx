import { useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { LogIn, ShieldCheck, UserRound } from 'lucide-react';
import { api, native } from '../lib/bridge';
import { PageHeader } from './ui';
import type { Account } from '../../../../packages/shared-types/src';
export function AccountPage({accounts,selected,select,connected}:{accounts:Account[];selected?:string;select:(id:string)=>void;connected:()=>void}){
 const qc=useQueryClient(),run=useRef(0);const [busy,setBusy]=useState(false),[loggingOut,setLoggingOut]=useState<string|null>(null),[error,setError]=useState(''),[info,setInfo]=useState('');
 useEffect(()=>()=>{run.current++;if(native)void api.cancelLogin().catch(()=>{});},[]);
 async function signIn(){const attempt=++run.current;setBusy(true);setError('');setInfo('Microsoft-Anmeldung vorbereiten …');try{
  await api.signIn();if(attempt!==run.current)return;setInfo('Melde dich im Microsoft-Fenster an. Hera übernimmt das Konto anschließend automatisch.');
  const account=await api.completeLogin();if(attempt!==run.current)return;select(account.id);setError('');await qc.invalidateQueries({queryKey:['accounts']});connected();
 }catch(e){if(attempt===run.current){setError(String(e));setInfo('');}}finally{if(attempt===run.current)setBusy(false);}}
 async function logout(id:string){setLoggingOut(id);setError('');try{await api.logout(id);await qc.invalidateQueries({queryKey:['accounts']});setInfo('Konto auf diesem Gerät abgemeldet.');}catch(e){setError(String(e));}finally{setLoggingOut(null);}}
 async function cancel(){run.current++;await api.cancelLogin();setBusy(false);setInfo('Anmeldung abgebrochen.');setError('');}
 return <><PageHeader title="Accounts" description="Minecraft-Konten für diesen Launcher verwalten."/>
 {accounts.length>0&&<section className="panel"><div className="section-heading"><h2>Deine Minecraft-Konten</h2><span className="count">{accounts.length}</span></div><div className="account-list">{accounts.map(a=><div className="setting-row" key={a.id}><label><input type="radio" name="account" checked={selected===a.id} onChange={()=>select(a.id)}/><span className="avatar"><UserRound size={18}/></span><span><strong>{a.name}</strong><small className="account-status">{selected===a.id?'Für den nächsten Start ausgewählt':'Auf diesem Gerät gespeichert'}</small></span></label><button className="secondary" disabled={busy||Boolean(loggingOut)} onClick={()=>void logout(a.id)}>{loggingOut===a.id?'Wird abgemeldet …':'Abmelden'}</button></div>)}</div></section>}
 <section className="panel"><div className="account-overview"><span className="avatar"><ShieldCheck size={23}/></span><div><h2>{accounts.length?'Weiteres Konto verbinden':'Mit Microsoft anmelden'}</h2><p>Die Anmeldung öffnet sich in einem eigenen Microsoft-Fenster.</p></div></div><div className="button-row"><button className="primary" disabled={!native||busy||Boolean(loggingOut)} onClick={()=>void signIn()}><LogIn size={16}/>{busy?'Anmeldung läuft …':'Microsoft-Konto verbinden'}</button>{busy&&<button className="secondary" onClick={()=>void cancel().catch(e=>setError(String(e)))}>Abbrechen</button>}</div>{!native&&<p className="hint">Die Anmeldung ist in der installierten Hera-App verfügbar.</p>}{info&&<p className="hint" role="status">{info}</p>}{error&&<p className="error" role="alert">{error}</p>}</section></>;
}
