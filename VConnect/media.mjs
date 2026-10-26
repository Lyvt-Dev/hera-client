import {inflateSync,deflateSync,crc32} from 'node:zlib';
const signature=Buffer.from([137,80,78,71,13,10,26,10]);
function invalid(){throw Object.assign(new Error('Das Bild ist ungültig. Bitte PNG, JPG oder WebP erneut auswählen.'),{status:400})}
function chunk(type,data){const output=Buffer.alloc(data.length+12);output.writeUInt32BE(data.length);output.write(type,4,4,'ascii');data.copy(output,8);output.writeUInt32BE(crc32(output.subarray(4,-4)),output.length-4);return output}
export function validateProfileImage(value,kind){
 if(typeof value!=='string'||value.length>5_600_000||!value.startsWith('data:image/png;base64,'))invalid();const encoded=value.slice(22);if(!/^[A-Za-z0-9+/]+={0,2}$/.test(encoded))invalid();const data=Buffer.from(encoded,'base64');if(data.length>4_000_000||data.toString('base64')!==encoded||!data.subarray(0,8).equals(signature))invalid();
 let offset=8,header=null,ended=false,count=0;const compressed=[];
 while(offset<data.length){if(++count>256||offset+12>data.length)invalid();const length=data.readUInt32BE(offset);if(length>4_000_000||offset+12+length>data.length)invalid();const type=data.toString('ascii',offset+4,offset+8),body=data.subarray(offset+8,offset+8+length);if(crc32(data.subarray(offset+4,offset+8+length))!==data.readUInt32BE(offset+8+length))invalid();if(!header&&type!=='IHDR')invalid();
 if(type==='IHDR'){if(header||length!==13)invalid();header=Buffer.from(body);}else if(type==='IDAT')compressed.push(body);else if(type==='IEND'){if(length!==0)invalid();ended=true;offset+=12;break;}else if(type==='acTL'||type==='fcTL'||type==='fdAT'||!/^[a-zA-Z]{4}$/.test(type)||(type[0]===type[0].toUpperCase()&&type!=='PLTE'))invalid();offset+=length+12;
 }
 if(!header||!ended||offset!==data.length||!compressed.length)invalid();const width=header.readUInt32BE(0),height=header.readUInt32BE(4),channels=header[9]===6?4:header[9]===2?3:0;
 const expected=kind==='avatar'?[512,512]:kind==='banner'?[1500,500]:[];const validDimensions=kind==='post'?width>0&&height>0&&width<=1600&&height<=1600&&width*height<=2_000_000:width===expected[0]&&height===expected[1];if(!validDimensions||header[8]!==8||!channels||header[10]!==0||header[11]!==0||header[12]!==0)invalid();
 const row=width*channels+1,size=row*height;let pixels;try{pixels=inflateSync(Buffer.concat(compressed),{maxOutputLength:size})}catch{invalid()}if(pixels.length!==size)invalid();for(let y=0;y<height;y++)if(pixels[y*row]>4)invalid();
 // Re-encode essential raster chunks only: strip metadata, animation and trailing zlib data.
 return Buffer.concat([signature,chunk('IHDR',header),chunk('IDAT',deflateSync(pixels)),chunk('IEND',Buffer.alloc(0))]);
}
