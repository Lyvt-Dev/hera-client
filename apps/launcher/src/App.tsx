import { useEffect, useRef, useState, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDownToLine, ArrowRight, Box, Check, ChevronRight, Download, Home, Layers, PanelLeftClose, PanelLeftOpen, Plus, Search, Settings2, Shirt, Trash2, UserRound, X } from 'lucide-react';
import { api, native } from './lib/bridge';
import { useUi, type Page } from './lib/store';
import { defaultSettings, profileSchema, type Profile, type Settings, type Transfer } from '../../../packages/shared-types/src';
import { ProfileEditor } from './components/ProfileEditor';
import { ProfileList } from './components/ProfileList';
import { ModsPage } from './components/ModsPage';
import { UpdatePanel } from './components/UpdatePanel';
import { AccountPage } from './components/AccountPage';
import { Empty, Modal, PageHeader, ProgressBar } from './components/ui';
import { VersionsPage } from './components/VersionsPage';
import { CapePage } from './components/CapePage';
import { HomePage } from './components/HomePage';
import { SettingsPage } from './components/SettingsPage';
import { phaseLabel, transferPhase, type Activity, type LaunchPhase } from './lib/launch-state';
import appPackage from '../package.json';

const navigation:{page:Page;label:string;icon:typeof Home}[]=[{page:'Übersicht',label:'Spielen',icon:Home},{page:'Profile',label:'Profile',icon:Layers},{page:'Mods',label:'Mods',icon:Box},{page:'Capes',label:'Capes',icon:Shirt},{page:'Versionen',label:'Versionen',icon:Layers},{page:'Downloads',label:'Downloads',icon:ArrowDownToLine}];
export function App(){
 const {page,navigate,selected,select,compact,toggleCompact}=useUi(),qc=useQueryClient();
 const profiles=useQuery({queryKey:['profiles'],queryFn:api.profiles});
 const settingsQuery=useQuery({queryKey:['settings'],queryFn:api.settings});
 const accounts=useQuery({queryKey:['accounts'],queryFn:api.accounts});
 const game=useQuery({queryKey:['game-running'],queryFn:api.gameRunning,refetchOnWindowFocus:true});
 const settings=settingsQuery.data??defaultSettings,all=profiles.data??[];
 const active=all.find(p=>p.id===selected)??all[0];
 const [accountId,setAccountId]=useState(''),[editor,setEditor]=useState<Profile|'new'|null>(null),[initialVersion,setInitialVersion]=useState<string>();
 const [notice,setNotice]=useState<{text:string;error:boolean}|null>(null),[busy,setBusy]=useState(false),[phase,setPhase]=useState<LaunchPhase>('idle');
 const [transfer,setTransfer]=useState<Transfer|null>(null),[activity,setActivity]=useState<Activity[]>([]),[verified,setVerified]=useState<Set<string>>(new Set());
 const [deleteTarget,setDeleteTarget]=useState<Profile|null>(null),[deleting,setDeleting]=useState(false),[deleteError,setDeleteError]=useState('');
 const [profileLimit,setProfileLimit]=useState(100);
 const [profileSearch,setProfileSearch]=useState(''),[profileFilter,setProfileFilter]=useState('all'),[profileSort,setProfileSort]=useState('default');
 const account=accounts.data?.find(a=>a.id===accountId)??accounts.data?.[0],running=game.data??false;
 const importRef=useRef<HTMLInputElement>(null),exitEpoch=useRef(0),busyRef=useRef(false),latest=useRef<Transfer|null>(null);
 const retryProfile=all.find(p=>p.id===activity[0]?.profileId);
 const currentPage=navigation.find(n=>n.page===page)?.label??page;
 useEffect(()=>{document.documentElement.dataset.accent=settings.accent;document.documentElement.dataset.motion=settings.reducedMotion?'reduced':'normal';document.documentElement.style.fontSize=`${settings.scale*16}px`;},[settings.accent,settings.scale,settings.reducedMotion]);
 useEffect(()=>{document.documentElement.dataset.running=String(running);},[running]);
 useEffect(()=>{
  if(!native)return;let disposed=false;let timer:ReturnType<typeof setTimeout>|undefined;const cleanups:(()=>void)[]=[];
  const flush=()=>{timer=undefined;if(!disposed&&latest.current){setTransfer(latest.current);if(busyRef.current)setPhase(transferPhase(latest.current.stage));}};
  const visibility=()=>{if(!document.hidden)flush();};document.addEventListener('visibilitychange',visibility);
  void api.onTransfer(t=>{latest.current=t;if(!document.hidden&&!timer)timer=setTimeout(flush,100);}).then(fn=>{if(disposed)fn();else cleanups.push(fn);});
  void api.onExit(message=>{exitEpoch.current++;qc.setQueryData(['game-running'],false);setPhase(message.includes('mit Status')?'failed':'idle');setNotice({text:message,error:message.includes('mit Status')});}).then(fn=>{if(disposed)fn();else cleanups.push(fn);});
  return()=>{disposed=true;clearTimeout(timer);cleanups.forEach(fn=>fn());document.removeEventListener('visibilitychange',visibility);};
 },[qc]);
 function inform(text:string,error=false){setNotice({text,error});}
 async function basic(fn:()=>Promise<unknown>,success?:string){try{await fn();if(success)inform(success);}catch(e){inform(String(e),true);}}
 async function saveProfile(p:Profile){await api.saveProfile(p);await qc.invalidateQueries({queryKey:['profiles']});select(p.id);}
 async function saveSettings(next:Settings){await api.saveSettings(next);qc.setQueryData(['settings'],next);}
 async function run(kind:'install'|'launch',p:Profile){
  if(busyRef.current||running)return;if(!native){inform('Starte Minecraft über die installierte Hera-App.');return;}if(kind==='launch'&&!account){navigate('Account');return;}
  busyRef.current=true;setBusy(true);setPhase('preparing');setTransfer(null);latest.current=null;setNotice(null);
  const startEpoch=exitEpoch.current;
  const item:Activity={id:crypto.randomUUID(),kind,name:p.name,profileId:p.id,started:Date.now(),result:'active'};setActivity(a=>[item,...a].slice(0,20));
  try{if(kind==='launch')await api.launch(p.id,account!.id);else await api.install(p.id);
   setVerified(v=>new Set(v).add(p.id));const isRunning=kind==='launch'?await api.gameRunning():false;
   if(kind==='launch')qc.setQueryData(['game-running'],isRunning);if(kind==='install'||exitEpoch.current===startEpoch)setPhase(isRunning?'running':'idle');
   setActivity(a=>a.map(v=>v.id===item.id?{...v,result:'complete',finished:Date.now()}:v));if(kind==='install')inform(`Dateien für ${p.name} geprüft.`);
  }catch(e){setPhase('failed');inform(String(e),true);setActivity(a=>a.map(v=>v.id===item.id?{...v,result:'failed',finished:Date.now(),error:String(e)}:v));}
  finally{busyRef.current=false;setBusy(false);}
 }
 function launch(){if(!account){navigate('Account');return;}if(!active){setEditor('new');return;}void run('launch',active);}
 function exportProfile(p:Profile){const blob=new Blob([JSON.stringify({schemaVersion:1,profile:p},null,2)],{type:'application/json'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='hera-profile.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
 async function importProfile(file:File){if(file.size>16384)throw Error('Profildatei ist zu groß.');const data:unknown=JSON.parse(await file.text());if(typeof data!=='object'||data===null||!('schemaVersion'in data)||data.schemaVersion!==1||!('profile'in data))throw Error('Unbekanntes Profilformat.');const p=profileSchema.parse(data.profile);await saveProfile({...p,id:crypto.randomUUID(),name:p.name.slice(0,40)+' Import',javaPath:'java'});}
 async function removeProfile(){if(!deleteTarget)return;setDeleting(true);setDeleteError('');try{await api.deleteProfile(deleteTarget.id);if(active?.id===deleteTarget.id)select(all.find(p=>p.id!==deleteTarget.id)?.id??null);await qc.invalidateQueries({queryKey:['profiles']});inform('Profil entfernt. Die Spieldateien wurden behalten.');setDeleteTarget(null);}catch(e){setDeleteError(String(e));}finally{setDeleting(false);}}
 const rows=(items:Profile[])=><ProfileList profiles={items} selected={active?.id} select={select} edit={setEditor} duplicate={p=>void basic(()=>saveProfile({...p,id:crypto.randomUUID(),name:p.name.slice(0,40)+' Kopie'}),'Profil dupliziert.')} exportProfile={exportProfile} remove={p=>{setDeleteTarget(p);setDeleteError('');}} busy={busy||running}/>;
 const filtered=useMemo(()=>all.filter(p=>(profileFilter==='all'||p.loader===profileFilter)&&`${p.name} ${p.version} ${p.loader}`.toLowerCase().includes(profileSearch.toLowerCase())).sort((a,b)=>profileSort==='name'?a.name.localeCompare(b.name,'de'):0),[all,profileFilter,profileSearch,profileSort]);
 return <div className={`app-shell ${compact?'compact':''}`}><a className="skip-link" href="#main-content">Zum Inhalt</a>
 <aside className="sidebar"><div className="sidebar-brand"><button className="brand" onClick={()=>navigate('Übersicht')} aria-label="Hera Startseite"><img src="/images/hera-avatar.png" alt=""/><span>hera</span></button><button className="icon-button sidebar-collapse" aria-label={compact?'Navigation erweitern':'Navigation einklappen'} title={compact?'Navigation erweitern':'Navigation einklappen'} onClick={toggleCompact}>{compact?<PanelLeftOpen size={16}/>:<PanelLeftClose size={16}/>}</button></div>
 <div className="sidebar-section-label">BIBLIOTHEK</div><nav aria-label="Hauptnavigation">{navigation.map(({page:p,label,icon:Icon})=><button key={p} onClick={()=>navigate(p)} aria-label={label} title={label} aria-current={page===p?'page':undefined} className={page===p?'active':''}><Icon size={18}/><span className="nav-text">{label}</span>{p==='Downloads'&&busy&&<span className="nav-dot"/>}</button>)}</nav>
 <div className="sidebar-bottom"><button className={`nav-extra ${page==='Einstellungen'?'active':''}`} aria-label="Einstellungen" title="Einstellungen" onClick={()=>navigate('Einstellungen')}><Settings2 size={18}/><span className="nav-text">Einstellungen</span></button><button className={`account-button ${page==='Account'?'active':''}`} aria-label={account?`Account ${account.name}`:'Account verbinden'} title={account?.name??'Account verbinden'} onClick={()=>navigate('Account')}><span className="avatar"><UserRound size={18}/></span><span className="account-copy"><strong>{account?.name??'Konto verbinden'}</strong><small>{account?'Minecraft-Konto':'Microsoft-Anmeldung'}</small></span><ChevronRight className="account-chevron" size={14}/></button><div className="build-info"><span>Hera Client</span><span>{appPackage.version}</span></div></div></aside>
 <main><header className="app-topbar"><div className="breadcrumb"><span>Hera</span><ChevronRight size={12}/><strong>{currentPage}</strong></div><div className="topbar-context">{running?<><span className="status-dot"/>Minecraft läuft</>:active?<><Box size={14}/><span>{active.name}</span><span className="topbar-version">{active.version}</span></>:<span>Kein Profil ausgewählt</span>}</div></header>
 <div className="content" id="main-content" tabIndex={-1}>{notice&&<div className={`notice ${notice.error?'notice-error':''}`} role={notice.error?'alert':'status'}><span>{notice.text}</span><button className="icon-button" aria-label="Meldung schließen" onClick={()=>setNotice(null)}><X size={16}/></button></div>}
 {(profiles.error||settingsQuery.error||accounts.error)&&<div className="notice notice-error" role="alert"><span>Lokale Daten konnten nicht geladen werden: {String(profiles.error??settingsQuery.error??accounts.error)}</span><button className="secondary" onClick={()=>void Promise.all([profiles.refetch(),settingsQuery.refetch(),accounts.refetch()])}>Erneut laden</button></div>}
 <div className="page-view" key={page}>
 {page==='Übersicht'&&<HomePage profiles={all} active={active} account={account} cards={rows(all.slice(0,4))} create={()=>setEditor('new')} manage={()=>navigate('Profile')} mods={()=>navigate('Mods')} edit={()=>active&&setEditor(active)} launch={launch} stop={()=>void basic(api.stop)} busy={busy} running={running} transfer={transfer} phase={phase} verified={Boolean(active&&verified.has(active.id))} loading={profiles.isPending||accounts.isPending}/>}
 {page==='Profile'&&<><PageHeader title="Profile" description="Eigene Spielumgebungen für Welten, Mods und Einstellungen." actions={<><button className="secondary" onClick={()=>importRef.current?.click()}><Download size={16}/>Importieren</button><button className="primary" onClick={()=>setEditor('new')}><Plus size={16}/>Profil erstellen</button></>}/><input ref={importRef} type="file" hidden accept="application/json,.json" onChange={e=>{const file=e.target.files?.[0];if(file)void basic(()=>importProfile(file),'Profil importiert.');e.target.value='';}}/>
 <div className="library-toolbar"><label className="search-field"><Search size={16}/><input aria-label="Profile suchen" placeholder="Profile suchen" value={profileSearch} onChange={e=>{setProfileSearch(e.target.value);setProfileLimit(100);}}/></label><select aria-label="Profile nach Loader filtern" value={profileFilter} onChange={e=>{setProfileFilter(e.target.value);setProfileLimit(100);}}><option value="all">Alle Loader</option><option value="fabric">Fabric</option><option value="forge">Forge</option><option value="vanilla">Vanilla</option></select><select aria-label="Profile sortieren" value={profileSort} onChange={e=>{setProfileSort(e.target.value);setProfileLimit(100);}}><option value="default">Erstellreihenfolge</option><option value="name">Name A–Z</option></select></div>
 {profiles.isPending?<p role="status" className="loading-state">Profile laden …</p>:filtered.length?rows(filtered.slice(0,profileLimit)):<Empty title={all.length?'Keine passenden Profile':'Noch keine Profile'}>{all.length?'Suche oder Filter ändern.':'Erstelle dein erstes Spielprofil über „Profil erstellen“.'}</Empty>}
 {filtered.length>profileLimit&&<button className="secondary wide" onClick={()=>setProfileLimit(v=>v+100)}>Weitere Profile anzeigen ({filtered.length-profileLimit})</button>}
 {active&&<section className="profile-launch-bar"><div><strong>{active.name}</strong><small>{active.version} · {active.loader} · {active.memoryMb/1024} GB</small></div><button className="secondary" disabled={!native||busy||running} onClick={()=>void run('install',active)}>Prüfen & reparieren</button><button className="primary" disabled={!native||busy||running||!account} onClick={launch}>Starten<ArrowRight size={15}/></button></section>}</>}
 {page==='Mods'&&<ModsPage profiles={all} active={active} select={select} create={()=>setEditor('new')}/>}
 {page==='Capes'&&<CapePage profiles={all} active={active} account={account} select={select}/>}
 {page==='Versionen'&&<VersionsPage create={version=>{setInitialVersion(version);setEditor('new');}}/>}
 {page==='Account'&&<AccountPage accounts={accounts.data??[]} selected={account?.id} select={setAccountId} connected={()=>navigate('Übersicht')}/>}
 {page==='Einstellungen'&&<SettingsPage settings={settings} save={saveSettings} profiles={()=>navigate('Profile')} updates={()=>navigate('Downloads')}/>}
 {page==='Downloads'&&<><PageHeader title="Downloads" description="Spieldateien und Launcher-Updates verwalten."/>
 <section className="panel"><div className="section-heading"><h2>Aktueller Vorgang</h2><span className="tag">{busy?'Aktiv':'Kein aktiver Download'}</span></div>{busy?<ProgressBar label={transfer?.stage??phaseLabel[phase]} total={transfer?.total??0} completed={transfer?.completed??0}/>:<p>{active?`${active.name} · ${active.version} · ${active.loader}`:'Wähle oder erstelle ein Spielprofil.'}</p>}<div className="button-row"><button className="secondary" disabled={!native||!active||busy||running} onClick={()=>active&&void run('install',active)}><Download size={16}/>Spieldateien prüfen</button>{phase==='failed'&&retryProfile&&<button className="secondary" disabled={busy||running} onClick={()=>void run(activity[0]?.kind??'install',retryProfile)}>Erneut versuchen</button>}</div></section>
 {activity.length>0&&<section className="download-history"><div className="section-heading"><h2>Diese Sitzung</h2><small>{activity.length} Vorgänge</small></div>{activity.map(item=><div className="activity-row" key={item.id}><span className={`activity-symbol ${item.result}`}>{item.result==='complete'?<Check size={16}/>:item.result==='failed'?<X size={16}/>:<span className="spinner"/>}</span><div><strong>{item.name}</strong><small>{item.kind==='install'?'Dateien prüfen':'Minecraft-Start'} · {new Date(item.started).toLocaleTimeString('de-DE',{hour:'2-digit',minute:'2-digit'})}{item.error?` · ${item.error}`:''}</small></div><span className="tag">{item.result==='complete'?'Abgeschlossen':item.result==='failed'?'Fehlgeschlagen':'Läuft'}</span></div>)}</section>}
 <UpdatePanel running={running}/></>}
 </div></div></main>
 {editor&&<ProfileEditor {...(editor==='new'?{}:{profile:editor})} initialVersion={initialVersion} java={null} close={()=>{setEditor(null);setInitialVersion(undefined);}} save={saveProfile}/>}
 {deleteTarget&&<Modal title="Profil entfernen?" close={()=>{if(!deleting)setDeleteTarget(null);}}><p>„{deleteTarget.name}“ aus dem Launcher entfernen? Die Spieldateien und Welten bleiben erhalten.</p>{deleteError&&<p className="error" role="alert">{deleteError}</p>}<div className="dialog-actions"><button className="secondary" disabled={deleting} onClick={()=>setDeleteTarget(null)}>Abbrechen</button><button className="danger-button" disabled={deleting} onClick={()=>void removeProfile()}><Trash2 size={16}/>{deleting?'Wird entfernt …':'Profil entfernen'}</button></div></Modal>}
 {busy&&page!=='Übersicht'&&page!=='Downloads'&&<button className="operation-toast" onClick={()=>navigate('Downloads')}><span className="spinner"/><span>{transfer?.stage??phaseLabel[phase]}</span><ArrowRight size={15}/></button>}
 </div>;
}
