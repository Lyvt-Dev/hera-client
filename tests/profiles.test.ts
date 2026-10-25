import { describe,it,expect } from 'vitest';
import { profileSchema,settingsSchema } from '../packages/shared-types/src';
const profile={id:'f1854701-8b0d-4eba-a288-c080932a5667',name:'Survival',version:'1.21.1',loader:'vanilla',memoryMb:4096,javaPath:'java'};
describe('profile boundary',()=>{
 it('round trips exported profiles',()=>expect(profileSchema.parse(JSON.parse(JSON.stringify(profile)))).toEqual(profile));
 it.each([{id:'../../outside'},{memoryMb:32},{loader:'neoforge'},{version:'1.8.9'},{name:' '},{javaPath:'java\0bad'},{javaPath:'powershell.exe'},{javaPath:'./java'}])('rejects unsafe or unsupported input %j',patch=>expect(profileSchema.safeParse({...profile,...patch}).success).toBe(false));
 it.each(['fabric','forge'])('accepts mod profile loader %s',loader=>expect(profileSchema.safeParse({...profile,loader}).success).toBe(true));
 it.each(['C:\\Program Files\\Java\\bin\\java.exe','/usr/bin/java'])('accepts absolute Java paths %s',javaPath=>expect(profileSchema.safeParse({...profile,javaPath}).success).toBe(true));
 it('normalizes profile names',()=>expect(profileSchema.parse({...profile,name:' Survival '}).name).toBe('Survival'));
 it('rejects imported settings outside the UI range',()=>expect(settingsSchema.safeParse({reducedMotion:false,accent:'violet',scale:100}).success).toBe(false));
});
