import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile,mkdtemp,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {createStore,demos} from '../store.mjs';
test('local assets and licensed Nunito exist',async()=>{const html=await readFile('dist/index.html','utf8');for(const path of ['style.css','app.js']){assert.ok(html.includes('/'+path));assert.ok((await readFile('dist/'+path)).length>0)}assert.ok((await readFile('dist/fonts/OFL-Nunito.txt','utf8')).includes('SIL OPEN FONT LICENSE'));assert.ok(!(await readFile('dist/app.js','utf8')).includes('Dein Community-Space'))});
test('SQLite authentication, ownership, persistence and demos',async()=>{
 const dir=await mkdtemp(join(tmpdir(),'vconnect-'));const path=join(dir,'test.sqlite');let store=createStore(path,{demo:true});
 try{
 assert.equal(store.snapshot(null).profiles.length,6);assert.ok(demos.every(p=>p.demo));
 const alice=await store.action('/api/register',{email:'alice@example.test',password:'a-secure-password-123',name:'Alice'},null);
 const bob=await store.action('/api/register',{email:'bob@example.test',password:'b-secure-password-123',name:'Bob'},null);
 assert.equal(store.session('vc_session='+alice.token),alice.id);assert.equal(store.session('vc_session='+'0'.repeat(64)),null);
 const login=await store.action('/api/login',{email:'alice@example.test',password:'a-secure-password-123'},null);assert.equal(store.session('vc_session='+login.token),alice.id);const sessionRow=store.db.prepare('SELECT token_hash FROM sessions WHERE user_id=?').get(alice.id);assert.notEqual(sessionRow.token_hash,alice.token);
 const raw=store.db.prepare('SELECT * FROM users WHERE id=?').get(alice.id);assert.notEqual(raw.password_hash,'a-secure-password-123');assert.ok(!JSON.stringify(store.snapshot(alice.id)).includes(raw.password_hash));
 await assert.rejects(store.action('/api/login',{email:'alice@example.test',password:'wrong-password-123'},null),/nicht korrekt/);
 await assert.rejects(store.action('/api/posts',{text:'no'},null),/melde dich an/);
 await store.action('/api/profile',{id:bob.id,name:'Alice updated',role:'VTuber',bio:'Hello',tags:'Art',link:'https://example.com',collab:true},alice.id);
 assert.equal(store.snapshot(bob.id).profiles.find(p=>p.id===bob.id).name,'Bob');
 await assert.rejects(store.action('/api/profile',{name:'Bad',role:'VTuber',bio:'',tags:'',link:'javascript:alert(1)',collab:false},alice.id),/HTTP/);
 await store.action('/api/posts',{text:'First post'},alice.id);const post=store.snapshot(bob.id).posts[0];assert.equal(post.author,alice.id);
 await store.action('/api/interactions',{target:post.id,kind:'post-like'},bob.id);await store.action('/api/interactions',{target:post.id,kind:'save'},bob.id);
 assert.equal(store.snapshot(alice.id).posts[0].liked,false);assert.equal(store.snapshot(bob.id).posts[0].likes,1);assert.equal(store.snapshot(alice.id).saved.length,0);
 await store.action('/api/interactions',{target:'demo-1',kind:'like'},bob.id);assert.deepEqual(store.snapshot(bob.id).liked,['demo-1']);
 await store.action('/api/reset-swipes',{},bob.id);assert.deepEqual(store.snapshot(bob.id).liked,[]);
 store.db.close();store=createStore(path);assert.equal(store.snapshot(null).profiles.length,2);assert.equal(store.snapshot(null).posts.length,1);assert.equal(store.session('vc_session='+alice.token),alice.id);
 await store.action('/api/logout',{},alice.id);assert.equal(store.session('vc_session='+alice.token),null);
 }finally{store.db.close();await rm(dir,{recursive:true,force:true})}
});

