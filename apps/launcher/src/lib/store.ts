import { create } from 'zustand';
export type Page = 'Übersicht' | 'Profile' | 'Mods' | 'Versionen' | 'Downloads' | 'Einstellungen' | 'Account';
export const useUi = create<{ page: Page; selected: string | null; navigate: (page: Page) => void; select: (id: string) => void }>(set => ({
  page:'Übersicht', selected:null, navigate: page => set({ page }), select: selected => set({ selected }),
}));
