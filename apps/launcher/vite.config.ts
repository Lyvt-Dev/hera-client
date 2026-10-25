import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { catalogPlugin } from './catalog-plugin';
export default defineConfig(({mode}) => {
 const env=loadEnv(mode,process.cwd(),'');
 return { plugins: [react(), tailwindcss(), catalogPlugin(env.CURSEFORGE_API_KEY??process.env.CURSEFORGE_API_KEY)], server: { port: 1420, strictPort: true, watch: { ignored: ['**/src-tauri/**'] } }, clearScreen: false };
});
