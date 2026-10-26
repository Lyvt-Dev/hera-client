import test from 'node:test';
import assert from 'node:assert/strict';
import {createStore} from '../store.mjs';
import {makePng,imageData} from './support/png.mjs';
import {formatPostText} from '../dist/composer.js';
test('Art and Design are shared channels with persistent safe image and code posts',async()=>{
 const store=createStore(':memory:');try{
 const a=await store.action('/api/register',{email:'artist@example.test',username:'artist',name:'Artist',password:'password-for-tests-123'},null);
 const b=await store.action('/api/register',{email:'coder@example.test',username:'coder',name:'Coder',password:'password-for-tests-123'},null);
 await store.action('/api/posts',{text:'Original artwork',topic:'art',imageUpload:imageData(makePng(700,400)),imageAlt:'A colored circle'},a.id);
 await store.action('/api/posts',{text:'Snippet\n```js\nconst value = "<script>";\n```',topic:'design'},b.id);
 const all=store.snapshot(a.id).posts;assert.equal(all.filter(p=>p.topic==='art').length,1);assert.equal(all.filter(p=>p.topic==='design').length,1);
 const art=all.find(p=>p.topic==='art');assert.ok(store.getMedia(art.image.slice(7)));assert.equal(art.image_alt,'A colored circle');assert.equal(store.snapshot(b.id).posts.find(p=>p.id===art.id).author,a.id);
 const profile=store.snapshot(a.id).profiles.find(p=>p.id===a.id);await store.action('/api/profile',{...profile,removeAvatar:true,removeBanner:true},a.id);assert.ok(store.getMedia(art.image.slice(7)));
 const code=formatPostText(all.find(p=>p.topic==='design').text);assert.ok(code.includes('<pre class="post-code">'));assert.ok(code.includes('&lt;script&gt;'));assert.ok(!code.includes('<script>'));
 await assert.rejects(store.action('/api/posts',{text:'bad',topic:"art');DROP TABLE posts;--"},a.id));
 await assert.rejects(store.action('/api/posts',{text:'removed channel',topic:'flow'},a.id));
 await assert.rejects(store.action('/api/posts',{text:'',topic:'art'},a.id));
 await store.action('/api/posts',{text:'',topic:'art',imageUpload:imageData(makePng(400,300))},a.id);
 assert.equal(store.snapshot(b.id).posts.filter(p=>p.topic==='art').length,2);
 }finally{store.db.close()}
});
test('tutorial pause, completion and replay are account-specific',async()=>{
 const store=createStore(':memory:');try{const a=await store.action('/api/register',{name:'Tour',email:'tour@example.test',password:'password-for-tests-123'},null),b=await store.action('/api/register',{name:'Other',email:'other@example.test',password:'password-for-tests-123'},null);
 await store.action('/api/onboarding',{step:4,status:'paused'},a.id);assert.deepEqual({...store.snapshot(a.id).onboarding},{step:4,status:'paused'});assert.equal(store.snapshot(b.id).onboarding.step,0);
 await store.action('/api/onboarding',{step:6,status:'completed'},a.id);assert.equal(store.snapshot(a.id).onboarding.status,'completed');await store.action('/api/onboarding',{step:0,status:'active'},a.id);assert.equal(store.snapshot(a.id).onboarding.step,0);
 }finally{store.db.close()}
});
