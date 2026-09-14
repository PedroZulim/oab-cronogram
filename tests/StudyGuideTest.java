package br.com.pedrozulim.oab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StudyGuideTest {
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        require(StudyGuide.forDay("DIA 55: ___\nDIA OFF\nDIA 56: ___\nSimulado",55).equals("DIA OFF"),"Shared page: rest");
        require(StudyGuide.forDay("DIA 55: ___\nDIA OFF\nDIA 56: ___\nSimulado",56).equals("Simulado"),"Shared page: mock");
        require(StudyGuide.forDay("SUMÁRIO\nDIA 43: ___\n4\nDIA 44: ___\n5\nDIA 43: ___\nÉTICA",43).equals("ÉTICA"),"Skip contents");
        require(StudyGuide.forDay("DIA 69 - 73: ___\nRECESSO",71).equals("RECESSO"),"Day range");
        require(StudyGuide.paragraphs("→ Frase que\ncontinua aqui.\n-\nSegundo ponto").equals("• Frase que continua aqui.\n• Segundo ponto"),"Wrapped bullets");
        if(args.length>0) {
            Path folder=Path.of(args[0]);
            for(int n=1;n<=73;n++) {
                String raw=Files.readString(folder.resolve(n+".txt"));
                List<StudyGuide.Topic> topics=StudyGuide.parse(raw,n,Files.readString(folder.resolve((n+1)+".txt")));
                require(!topics.isEmpty(),"Missing day "+n);
                for(StudyGuide.Topic topic:topics) {
                    require(!topic.title.isEmpty(),"Missing title "+n);
                    if(StudyGuide.forDay(raw,n).contains("Tema 1:")) {
                        require(!topic.title.matches("Tema \\d+"),"Unrecognized title "+n);
                        require(topic.sections.stream().filter(s->s.title.matches("[1-4] ·.*")).count()==4,"Four stages on day "+n+": "+topic.title);
                    }
                }
            }
            List<StudyGuide.Topic> first=StudyGuide.parse(Files.readString(folder.resolve("1.txt")),1);
            require(first.size()==2,"Two themes on day 1");
            require(first.get(0).title.equals("TEORIA DA CONSTITUIÇÃO"),"Reflow title");
            require(first.get(0).sections.stream().anyMatch(s->s.title.equals("Referências nos livros")&&s.content.contains("7ª edição")),"References across pages");
            require(first.get(1).sections.stream().anyMatch(s->s.content.contains("15 QUESTÕES")),"Second theme question target");
        }
        System.out.println("StudyGuide tests passed");
    }
}
