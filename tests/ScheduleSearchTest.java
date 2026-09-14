package br.com.pedrozulim.oab;

public class ScheduleSearchTest {
    public static void main(String[] args) {
        for (String query : new String[]{"1", "Dia 1", " DIA 01 ", "dia1", "dia   1"}) {
            for (int day = 1; day <= 126; day++) {
                expect(day == 1, day, "Revisão", "Revisar conteúdo do Dia 1", query);
            }
        }
        expect(true, 10, "Civil", "Tema", "Dia 10");
        expect(true, 126, "Exame", "Tema", "126");
        expect(false, 1, "Dia 999", "Dia 999", "Dia 999");
        expect(false, 1, "Tema", "Tema", "999999999999999999999999999");
        expect(false, 1, "Tema", "Tema", "0");
        expect(true, 1, "Ética", "Tema", " etica ");
        expect(true, 1, "Tema", "Direito Constitucional", "CONSTITUCIONAL");
        expect(true, 1, "Tema", "Tema", "   ");
        expect(false, 1, "Tema", "Tema", "inexistente");
        System.out.println("ScheduleSearch: all checks passed.");
    }

    private static void expect(boolean expected, int day, String title, String summary, String query) {
        if (ScheduleSearch.matches(day, title, summary, query) != expected) {
            throw new AssertionError("Unexpected result for query " + query + ", day " + day);
        }
    }
}
