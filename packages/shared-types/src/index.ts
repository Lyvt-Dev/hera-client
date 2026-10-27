import { z } from 'zod';
export const profileSchema = z.object({
  id: z.uuid(), name: z.string().trim().min(1).max(48), version: z.string().regex(/^[A-Za-z0-9][A-Za-z0-9._+ \-]{0,79}$/).refine(v=>!v.includes('..'), 'Ungültige Versions-ID'),
  loader: z.enum(['vanilla','fabric','forge']), memoryMb: z.number().int().min(2048).max(16384),
  javaPath: z.string().min(1).max(1024).refine(s => !/[\r\n\0]/.test(s) && /^(?:java(?:\.exe)?|(?:[A-Za-z]:[\\/]|\/).*[/\\]java(?:\.exe)?)$/i.test(s), 'Java-Pfad muss auf java oder java.exe zeigen'),
});
export type Profile = z.infer<typeof profileSchema>;
export const settingsSchema = z.object({ reducedMotion: z.boolean(), accent: z.enum(['orange','violet','cyan']), scale: z.number().min(0.85).max(1.2) }).strict();
export type Settings = z.infer<typeof settingsSchema>;
export interface Account { id: string; name: string }
export interface Transfer { stage: string; completed: number; total: number }
export interface Runtime { java: string | null; platform: string; authConfigured: boolean }
export interface Version { id: string; type: string; releaseTime: string }
export interface LoaderOptions {fabric:string|null;forge:string|null}
export const defaultSettings: Settings = { reducedMotion: false, accent: 'orange', scale: 1 };
export const modSourceSchema = z.enum(['modrinth','curseforge']);
export type ModSource = z.infer<typeof modSourceSchema>;
export const modFileSchema = z.object({ filename:z.string().regex(/^[a-zA-Z0-9][a-zA-Z0-9._+\-]*\.jar$/),url:z.url(),size:z.number().int().positive().max(128*1024*1024),sha1:z.string().regex(/^[a-f0-9]{40}$/i) });
export const modVersionSchema = z.object({ id:z.string(),projectId:z.string(),name:z.string(),number:z.string(),gameVersions:z.array(z.string()),loaders:z.array(z.string()),release:z.enum(['release','beta','alpha']),file:modFileSchema.nullable(),dependencies:z.array(z.object({projectId:z.string().nullable(),versionId:z.string().nullable(),kind:z.enum(['required','optional','incompatible','embedded'])})) });
export type ModVersion = z.infer<typeof modVersionSchema>;
export const modProjectSchema = z.object({ id:z.string(),slug:z.string(),title:z.string(),description:z.string(),author:z.string(),downloads:z.number(),iconUrl:z.string().nullable(),url:z.url() });
export type ModProject = z.infer<typeof modProjectSchema>;
export const installedModSchema = z.object({ source:modSourceSchema,projectId:z.string(),versionId:z.string(),title:z.string(),version:z.string(),filename:modFileSchema.shape.filename,sha1:modFileSchema.shape.sha1,size:modFileSchema.shape.size });
export type InstalledMod = z.infer<typeof installedModSchema>;
