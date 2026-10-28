import { useEffect, useRef, useId, type ReactNode } from 'react';
import { Inbox, X } from 'lucide-react';
import { fileProgress } from '../lib/launch-state';
export function Modal({ title, close, children }: { title:string; close:()=>void; children:ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId=useId();
  useEffect(() => {const dialog=ref.current,root=document.documentElement,overflow=root.style.overflow;root.style.overflow='hidden';dialog?.showModal();dialog?.querySelector<HTMLElement>('input:not([type=hidden]),select,textarea')?.focus();return()=>{dialog?.close();root.style.overflow=overflow;};}, []);
  return <dialog ref={ref} aria-labelledby={titleId} onCancel={e=>{e.preventDefault();close();}}><div className="dialog-head"><h2 id={titleId}>{title}</h2><button type="button" className="icon-button" onClick={close} aria-label="Schließen"><X size={18}/></button></div>{children}</dialog>;
}
export function Toggle({ checked, onChange, label, disabled=false }: { checked:boolean; onChange:(v:boolean)=>void; label:string;disabled?:boolean }) {
  return <button type="button" role="switch" aria-checked={checked} aria-label={label} disabled={disabled} className={`toggle ${checked?'on':''}`} onClick={()=>onChange(!checked)}><span/></button>;
}
export function Empty({ title, children }: { title:string; children:ReactNode }) {
  return <div className="empty"><span className="empty-symbol" aria-hidden="true"><Inbox size={20}/></span><h3>{title}</h3><p>{children}</p></div>;
}
export function PageHeader({title,description,actions}:{title:string;description?:string;actions?:ReactNode}){return <div className="page-heading"><div><h1>{title}</h1>{description&&<p>{description}</p>}</div>{actions&&<div className="button-row">{actions}</div>}</div>;}
export function ProgressBar({completed,total,label}:{completed:number;total:number;label:string}){const progress=fileProgress({stage:label,completed,total});return <div className="progress-block"><div><span>{label}</span>{progress.total>0&&<small>{progress.completed.toLocaleString('de-DE')} / {progress.total.toLocaleString('de-DE')} Dateien</small>}</div>{progress.total>0?<progress aria-label={label} max={progress.total} value={progress.completed}/>:<progress aria-label={label}/>}</div>;}
export function Tabs({id,label,items,value,onChange}:{id:string;label:string;items:{value:string;label:string;count?:number}[];value:string;onChange:(v:string)=>void}){
 return <div className="page-tabs" role="tablist" aria-label={label} onKeyDown={e=>{const buttons=Array.from(e.currentTarget.querySelectorAll<HTMLButtonElement>('[role=tab]'));const index=buttons.indexOf(e.target as HTMLButtonElement);if(index<0||!['ArrowLeft','ArrowRight','Home','End'].includes(e.key))return;e.preventDefault();const next=e.key==='Home'?0:e.key==='End'?buttons.length-1:(index+(e.key==='ArrowRight'?1:-1)+buttons.length)%buttons.length;buttons[next].click();buttons[next].focus();}}>{items.map(item=><button key={item.value} id={`${id}-${item.value}-tab`} role="tab" aria-selected={item.value===value} aria-controls={`${id}-${item.value}`} tabIndex={item.value===value?0:-1} onClick={()=>onChange(item.value)}>{item.label}{item.count!==undefined&&<span className="count">{item.count}</span>}</button>)}</div>;
}
