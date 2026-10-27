import type { Transfer } from '../../../../packages/shared-types/src';
export type LaunchPhase = 'idle' | 'preparing' | 'downloading' | 'installing' | 'launching' | 'running' | 'failed';
export function transferPhase(stage:string):LaunchPhase {
 if(/Spielprozess starten/i.test(stage))return 'launching';
 if(/einrichten|install|entpack|Java|Forge|Fabric/i.test(stage))return 'installing';
 if(/Bibliotheken|Assets|Dateien|herunter|Download/i.test(stage))return 'downloading';
 return 'preparing';
}
export const phaseLabel:Record<LaunchPhase,string>={idle:'Dateien werden beim Start geprüft',preparing:'Start vorbereiten',downloading:'Dateien herunterladen',installing:'Spiel einrichten',launching:'Minecraft starten',running:'Minecraft läuft',failed:'Start fehlgeschlagen'};
export function fileProgress(t:Transfer){const total=Math.max(0,t.total),completed=Math.max(0,Math.min(t.completed,total));return {total,completed,percent:total?Math.round(completed/total*100):null};}
export interface Activity {id:string;kind:'launch'|'install';name:string;profileId:string;started:number;finished?:number;result:'active'|'complete'|'failed';error?:string}
