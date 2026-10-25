import { Pool } from 'pg';
import { Redis } from 'ioredis';
import { createApp, type SyncRecord } from './app.js';

const {DATABASE_URL,REDIS_URL}=process.env;
if(!DATABASE_URL||!REDIS_URL)throw new Error('DATABASE_URL and REDIS_URL are required; no insecure fallback storage.');
const pool=new Pool({connectionString:DATABASE_URL,max:10,connectionTimeoutMillis:5000,statement_timeout:5000});
const redis=new Redis(REDIS_URL,{maxRetriesPerRequest:1,connectTimeout:5000});
const app=await createApp({
 curseforgeKey:process.env.CURSEFORGE_API_KEY,
 redis,
 ready:async()=>{await pool.query('SELECT 1');return await redis.ping()==='PONG';},
 authenticate:async token=>{
  const response=await fetch('https://api.minecraftservices.com/minecraft/profile',{headers:{Authorization:`Bearer ${token}`},signal:AbortSignal.timeout(10_000),redirect:'error'});
  if(!response.ok)return null;
  const body=await response.json() as {id?:unknown};return typeof body.id==='string'?body.id:null;
 },
 read:async id=>{const {rows}=await pool.query<{revision:number;settings:SyncRecord['settings']}>('SELECT revision,settings FROM user_settings WHERE minecraft_uuid=$1',[id]);return rows[0]??null;},
 write:async(id,input)=>{
  // Compare and swap, including race-free creation; no user-supplied identity.
  const query=input.revision===0
   ? 'INSERT INTO user_settings(minecraft_uuid,revision,settings) VALUES($1,1,$2::jsonb) ON CONFLICT DO NOTHING RETURNING revision,settings'
   : 'UPDATE user_settings SET settings=$2::jsonb,revision=revision+1,updated_at=now() WHERE minecraft_uuid=$1 AND revision=$3 RETURNING revision,settings';
  const params:unknown[]=[id,JSON.stringify(input.settings)];if(input.revision!==0)params.push(input.revision);
  const {rows}=await pool.query<{revision:number;settings:SyncRecord['settings']}>(query,params);
  return rows[0]??null;
 },
});
await app.listen({host:process.env.HOST??'127.0.0.1',port:Number(process.env.PORT??3100)});
async function shutdown(){await app.close();await pool.end();redis.disconnect();}
process.once('SIGINT',()=>void shutdown());process.once('SIGTERM',()=>void shutdown());
