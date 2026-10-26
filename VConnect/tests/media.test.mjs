import test from 'node:test';
import assert from 'node:assert/strict';
import {validateProfileImage} from '../media.mjs';
import {createStore} from '../store.mjs';
import {makePng,imageData} from './support/png.mjs';
test('raster validator strips metadata and rejects corrupt, oversized and active content',()=>{
 const original=makePng(512,512,{metadata:true}),safe=validateProfileImage(imageData(original),'avatar');assert.ok(!safe.includes(Buffer.from('PRIVATE metadata')));assert.ok(safe.length>0);
 const corrupt=Buffer.from(original);corrupt[corrupt.length-1]^=1;assert.throws(()=>validateProfileImage(imageData(corrupt),'avatar'));
 for(const value of ['data:image/svg+xml;base64,'+Buffer.from('<svg onload="alert(1)"/>').toString('base64'),'data:image/png;base64,notvalid',imageData(makePng(10,10)),imageData(makePng(512,512,{extra:1})),imageData(makePng(512,512,{filter:5}))])assert.throws(()=>validateProfileImage(value,'avatar'));
 assert.throws(()=>validateProfileImage(imageData(original),'banner'));assert.ok(validateProfileImage(imageData(makePng(1500,500)),'banner').length);
});
test('media upload, replacement and removal are atomic and account-scoped',async()=>{
 const store=createStore(':memory:');try{const a=await store.action('/api/register',{name:'Alice',email:'a@example.test',password:'secure-test-password-123'},null),b=await store.action('/api/register',{name:'Bob',email:'b@example.test',password:'secure-test-password-123'},null);
 const get=id=>store.snapshot(id).profiles.find(p=>p.id===id);const upload=imageData(makePng());
 await store.action('/api/profile',{...get(a.id),avatarUpload:upload,bannerUpload:imageData(makePng(1500,500))},a.id);
 const first=get(a.id),oldId=first.avatarImage.slice(7);assert.ok(store.getMedia(oldId));assert.ok(store.getMedia(first.bannerImage.slice(7)));
 await store.action('/api/profile',{...get(b.id),avatarImage:first.avatarImage,removeAvatar:true},b.id);assert.equal(get(b.id).avatarImage,'');assert.ok(store.getMedia(oldId));
 await assert.rejects(store.action('/api/profile',{...first,name:'Should rollback',avatarUpload:upload,bannerUpload:'invalid'},a.id));assert.equal(get(a.id).name,'Alice');assert.equal(get(a.id).avatarImage,first.avatarImage);
 await store.action('/api/profile',{...first,avatarUpload:upload},a.id);assert.equal(store.getMedia(oldId),undefined);assert.notEqual(get(a.id).avatarImage,first.avatarImage);
 await store.action('/api/profile',{...get(a.id),removeAvatar:true,removeBanner:true},a.id);assert.equal(get(a.id).avatarImage,'');assert.equal(get(a.id).bannerImage,'');assert.equal(store.db.prepare('SELECT COUNT(*) AS n FROM media').get().n,0);
 }finally{store.db.close()}
});
