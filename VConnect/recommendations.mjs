const words=value=>new Set(String(value||'').normalize('NFKC').toLocaleLowerCase('de').split(/[,;\n]+/).map(s=>s.trim()).filter(Boolean));
const overlap=(a,b)=>[...a].filter(x=>b.has(x));
export function rankProfiles({profiles,viewer,liked=[],skipped=[]}){
 const byId=new Map(profiles.map(p=>[p.id,p]));const tastes=new Map();
 for(const [ids,weight] of [[liked,1.5],[skipped,-.7]])for(const id of ids)for(const tag of words(byId.get(id)?.tags))tastes.set(tag,(tastes.get(tag)||0)+weight);
 const ownTags=words(viewer?.tags),ownLanguages=words(viewer?.languages),ownFormats=words(viewer?.formats);
 const seen=new Set([...liked,...skipped]);
 const ranked=profiles.filter(p=>p.role==='VTuber'&&p.id!==viewer?.id&&!seen.has(p.id)).map(p=>{
 const tags=words(p.tags),common=overlap(tags,ownTags),languages=overlap(words(p.languages),ownLanguages),formats=overlap(words(p.formats),ownFormats);let score=common.length*6+languages.length*2;
 const learned=[...tags].reduce((n,t)=>n+(tastes.get(t)||0),0);score+=Math.max(-5,Math.min(8,learned));
 const reasons=[];if(common.length)reasons.push('Gemeinsame Interessen: '+common.slice(0,2).join(', '));if(languages.length)reasons.push('Gemeinsame Sprache: '+languages[0]);if(viewer?.collab&&p.collab){score+=3+formats.length*3;reasons.push(formats.length?'Collab-Format: '+formats[0]:'Auch offen für Collabs');}if(learned>0&&!common.length)reasons.push('Ähnliche Interessen wie gemerkte Profile');if(!reasons.length)reasons.push('Etwas Neues entdecken');
 return {id:p.id,score,reasons};
 }).sort((a,b)=>b.score-a.score||a.id.localeCompare(b.id));
 // One exploration slot per four cards; deterministic and never repeats a seen profile.
 const result=[];while(ranked.length){if(result.length%4===3&&ranked.length>1)result.push(ranked.pop());else result.push(ranked.shift());}return result;
}
export function rankPosts({posts,profiles,viewer,liked=[],now=Date.now()}){
 const profileMap=new Map(profiles.map(p=>[p.id,p]));const tags=words(viewer?.tags),connections=new Set(liked);
 const scored=posts.map(p=>({id:p.id,author:p.author,score:(connections.has(p.author)?10:0)+overlap(words(profileMap.get(p.author)?.tags),tags).length*3+12/(1+Math.max(0,now-p.date)/3600000)})).sort((a,b)=>b.score-a.score||a.id.localeCompare(b.id));
 const output=[];while(scored.length){let index=0;if(output.length>=2&&output.at(-1).author===output.at(-2).author){const different=scored.findIndex(p=>p.author!==output.at(-1).author);if(different>=0)index=different;}output.push(scored.splice(index,1)[0]);}return output.map(p=>p.id);
}
