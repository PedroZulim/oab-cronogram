package br.com.pedrozulim.oab;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

/** Offline planner. Stable day IDs keep progress intact when the start date changes. */
public class MainActivity extends Activity {
    private final int ink=Color.rgb(35,30,56), purple=Color.rgb(109,75,209), muted=Color.rgb(113,108,128);
    private SharedPreferences prefs;
    private JSONArray days;
    private LinearLayout body;
    private String tab="Hoje", filter="";
    private boolean pending=false;
    private int selected=0;
    private int limit(){return prefs.getBoolean("extension",false)?126:120;}
    private LocalDate start(){return LocalDate.parse(prefs.getString("start",LocalDate.now().toString()));}
    private int today(){return (int)ChronoUnit.DAYS.between(start(),LocalDate.now())+1;}
    private JSONObject day(int n){return days.optJSONObject(n-1);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private String key(int n,String field){return "day."+n+"."+field;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("oab120",MODE_PRIVATE);
        if(!prefs.contains("start"))prefs.edit().putString("start",LocalDate.now().toString()).apply();
        try {days=new JSONObject(loadSchedule()).getJSONArray("days");}
        catch(Exception e){TextView error=new TextView(this);error.setText("Não foi possível abrir o cronograma. Reinstale o aplicativo.");setContentView(error);return;}
        if(state!=null){tab=state.getString("tab","Hoje");selected=state.getInt("selected",0);}
        render();
    }
    private String loadSchedule() throws java.io.IOException {
        try (java.io.InputStream in=getAssets().open("schedule.json"); java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("tab",tab);b.putInt("selected",selected);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(5),0,dp(5));v.setLineSpacing(dp(3),1);return v;}
    private void heading(String s){TextView v=text(s,28,ink);v.setTypeface(null,Typeface.BOLD);body.addView(v);}
    private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(purple);b.setOnClickListener(v->action.run());return b;}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(18),dp(14),dp(18),dp(14));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(20));bg.setStroke(dp(1),Color.rgb(232,228,243));c.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));body.addView(c,lp);return c;}
    private void render(){
        LinearLayout root=column();root.setBackgroundColor(Color.rgb(247,246,252));root.setPadding(dp(18),dp(12),dp(18),0);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(18),insets.getSystemWindowInsetTop()+dp(12),dp(18),insets.getSystemWindowInsetBottom());return insets;});
        root.addView(text("OAB / SUA ROTINA DE APROVAÇÃO",12,purple));
        ScrollView scroll=new ScrollView(this);body=column();scroll.setFillViewport(true);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);
        for(String label:new String[]{"Hoje","Plano","Progresso","Ajustes"}){Button b=button(label,()->{tab=label;selected=0;render();});b.setTextSize(12);b.setPadding(0,0,0,0);nav.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));}
        root.addView(nav);setContentView(root);
        if(selected>0){detail(selected);return;}
        switch(tab){case "Plano":plan();break;case "Progresso":progress();break;case "Ajustes":settings();break;default:home();}
    }
    private boolean done(int n){return prefs.getBoolean(key(n,"done"),false);}
    private String date(int n){return start().plusDays(n-1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    private String title(JSONObject d){String t=d.optString("title");return t.equals("Semana de")?"Revisão":t;}
    private void home(){
        heading("Um dia de cada vez.");body.addView(text("Seu plano de "+limit()+" dias · conteúdo-base OAB 46",14,muted));
        int n=today();
        if(n<1){body.addView(text("Seu plano começa em "+date(1)+". Você já pode explorar os conteúdos.",17,ink));n=1;}
        else if(n>limit()){body.addView(text("O período terminou. Revise as pendências no Plano.",17,ink));n=limit();}
        int complete=0;for(int i=1;i<=limit();i++)if(done(i))complete++;
        LinearLayout c=card();c.addView(text(complete+" / "+limit()+" dias concluídos",22,purple));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(limit());bar.setProgress(complete);c.addView(bar);
        dayCard(n);body.addView(button("Ver cronograma completo",()->{tab="Plano";render();}));
    }
    private void dayCard(int n){JSONObject d=day(n);LinearLayout c=card();c.addView(text("DIA "+n+" · SEMANA "+d.optInt("week")+" · "+date(n),12,muted));c.addView(text(title(d),21,ink));c.addView(text(d.optString("summary"),15,muted));c.addView(button(done(n)?"Concluído · ver detalhes":"Abrir estudo",()->{selected=n;render();}));}
    private void plan(){
        heading("Seu caminho");body.addView(text("Pesquise matérias, temas ou o número do dia.",14,muted));
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Buscar no cronograma");search.setText(filter);body.addView(search);
        CheckBox check=new CheckBox(this);check.setText("Mostrar apenas pendências");check.setChecked(pending);body.addView(check);
        LinearLayout list=column();body.addView(list);
        Runnable refresh=()->{list.removeAllViews();int week=0;for(int i=1;i<=limit();i++){JSONObject d=day(i);if(pending&&done(i))continue;if(!ScheduleSearch.matches(i,title(d),d.optString("summary"),filter))continue;int n=i;if(week!=d.optInt("week")){week=d.optInt("week");list.addView(text("SEMANA "+week,16,purple));}list.addView(button((done(i)?"✓ ":"")+"Dia "+i+" · "+title(d)+"\n"+date(i),()->{selected=n;render();}));}if(list.getChildCount()==0)list.addView(text("Nenhum dia encontrado.",16,muted));};
        search.addTextChangedListener(watcher(()->{filter=search.getText().toString();refresh.run();}));check.setOnCheckedChangeListener((v,b)->{pending=b;refresh.run();});refresh.run();
    }
    private TextWatcher watcher(Runnable r){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){r.run();}public void afterTextChanged(Editable e){}};}
    private void detail(int n){
        JSONObject d=day(n);body.addView(button("‹ Voltar",()->{selected=0;render();}));heading("Dia "+n+" · "+title(d));body.addView(text(date(n)+" · Semana "+d.optInt("week"),14,muted));
        LinearLayout c=card();c.addView(text(d.optString("summary"),18,ink));c.addView(text("Fonte: "+d.optString("source"),12,muted));
        boolean rest=d.optString("kind").equals("rest")||d.optString("kind").equals("exam");
        if(!rest){body.addView(text("Meu checklist",20,ink));for(String stage:new String[]{"Teoria / revisão","Questões","Lei seca","Revisar erros"}){CheckBox b=new CheckBox(this);b.setText(stage);b.setChecked(prefs.getBoolean(key(n,stage),false));b.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key(n,stage),on).apply());body.addView(b);}
        body.addView(text("Desempenho nas questões",20,ink));LinearLayout row=new LinearLayout(this);EditText hits=number(n,"hits","Acertos"),misses=number(n,"misses","Erros");row.addView(hits,new LinearLayout.LayoutParams(0,-2,1));row.addView(misses,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);}
        body.addView(text("Caderno de erros e anotações",20,ink));EditText notes=new EditText(this);notes.setHint("O que preciso lembrar na revisão?");notes.setMinLines(3);notes.setGravity(Gravity.TOP);notes.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);notes.setText(prefs.getString(key(n,"notes"),""));notes.addTextChangedListener(watcher(()->prefs.edit().putString(key(n,"notes"),notes.getText().toString()).apply()));body.addView(notes);
        CheckBox finished=new CheckBox(this);finished.setText(rest?"Marcar dia como concluído":"Concluir este dia de estudo");finished.setChecked(done(n));finished.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key(n,"done"),on).apply());body.addView(finished);
        if(!d.optString("details").isEmpty()){body.addView(button("Consultar roteiro detalhado do PDF",()->{TextView t=text(d.optString("details"),15,ink);t.setPadding(dp(20),dp(16),dp(20),dp(16));t.setTextIsSelectable(true);ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle("Roteiro · dia "+n).setView(s).setPositiveButton("Fechar",null).show();}));}
        else body.addView(text("Este dia tem o resumo do calendário. O PDF semanal detalhado não foi disponibilizado.",14,muted));
        if(n<limit())body.addView(button("Próximo dia →",()->{selected=n+1;render();}));
    }
    private EditText number(int n,String field,String label){EditText e=new EditText(this);e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});e.setHint(label);int value=prefs.getInt(key(n,field),0);if(value>0)e.setText(String.valueOf(value));e.setContentDescription(label);e.addTextChangedListener(watcher(()->{String s=e.getText().toString();prefs.edit().putInt(key(n,field),s.isEmpty()?0:Integer.parseInt(s)).apply();}));return e;}
    private void progress(){heading("Cada passo conta");int completed=0,hits=0,misses=0,late=0;for(int i=1;i<=limit();i++){if(done(i))completed++;else if(i<today()&&!day(i).optString("kind").equals("rest"))late++;hits+=prefs.getInt(key(i,"hits"),0);misses+=prefs.getInt(key(i,"misses"),0);}LinearLayout c=card();c.addView(text(completed+" dias concluídos",25,purple));c.addView(text(late+" dias anteriores pendentes",16,muted));c.addView(text((hits+misses==0?"Sem questões registradas":Math.round(100f*hits/(hits+misses))+"% de acertos · "+(hits+misses)+" questões"),20,ink));body.addView(text("Progresso por semana",20,ink));for(int w=1;w<=(limit()+6)/7;w++){int total=0,doneCount=0;for(int i=(w-1)*7+1;i<=Math.min(w*7,limit());i++){total++;if(done(i))doneCount++;}body.addView(text("Semana "+w+"     "+doneCount+" / "+total,16,ink));ProgressBar b=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);b.setMax(total);b.setProgress(doneCount);body.addView(b);}}
    private void settings(){heading("No seu ritmo");body.addView(text("Início do plano: "+date(1),18,ink));body.addView(button("Alterar data inicial",()->{LocalDate s=start();new DatePickerDialog(this,(v,y,m,d)->{prefs.edit().putString("start",LocalDate.of(y,m+1,d).toString()).apply();render();},s.getYear(),s.getMonthValue()-1,s.getDayOfMonth()).show();}));CheckBox extra=new CheckBox(this);extra.setText("Incluir extensão: dias 121 a 126");extra.setChecked(limit()==126);extra.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("extension",on).apply();render();});body.addView(extra);body.addView(text("Os materiais são do 46º Exame. O calendário contém 126 dias; o plano principal mostra os primeiros 120. As pausas do material foram preservadas na mesma posição, mesmo ao mudar a data inicial. A data do exame não é calculada nem confirmada pelo app.",16,muted));body.addView(text("As semanas 1 a 10 têm roteiro detalhado. O recesso dos dias 71 a 73 também consta no PDF da semana 10. Os demais dias usam o calendário geral. O título “180 dias” na semana 5 foi mantido apenas como característica da fonte.",15,muted));body.addView(text("Os registros são salvos automaticamente neste aparelho. Desinstalar o app apaga o progresso. Sem login e sem conexão com servidores.",15,muted));}
    @Override public void onBackPressed(){if(selected>0){selected=0;render();}else if(!tab.equals("Hoje")){tab="Hoje";render();}else super.onBackPressed();}
}
