import {request} from 'node:http';
import test from 'node:test';
import assert from 'node:assert/strict';
import {once} from 'node:events';
import {createStore} from '../store.mjs';
import {createHttpServer} from '../transport.mjs';
import {makePng,imageData} from './support/png.mjs';
import {escapeHtml,imageSrc} from '../dist/ui.js';
import {profileMarkup} from '../dist/profile.js';
import {postMarkup} from '../dist/composer.js';
test('HTTP boundaries reject injection, foreign writes, CSRF, unsafe media and oversized bodies',async()=>{
 const store=createStore(':memory:',{demo:true}),server=createHttpServer({store});server.listen(0,'127.0.0.1');await once(server,'listening');const origin='http://127.0.0.1:'+server.address().port;
 const send=(path,body,cookie='',extra={})=>fetch(origin+path,{method:'POST',headers:{Origin:origin,'Content-Type':'application/json',Cookie:cookie,...extra},body:JSON.stringify(body)});
 try{
 const first=await send('/api/register',{name:'Alice',username:'alice',email:'alice@example.test',password:'test-password-123456'});assert.equal(first.status,200);assert.match(first.headers.get('set-cookie'),/HttpOnly/);assert.match(first.headers.get('set-cookie'),/SameSite=Lax/);const cookie=first.headers.get('set-cookie').split(';')[0],sessionToken=cookie.split('=')[1],alice=await first.json();
 const second=await send('/api/register',{name:'Bob',username:'bob',email:'bob@example.test',password:'test-password-123456'}),bob=await second.json();assert.equal(second.status,200);
 assert.equal((await send('/api/register',{name:'Copy',username:'ALICE',email:'copy@example.test',password:'test-password-123456'})).status,409);
 assert.equal((await send('/api/login',{email:"'OR'1'='1@attack.test",password:'test-password-123456'})).status,401);
 assert.equal((await send('/api/posts',{text:'no login'})).status,401);
 assert.equal((await send('/api/posts',{text:'cross site'},cookie,{Origin:'https://attacker.example'})).status,403);
 const payload="'); DROP TABLE users;--";assert.equal((await send('/api/posts',{text:payload},cookie)).status,200);
 const current=alice.profiles.find(p=>p.id===alice.active);
 assert.equal((await send('/api/profile',{...current,id:bob.active,username:'bob'},cookie)).status,409);
 const upload=await send('/api/profile',{...current,name:payload,avatarUpload:imageData(makePng())},cookie);assert.equal(upload.status,200);const data=await upload.json(),profile=data.profiles.find(p=>p.id===alice.active);
 assert.equal(data.profiles.find(p=>p.id===bob.active).name,'Bob');assert.equal(profile.name,payload);assert.ok(!JSON.stringify(data).includes(sessionToken));assert.ok(!JSON.stringify(data).includes('password_hash'));assert.ok(!JSON.stringify(data).includes('alice@example.test'));
 const media=await fetch(origin+profile.avatarImage);assert.equal(media.status,200);assert.equal(media.headers.get('content-type'),'image/png');assert.equal(media.headers.get('x-content-type-options'),'nosniff');assert.ok((await media.arrayBuffer()).byteLength>0);
 assert.equal((await send('/api/profile',{...profile,avatarUpload:'data:image/svg+xml;base64,PHN2Zy8+'},cookie)).status,400);
 const oversized=await send('/api/profile',{...profile,bio:'x'.repeat(7_000_001)},cookie);assert.equal(oversized.status,413);
 const hostStatus=await new Promise((resolve,reject)=>{const req=request(origin+'/api/state',{headers:{Host:'attacker.example'}},res=>{res.resume();resolve(res.statusCode)});req.on('error',reject);req.end()});assert.equal(hostStatus,421);
 const final=await (await fetch(origin+'/api/state')).json();assert.equal(final.posts[0].text,payload);assert.equal(final.profiles.length,8);
 assert.equal((await fetch(origin+'/store.mjs')).status,404);
 }finally{server.closeAllConnections();await new Promise(resolve=>server.close(resolve));store.db.close()}
});
test('renderers escape stored text and disallow arbitrary remote image references',()=>{
 const payload='<img src=x onerror="alert(1)">';assert.equal(imageSrc('https://attacker.example/a.svg'),'');assert.equal(imageSrc('javascript:alert(1)'),'');assert.ok(escapeHtml(payload).includes('&lt;img'));
 const author={name:payload,username:'safe',bio:payload,role:'VTuber',tags:payload};const profile=profileMarkup(author),post=postMarkup({id:'p',author:'u',text:payload,date:Date.now()},author);
 for(const html of [profile,post]){assert.ok(html.includes('&lt;img'));assert.ok(!html.includes('<img src=x'));assert.ok(!html.includes('href="javascript:'));}
});
