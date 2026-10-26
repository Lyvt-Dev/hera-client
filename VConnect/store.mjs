import { DatabaseSync } from 'node:sqlite';
import { randomBytes, randomUUID, scrypt as scryptCallback, createHash, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';
const scrypt=promisify(scryptCallback);
const hash=s=>createHash('sha256').update(s).digest('hex');
export const demos=[['Luna Mochi','Cozy Games, entspannte Abende und eine kleine Portion Chaos.','Cozy Games, Deutsch',true],['Kuro Byte','Cyber-Cat mit einer Vorliebe für Roguelikes und Technik.','Roguelikes, Tech',true],['Mika Moss','Waldgeist, digitale Kunst und kreative gemeinsame Streams.','Art, Drawing',true],['Nova Nyx','Weltraumpilotin auf der Suche nach dem nächsten Co-op-Abenteuer.','Co-op, Sci-Fi',true],['Yuki Pixel','Rhythm Games, Retro und gelegentlich ein sehr lauter Bossfight.','Rhythm, Retro',false],['Ember Fox','Fuchs-VTuber für Horror-Games und Multiplayer-Runden.','Horror, Multiplayer',true]].map(([name,bio,tags,collab],i)=>({id:`demo-${i+1}`,name,bio,tags,collab,role:'VTuber',link:'',demo:true}));
export function createStore(path,{demo=false}={}){
 const db=new DatabaseSync(path);db.exec(`PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL;
 CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,salt TEXT NOT NULL,profile TEXT NOT NULL);
 CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,expires INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS posts(id TEXT PRIMARY KEY,author TEXT NOT NULL REFERENCES users(id),text TEXT NOT NULL,date INTEGER NOT NULL);
 CREATE TABLE IF NOT EXISTS interactions(user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,target TEXT NOT NULL,kind TEXT NOT NULL,PRIMARY KEY(user_id,target,kind));`);
 const profiles=()=>db.prepare('SELECT id,profile FROM users').all().map(r=>({...JSON.parse(r.profile),id:r.id}));
 function session(cookie=''){const token=cookie.match(/(?:^|;\s*)vc_session=([a-f0-9]{64})(?:;|$)/)?.[1];if(!token)return null;return db.prepare('SELECT user_id FROM sessions WHERE token_hash=? AND expires>?').get(hash(token),Date.now())?.user_id??null;}
 function issue(id){const token=randomBytes(32).toString('hex');db.prepare('DELETE FROM sessions WHERE expires<?').run(Date.now());db.prepare('INSERT INTO sessions VALUES(?,?,?)').run(hash(token),id,Date.now()+7*86400000);return token;}
 function fail(message,status=400){throw Object.assign(new Error(message),{status});}
 function text(value,max,required=false){if(typeof value!=='string'||value.length>max||(required&&!value.trim()))fail('Bitte überprüfe deine Eingaben.');return value.trim();}
 const requireUser=id=>{if(!id)fail('Bitte melde dich an.',401)};
 function snapshot(id){const rows=id?db.prepare('SELECT target,kind FROM interactions WHERE user_id=?').all(id):[];const all=profiles();return {profiles:[...all,...(demo?demos:[])],posts:db.prepare('SELECT * FROM posts ORDER BY date DESC LIMIT 200').all().map(p=>({...p,liked:rows.some(r=>r.kind==='post-like'&&r.target===p.id),likes:db.prepare("SELECT COUNT(*) AS n FROM interactions WHERE target=? AND kind='post-like'").get(p.id).n})),active:id,saved:rows.filter(r=>r.kind==='save').map(r=>r.target),liked:rows.filter(r=>r.kind==='like').map(r=>r.target),skipped:rows.filter(r=>r.kind==='skip').map(r=>r.target),demo};}
 return {db,session,snapshot,async action(path,b,id){
 if(path==='/api/register'||path==='/api/login'){
 const email=text(b.email,254,true).toLowerCase(),password=b.password;if(typeof password!=='string'||password.length>256)fail('Ungültiges Passwort.');if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)||password.length<12)fail('Gültige E-Mail und ein Passwort mit mindestens 12 Zeichen erforderlich.');
 if(path==='/api/register'){const name=text(b.name,40,true);if(db.prepare('SELECT id FROM users WHERE email=?').get(email))fail('Registrierung mit diesen Daten nicht möglich.',409);const uid=randomUUID(),salt=randomBytes(16).toString('hex'),digest=Buffer.from(await scrypt(password,salt,64)).toString('hex');try{db.prepare('INSERT INTO users VALUES(?,?,?,?,?)').run(uid,email,digest,salt,JSON.stringify({name,role:'Zuschauer',bio:'',tags:'',link:'',collab:false}));}catch{fail('Registrierung mit diesen Daten nicht möglich.',409)}return {token:issue(uid),id:uid};}
 const user=db.prepare('SELECT * FROM users WHERE email=?').get(email),digest=Buffer.from(await scrypt(password,user?.salt??'invalid-user-salt',64));if(!user||!timingSafeEqual(digest,Buffer.from(user.password_hash,'hex')))fail('E-Mail oder Passwort ist nicht korrekt.',401);return {token:issue(user.id),id:user.id};
 }
 requireUser(id);
 if(path==='/api/logout'){db.prepare('DELETE FROM sessions WHERE user_id=?').run(id);return {};}
 if(path==='/api/profile'){const link=text(b.link,500);if(link){try{const u=new URL(link);if(!['http:','https:'].includes(u.protocol)||u.username||u.password)fail('Ungültiger Link.')}catch{fail('Bitte einen gültigen HTTP(S)-Link verwenden.')}}if(!['VTuber','Zuschauer'].includes(b.role)||typeof b.collab!=='boolean')fail('Ungültiges Profil.');const extra={};for(const key of ['twitch','youtube','social']){const value=text(b[key]??'',500);if(value){try{const u=new URL(value);if(!['http:','https:'].includes(u.protocol)||u.username||u.password)fail('Ungültiger Link.')}catch{fail('Bitte einen gültigen HTTP(S)-Link verwenden.')}}extra[key]=value;}const p={...extra,name:text(b.name,40,true),role:b.role,bio:text(b.bio,280),tags:text(b.tags,100),link,collab:b.collab};db.prepare('UPDATE users SET profile=? WHERE id=?').run(JSON.stringify(p),id);return {};}
 if(path==='/api/posts'){db.prepare('INSERT INTO posts VALUES(?,?,?,?)').run(randomUUID(),id,text(b.text,500,true),Date.now());return {};}
 if(path==='/api/interactions'){const kind=b.kind,target=text(b.target,100,true);if(!['like','skip','save','post-like'].includes(kind))fail('Ungültige Aktion.');if(['like','skip'].includes(kind)){if(target===id||![...profiles(),...(demo?demos:[])].some(p=>p.id===target))fail('Profil nicht gefunden.',404);db.prepare("DELETE FROM interactions WHERE user_id=? AND target=? AND kind IN ('like','skip')").run(id,target);}else if(!db.prepare('SELECT id FROM posts WHERE id=?').get(target))fail('Beitrag nicht gefunden.',404);if(b.enabled===false)db.prepare('DELETE FROM interactions WHERE user_id=? AND target=? AND kind=?').run(id,target,kind);else db.prepare('INSERT OR IGNORE INTO interactions VALUES(?,?,?)').run(id,target,kind);return {};}
 if(path==='/api/reset-swipes'){db.prepare("DELETE FROM interactions WHERE user_id=? AND kind IN ('like','skip')").run(id);return {};}
 fail('Nicht gefunden.',404);
 }};
}
