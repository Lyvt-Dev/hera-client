import type { ReactNode } from 'react';
import { ArrowRight, Box, Layers, Plus, Puzzle, ShieldCheck } from 'lucide-react';
import type { Account, Profile } from '../../../../packages/shared-types/src';
import type { Page } from '../lib/store';

export function HomePage({profiles, active, account, cards, create, navigate, launch, busy, running}: {
 profiles: Profile[]; active?: Profile; account?: Account; cards: ReactNode;
 create:()=>void; navigate:(page:Page)=>void; launch:()=>void; busy:boolean; running:boolean;
}) {
 return <>
  <div className="page-heading"><div><span className="eyebrow">DEINE WELTEN / DEIN ZUHAUSE</span><h1>{account?`Schön, dass du da bist, ${account.name}.`:'Dein Abenteuer beginnt hier.'}</h1><p>Alles für deinen nächsten Minecraft-Moment. An einem Ort.</p></div><button className="secondary" onClick={create}><Plus size={18}/>Neues Profil</button></div>
  <section className="overview-stats" aria-label="Dein Launcher im Überblick">
   <div><span><Layers size={16}/> DEINE PROFILE</span><strong>{profiles.length}</strong><small>{profiles.length===1?'Ein Platz für deine Welt.':'Jedes Abenteuer hat seinen Platz.'}</small></div>
   <div><span><Box size={16}/> AUSGEWÄHLTES PROFIL</span><strong>{active?.name??'Noch kein Profil'}</strong><small>{active?`${active.loader} · Minecraft ${active.version}`:'Erstelle dein erstes Minecraft-Profil.'}</small></div>
   <div><span><ShieldCheck size={16}/> DEIN ACCOUNT</span><strong>{account?.name??'Noch nicht verbunden'}</strong><small>{account?'Microsoft-Konto verbunden':'Melde dich sicher mit Microsoft an.'}</small></div>
  </section>
  <section className="hero">
   <img className="hero-world" src="/images/minecraft-world.png" alt="" aria-hidden="true" fetchPriority="high"/>
   <div className="hero-content"><span className="hero-label"><span/> EIN KLEINES BISSCHEN HERA.</span><h2>Große Welten.<br/>Dein eigener <em>Platz.</em></h2><p>Deine Lieblingsmods, deine Einstellungen und die nächste Welt, die du dein Zuhause nennst.</p><div className="hero-actions"><button className="primary" disabled={busy||running} onClick={launch}>{!account?'Mit Microsoft anmelden':!active?'Erstes Profil erstellen':running?'Minecraft läuft':'Minecraft starten'}<ArrowRight size={18}/></button><button className="hero-secondary" onClick={()=>navigate('Mods')}>Mods entdecken<ArrowRight size={16}/></button></div><span className="hero-footnote">MINECRAFT JAVA EDITION · AUF DEINE ART.</span></div>
   {active&&<div className="hero-profile"><Box size={18}/><div><strong>{active.name}</strong><span>{active.loader} · {active.version}</span></div></div>}
  </section>
  <div className="section-heading"><div><span className="eyebrow">DEIN SPIEL. DEIN SETUP.</span><h2>Deine Profile <span>{profiles.length}</span></h2></div><button className="text-button" onClick={()=>navigate('Profile')}>Alle Profile<ArrowRight size={16}/></button></div>
  {profiles.length?cards:<button className="create-profile" onClick={create}><span className="add-icon"><Plus size={24}/></span><span><strong>Eine neue Welt wartet auf dich.</strong><small>Erstelle ein Profil. Hera hält deine Welten, Mods und Einstellungen zusammen.</small></span><ArrowRight size={20}/></button>}
  <div className="home-shortcuts"><button onClick={()=>navigate('Mods')}><span className="shortcut-icon"><Puzzle size={22}/></span><div><strong>Mach Minecraft zu deinem.</strong><span>Entdecke Mods, die zu deinem Profil passen.</span></div><ArrowRight size={18}/></button><button onClick={()=>navigate('Downloads')}><span className="shortcut-icon blue"><ShieldCheck size={22}/></span><div><strong>Gut vorbereitet ins Abenteuer.</strong><span>Spieldateien prüfen und Hera aktuell halten.</span></div><ArrowRight size={18}/></button></div>
 </>;
}
