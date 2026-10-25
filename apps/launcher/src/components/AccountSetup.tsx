import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, native } from '../lib/bridge';
export function AccountSetup(){
 const qc=useQueryClient();const saved=useQuery({queryKey:['client-id'],queryFn:api.clientId});const [value,setValue]=useState('');const [message,setMessage]=useState('');const [pending,setPending]=useState(false);
 useEffect(()=>{if(saved.data!==undefined)setValue(saved.data);},[saved.data]);
 async function save(){setPending(true);setMessage('');try{await api.saveClientId(value);await Promise.all([qc.invalidateQueries({queryKey:['runtime']}),qc.invalidateQueries({queryKey:['client-id']})]);setMessage(native?'Client-ID gespeichert. Du kannst dich jetzt anmelden, sofern Microsoft deiner App den Minecraft-/Xbox-Zugriff erlaubt.':'Öffentliche Client-ID im Browser gespeichert. Für die Anmeldung dieselbe ID in der Desktop-App eintragen.');}catch(e){setMessage(String(e));}finally{setPending(false);}}
 return <section className="account-setup"><h3>Microsoft-App-Konfiguration</h3><p>Hera verwendet bereits die hinterlegte Anwendungs-ID. Ändere sie nur, wenn du eine eigene Hera-Installation verwaltest.</p><label>Öffentliche Microsoft-Client-ID<input aria-label="Microsoft-Client-ID" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" value={value} onChange={e=>setValue(e.target.value)} maxLength={36}/></label><button className="secondary" disabled={pending||saved.isPending} onClick={()=>void save()}>{pending?'Speichern …':'Client-ID speichern'}</button>{saved.error&&<p className="error">{String(saved.error)}</p>}{message&&<p role="status" className="hint">{message}</p>}</section>;
}
