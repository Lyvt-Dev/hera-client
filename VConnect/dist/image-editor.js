import {uiIcons} from './ui.js';
export async function cropImage(file,kind){
 if(!file||!['image/png','image/jpeg','image/webp'].includes(file.type))throw new Error('Bitte ein PNG-, JPG- oder WebP-Bild auswählen.');
 if(file.size>8_000_000)throw new Error('Das Originalbild darf höchstens 8 MB groß sein.');
 let bitmap;try{bitmap=await createImageBitmap(file,{imageOrientation:'from-image'})}catch{throw new Error('Dieses Bild konnte nicht gelesen werden.')}
 if(bitmap.width*bitmap.height>40_000_000||bitmap.width>16000||bitmap.height>16000){bitmap.close();throw new Error('Das Bild ist zu groß. Bitte vorher auf höchstens 40 Megapixel verkleinern.')}
 const avatar=kind==='avatar';const dialog=document.createElement('dialog');dialog.id='cropDialog';dialog.setAttribute('aria-label',avatar?'Profilbild zuschneiden':'Banner zuschneiden');
 dialog.innerHTML=`<div class="crop-header"><div><span class="eyebrow">DEIN AUSSCHNITT</span><h2>${avatar?'Profilbild':'Banner'} anpassen</h2></div><button class="icon-control" id="cancelCrop" aria-label="Zuschneiden abbrechen">${uiIcons.close}</button></div><div class="crop-viewport ${avatar?'crop-avatar-preview':''}"><canvas width="${avatar?512:900}" height="${avatar?512:300}" aria-label="Vorschau des Bildausschnitts"></canvas></div><p class="crop-help">Ziehe das Bild oder passe den Ausschnitt mit den Reglern an.</p><div class="crop-ranges"><label>Zoom<input id="cropZoom" type="range" min="1" max="3" step="0.01" value="1"></label><label>Horizontal<input id="cropX" type="range" min="0" max="100" step="1" value="50"></label><label>Vertikal<input id="cropY" type="range" min="0" max="100" step="1" value="50"></label></div><p id="cropError" role="alert"></p><div class="crop-footer"><span>${avatar?'512 × 512 px':'1500 × 500 px'} · Metadaten werden entfernt</span><button class="primary" id="applyCrop">Ausschnitt übernehmen ${uiIcons.check}</button></div>`;
 document.body.append(dialog);const canvas=dialog.querySelector('canvas'),zoom=dialog.querySelector('#cropZoom'),x=dialog.querySelector('#cropX'),y=dialog.querySelector('#cropY');
 function draw(target){const ctx=target.getContext('2d');const scale=Math.max(target.width/bitmap.width,target.height/bitmap.height)*Number(zoom.value);const width=bitmap.width*scale,height=bitmap.height*scale;ctx.clearRect(0,0,target.width,target.height);ctx.drawImage(bitmap,-(width-target.width)*Number(x.value)/100,-(height-target.height)*Number(y.value)/100,width,height)}
 dialog.querySelectorAll('input').forEach(input=>input.oninput=()=>draw(canvas));let drag=null;
 canvas.onpointerdown=event=>{drag={x:event.clientX,y:event.clientY,h:Number(x.value),v:Number(y.value)};canvas.setPointerCapture(event.pointerId)};
 canvas.onpointermove=event=>{if(!drag)return;x.value=String(Math.max(0,Math.min(100,drag.h-(event.clientX-drag.x)/2)));y.value=String(Math.max(0,Math.min(100,drag.v-(event.clientY-drag.y)/2)));draw(canvas)};
 canvas.onpointerup=canvas.onpointercancel=()=>{drag=null};
 return await new Promise((resolve,reject)=>{let result=null;dialog.querySelector('#cancelCrop').onclick=()=>dialog.close();dialog.onclose=()=>{bitmap.close();dialog.remove();if(result)resolve(result);else reject(Object.assign(new Error('Auswahl abgebrochen.'),{name:'AbortError'}))};dialog.querySelector('#applyCrop').onclick=()=>{try{const output=document.createElement('canvas');output.width=avatar?512:1500;output.height=avatar?512:500;draw(output);const url=output.toDataURL('image/png');if(url.length>5_600_000)throw new Error('Dieser Ausschnitt ist zu groß. Bitte ein anderes Bild wählen.');result=url;dialog.close()}catch(error){dialog.querySelector('#cropError').textContent=error.message}};draw(canvas);dialog.showModal()});
}

export async function preparePostImage(file){
 if(!file||!['image/png','image/jpeg','image/webp'].includes(file.type))throw new Error('Bitte ein PNG-, JPG- oder WebP-Bild auswählen.');
 if(file.size>8_000_000)throw new Error('Das Originalbild darf höchstens 8 MB groß sein.');
 let bitmap;try{bitmap=await createImageBitmap(file,{imageOrientation:'from-image'})}catch{throw new Error('Dieses Bild konnte nicht gelesen werden.')}
 try{if(bitmap.width*bitmap.height>40_000_000||bitmap.width>16000||bitmap.height>16000)throw new Error('Bitte das Bild vorher auf höchstens 40 Megapixel verkleinern.');
 const scale=Math.min(1,1600/bitmap.width,1600/bitmap.height,Math.sqrt(850000/(bitmap.width*bitmap.height))),canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.floor(bitmap.width*scale));canvas.height=Math.max(1,Math.floor(bitmap.height*scale));canvas.getContext('2d').drawImage(bitmap,0,0,canvas.width,canvas.height);const data=canvas.toDataURL('image/png');if(data.length>5_600_000)throw new Error('Das Bild ist zu groß. Bitte ein kleineres Bild wählen.');return data;
 }finally{bitmap.close()}
}
