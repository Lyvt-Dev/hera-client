import { describe,it,expect } from 'vitest';
import { fileProgress,transferPhase } from '../apps/launcher/src/lib/launch-state';
describe('native launch feedback',()=>{
 it('uses the actual native stage without guessing byte transfers',()=>{expect(transferPhase('Microsoft-Konto prüfen')).toBe('preparing');expect(transferPhase('Java 21 vorbereiten')).toBe('installing');expect(transferPhase('Minecraft-Dateien prüfen und installieren')).toBe('installing');expect(transferPhase('Dateien herunterladen')).toBe('downloading');expect(transferPhase('Spielprozess starten')).toBe('launching');});
 it('keeps unknown totals indeterminate',()=>expect(fileProgress({stage:'Start',completed:0,total:0})).toEqual({total:0,completed:0,percent:null}));
 it('bounds stage-local counts and does not accumulate stages',()=>{expect(fileProgress({stage:'Assets',completed:4500,total:4000})).toEqual({total:4000,completed:4000,percent:100});expect(fileProgress({stage:'Java',completed:1,total:100}).percent).toBe(1);expect(fileProgress({stage:'Assets',completed:-1,total:10}).completed).toBe(0);});
});
