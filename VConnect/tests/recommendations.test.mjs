import test from 'node:test';
import assert from 'node:assert/strict';
import {rankProfiles,rankPosts} from '../recommendations.mjs';
const creator=(id,tags,extra={})=>({id,name:id,role:'VTuber',tags,languages:'Deutsch',...extra});
test('recommendations exclude self, viewers and both directions of seen swipes',()=>{
 const viewer=creator('me','Art');const ranked=rankProfiles({profiles:[viewer,creator('seen','Art'),creator('skip','Art'),creator('new','Art'),creator('viewer','Art',{role:'Zuschauer'})],viewer,liked:['seen'],skipped:['skip']});assert.deepEqual(ranked.map(p=>p.id),['new']);
});
test('common interests, language and collab formats have explainable priority',()=>{
 const viewer=creator('me','Art, Cozy Games',{collab:true,formats:'Co-Working'});
 const profiles=[viewer,creator('unrelated','Horror',{languages:'Englisch'}),creator('shared',' art , cozy games ',{collab:true,formats:'co-working'})];
 const ranked=rankProfiles({profiles,viewer});assert.equal(ranked[0].id,'shared');assert.ok(ranked[0].reasons.some(r=>r.includes('Gemeinsame Interessen')));assert.ok(ranked[0].reasons.some(r=>r.includes('Collab-Format')));
});
test('positive and negative swipe feedback changes ranking without using pronouns or bio',()=>{
 const profiles=[creator('positive','Art'),creator('negative','Horror'),creator('art','Art'),creator('horror','Horror')];
 const ranked=rankProfiles({profiles,viewer:{id:'me',tags:''},liked:['positive'],skipped:['negative']});assert.equal(ranked[0].id,'art');assert.ok(ranked[0].reasons[0].includes('gemerkte'));
 assert.deepEqual(ranked,rankProfiles({profiles:profiles.map(p=>({...p,pronouns:'different',bio:'different'})),viewer:{id:'me',tags:''},liked:['positive'],skipped:['negative']}));
});
test('cold-start discovery is deterministic, diverse and contains every candidate once',()=>{
 const profiles=Array.from({length:9},(_,i)=>creator('id-'+i,i<6?'Art':'Horror'));const args={profiles,viewer:{id:'me',tags:'Art'}};const ranked=rankProfiles(args);assert.deepEqual(ranked,rankProfiles(args));assert.equal(new Set(ranked.map(r=>r.id)).size,9);assert.equal(ranked[3].score,0);assert.equal(rankProfiles({profiles}).length,9);
});
test('feed balances relevance, time and consecutive-author diversity',()=>{
 const now=1_000_000;const profiles=[creator('a','Art'),creator('b','Horror')];const posts=[{id:'a1',author:'a',date:now},{id:'a2',author:'a',date:now-1},{id:'a3',author:'a',date:now-2},{id:'b1',author:'b',date:now-3}];
 const result=rankPosts({posts,profiles,viewer:{tags:'Art'},liked:['a'],now});assert.deepEqual(result,['a1','a2','b1','a3']);assert.equal(new Set(result).size,4);
});
