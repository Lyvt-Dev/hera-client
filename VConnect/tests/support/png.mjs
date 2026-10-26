import {deflateSync,crc32} from 'node:zlib';
export function pngChunk(type,data){const b=Buffer.alloc(data.length+12);b.writeUInt32BE(data.length);b.write(type,4);data.copy(b,8);b.writeUInt32BE(crc32(b.subarray(4,-4)),b.length-4);return b}
export function makePng(width=512,height=512,{metadata=false,extra=0,filter=0}={}){
 const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(width);ihdr.writeUInt32BE(height,4);ihdr[8]=8;ihdr[9]=6;
 const row=width*4+1,raw=Buffer.alloc(row*height+extra);
 for(let y=0;y<height;y++){raw[y*row]=filter;for(let x=0;x<width;x++){const n=y*row+1+x*4;raw[n]=50+Math.floor(110*x/width);raw[n+1]=70+Math.floor(50*y/height);raw[n+2]=140+Math.floor(70*x/width);raw[n+3]=255;if(Math.hypot(x-width*.5,y-height*.5)<Math.min(width,height)*.22){raw[n]=208;raw[n+1]=184;raw[n+2]=242;}}}
 return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),pngChunk('IHDR',ihdr),...(metadata?[pngChunk('tEXt',Buffer.from('Comment\0PRIVATE metadata'))]:[]),pngChunk('IDAT',deflateSync(raw)),pngChunk('IEND',Buffer.alloc(0))]);
}
export const imageData=buffer=>'data:image/png;base64,'+buffer.toString('base64');
