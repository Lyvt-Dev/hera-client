import { create } from 'zustand';
import { persist } from 'zustand/middleware';
export type Page = 'Übersicht' | 'Profile' | 'Mods' | 'Versionen' | 'Downloads' | 'Einstellungen' | 'Account' | 'Capes';
type UiState = { page:Page; selected:string|null; compact:boolean; toggleCompact:()=>void; navigate:(page:Page)=>void; select:(id:string|null)=>void };
export const useUi = create<UiState>()(persist(set=>({
 page:'Übersicht', selected:null, compact:false,toggleCompact:()=>set(s=>({compact:!s.compact})),navigate:page=>set({page}), select:selected=>set({selected}),
}), {name:'hera.ui',partialize:state=>({selected:state.selected,compact:state.compact})}));
