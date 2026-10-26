import { DatabaseSync } from 'node:sqlite';
import { randomBytes, randomUUID, scrypt as scryptCallback, createHash, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';
import { validateProfileImage } from './media.mjs';
import { rankProfiles, rankPosts } from './recommendations.mjs';
const scrypt = promisify(scryptCallback);
const hash = value => createHash('sha256').update(value).digest('hex');
const reserved = new Set(['admin','administrator','support','system','vconnect']);
function fail(message, status = 400) { throw Object.assign(new Error(message), { status }); }
function text(value, max, required = false) {
  if (typeof value !== 'string' || value.length > max || (required && !value.trim())) fail('Bitte überprüfe deine Eingaben.');
  return value.trim();
}
export function normalizeUsername(value) {
  if (typeof value !== 'string' || value.length > 100) fail('Ungültiger Nutzername.');
  const username = value.normalize('NFKC').trim().replace(/^@/, '').toLowerCase();
  if (!/^[a-z0-9_]{3,24}$/.test(username) || reserved.has(username)) fail('Nutzername: 3–24 Buchstaben, Zahlen oder Unterstriche. Dieser Name darf nicht reserviert sein.');
  return username;
}
function legacyUsername(name) {
  const slug = String(name || '').toLowerCase().replace(/ä/g, 'ae').replace(/ö/g, 'oe').replace(/ü/g, 'ue').replace(/ß/g, 'ss').normalize('NFKD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9_]+/g, '_').replace(/^_+|_+$/g, '').slice(0,24);
  const result=slug.length >= 3 && !reserved.has(slug) ? slug : (slug || 'creator').slice(0,19) + '_user';
  return result.startsWith('demo_')?('user_'+result).slice(0,24):result;
}
export const demos = [
  ['Luna Mochi','Cozy Games, entspannte Abende und eine kleine Portion Chaos.','Cozy Games, Deutsch',true],
  ['Kuro Byte','Cyber-Cat mit einer Vorliebe für Roguelikes und Technik.','Roguelikes, Tech',true],
  ['Mika Moss','Waldgeist, digitale Kunst und kreative gemeinsame Streams.','Art, Drawing',true],
  ['Nova Nyx','Weltraumpilotin auf der Suche nach dem nächsten Co-op-Abenteuer.','Co-op, Sci-Fi',true],
  ['Yuki Pixel','Rhythm Games, Retro und gelegentlich ein sehr lauter Bossfight.','Rhythm, Retro',false],
  ['Ember Fox','Fuchs-VTuber für Horror-Games und Multiplayer-Runden.','Horror, Multiplayer',true]
].map(([name,bio,tags,collab],i) => ({
  id: `demo-${i+1}`, username: `demo_${legacyUsername(name)}`, name,bio,tags,collab,role:'VTuber',link:'',demo:true,
  languages:i===3?'Englisch':'Deutsch, Englisch',theme:['rose','ocean','forest','lilac','rose','sunset'][i],
  avatar:['moon','bolt','flower','star','heart','diamond'][i],banner:'orbit',
  formats:i===2?'Art-Tausch, Co-Working':'Co-op, Talk',schedule:i%2?'Abends':'Wochenende'
}));

export function createStore(path, { demo = false } = {}) {
  const db = new DatabaseSync(path);
  db.exec(`PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;
    CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,salt TEXT NOT NULL,profile TEXT NOT NULL,username TEXT NOT NULL COLLATE NOCASE UNIQUE);
    CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,expires INTEGER NOT NULL);
    CREATE TABLE IF NOT EXISTS posts(id TEXT PRIMARY KEY,author TEXT NOT NULL REFERENCES users(id),text TEXT NOT NULL,date INTEGER NOT NULL);
    CREATE TABLE IF NOT EXISTS media(id TEXT PRIMARY KEY,user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,kind TEXT NOT NULL,data BLOB NOT NULL);
    CREATE TABLE IF NOT EXISTS onboarding(user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,step INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'active');
    CREATE TABLE IF NOT EXISTS interactions(user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,target TEXT NOT NULL,kind TEXT NOT NULL,PRIMARY KEY(user_id,target,kind));
    CREATE INDEX IF NOT EXISTS interactions_target_kind ON interactions(target,kind);
    CREATE INDEX IF NOT EXISTS posts_date ON posts(date DESC);`);
  // Constant SQL only. Legacy display names are retained; unique handles are assigned once.
  db.exec('BEGIN IMMEDIATE');
  try {
    if (!db.prepare('PRAGMA table_info(users)').all().some(c => c.name === 'username')) db.exec('ALTER TABLE users ADD COLUMN username TEXT COLLATE NOCASE');
    const existing = db.prepare('SELECT id,profile,username FROM users ORDER BY rowid').all();
    const used = new Set(existing.filter(r => r.username).map(r => r.username.toLowerCase()));
    for (const row of existing.filter(r => !r.username)) {
      const base = legacyUsername(JSON.parse(row.profile).name); let username = base, suffix = 2;
      while (used.has(username)) { const tail = '_' + suffix++; username = base.slice(0,24-tail.length) + tail; }
      db.prepare('UPDATE users SET username=? WHERE id=?').run(username,row.id); used.add(username);
    }
    db.exec(`CREATE UNIQUE INDEX IF NOT EXISTS users_username_unique ON users(username COLLATE NOCASE);
      CREATE TRIGGER IF NOT EXISTS users_username_insert BEFORE INSERT ON users WHEN NEW.username IS NULL OR length(NEW.username) NOT BETWEEN 3 AND 24 OR NEW.username GLOB '*[^a-z0-9_]*' BEGIN SELECT RAISE(ABORT,'invalid username'); END;
      CREATE TRIGGER IF NOT EXISTS users_username_update BEFORE UPDATE OF username ON users WHEN NEW.username IS NULL OR length(NEW.username) NOT BETWEEN 3 AND 24 OR NEW.username GLOB '*[^a-z0-9_]*' BEGIN SELECT RAISE(ABORT,'invalid username'); END;`);
    if (!db.prepare('PRAGMA table_info(posts)').all().some(c => c.name === 'kind')) db.exec("ALTER TABLE posts ADD COLUMN kind TEXT NOT NULL DEFAULT 'post'");
    if (!db.prepare('PRAGMA table_info(posts)').all().some(c => c.name === 'topic')) db.exec("ALTER TABLE posts ADD COLUMN topic TEXT NOT NULL DEFAULT 'general'");
    if (!db.prepare('PRAGMA table_info(posts)').all().some(c => c.name === 'image')) db.exec("ALTER TABLE posts ADD COLUMN image TEXT NOT NULL DEFAULT ''");
    if (!db.prepare('PRAGMA table_info(posts)').all().some(c => c.name === 'image_alt')) db.exec("ALTER TABLE posts ADD COLUMN image_alt TEXT NOT NULL DEFAULT ''");
    db.exec('COMMIT');
  } catch (error) { db.exec('ROLLBACK'); db.close(); throw error; }

  const profiles = () => db.prepare('SELECT id,profile,username FROM users').all().map(row => ({...JSON.parse(row.profile),id:row.id,username:row.username}));
  const requireUser = id => { if (!id) fail('Bitte melde dich an.',401); };
  const usernameTaken = (username, except = '') => !!db.prepare('SELECT id FROM users WHERE username=? COLLATE NOCASE AND id<>?').get(username,except);
  function session(cookie = '') {
    const token = cookie.match(/(?:^|;\s*)vc_session=([a-f0-9]{64})(?:;|$)/)?.[1];
    if (!token) return null;
    return db.prepare('SELECT user_id FROM sessions WHERE token_hash=? AND expires>?').get(hash(token),Date.now())?.user_id ?? null;
  }
  function issue(id) {
    const token = randomBytes(32).toString('hex');
    db.prepare('DELETE FROM sessions WHERE expires<?').run(Date.now());
    db.prepare('INSERT INTO sessions VALUES(?,?,?)').run(hash(token),id,Date.now()+7*86400000);
    return token;
  }
  function snapshot(id) {
    const rows = id ? db.prepare('SELECT target,kind FROM interactions WHERE user_id=?').all(id) : [];
    const all = [...profiles(),...(demo?demos:[])], viewer = all.find(p => p.id === id);
    const liked = rows.filter(r => r.kind==='like').map(r => r.target), skipped = rows.filter(r => r.kind==='skip').map(r => r.target);
    const posts = db.prepare("SELECT posts.*,COUNT(i.target) AS likes FROM posts LEFT JOIN interactions i ON i.target=posts.id AND i.kind='post-like' GROUP BY posts.id ORDER BY date DESC LIMIT 200").all().map(p => ({...p,liked:rows.some(r=>r.kind==='post-like'&&r.target===p.id)}));
    return {profiles:all,posts,active:id,saved:rows.filter(r=>r.kind==='save').map(r=>r.target),liked,skipped,demo,
      recommendations:rankProfiles({profiles:all,viewer,liked,skipped}),feedOrder:rankPosts({posts,profiles:all,viewer,liked}),
      onboarding:id?(db.prepare('SELECT step,status FROM onboarding WHERE user_id=?').get(id)??{step:0,status:'active'}):null};
  }
  function validatedProfile(body) {
    if (!['VTuber','Zuschauer'].includes(body.role) || typeof body.collab !== 'boolean') fail('Ungültiges Profil.');
    const profile = {name:text(body.name,40,true),role:body.role,bio:text(body.bio,280),tags:text(body.tags,100),collab:body.collab};
    for (const key of ['link','twitch','youtube','social']) {
      const value = text(body[key]??'',500);
      if (value) { try { const url=new URL(value); if(!['http:','https:'].includes(url.protocol)||url.username||url.password) fail('Ungültiger Link.'); } catch { fail('Bitte einen gültigen HTTP(S)-Link verwenden.'); } }
      profile[key] = value;
    }
    for (const [key,allowed,fallback] of [
      ['theme',['lilac','ocean','rose','forest','sunset','mono'],'lilac'],
      ['avatar',['initial','star','moon','heart','spark','flower','diamond','bolt'],'initial'],
      ['banner',['orbit','grid','waves'],'orbit']
    ]) { const value=body[key]??fallback; if(!allowed.includes(value))fail('Ungültiges Design.'); profile[key]=value; }
    for (const [key,max] of [['pronouns',30],['languages',100],['schedule',120],['formats',150]]) profile[key]=text(body[key]??'',max);
    return profile;
  }
  async function action(path, body, id) {
    if (path === '/api/register' || path === '/api/login') {
      const identifier=text(body.email,254,true).toLowerCase(),password=body.password;
      if(typeof password!=='string'||password.length<12||password.length>256)fail('Ein Passwort mit 12–256 Zeichen ist erforderlich.');
      if(path==='/api/register') {
        if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identifier))fail('Bitte eine gültige E-Mail-Adresse angeben.');
        const name=text(body.name,40,true),username=normalizeUsername(body.username??legacyUsername(name));
        if(username.startsWith('demo_')||usernameTaken(username))fail('Dieser Nutzername ist bereits vergeben oder reserviert.',409);
        if(db.prepare('SELECT id FROM users WHERE email=?').get(identifier))fail('Registrierung mit diesen Daten nicht möglich.',409);
        const uid=randomUUID(),salt=randomBytes(16).toString('hex'),digest=Buffer.from(await scrypt(password,salt,64)).toString('hex');
        try { db.prepare('INSERT INTO users(id,email,password_hash,salt,profile,username) VALUES(?,?,?,?,?,?)').run(uid,identifier,digest,salt,JSON.stringify({name,role:'Zuschauer',bio:'',tags:'',link:'',collab:false}),username); }
        catch(error) { if(usernameTaken(username))fail('Dieser Nutzername ist bereits vergeben.',409); if(db.prepare('SELECT id FROM users WHERE email=?').get(identifier))fail('Registrierung mit diesen Daten nicht möglich.',409); throw error; }
        return {token:issue(uid),id:uid};
      }
      const loginUsername = identifier.includes('@') && !identifier.startsWith('@') ? '' : normalizeUsername(identifier);
      const user = db.prepare('SELECT * FROM users WHERE email=? OR username=? COLLATE NOCASE').get(identifier,loginUsername);
      const digest=Buffer.from(await scrypt(password,user?.salt??'invalid-user-salt',64));
      if(!user||!timingSafeEqual(digest,Buffer.from(user.password_hash,'hex')))fail('E-Mail, Nutzername oder Passwort ist nicht korrekt.',401);
      return {token:issue(user.id),id:user.id};
    }
    requireUser(id);
    if(path==='/api/logout') { db.prepare('DELETE FROM sessions WHERE user_id=?').run(id); return {}; }
    if(path==='/api/onboarding') {
      if(!Number.isInteger(body.step)||body.step<0||body.step>6||!['active','paused','completed'].includes(body.status)||(body.status==='completed'&&body.step!==6))fail('Ungültiger Tutorial-Fortschritt.');
      db.prepare('INSERT INTO onboarding VALUES(?,?,?) ON CONFLICT(user_id) DO UPDATE SET step=excluded.step,status=excluded.status').run(id,body.step,body.status); return {};
    }
    if(path==='/api/undo-swipe') { db.prepare("DELETE FROM interactions WHERE user_id=? AND target=? AND kind IN ('like','skip')").run(id,text(body.target,100,true)); return {}; }
    if(path==='/api/profile') {
      const profile=validatedProfile(body),row=db.prepare('SELECT profile,username FROM users WHERE id=?').get(id);
      const username=normalizeUsername(body.username??row.username);
      if(username.startsWith('demo_')||usernameTaken(username,id))fail('Dieser Nutzername ist bereits vergeben oder reserviert.',409);
      const previous=JSON.parse(row.profile),uploads={};
      for(const kind of ['avatar','banner']) if(body[kind+'Upload']!==undefined)uploads[kind]=validateProfileImage(body[kind+'Upload'],kind);
      db.exec('BEGIN IMMEDIATE');
      try {
        for(const kind of ['avatar','banner']) {
          profile[kind+'Image']=previous[kind+'Image']||'';
          const remove=body[kind==='avatar'?'removeAvatar':'removeBanner']===true;
          if(remove||uploads[kind]) { db.prepare('DELETE FROM media WHERE user_id=? AND kind=?').run(id,kind); profile[kind+'Image']=''; }
          if(uploads[kind]) { const mediaId=randomUUID(); db.prepare('INSERT INTO media VALUES(?,?,?,?)').run(mediaId,id,kind,uploads[kind]); profile[kind+'Image']='/media/'+mediaId; }
        }
        db.prepare('UPDATE users SET profile=?,username=? WHERE id=?').run(JSON.stringify(profile),username,id); db.exec('COMMIT');
      } catch(error) { db.exec('ROLLBACK'); if(usernameTaken(username,id))fail('Dieser Nutzername ist bereits vergeben.',409); throw error; }
      return {};
    }
    if(path==='/api/posts') {
      const kind=body.kind??'post',topic=body.topic??'general'; if(!['post','collab','stream'].includes(kind)||!['general','art','design'].includes(topic))fail('Ungültige Beitragsart.');
      const content=text(body.text??'',500,!body.imageUpload),alt=text(body.imageAlt??'',200),image=body.imageUpload?validateProfileImage(body.imageUpload,'post'):null,postId=randomUUID();
      db.exec('BEGIN IMMEDIATE');try{let imageUrl='';if(image){const mediaId=randomUUID();db.prepare('INSERT INTO media VALUES(?,?,?,?)').run(mediaId,id,'post',image);imageUrl='/media/'+mediaId;}db.prepare('INSERT INTO posts(id,author,text,date,kind,topic,image,image_alt) VALUES(?,?,?,?,?,?,?,?)').run(postId,id,content,Date.now(),kind,topic,imageUrl,alt);db.exec('COMMIT');}catch(error){db.exec('ROLLBACK');throw error}return {};
    }
    if(path==='/api/interactions') {
      const kind=body.kind,target=text(body.target,100,true);
      if(!['like','skip','save','post-like'].includes(kind))fail('Ungültige Aktion.');
      if(['like','skip'].includes(kind)) {
        if(target===id||![...profiles(),...(demo?demos:[])].some(p=>p.id===target))fail('Profil nicht gefunden.',404);
        db.prepare("DELETE FROM interactions WHERE user_id=? AND target=? AND kind IN ('like','skip')").run(id,target);
      } else if(!db.prepare('SELECT id FROM posts WHERE id=?').get(target))fail('Beitrag nicht gefunden.',404);
      if(body.enabled===false) db.prepare('DELETE FROM interactions WHERE user_id=? AND target=? AND kind=?').run(id,target,kind);
      else db.prepare('INSERT OR IGNORE INTO interactions VALUES(?,?,?)').run(id,target,kind);
      return {};
    }
    if(path==='/api/reset-swipes') { db.prepare("DELETE FROM interactions WHERE user_id=? AND kind IN ('like','skip')").run(id); return {}; }
    fail('Nicht gefunden.',404);
  }
  return {db,session,snapshot,action,getMedia:id=>db.prepare('SELECT data FROM media WHERE id=?').get(id)?.data};
}
