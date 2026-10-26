import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {mkdtemp,rm} from 'node:fs/promises';
import {join} from 'node:path';
import {tmpdir} from 'node:os';
import {createStore} from '../store.mjs';
const password='testing-security-password-123';
const register=(store,email,username,name=username)=>store.action('/api/register',{email,username,name,password},null);
const profile=(store,id)=>store.snapshot(id).profiles.find(p=>p.id===id);

test('usernames are canonical, database-unique, editable and safe under concurrent registration',async()=>{
 const store=createStore(':memory:');try{
 const alice=await register(store,'alice@example.test','Alice_Name','Shared display');
 assert.equal(profile(store,alice.id).username,'alice_name');
 await assert.rejects(register(store,'collision@example.test','ＡＬＩＣＥ＿ＮＡＭＥ'),error=>error.status===409);
 const bob=await register(store,'bob@example.test','bob_name','Shared display');
 assert.equal(profile(store,bob.id).name,'Shared display');
 await assert.rejects(store.action('/api/profile',{...profile(store,bob.id),username:'ALICE_NAME'},bob.id),error=>error.status===409);
 assert.equal(profile(store,bob.id).username,'bob_name');
 await store.action('/api/profile',{...profile(store,bob.id),username:'bob_new'},bob.id);
 const login=await store.action('/api/login',{email:'@BOB_NEW',password},null);assert.equal(login.id,bob.id);
 const race=await Promise.allSettled([register(store,'race1@example.test','race_name'),register(store,'race2@example.test','RACE_NAME')]);
 assert.equal(race.filter(r=>r.status==='fulfilled').length,1);assert.equal(race.filter(r=>r.status==='rejected'&&r.reason.status===409).length,1);
 assert.throws(()=>store.db.prepare('UPDATE users SET username=? WHERE id=?').run('alice_name',bob.id),/UNIQUE/);
 assert.throws(()=>store.db.prepare('UPDATE users SET username=? WHERE id=?').run(null,bob.id));
 assert.throws(()=>store.db.prepare('UPDATE users SET username=? WHERE id=?').run('Invalid Space',bob.id));
 }finally{store.db.close()}
});

test('legacy duplicate display names gain distinct handles without losing profiles or posts',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'vconnect-migration-')),path=join(dir,'old.sqlite');let store;
 try{const db=new DatabaseSync(path);db.exec('CREATE TABLE users(id TEXT PRIMARY KEY,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,salt TEXT NOT NULL,profile TEXT NOT NULL); CREATE TABLE posts(id TEXT PRIMARY KEY,author TEXT NOT NULL,text TEXT NOT NULL,date INTEGER NOT NULL);');
 for(const [id,name] of [['a','Alice'],['b','alice'],['c','Ａlice']])db.prepare('INSERT INTO users VALUES(?,?,?,?,?)').run(id,id+'@example.test','test','salt',JSON.stringify({name,role:'VTuber',bio:'Legacy bio'}));
 db.prepare('INSERT INTO posts VALUES(?,?,?,?)').run('p1','a','Original post',1);db.close();store=createStore(path);
 assert.deepEqual(store.snapshot(null).profiles.map(p=>p.username),['alice','alice_2','alice_3']);assert.deepEqual(store.snapshot(null).profiles.map(p=>p.name),['Alice','alice','Ａlice']);assert.equal(store.snapshot(null).posts[0].text,'Original post');assert.equal(store.snapshot(null).posts[0].kind,'post');
 store.db.close();store=createStore(path);assert.equal(profile(store,'b').username,'alice_2');
 }finally{store?.db.close();await rm(dir,{recursive:true,force:true})}
});

test('SQL injection payloads remain bound data and never bypass authentication or ownership',async()=>{
 const store=createStore(':memory:',{demo:true});try{
 const alice=await register(store,'a@example.test','alice'),bob=await register(store,'b@example.test','bob');
 await assert.rejects(store.action('/api/login',{email:"'OR'1'='1@attack.test",password},null),error=>error.status===401);
 await assert.rejects(store.action('/api/login',{email:'a@example.test',password:"123456789012' OR '1'='1"},null),error=>error.status===401);
 assert.equal(store.session("vc_session=' OR 1=1 --"),null);
 const payload="x'); DROP TABLE users;--";
 await store.action('/api/profile',{...profile(store,alice.id),id:bob.id,name:payload,bio:"' UNION SELECT password_hash FROM users--"},alice.id);
 assert.equal(profile(store,alice.id).name,payload);assert.equal(profile(store,bob.id).name,'bob');
 await store.action('/api/posts',{text:payload},alice.id);const post=store.snapshot(bob.id).posts[0];assert.equal(post.text,payload);assert.equal(post.author,alice.id);
 await store.action('/api/interactions',{target:post.id,kind:'save'},bob.id);
 await assert.rejects(store.action('/api/interactions',{target:"' OR 1=1 --",kind:'save'},alice.id),error=>error.status===404);
 await assert.rejects(store.action('/api/interactions',{target:post.id,kind:"save');DELETE FROM users;--"},alice.id),error=>error.status===400);
 await store.action('/api/undo-swipe',{target:"' OR 1=1 --",user_id:bob.id},alice.id);
 await store.action('/api/reset-swipes',{user_id:bob.id},alice.id);
 assert.deepEqual(store.snapshot(bob.id).saved,[post.id]);assert.equal(store.getMedia("' OR 1=1 --"),undefined);
 assert.equal(store.db.prepare('SELECT COUNT(*) AS n FROM users').get().n,2);assert.equal(store.db.prepare('PRAGMA integrity_check').get().integrity_check,'ok');
 }finally{store.db.close()}
});
