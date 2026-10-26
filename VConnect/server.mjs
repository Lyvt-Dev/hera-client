import http from 'node:http';
import {readFile,mkdir} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import {createStore} from './store.mjs';
await mkdir(new URL('./.local/',import.meta.url),{recursive:true});
const production=process.env.NODE_ENV==='production';
if(production&&(!process.env.PUBLIC_ORIGIN||!process.env.PUBLIC_ORIGIN.startsWith('https://')))throw new Error('Production requires an HTTPS PUBLIC_ORIGIN.');
const store=createStore(fileURLToPath(new URL('./.local/vconnect.sqlite',import.meta.url)),{demo:process.env.VCONNECT_DEMO==='1'||(!production&&process.env.VCONNECT_DEMO!=='0')});
const files={'/':'index.html','/login':'index.html','/register':'index.html','/auth.js':'auth.js','/app.js':'app.js','/style.css':'style.css','/fonts/nunito-variable.ttf':'fonts/nunito-variable.ttf','/fonts/OFL-Nunito.txt':'fonts/OFL-Nunito.txt'};
const attempts=new Map();
const server=http.createServer(async(req,res)=>{
 res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','strict-origin-when-cross-origin');res.setHeader('X-Frame-Options','DENY');res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
 const path=new URL(req.url,'http://localhost').pathname;
 try{
 if(path.startsWith('/api/')){res.setHeader('Cache-Control','no-store');res.setHeader('Content-Type','application/json; charset=utf-8');const id=store.session(req.headers.cookie);
 if(req.method==='GET'&&path==='/api/state'){res.end(JSON.stringify(store.snapshot(id)));return;}
 if(req.method!=='POST'){res.writeHead(405).end(JSON.stringify({error:'Methode nicht erlaubt.'}));return;}
 const expected=process.env.PUBLIC_ORIGIN||`http://${req.headers.host}`;if(req.headers.origin!==expected){res.writeHead(403).end(JSON.stringify({error:'Anfrage nicht erlaubt.'}));return;}
 if(!req.headers['content-type']?.startsWith('application/json'))throw Object.assign(new Error('JSON erforderlich.'),{status:415});
 const key=req.socket.remoteAddress;let rate=attempts.get(key);if(!rate||Date.now()-rate.start>60000){rate={start:Date.now(),n:0,auth:0};attempts.set(key,rate)}if(++rate.n>100||(path==='/api/login'||path==='/api/register')&&++rate.auth>10)throw Object.assign(new Error('Zu viele Anfragen. Bitte kurz warten.'),{status:429});
 let raw='';for await(const chunk of req){raw+=chunk;if(Buffer.byteLength(raw)>8192)throw Object.assign(new Error('Anfrage zu groß.'),{status:413});}let body;try{body=JSON.parse(raw);if(!body||typeof body!=='object'||Array.isArray(body))throw 0}catch{throw Object.assign(new Error('Ungültige Anfrage.'),{status:400})}
 const result=await store.action(path,body,id);if(result.token)res.setHeader('Set-Cookie',`vc_session=${result.token}; HttpOnly; SameSite=Lax; Path=/; Max-Age=604800${production?'; Secure':''}`);if(path==='/api/logout')res.setHeader('Set-Cookie',`vc_session=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0${production?'; Secure':''}`);res.end(JSON.stringify(store.snapshot(result.id??(path==='/api/logout'?null:id))));return;
 }
 const file=files[path];if(!file||!['GET','HEAD'].includes(req.method)){res.writeHead(404).end();return;}res.setHeader('Cache-Control','no-cache');res.setHeader('Content-Type',file.endsWith('.ttf')?'font/ttf':file.endsWith('.txt')?'text/plain; charset=utf-8':file.endsWith('.css')?'text/css':file.endsWith('.js')?'text/javascript':'text/html; charset=utf-8');res.end(await readFile(new URL('./dist/'+file,import.meta.url)));
 }catch(e){res.writeHead(e.status??500).end(JSON.stringify({error:e.status?e.message:'Serverfehler. Bitte erneut versuchen.'}));}
});
server.requestTimeout=15000;server.headersTimeout=10000;
server.listen(Number(process.env.PORT||4173),process.env.HOST||'127.0.0.1',()=>console.log(`Local: http://127.0.0.1:${process.env.PORT||4173}`));
