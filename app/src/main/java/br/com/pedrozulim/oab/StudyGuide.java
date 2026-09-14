package br.com.pedrozulim.oab;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation model for the supplied PDF text. The original asset remains intact. */
final class StudyGuide {
    static final class Section {
        final String title, content;
        final boolean secondary;
        Section(String title, String content, boolean secondary) {
            this.title=title; this.content=content; this.secondary=secondary;
        }
    }
    static final class Topic {
        String title;
        final List<Section> sections=new ArrayList<>();
        Topic(String title) { this.title=title; }
    }
    private static final Pattern DAY=Pattern.compile("(?m)^DIA\\s+(\\d+)(?:\\s*-\\s*(\\d+))?:[^\\n]*");
    private static final Pattern TOPIC=Pattern.compile("(?mi)^Tema\\s+\\d+\\s*:");
    private static final Pattern STEP=Pattern.compile("(?m)^([1-4])[º°o]\\s*PASSO\\s*:");

    static String forDay(String raw, int day) {
        String clean=raw.replace("\r", "").replace("\u200b", "")
                .replaceAll("(?m)^Página \\d+\\s*$", "");
        Matcher headers=DAY.matcher(clean);
        int start=-1, end=clean.length();
        while(headers.find()) {
            int first=Integer.parseInt(headers.group(1));
            int last=headers.group(2)==null?first:Integer.parseInt(headers.group(2));
            // Last matching header skips a duplicated table of contents.
            if(day>=first&&day<=last) { start=headers.end(); end=clean.length(); }
            else if(start>=0&&end==clean.length()) end=headers.start();
        }
        return (start<0?clean:clean.substring(start,end)).trim();
    }

    static List<Topic> parse(String raw, int day) {
        String clean=forDay(raw,day);
        List<Topic> result=new ArrayList<>();
        Matcher topics=TOPIC.matcher(clean);
        List<Integer> starts=new ArrayList<>(), ends=new ArrayList<>();
        while(topics.find()) { starts.add(topics.start()); ends.add(topics.end()); }
        if(starts.isEmpty()) {
            Topic topic=new Topic("Orientações do dia");
            int review=clean.indexOf("TEMAS A SEREM REVISADOS");
            if(review>=0) {
                topic.sections.add(new Section("Como revisar",paragraphs(clean.substring(0,review)),false));
                topic.sections.add(new Section("Temas e referências para revisar",paragraphs(clean.substring(review).replace("TEMAS A SEREM REVISADOS","")),false));
            } else topic.sections.add(new Section("Roteiro", paragraphs(clean),false));
            result.add(topic);
            return result;
        }
        for(int i=0;i<starts.size();i++) {
            String block=clean.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):clean.length()).trim();
            Matcher steps=STEP.matcher(block);
            List<Integer> stepStarts=new ArrayList<>(), stepEnds=new ArrayList<>(), numbers=new ArrayList<>();
            while(steps.find()) { stepStarts.add(steps.start()); stepEnds.add(steps.end()); numbers.add(Integer.parseInt(steps.group(1))); }
            Topic topic=new Topic("Tema "+(i+1));
            String header=block.substring(0,stepStarts.isEmpty()?block.length():stepStarts.get(0));
            Matcher name=Pattern.compile("(?s)\\(consideramos provas\\s+a partir de 2018\\)\\s*(.*?)\\s*TOTAL DE ACERTOS").matcher(header);
            if(name.find()) topic.title=name.group(1).replaceAll("\\s+"," ").trim();
            else topic.sections.add(new Section("Sobre o tema",paragraphs(header),false));
            StringBuilder history=new StringBuilder();
            Matcher exams=Pattern.compile("(?m)^OAB[^\\n]*").matcher(header);
            while(exams.find()) history.append(exams.group()).append('\n');
            for(int j=0;j<stepStarts.size();j++) {
                int number=numbers.get(j);
                String content=block.substring(stepEnds.get(j),j+1<stepStarts.size()?stepStarts.get(j+1):block.length()).trim();
                String[] labels={"", "1 · Teoria e pontos importantes", "2 · Questões", "3 · Lei seca", "4 · Aprendizados e erros"};
                // Replace repeated PDF navigation instructions with readable app copy.
                int instructionEnd=content.indexOf(')');
                if(number<=3&&instructionEnd>=0) {
                    String[] instructions={"", "Leia o resumo do tema nos materiais de apoio.",
                            "Resolva as questões na plataforma ou no kit de livros.",
                            "Consulte os dispositivos indicados no caderno legislativo da disciplina."};
                    content=instructions[number]+"\n\n"+content.substring(instructionEnd+1).trim();
                }
                if(number==4&&content.contains("Anote aqui"))content=content.substring(content.indexOf("Anote aqui"));
                int references=content.indexOf("Páginas do Kit");
                String books=references<0?"":content.substring(references);
                if(references>=0) content=content.substring(0,references);
                topic.sections.add(new Section(labels[number],paragraphs(content),false));
                if(!books.isEmpty()) topic.sections.add(new Section("Referências nos livros",paragraphs(books),true));
            }
            if(history.length()>0) topic.sections.add(new Section("Histórico de cobrança",history.toString().trim(),true));
            result.add(topic);
        }
        return result;
    }

    static List<Topic> parse(String raw,int day,String nextRaw) {
        // The importer assigns a whole page to its new day. Recover the previous
        // day's continuation above that first header, without changing stored data.
        String next=nextRaw.replace("\r", "").replace("\u200b", "")
                .replaceAll("(?m)^Página \\d+\\s*$", "");
        Matcher header=DAY.matcher(next);
        if(header.find()&&!next.contains("SUMÁRIO")) {
            String continuation=next.substring(0,header.start()).trim();
            if(!continuation.isEmpty()&&!raw.contains(continuation))raw+="\n"+continuation;
        }
        return parse(raw,day);
    }

    static String paragraphs(String value) {
        StringBuilder out=new StringBuilder();
        String previous="";
        boolean bulletPending=false;
        for(String raw:value.split("\n")) {
            String line=raw.trim();
            if(line.matches("(?:CHECK|_+%?|(?:TOTAL|NÚMERO) DE (?:ACERTOS|ERROS):.*|NOTA FINAL:.*|RENDIMENTO DO SIMULADO:|ARTIGOS MAIS|IMPORTANTES)")) continue;
            if(line.equals("-")||line.equals("→")) { bulletPending=true; continue; }
            if(bulletPending) { line="• "+line; bulletPending=false; }
            line=line.replaceFirst("^(?:→|-)\\s*", "• ");
            boolean boundary=line.isEmpty()||previous.isEmpty()||line.startsWith("• ")
                    ||line.matches("(?i)^(?:Art\\.?|Arts\\.?|Súmula|Lei |OJ |[4-7][ªº] edição|META DE|Páginas do|Pontos importantes|TEMAS A).*")
                    ||line.equals(line.toUpperCase(java.util.Locale.ROOT))||previous.endsWith(":")||previous.endsWith(".");
            if(out.length()>0) out.append(boundary?'\n':' ');
            out.append(line); previous=line;
        }
        return out.toString().replaceAll("\n{3,}","\n\n").trim();
    }
}
