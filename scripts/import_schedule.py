"""Importa os PDFs fornecidos. Uso: python scripts/import_schedule.py pasta_dos_pdfs."""
import json, re, sys
from pathlib import Path
import pymupdf

def clean(text):
    return '\n'.join(s.strip() for s in text.splitlines() if s.strip())

def extract(folder):
    files = list(Path(folder).glob('*.pdf'))
    calendar = next(p for p in files if '120D' in p.name)
    doc = pymupdf.open(calendar)
    rects = [(148,85,324,269),(349,85,527,269),(550,85,732,269),(48,350,225,538),(247,350,425,538),(448,350,627,538),(650,350,832,538)]
    days=[]
    for week,page in enumerate(list(doc)[1:],1):
        for offset,rect in enumerate(rects):
            text=clean(page.get_text(clip=pymupdf.Rect(rect),sort=True)).replace('PrincípiosPrincípios TributáriosTributários', 'Princípios Tributários')
            lines=text.splitlines()
            title=lines[0] if lines else 'Revisão'
            kind='study'
            if any(w in text.upper() for w in ['OFF','RECESSO','FERIADO']): kind='rest'
            elif 'SIMULADO' in text: kind='mock'
            elif 'Revisão' in text or 'REVISÃO' in text: kind='review'
            elif 'APROVAÇÃO' in text: kind='exam'
            days.append(dict(day=len(days)+1,week=week,title=title,summary=text,kind=kind,source=f'{calendar.name} · página {week+1}',details='',detailSource=''))
    for pdf in files:
        if pdf==calendar: continue
        d=pymupdf.open(pdf)
        current=[]
        for page_no,page in enumerate(d,1):
            if page_no<4: continue
            text=page.get_text()
            text=re.sub(r'    Cronograma.*?Proibido o repasse!\s*\d+\s*','',text,flags=re.S)
            matches=list(re.finditer(r'DIA\s+(\d+)(?:\s*-\s*(\d+))?\s*:',text))
            if matches:
                current=[]
                for m in matches:
                    current.extend(range(int(m[1]),int(m[2] or m[1])+1))
            for n in current:
                if 1<=n<=len(days):
                    days[n-1]['details']+=f'\n\nPágina {page_no}\n'+clean(text)
                    days[n-1]['detailSource']=pdf.name
    assert len(days)==126
    assert all(d['summary'] for d in days)
    return {'schemaVersion':1,'mainDays':120,'sourceExam':46,'days':days}

if __name__=='__main__':
    out=Path(__file__).resolve().parents[1]/'app/src/main/assets/schedule.json'
    data=extract(sys.argv[1]);out.write_text(json.dumps(data,ensure_ascii=False,indent=2))
    print(f"{len(data['days'])} dias; {sum(bool(d['details']) for d in data['days'])} com detalhes")
