export const escapeHtml=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const themes=['lilac','ocean','rose','forest','sunset','mono'];
export const symbols={initial:'',star:'✦',moon:'☾',heart:'♡',spark:'✧',flower:'❀',diamond:'◇',bolt:'ϟ'};
export const svg=paths=>'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+paths+'</svg>';
export const uiIcons={heart:svg('<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>'),bookmark:svg('<path d="M6 3h12v18l-6-4-6 4Z"/>'),share:svg('<path d="M12 16V3m-5 5 5-5 5 5M5 13v7h14v-7"/>'),pen:svg('<path d="m15 4 5 5M4 20l4-1L21 6l-5-5L3 14Z"/>'),close:svg('<path d="m6 6 12 12M18 6 6 18"/>'),arrow:svg('<path d="M5 12h14m-6-6 6 6-6 6"/>'),check:svg('<path d="m5 12 4 4L19 6"/>')};
export function themeClass(p){return 'theme-'+(themes.includes(p?.theme)?p.theme:'lilac')}
export function imageSrc(value){return typeof value==='string'&&(/^\/media\/[a-f0-9-]{36}$/.test(value)||/^data:image\/png;base64,[A-Za-z0-9+/]+=*$/.test(value))?value:''}
export function avatar(p,size=''){const source=p?.removeAvatar?'':imageSrc(p?.avatarUpload||p?.avatarImage);return `<span class="avatar ${size} ${themeClass(p)}" aria-hidden="true">${source?`<img src="${escapeHtml(source)}" alt="" decoding="async">`:escapeHtml(symbols[p?.avatar]||p?.name?.[0]||'V')}</span>`}
export const tagList=value=>String(value||'').split(',').map(t=>t.trim()).filter(Boolean);
