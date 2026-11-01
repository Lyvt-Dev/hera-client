import type { ReactNode } from 'react';
import { ArrowRight, Box, Check, Cpu, Layers, Play, Plus, Settings2, Shirt, Square, UserRound } from 'lucide-react';
import type { Account, Profile, Transfer } from '../../../../packages/shared-types/src';
import { includesHera, HERA_MOD_VERSION } from '../lib/hera-profile';
import { phaseLabel, type LaunchPhase } from '../lib/launch-state';
import type { Page } from '../lib/store';
import { ProgressBar } from './ui';

const loaderName=(p:Profile)=>p.loader==='vanilla'?'Vanilla':p.loader==='fabric'?'Fabric':'Forge';

export function HomePage({profiles,active,account,cards,create,manage,mods,edit,launch,stop,go,busy,running,transfer,phase,verified,loading}:{profiles:Profile[];active?:Profile;account?:Account;cards:ReactNode;create:()=>void;manage:()=>void;mods:()=>void;edit:()=>void;launch:()=>void;stop:()=>void;go:(page:Page)=>void;busy:boolean;running:boolean;transfer:Transfer|null;phase:LaunchPhase;verified:boolean;loading:boolean}){
 const state=running?'running':phase;
 const label=loading?'Laden …':busy?phaseLabel[state]:!account?'Konto verbinden':!active?'Profil erstellen':state==='failed'?'Erneut starten':'Spielen';
 const quick:{page:Page;title:string;text:string;icon:typeof Box}[]=[{page:'Mods',title:'Mods',text:'Modrinth & CurseForge durchsuchen',icon:Box},{page:'Capes',title:'Capes',text:'Eigenes Cape gestalten',icon:Shirt},{page:'Versionen',title:'Versionen',text:'Neue Minecraft-Version wählen',icon:Layers},{page:'Einstellungen',title:'Einstellungen',text:'Launcher anpassen',icon:Settings2}];
 return <>
 <section className={`hero ${state}`} aria-label="Minecraft starten">
  <img className="hero-art" src="/images/minecraft-world.png" alt="" fetchPriority="high"/>
  <div className="hero-shade"/>
  <div className="hero-body">
   <div className="hero-copy">
    <span className="hero-kicker"><span className="hero-pulse"/>{running?'Minecraft läuft':account?`Willkommen zurück, ${account.name}`:'Minecraft Java Edition'}</span>
    <h1>{active?.name??'Dein erstes Profil'}</h1>
    <div className="hero-tags">{active?<><span>{active.version}</span><span>{loaderName(active)}</span>{includesHera(active)&&<span className="accent">Hera {HERA_MOD_VERSION}</span>}<span><Cpu size={12}/>{active.memoryMb/1024} GB</span></>:<span>Welten, Mods und Einstellungen getrennt verwalten</span>}</div>
   </div>
   <div className="hero-launch">
    {running?<button className="hero-play stop" disabled={busy} onClick={stop}><Square size={20} fill="currentColor"/>Beenden</button>
    :<button className="hero-play" disabled={busy||loading} onClick={launch}>{busy||loading?<span className="spinner"/>:<Play size={22} fill="currentColor"/>}{label}</button>}
    <button className="hero-icon" aria-label="Ausgewähltes Profil bearbeiten" title="Profil bearbeiten" disabled={!active||busy} onClick={edit}><Settings2 size={18}/></button>
   </div>
  </div>
  {busy&&<div className="hero-progress"><ProgressBar label={transfer?.stage??phaseLabel[state]} completed={transfer?.completed??0} total={transfer?.total??0}/></div>}
 </section>
 <div className="hero-strip">
  <div><span className="detail-caption">KONTO</span><strong><UserRound size={14}/>{account?.name??'Nicht verbunden'}</strong></div>
  <div><span className="detail-caption">STATUS</span><strong className={state==='failed'?'danger':''}>{verified&&state==='idle'?<><Check size={14}/>Dateien geprüft</>:phaseLabel[state]}</strong></div>
  <div className="hero-strip-note">{includesHera(active)?'Hera-Mod, Fabric API und Java werden beim Start geprüft.':'Spieldateien und Java werden beim Start geprüft.'}</div>
  {active&&<button className="text-button" onClick={mods}>Mods verwalten<ArrowRight size={14}/></button>}
 </div>
 <section className="quick-grid" aria-label="Schnellzugriff">{quick.map(({page,title,text,icon:Icon})=><button key={page} className="quick-card" onClick={()=>go(page)}><span className="quick-icon"><Icon size={18}/></span><span><strong>{title}</strong><small>{text}</small></span><ArrowRight className="quick-arrow" size={15}/></button>)}</section>
 <section className="home-library"><div className="section-heading"><h2><Layers size={17}/>Profile <span className="count">{profiles.length}</span></h2><div className="button-row"><button className="text-button" onClick={create}><Plus size={14}/>Neues Profil</button><button className="text-button" onClick={manage}>Alle verwalten<ArrowRight size={14}/></button></div></div>{profiles.length?cards:<div className="empty"><h3>Noch kein Spielprofil</h3><p>Erstelle ein Hera-, Fabric-, Forge- oder Vanilla-Profil.</p><button className="secondary" onClick={create}><Plus size={16}/>Profil erstellen</button></div>}</section>
 </>;
}
