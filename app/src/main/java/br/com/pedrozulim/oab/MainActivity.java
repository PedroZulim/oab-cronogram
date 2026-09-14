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
    private String tab="Início", filter="";
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
        if(state!=null){tab=state.getString("tab","Início");selected=state.getInt("selected",0);}
        else openStudy(getIntent());
        render();
    }
    private void openStudy(Intent intent){int n=intent.getIntExtra("study_day",0);if(n>=1&&n<=limit()){tab="Início";selected=n;}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openStudy(intent);if(days!=null)render();}
    @Override protected void onResume(){super.onResume();if(days!=null){Reminders.schedule(this);if(selected==0)render();}}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==42){Reminders.schedule(this);render();}}
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
        for(String label:new String[]{"Início","Plano","Progresso","Ajustes"}){Button b=button(label,()->{tab=label;selected=0;render();});b.setTextSize(12);b.setPadding(0,0,0,0);if(tab.equals(label))b.setTypeface(null,Typeface.BOLD);nav.addView(b,new LinearLayout.LayoutParams(0,dp(56),1));}
        root.addView(nav);setContentView(root);
        if(selected>0){detail(selected);return;}
        switch(tab){case "Plano":plan();break;case "Progresso":progress();break;case "Ajustes":settings();break;case "Lembretes":reminderSettings();break;default:home();}
    }
    private boolean done(int n){return prefs.getBoolean(key(n,"done"),false);}
    private String date(int n){return start().plusDays(n-1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    private String title(JSONObject d){String t=d.optString("title");return t.equals("Semana de")?"Revisão":t;}
    private void home(){
        heading("Seu próximo passo começa aqui.");body.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM",new java.util.Locale("pt","BR"))),14,muted));
        body.addView(text("Seu plano de "+limit()+" dias · OAB 46",14,purple));
        int n=today();
        if(n<1){body.addView(text("Seu plano começa em "+date(1)+". Você já pode explorar os conteúdos.",17,ink));n=1;}
        else if(n>limit()){body.addView(text("O período terminou. Revise as pendências no Plano.",17,ink));n=limit();}
        int complete=0;for(int i=1;i<=limit();i++)if(done(i))complete++;
        LinearLayout c=card();c.addView(text(complete+" / "+limit()+" dias concluídos",22,purple));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(limit());bar.setProgress(complete);c.addView(bar);
        body.addView(text(today()<1?"Prepare-se para começar":today()>limit()?"Retome seus estudos":"Seu estudo de hoje",20,ink));
        dayCard(n);
        LinearLayout reminders=card();reminders.addView(text("Lembretes de estudo",20,ink));reminders.addView(text(reminderSummary(),15,muted));reminders.addView(button("Configurar horários",()->{tab="Lembretes";render();}));
        body.addView(text("Acessos rápidos",20,ink));
        body.addView(button("Ver cronograma completo",()->{tab="Plano";render();}));
        body.addView(button("Revisar pendências",()->{tab="Plano";pending=true;filter="";render();}));
        body.addView(button("Acompanhar meu progresso",()->{tab="Progresso";render();}));
    }
    private String reminderSummary(){
        boolean enabled=false;java.time.ZonedDateTime nearest=null;
        for(int i=0;i<Reminders.COUNT;i++){enabled|=Reminders.enabled(this,i);java.time.ZonedDateTime next=Reminders.next(this,i);if(next!=null&&(nearest==null||next.isBefore(nearest)))nearest=next;}
        if(!enabled)return "Escolha seus horários para criar uma rotina. Os lembretes estão desativados.";
        if(!Reminders.notificationsAllowed(this))return "Notificações bloqueadas. Habilite-as para receber os lembretes.";
        if(nearest==null)return "O período do plano terminou. Ajuste a data inicial para retomar os lembretes.";
        return "Próximo horário: "+nearest.format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"))+(Reminders.exactAllowed(this)?"":" · aproximado")+". Dias concluídos não geram avisos.";
    }
    private void notificationSettings(){
        Intent intent=new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(android.provider.Settings.EXTRA_CHANNEL_ID,Reminders.CHANNEL);
        startActivity(intent);
    }
    private void askNotifications(){
        if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},42);
    }
    private void reminderSettings(){
        heading("Hora de estudar");body.addView(text("Ative até três lembretes por dia e escolha os horários. Eles se repetem diariamente durante o seu plano, no horário local do celular.",16,muted));
        for(int i=0;i<Reminders.COUNT;i++){
            final int slot=i;LinearLayout c=card();Switch toggle=new Switch(this);toggle.setText("Lembrete "+(i+1));toggle.setTextSize(18);toggle.setTextColor(ink);toggle.setChecked(Reminders.enabled(this,i));c.addView(toggle);
            int minute=Reminders.minutes(this,i);
            c.addView(button(String.format(java.util.Locale.ROOT,"%02d:%02d · alterar horário",minute/60,minute%60),()->new TimePickerDialog(this,(picker,h,m)->{
                int value=h*60+m;
                for(int other=0;other<Reminders.COUNT;other++)if(other!=slot&&Reminders.enabled(this,other)&&Reminders.minutes(this,other)==value){Toast.makeText(this,"Já existe um lembrete ativo neste horário.",Toast.LENGTH_LONG).show();return;}
                prefs.edit().putInt("reminder.time."+slot,value).apply();Reminders.schedule(this);render();
            },minute/60,minute%60,true).show()));
            toggle.setOnCheckedChangeListener((view,on)->{
                if(on)for(int other=0;other<Reminders.COUNT;other++)if(other!=slot&&Reminders.enabled(this,other)&&Reminders.minutes(this,other)==Reminders.minutes(this,slot)){Toast.makeText(this,"Escolha um horário diferente do outro lembrete ativo.",Toast.LENGTH_LONG).show();render();return;}
                prefs.edit().putBoolean("reminder.on."+slot,on).apply();Reminders.schedule(this);render();if(on)askNotifications();
            });
        }
        body.addView(text(reminderSummary(),16,ink));
        if(!Reminders.notificationsAllowed(this)){
            body.addView(button("Permitir notificações",()->{if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED&&shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS))askNotifications();else notificationSettings();}));
        }
        if(!Reminders.exactAllowed(this)){
            body.addView(text("Para receber no horário escolhido, permita alarmes e lembretes. Sem essa permissão, o Android pode atrasar os avisos.",15,muted));
            body.addView(button("Permitir horários exatos",()->{if(android.os.Build.VERSION.SDK_INT>=31)startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));}));
        }
        body.addView(text("Ao concluir o estudo do dia, os avisos restantes daquele dia são dispensados. Seus horários ficam salvos e são recuperados após reiniciar o celular.",15,muted));
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
        LinearLayout c=card();c.addView(text(d.optString("summary"),18,ink));
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
    private void settings(){heading("No seu ritmo");body.addView(button("Lembretes e horários",()->{tab="Lembretes";render();}));body.addView(text("Início do plano: "+date(1),18,ink));body.addView(button("Alterar data inicial",()->{LocalDate s=start();new DatePickerDialog(this,(v,y,m,d)->{prefs.edit().putString("start",LocalDate.of(y,m+1,d).toString()).apply();Reminders.schedule(this);render();},s.getYear(),s.getMonthValue()-1,s.getDayOfMonth()).show();}));CheckBox extra=new CheckBox(this);extra.setText("Incluir extensão: dias 121 a 126");extra.setChecked(limit()==126);extra.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("extension",on).apply();Reminders.schedule(this);render();});body.addView(extra);body.addView(text("Os materiais são do 46º Exame. O calendário contém 126 dias; o plano principal mostra os primeiros 120. As pausas do material foram preservadas na mesma posição, mesmo ao mudar a data inicial. A data do exame não é calculada nem confirmada pelo app.",16,muted));body.addView(text("As semanas 1 a 10 têm roteiro detalhado. O recesso dos dias 71 a 73 também consta no PDF da semana 10. Os demais dias usam o calendário geral. O título “180 dias” na semana 5 foi mantido apenas como característica da fonte.",15,muted));body.addView(text("Os registros são salvos automaticamente neste aparelho. Desinstalar o app apaga o progresso. Sem login e sem conexão com servidores.",15,muted));}
    @Override public void onBackPressed(){if(selected>0){selected=0;render();}else if(!tab.equals("Início")){tab="Início";render();}else super.onBackPressed();}
}
