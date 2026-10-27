import type { ReactNode } from 'react';
import { ArrowRight, Box, Plus, Puzzle, ShieldCheck } from 'lucide-react';
import type { Account, Profile, Transfer } from '../../../../packages/shared-types/src';
import { includesHera, HERA_MOD_VERSION } from '../lib/hera-profile';
import type { Page } from '../lib/store';

export function HomePage({profiles, active, account, cards, create, navigate, launch, busy, running, transfer}: {
 profiles: Profile[]; active?: Profile; account?: Account; cards: ReactNode;
 create:()=>void; navigate:(page:Page)=>void; launch:()=>void; busy:boolean; running:boolean;transfer:Transfer|null;
}) {
 return <>
  <div className="page-heading"><div><span className="eyebrow">DEINE WELTEN / DEIN ZUHAUSE</span><h1>{account?`Schön, dass du da bist, ${account.name}.`:'Dein Abenteuer beginnt hier.'}</h1><p>Alles für deinen nächsten Minecraft-Moment. An einem Ort.</p></div><button className="secondary" onClick={create}><Plus size={18}/>Neues Profil</button></div>
  <section className="launch-journey" aria-label="Dein Weg ins Spiel">
   <button className={account?'complete':''} onClick={()=>navigate('Account')}><span>{account?<CheckIcon/>:'1'}</span><div><strong>{account?'Konto verbunden':'Microsoft verbinden'}</strong><small>{account?.name??'Mit deinem Minecraft-Konto anmelden'}</small></div></button>
   <button className={active?'complete':''} onClick={()=>active?navigate('Profile'):create()}><span>{active?<CheckIcon/>:'2'}</span><div><strong>{active?'Profil ausgewählt':'Hera-Profil erstellen'}</strong><small>{active?`${active.name} · ${active.version}`:'Fabric, Hera und Fabric API zusammen'}</small></div></button>
   <button disabled={!account||!active||busy||running} onClick={launch}><span>{running?<CheckIcon/>:'3'}</span><div><strong>{running?'Minecraft läuft':busy?'Wird vorbereitet':'Minecraft starten'}</strong><small>Dateien werden automatisch eingerichtet</small></div></button>
  </section>
  <section className="hero">
   <img className="hero-world" src="/images/minecraft-world.png" alt="" aria-hidden="true" fetchPriority="high"/>
   <div className="hero-content"><span className="hero-label"><span/> DEIN SPIEL. DEIN HERA.</span><h2>Ein Klick.<br/>Deine <em>Welt.</em></h2><p>{includesHera(active)?`Hera ${HERA_MOD_VERSION}, Fabric und Fabric API werden beim Start automatisch eingerichtet. Deine Module bleiben zunächst ausgeschaltet.`:'Wähle dein Profil und starte Minecraft. Für die integrierte Hera-Mod erstellst du ein Hera-Profil mit Fabric 1.21.11.'}</p><div className="hero-actions"><button className="primary" disabled={busy||running} onClick={launch}>{busy?'Wird vorbereitet …':!account?'Mit Microsoft anmelden':!active?'Hera-Profil erstellen':running?'Minecraft läuft':'Minecraft starten'}<ArrowRight size={18}/></button><button className="hero-secondary" onClick={()=>includesHera(active)?navigate('Mods'):create()}>{includesHera(active)?'Weitere Mods':'Hera-Profil erstellen'}<ArrowRight size={16}/></button></div>{busy&&transfer?<div className="hero-progress" role="status"><span>{transfer.stage}</span><progress max={Math.max(transfer.total,1)} value={transfer.completed}/></div>:<span className="hero-footnote">{includesHera(active)?`HERA ${HERA_MOD_VERSION} INKLUSIVE · MINECRAFT 1.21.11`:'MINECRAFT JAVA EDITION · DEINE PROFILE BLEIBEN GETRENNT'}</span>}</div>
   {active&&<div className="hero-profile"><Box size={18}/><div><strong>{active.name}</strong><span>{active.loader} · {active.version}</span></div></div>}
  </section>
  <div className="section-heading"><div><span className="eyebrow">DEIN SPIEL. DEIN SETUP.</span><h2>Deine Profile <span>{profiles.length}</span></h2></div><button className="text-button" onClick={()=>navigate('Profile')}>Alle Profile<ArrowRight size={16}/></button></div>
  {profiles.length?cards:<button className="create-profile" onClick={create}><span className="add-icon"><Plus size={24}/></span><span><strong>Eine neue Welt wartet auf dich.</strong><small>Erstelle ein Profil. Hera hält deine Welten, Mods und Einstellungen zusammen.</small></span><ArrowRight size={20}/></button>}
  <div className="home-shortcuts"><button onClick={()=>navigate('Mods')}><span className="shortcut-icon"><Puzzle size={22}/></span><div><strong>Mach Minecraft zu deinem.</strong><span>Entdecke Mods, die zu deinem Profil passen.</span></div><ArrowRight size={18}/></button><button onClick={()=>navigate('Downloads')}><span className="shortcut-icon blue"><ShieldCheck size={22}/></span><div><strong>Gut vorbereitet ins Abenteuer.</strong><span>Spieldateien prüfen und Hera aktuell halten.</span></div><ArrowRight size={18}/></button></div>
 </>;
}
function CheckIcon(){return <ShieldCheck size={17} aria-label="Abgeschlossen"/>;}
