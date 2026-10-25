import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
export function Modal({ title, close, children }: { title:string; close:()=>void; children:ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => { ref.current?.showModal(); }, []);
  return <dialog ref={ref} onCancel={close} onClick={e => { if(e.target === e.currentTarget) close(); }}><div className="dialog-head"><h2>{title}</h2><button className="icon-button" onClick={close} aria-label="Schließen"><X size={18}/></button></div>{children}</dialog>;
}
export function Toggle({ checked, onChange, label }: { checked:boolean; onChange:(v:boolean)=>void; label:string }) {
  return <button type="button" role="switch" aria-checked={checked} aria-label={label} className={`toggle ${checked?'on':''}`} onClick={()=>onChange(!checked)}><span/></button>;
}
export function Empty({ title, children }: { title:string; children:ReactNode }) {
  return <div className="empty"><div className="empty-mark">◇</div><h3>{title}</h3><p>{children}</p></div>;
}
