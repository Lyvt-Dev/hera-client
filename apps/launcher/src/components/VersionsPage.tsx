import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight, Box, RefreshCw, Search } from 'lucide-react';
import { api } from '../lib/bridge';
import { catalogThrough, versionKind } from '../lib/versions';
import { Empty } from './ui';
export function VersionsPage({create}:{create:(version:string)=>void}) {
 const query=useQuery({queryKey:['versions'],queryFn:async()=>catalogThrough(await api.versions()),staleTime:300000});
 const [search,setSearch]=useState(''),[kind,setKind]=useState('release'),[limit,setLimit]=useState(60);
 const versions=(query.data??[]).filter(v=>(kind==='all'||v.type===kind)&&v.id.toLowerCase().includes(search.toLowerCase().trim()));
 return <><div className="page-heading"><div><span className="eyebrow">VON DEN ANFÄNGEN BIS 26.3</span><h1>Deine Version. Deine Welt<span>.</span></h1><p>Offizielle Java-Versionen entdecken und direkt ein eigenes Profil erstellen.</p></div><button className="secondary" onClick={()=>void query.refetch()} disabled={query.isFetching}><RefreshCw size={16}/>Aktualisieren</button></div>
 <div className="catalog-toolbar"><label className="catalog-search"><Search size={18}/><input aria-label="Minecraft-Version suchen" placeholder="Zum Beispiel 1.8.9, 1.20.1 oder 26.3" value={search} onChange={e=>{setSearch(e.target.value);setLimit(60);}}/></label><select aria-label="Versionstyp" value={kind} onChange={e=>{setKind(e.target.value);setLimit(60);}}><option value="release">Releases</option><option value="snapshot">Snapshots</option><option value="old_beta">Historische Beta</option><option value="old_alpha">Historische Alpha</option><option value="all">Alle Versionen</option></select></div>
 <p className="hint">{versions.length} Versionen · Profile aus diesem Katalog verwenden Vanilla. Für Fabric und Forge ist derzeit 1.21.1 eingerichtet.</p>
 {query.isPending&&<p role="status">Offizielle Versionsliste wird geladen …</p>}{query.error&&<div className="error" role="alert">{String(query.error)} <button onClick={()=>void query.refetch()}>Erneut versuchen</button></div>}
 <div className="version-list">{versions.slice(0,limit).map(v=><article className="version-row" key={v.id}><div className="version-icon"><Box size={22}/></div><div className="version-name"><strong>Minecraft {v.id}</strong><small>{versionKind(v.type)} · {new Date(v.releaseTime).toLocaleDateString('de-DE')}</small></div><button className="secondary" onClick={()=>create(v.id)}>Profil erstellen<ArrowRight size={16}/></button></article>)}</div>
 {!query.isPending&&!query.error&&!versions.length&&<Empty title="Keine passende Version">Versuche einen anderen Suchbegriff oder Versionstyp.</Empty>}{versions.length>limit&&<button className="secondary wide" onClick={()=>setLimit(limit+60)}>Weitere Versionen anzeigen ({versions.length-limit})</button>}
 </>;
}
