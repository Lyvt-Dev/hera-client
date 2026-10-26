import {watch} from 'node:fs';
import http from 'node:http';
import {readFile} from 'node:fs/promises';
const files={
 '/':'index.html','/login':'index.html','/register':'index.html',
 '/auth.js':'auth.js','/ui.js':'ui.js','/profile.js':'profile.js','/composer.js':'composer.js','/tutorial.js':'tutorial.js','/image-editor.js':'image-editor.js',
 '/app.js':'app.js','/style.css':'style.css','/fonts/nunito-variable.ttf':'fonts/nunito-variable.ttf','/fonts/OFL-Nunito.txt':'fonts/OFL-Nunito.txt'
};
function failure(message,status){return Object.assign(new Error(message),{status})}
function readJson(req,limit){return new Promise((resolve,reject)=>{
 if(Number(req.headers['content-length'])>limit){req.resume();reject(failure('Anfrage zu groß.',413));return;}
 const chunks=[];let total=0,failed=false;
 req.on('data',chunk=>{if(failed)return;total+=chunk.length;if(total>limit){failed=true;chunks.length=0;reject(failure('Anfrage zu groß.',413));return;}chunks.push(chunk)});
 req.on('end',()=>{if(failed)return;try{const value=JSON.parse(Buffer.concat(chunks).toString('utf8'));if(!value||typeof value!=='object'||Array.isArray(value))throw 0;resolve(value)}catch{reject(failure('Ungültige Anfrage.',400))}});
 req.on('error',reject);req.on('aborted',()=>reject(failure('Anfrage abgebrochen.',400)));
})}
export function createHttpServer({store,production=false,publicOrigin,liveReload=false}){
 const attempts=new Map(),clients=new Set();let sweep=Date.now(),watcher,reloadTimer;
 const server=http.createServer(async(req,res)=>{
  res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','strict-origin-when-cross-origin');res.setHeader('X-Frame-Options','DENY');
  res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
  try{
   const host=new URL('http://'+req.headers.host).hostname;
   if(publicOrigin?host!==new URL(publicOrigin).hostname:!['localhost','127.0.0.1','[::1]'].includes(host))throw failure('Host nicht erlaubt.',421);
   const path=new URL(req.url,'http://localhost').pathname;
   if(path==='/dev.js'&&req.method==='GET'){res.setHeader('Content-Type','text/javascript');res.setHeader('Cache-Control','no-store');res.end(liveReload&&!production?"const events=new EventSource('/__dev/events');let connected=false,pending=false;const reload=()=>{pending=true};setInterval(()=>{if(pending&&!document.querySelector('dialog[open]')&&!['INPUT','TEXTAREA','SELECT'].includes(document.activeElement?.tagName))location.reload()},600);events.onopen=()=>{if(connected)reload();connected=true};events.onmessage=event=>{if(event.data==='reload')reload()};":'');return;}
   if(path==='/__dev/events'&&liveReload&&!production&&req.method==='GET'){res.writeHead(200,{'Content-Type':'text/event-stream','Cache-Control':'no-store','Connection':'keep-alive'});res.write(': connected\n\n');clients.add(res);req.on('close',()=>clients.delete(res));return;}
   if(/^\/media\/[a-f0-9-]{36}$/.test(path)&&['GET','HEAD'].includes(req.method)){
    const data=store.getMedia(path.slice(7));if(!data){res.writeHead(404).end();return;}
    res.setHeader('Content-Type','image/png');res.setHeader('Cache-Control','public, max-age=3600');res.setHeader('Content-Length',data.length);
    res.end(req.method==='HEAD'?undefined:Buffer.from(data));return;
   }
   if(path.startsWith('/api/')){
    res.setHeader('Cache-Control','no-store');res.setHeader('Content-Type','application/json; charset=utf-8');
    const now=Date.now();if(now-sweep>60000){for(const [key,bucket] of attempts)if(now-bucket.start>60000)attempts.delete(key);sweep=now;}
    const key=req.socket.remoteAddress;let rate=attempts.get(key);if(!rate||now-rate.start>60000){if(attempts.size>=10000)throw failure('Bitte später erneut versuchen.',429);rate={start:now,n:0,auth:0,images:0};attempts.set(key,rate)}
    if(++rate.n>200)throw failure('Zu viele Anfragen. Bitte kurz warten.',429);
    const id=store.session(req.headers.cookie);
    if(req.method==='GET'&&path==='/api/state'){res.end(JSON.stringify(store.snapshot(id)));return;}
    if(req.method!=='POST')throw failure('Methode nicht erlaubt.',405);
    const expected=publicOrigin||'http://'+req.headers.host;if(req.headers.origin!==expected)throw failure('Anfrage nicht erlaubt.',403);
    if(!req.headers['content-type']?.startsWith('application/json'))throw failure('JSON erforderlich.',415);
    const auth=path==='/api/login'||path==='/api/register';if(auth&&++rate.auth>10)throw failure('Zu viele Anmeldeversuche. Bitte kurz warten.',429);
    if(!auth&&!id)throw failure('Bitte melde dich an.',401);
    if(path==='/api/profile'&&++rate.images>20)throw failure('Zu viele Profiländerungen. Bitte kurz warten.',429);
    const body=await readJson(req,['/api/profile','/api/posts'].includes(path)?7_000_000:8192);
    const result=await store.action(path,body,id);
    if(result.token)res.setHeader('Set-Cookie',`vc_session=${result.token}; HttpOnly; SameSite=Lax; Path=/; Max-Age=604800${production?'; Secure':''}`);
    if(path==='/api/logout')res.setHeader('Set-Cookie',`vc_session=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0${production?'; Secure':''}`);
    res.end(JSON.stringify(store.snapshot(result.id??(path==='/api/logout'?null:id))));return;
   }
   const file=files[path];if(!file||!['GET','HEAD'].includes(req.method)){res.writeHead(404).end();return;}
   res.setHeader('Cache-Control','no-cache');res.setHeader('Content-Type',file.endsWith('.ttf')?'font/ttf':file.endsWith('.txt')?'text/plain; charset=utf-8':file.endsWith('.css')?'text/css':file.endsWith('.js')?'text/javascript':'text/html; charset=utf-8');
   const contents=await readFile(new URL('./dist/'+file,import.meta.url));res.end(req.method==='HEAD'?undefined:contents);
  }catch(error){if(!res.headersSent){res.setHeader('Content-Type','application/json; charset=utf-8');res.writeHead(error.status??500)}res.end(JSON.stringify({error:error.status?error.message:'Serverfehler. Bitte erneut versuchen.'}))}
 });
 if(liveReload&&!production){watcher=watch(new URL('./dist/',import.meta.url),{recursive:true},()=>{clearTimeout(reloadTimer);reloadTimer=setTimeout(()=>{for(const client of clients)client.write('data: reload\n\n')},250)});server.on('close',()=>{watcher.close();clearTimeout(reloadTimer);for(const client of clients)client.end()})}
 server.requestTimeout=15000;server.headersTimeout=10000;return server;
}
