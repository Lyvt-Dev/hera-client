import type { ReactNode } from 'react';
import { ArrowRight, Check, Layers, Play, Plus, Settings2, Square, UserRound } from 'lucide-react';
import type { Account, Profile, Transfer } from '../../../../packages/shared-types/src';
import { includesHera, HERA_MOD_VERSION } from '../lib/hera-profile';
import { phaseLabel, type LaunchPhase } from '../lib/launch-state';
import { PageHeader, ProgressBar } from './ui';

export function HomePage({profiles,active,account,cards,create,manage,mods,edit,launch,stop,busy,running,transfer,phase,verified,loading}:{profiles:Profile[];active?:Profile;account?:Account;cards:ReactNode;create:()=>void;manage:()=>void;mods:()=>void;edit:()=>void;launch:()=>void;stop:()=>void;busy:boolean;running:boolean;transfer:Transfer|null;phase:LaunchPhase;verified:boolean;loading:boolean}){
 const state=running?'running':phase;
 return <><PageHeader title={account?`Bereit, ${account.name}?`:'Dein nächstes Abenteuer.'} description="Profil auswählen, starten und in deine Welt eintauchen." actions={<button className="secondary" onClick={create}><Plus size={16}/>Neues Profil</button>}/>
 <section className={`play-workspace ${state}`} aria-label="Minecraft starten">
  <div className="play-art"><img src="/images/minecraft-world.png" alt="" fetchPriority="high"/><div className="play-art-copy"><span className="overline">MINECRAFT JAVA EDITION</span><h2>{active?.name??'Dein erstes Profil'}</h2><div className="play-tags">{active?<><span>{active.version}</span><span>{active.loader==='vanilla'?'Vanilla':active.loader==='fabric'?'Fabric':'Forge'}</span>{includesHera(active)&&<span>Hera {HERA_MOD_VERSION}</span>}</>:<span>Welten, Mods und Einstellungen getrennt verwalten</span>}</div></div></div>
  <div className="play-controls"><div className="play-details"><div><span className="detail-caption">MINECRAFT-KONTO</span><strong><UserRound size={14}/>{account?.name??'Nicht verbunden'}</strong></div><div><span className="detail-caption">ARBEITSSPEICHER</span><strong>{active?`${active.memoryMb/1024} GB`:'—'}</strong></div><div className="play-readiness"><span className="detail-caption">STATUS</span><strong className={state==='failed'?'danger':''}>{verified&&state==='idle'?<><Check size={14}/>Dateien in dieser Sitzung geprüft</>:phaseLabel[state]}</strong></div></div>
  <div className="play-action">{running?<button className="secondary" disabled={busy} onClick={stop}><Square size={16}/>Spiel beenden</button>:<button className="primary play-button" disabled={busy||loading} onClick={launch}>{busy||loading?<span className="spinner"/>:<Play size={17} fill="currentColor"/>}{loading?'Launcher laden …':busy?phaseLabel[state]:!account?'Konto verbinden':!active?'Profil erstellen':state==='failed'?'Erneut starten':'Minecraft starten'}</button>}<button className="icon-button" aria-label="Ausgewähltes Profil bearbeiten" title="Profil bearbeiten" disabled={!active||busy} onClick={edit}><Settings2 size={17}/></button></div></div>
  {busy&&<div className="play-progress"><ProgressBar label={transfer?.stage??phaseLabel[state]} completed={transfer?.completed??0} total={transfer?.total??0}/></div>}
  <div className="play-bottom"><span>{includesHera(active)?'Hera-Mod, Fabric API und passende Java-Laufzeit werden beim Start geprüft.':'Spieldateien und passende Java-Laufzeit werden beim Start geprüft.'}</span>{active&&<button className="text-button" onClick={mods}>Mods verwalten<ArrowRight size={14}/></button>}</div>
 </section>
 <section className="home-library"><div className="section-heading"><h2><Layers size={17}/>Profile <span className="count">{profiles.length}</span></h2><button className="text-button" onClick={manage}>Alle verwalten<ArrowRight size={14}/></button></div>{profiles.length?cards:<div className="empty"><h3>Noch kein Spielprofil</h3><p>Erstelle ein Hera-, Fabric-, Forge- oder Vanilla-Profil.</p><button className="secondary" onClick={create}><Plus size={16}/>Profil erstellen</button></div>}</section>
 </>;
}
