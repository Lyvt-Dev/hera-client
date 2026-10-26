import { create } from 'zustand';
import { persist } from 'zustand/middleware';
export type Page = 'Übersicht' | 'Profile' | 'Mods' | 'Versionen' | 'Downloads' | 'Einstellungen' | 'Account';
type UiState = { page:Page; selected:string|null; navigate:(page:Page)=>void; select:(id:string)=>void };
export const useUi = create<UiState>()(persist(set=>({
 page:'Übersicht', selected:null, navigate:page=>set({page}), select:selected=>set({selected}),
}), {name:'hera.ui',partialize:state=>({selected:state.selected})}));
