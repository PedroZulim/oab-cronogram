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
    private int selected=0, planWeek=0;
    private final int background=Color.rgb(247,246,252), soft=Color.rgb(237,232,251), border=Color.rgb(231,226,245), green=Color.rgb(30,115,77);
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
        if(state!=null){tab=state.getString("tab","Início");selected=state.getInt("selected",0);planWeek=state.getInt("planWeek",0);filter=state.getString("filter","");pending=state.getBoolean("pending",false);}
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
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("tab",tab);b.putInt("selected",selected);b.putInt("planWeek",planWeek);b.putString("filter",filter);b.putBoolean("pending",pending);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable surface(int color,int radius){
        GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));bg.setStroke(dp(1),border);return bg;
    }
    private TextView text(String value,int size,int color){
        TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);
        v.setFontFeatureSettings("kern");v.setPadding(0,dp(5),0,dp(5));v.setLineSpacing(dp(3),1);return v;
    }
    private TextView bold(String value,int size,int color){TextView v=text(value,size,color);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return v;}
    private void heading(String value){TextView v=bold(value,28,ink);if(android.os.Build.VERSION.SDK_INT>=28)v.setAccessibilityHeading(true);body.addView(v);}
    private void section(String value){TextView v=bold(value,18,ink);v.setPadding(0,dp(20),0,dp(8));body.addView(v);}
    private Button button(String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(purple);
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(14),dp(12),dp(14),dp(12));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x226D4BD1),surface(soft,12),null));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(6));b.setLayoutParams(lp);
        b.setOnClickListener(v->action.run());return b;
    }
    private Button primary(String label,Runnable action){
        Button b=button(label,action);b.setTextColor(Color.WHITE);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF),surface(purple,12),null));return b;
    }
    private LinearLayout panel(LinearLayout parent){
        LinearLayout c=column();c.setPadding(dp(18),dp(16),dp(18),dp(16));
        c.setBackground(surface(Color.WHITE,20));c.setElevation(dp(1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(1),dp(8),dp(1),dp(8));parent.addView(c,lp);return c;
    }
    private LinearLayout card(){return panel(body);}
    private void progressBar(LinearLayout parent,int value,int total){
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(total);bar.setProgress(value);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(purple));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(soft));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(10));lp.setMargins(0,dp(10),0,dp(10));parent.addView(bar,lp);
        bar.setContentDescription(value+" de "+total+" dias concluídos");
    }
    private void styleInput(EditText input){
        input.setTextColor(ink);input.setHintTextColor(muted);input.setTextSize(16);
        input.setPadding(dp(14),dp(12),dp(14),dp(12));input.setMinHeight(dp(52));
        input.setBackground(surface(background,12));
    }
    private CheckBox check(String label,boolean checked){
        CheckBox c=new CheckBox(this);c.setText(label);c.setTextSize(16);c.setTextColor(ink);c.setMinHeight(dp(48));
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(purple));c.setChecked(checked);return c;
    }
    private void render(){
        LinearLayout root=column();root.setBackgroundColor(background);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        TextView brand=bold("OAB  /  "+limit()+" dias",17,purple);brand.setPadding(dp(22),dp(14),dp(22),dp(14));root.addView(brand);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        body=column();body.setPadding(dp(20),dp(8),dp(20),dp(24));body.setFocusableInTouchMode(true);
        scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(8),dp(6),dp(8),dp(6));nav.setBackgroundColor(Color.WHITE);nav.setElevation(dp(6));
        String[] labels={"Início","Plano","Progresso","Ajustes"};
        int[] icons={R.drawable.ic_home,R.drawable.ic_calendar,R.drawable.ic_progress,R.drawable.ic_settings};
        for(int i=0;i<labels.length;i++){
            final String label=labels[i];boolean active=tab.equals(label)||(tab.equals("Lembretes")&&label.equals("Ajustes"));
            Button item=button(label,()->{tab=label;selected=0;render();});item.setTextSize(11);item.setPadding(dp(2),dp(8),dp(2),dp(8));
            item.setTextColor(active?purple:muted);item.setSelected(active);
            item.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x226D4BD1),surface(active?soft:Color.WHITE,14),null));
            android.graphics.drawable.Drawable icon=getDrawable(icons[i]).mutate();icon.setTint(active?purple:muted);icon.setBounds(0,0,dp(22),dp(22));
            item.setCompoundDrawables(null,icon,null,null);item.setCompoundDrawablePadding(dp(4));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);nav.addView(item,lp);
        }
        root.addView(nav);setContentView(root);root.requestApplyInsets();
        if(selected>0){detail(selected);return;}
        switch(tab){case "Plano":plan();break;case "Progresso":progress();break;case "Ajustes":settings();break;case "Lembretes":reminderSettings();break;default:home();}
    }
    private boolean done(int n){return prefs.getBoolean(key(n,"done"),false);}
    private String date(int n){return start().plusDays(n-1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    private String title(JSONObject d){String t=d.optString("title");return t.equals("Semana de")?"Revisão":t;}
    private void home(){
        body.addView(text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM",new java.util.Locale("pt","BR"))),14,muted));
        heading("Um dia mais perto da aprovação.");
        int n=Math.max(1,Math.min(today(),limit())), complete=0;
        for(int i=1;i<=limit();i++)if(done(i))complete++;
        if(today()<1)body.addView(text("Seu plano começa em "+date(1)+". Explore os conteúdos enquanto isso.",15,muted));
        if(today()>limit())body.addView(text("O período terminou. Você pode retomar os dias pendentes.",15,muted));
        LinearLayout overview=card();overview.addView(text("SEU PLANO · OAB 46",12,muted));
        overview.addView(bold(complete+" de "+limit()+" dias",26,ink));progressBar(overview,complete,limit());
        overview.addView(text(Math.round(100f*complete/limit())+"% concluído · Semana "+day(n).optInt("week"),14,purple));
        dayCard(n);
        section("Sua semana");weekStrip(n);
        LinearLayout overdue=card();overdue.addView(bold("Pendências anteriores",18,ink));
        int count=0;
        for(int i=1;i<today()&&i<=limit();i++){
            if(done(i)||day(i).optString("kind").equals("rest"))continue;
            final int item=i;if(count++<3)overdue.addView(button("Dia "+i+" · "+title(day(i)),()->{selected=item;render();}));
        }
        if(count==0)overdue.addView(text("Nenhuma pendência até aqui. Siga no seu ritmo.",15,muted));
        else overdue.addView(button("Ver pendências ("+count+")",()->{tab="Plano";pending=true;filter="";planWeek=0;render();}));
        LinearLayout reminders=card();reminders.addView(bold("Próximo lembrete",18,ink));reminders.addView(text(reminderSummary(),15,muted));
        reminders.addView(button("Ajustar horários",()->{tab="Lembretes";render();}));
        section("Atalhos");
        body.addView(button("Ver cronograma  →",()->{tab="Plano";planWeek=0;pending=false;filter="";render();}));
        body.addView(button("Meu progresso  →",()->{tab="Progresso";render();}));
    }
    private void weekStrip(int current){
        HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(this);int first=((current-1)/7)*7+1;
        for(int i=first;i<=Math.min(first+6,limit());i++){
            final int n=i;String weekday=start().plusDays(i-1).format(DateTimeFormatter.ofPattern("EEE",new java.util.Locale("pt","BR")));
            Button b=button(weekday+"\\n"+i+(done(i)?"  ✓":""),()->{selected=n;render();});
            b.setContentDescription("Dia "+i+", "+title(day(i))+(done(i)?", concluído":""));
            b.setTextColor(done(i)?green:purple);if(i==today())b.setBackground(surface(soft,16));else b.setBackground(surface(Color.WHITE,16));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(76),-2);lp.setMargins(0,dp(4),dp(8),dp(4));row.addView(b,lp);
        }
        scroll.addView(row);body.addView(scroll);
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
        body.addView(button("‹ Voltar aos ajustes",()->{tab="Ajustes";render();}));heading("Hora de estudar");body.addView(text("Ative até três lembretes por dia e escolha os horários. Eles se repetem diariamente durante o seu plano, no horário local do celular.",16,muted));
        for(int i=0;i<Reminders.COUNT;i++){
            final int slot=i;LinearLayout c=card();Switch toggle=new Switch(this);toggle.setText("Lembrete "+(i+1));toggle.setTextSize(18);toggle.setMinHeight(dp(48));toggle.setTextColor(ink);toggle.setChecked(Reminders.enabled(this,i));c.addView(toggle);
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
    private String kindLabel(JSONObject d){
        switch(d.optString("kind")){case "rest":return "Pausa";case "exam":return "Exame";case "review":return "Revisão";case "mock":return "Simulado";default:return "Estudo";}
    }
    private void dayCard(int n){
        JSONObject d=day(n);LinearLayout c=card();
        TextView banner=bold((today()>=1&&today()<=limit()?"Seu estudo de hoje":"Retome seu plano")+"\\nDia "+n+" · Semana "+d.optInt("week"),18,Color.WHITE);
        GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{purple,Color.rgb(74,47,163)});
        gradient.setCornerRadius(dp(14));banner.setBackground(gradient);banner.setPadding(dp(16),dp(16),dp(16),dp(16));c.addView(banner);
        c.addView(text(kindLabel(d)+" · "+(done(n)?"Concluído":"A fazer"),13,done(n)?green:purple));
        c.addView(bold(title(d),22,ink));c.addView(text(d.optString("summary"),15,muted));
        c.addView(primary(done(n)?"Ver estudo concluído  →":"Começar estudo  →",()->{selected=n;render();}));
    }
    private void plan(){
        heading("Seu cronograma");body.addView(text("Um passo de cada vez, no seu ritmo.",15,muted));
        EditText search=new EditText(this);styleInput(search);search.setSingleLine(true);search.setHint("Buscar matéria, tema ou dia");search.setContentDescription("Buscar no cronograma");search.setText(filter);body.addView(search);
        HorizontalScrollView weeks=new HorizontalScrollView(this);weeks.setHorizontalScrollBarEnabled(false);LinearLayout weekRow=new LinearLayout(this);
        for(int w=0;w<=(limit()+6)/7;w++){
            final int chosen=w;Button chip=button(w==0?"Todas":"Semana "+w,()->{planWeek=chosen;render();});
            if(w==planWeek){chip.setTextColor(Color.WHITE);chip.setBackground(surface(purple,12));}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMargins(0,dp(8),dp(8),dp(8));weekRow.addView(chip,lp);
        }
        weeks.addView(weekRow);body.addView(weeks);
        CheckBox onlyPending=check("Mostrar apenas pendências",pending);body.addView(onlyPending);
        LinearLayout list=column();body.addView(list);
        Runnable refresh=()->{
            list.removeAllViews();int week=0;
            for(int i=1;i<=limit();i++){
                JSONObject d=day(i);if(pending&&done(i))continue;
                // Search spans every week; the selected week applies only without a query.
                if(filter.trim().isEmpty()&&planWeek>0&&d.optInt("week")!=planWeek)continue;
                if(!ScheduleSearch.matches(i,title(d),d.optString("summary"),filter))continue;
                if(week!=d.optInt("week")){week=d.optInt("week");list.addView(bold("SEMANA "+week,14,purple));}
                final int n=i;LinearLayout c=panel(list);
                c.addView(text("DIA "+i+" · "+date(i),12,muted));c.addView(bold(title(d),18,ink));
                c.addView(text(kindLabel(d)+" · "+(done(i)?"Concluído":"A fazer"),13,done(i)?green:muted));
                c.addView(button("Abrir estudo  →",()->{selected=n;render();}));
            }
            if(list.getChildCount()==0)list.addView(text("Nenhum dia encontrado. Tente outro tema ou filtro.",16,muted));
        };
        search.addTextChangedListener(watcher(()->{filter=search.getText().toString();refresh.run();}));
        onlyPending.setOnCheckedChangeListener((v,on)->{pending=on;refresh.run();});refresh.run();
    }
    private TextWatcher watcher(Runnable r){return new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){r.run();}public void afterTextChanged(Editable e){}};}
    private void detail(int n){
        JSONObject d=day(n);body.addView(button("‹ Voltar",()->{selected=0;render();}));
        body.addView(text("DIA "+n+" · SEMANA "+d.optInt("week")+" · "+date(n),12,purple));heading(title(d));
        LinearLayout summary=card();summary.addView(text(kindLabel(d),13,purple));summary.addView(text(d.optString("summary"),17,ink));
        boolean rest=d.optString("kind").equals("rest")||d.optString("kind").equals("exam");
        if(!rest){
            LinearLayout checklist=card();checklist.addView(bold("Meu checklist",18,ink));
            for(String stage:new String[]{"Teoria / revisão","Questões","Lei seca","Revisar erros"}){
                CheckBox item=check(stage,prefs.getBoolean(key(n,stage),false));
                item.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key(n,stage),on).apply());checklist.addView(item);
            }
            LinearLayout performance=card();performance.addView(bold("Desempenho nas questões",18,ink));
            performance.addView(text("Registre as questões que você resolveu.",14,muted));
            LinearLayout row=new LinearLayout(this);
            String[] fields={"hits","misses"},labels={"Acertos","Erros"};
            for(int i=0;i<fields.length;i++){
                LinearLayout field=column();field.addView(text(labels[i],14,ink));field.addView(number(n,fields[i],labels[i]));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(i==0?0:dp(6),0,i==0?dp(6):0,0);row.addView(field,lp);
            }
            performance.addView(row);
        }
        LinearLayout notebook=card();notebook.addView(bold("Caderno de erros e anotações",18,ink));
        EditText notes=new EditText(this);styleInput(notes);notes.setHint("O que preciso lembrar na revisão?");
        notes.setContentDescription("Caderno de erros e anotações");notes.setMinLines(4);notes.setGravity(Gravity.TOP);
        notes.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setText(prefs.getString(key(n,"notes"),""));
        notes.addTextChangedListener(watcher(()->prefs.edit().putString(key(n,"notes"),notes.getText().toString()).apply()));notebook.addView(notes);
        notebook.addView(text("Salvo automaticamente neste aparelho",12,muted));
        if(!d.optString("details").isEmpty())body.addView(button("Consultar roteiro detalhado",()->{
            TextView t=text(d.optString("details"),16,ink);t.setPadding(dp(22),dp(16),dp(22),dp(16));t.setTextIsSelectable(true);
            ScrollView scroll=new ScrollView(this);scroll.addView(t);
            new AlertDialog.Builder(this).setTitle("Roteiro · dia "+n).setView(scroll).setPositiveButton("Fechar",null).show();
        }));
        else body.addView(text("Este dia tem apenas o resumo do calendário. O roteiro semanal não foi disponibilizado.",14,muted));
        Button finished=primary(done(n)?"Concluído · reabrir dia":"Concluir dia",()->{});
        finished.setOnClickListener(v->{
            boolean complete=!done(n);prefs.edit().putBoolean(key(n,"done"),complete).apply();Reminders.schedule(this);
            finished.setText(complete?"Concluído · reabrir dia":"Concluir dia");
            Toast.makeText(this,complete?"Dia concluído. Bom trabalho!":"Dia reaberto.",Toast.LENGTH_SHORT).show();
        });body.addView(finished);
        if(n<limit())body.addView(button("Próximo dia  →",()->{selected=n+1;render();}));
    }
    private EditText number(int n,String field,String label){EditText e=new EditText(this);styleInput(e);e.setInputType(InputType.TYPE_CLASS_NUMBER);e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});e.setHint(label);int value=prefs.getInt(key(n,field),0);if(value>0)e.setText(String.valueOf(value));e.setContentDescription(label);e.addTextChangedListener(watcher(()->{String s=e.getText().toString();prefs.edit().putInt(key(n,field),s.isEmpty()?0:Integer.parseInt(s)).apply();}));return e;}
    private void progress(){
        heading("Cada passo conta");body.addView(text("Acompanhe a evolução da sua rotina.",15,muted));
        int completed=0,hits=0,misses=0,late=0;
        for(int i=1;i<=limit();i++){
            if(done(i))completed++;else if(i<today()&&!day(i).optString("kind").equals("rest"))late++;
            hits+=prefs.getInt(key(i,"hits"),0);misses+=prefs.getInt(key(i,"misses"),0);
        }
        LinearLayout total=card();total.addView(text("DIAS CONCLUÍDOS",12,muted));
        total.addView(bold(completed+" de "+limit(),28,purple));progressBar(total,completed,limit());
        LinearLayout accuracy=card();accuracy.addView(text("DESEMPENHO",12,muted));
        accuracy.addView(bold(hits+misses==0?"Sem questões registradas":Math.round(100f*hits/(hits+misses))+"% de acertos",24,ink));
        accuracy.addView(text(hits+" acertos · "+misses+" erros · "+(hits+misses)+" questões",14,muted));
        LinearLayout pendingCard=card();pendingCard.addView(bold(late+" dias anteriores pendentes",18,ink));
        if(late>0)pendingCard.addView(button("Revisar pendências",()->{tab="Plano";pending=true;filter="";planWeek=0;render();}));
        section("Progresso por semana");
        for(int w=1;w<=(limit()+6)/7;w++){
            int count=0,totalDays=Math.min(w*7,limit())-(w-1)*7;
            for(int i=(w-1)*7+1;i<=Math.min(w*7,limit());i++)if(done(i))count++;
            LinearLayout c=card();c.addView(bold("Semana "+w+" · "+count+" / "+totalDays,16,ink));progressBar(c,count,totalDays);
        }
    }
    private void settings(){
        heading("No seu ritmo");body.addView(text("Organize seu plano e seus horários.",15,muted));
        LinearLayout dates=card();dates.addView(bold("Seu plano de estudos",18,ink));
        dates.addView(text("Início do plano: "+date(1),16,ink));
        dates.addView(button("Alterar data inicial",()->{
            LocalDate initial=start();
            new DatePickerDialog(this,(v,y,m,d)->{prefs.edit().putString("start",LocalDate.of(y,m+1,d).toString()).apply();Reminders.schedule(this);render();},initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth()).show();
        }));
        dates.addView(text("Alterar a data mantém seus estudos e anotações.",14,muted));
        CheckBox extra=check("Incluir extensão: dias 121 a 126",limit()==126);
        extra.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("extension",on).apply();if(!on&&planWeek>18)planWeek=0;Reminders.schedule(this);render();});dates.addView(extra);
        LinearLayout reminders=card();reminders.addView(bold("Lembretes de estudo",18,ink));
        reminders.addView(text(reminderSummary(),15,muted));reminders.addView(button("Lembretes e horários",()->{tab="Lembretes";render();}));
        LinearLayout material=card();material.addView(bold("Sobre o material",18,ink));
        material.addView(text("Materiais do 46º Exame. São 120 dias no plano principal e 6 dias opcionais. A data do exame não é calculada nem confirmada pelo app.",15,muted));
        material.addView(text("As semanas 1 a 10 têm roteiro detalhado, incluindo o recesso dos dias 71 a 73. Os demais dias usam o calendário geral. O título “180 dias” na semana 5 é uma característica da fonte.",15,muted));
        material.addView(text("As pausas mantêm sua posição no plano mesmo quando você altera a data inicial.",15,muted));
        LinearLayout storage=card();storage.addView(bold("Seus registros",18,ink));
        storage.addView(text("Tudo é salvo automaticamente neste aparelho, sem login. Desinstalar o app apaga o progresso.",15,muted));
    }
    @Override public void onBackPressed(){if(selected>0){selected=0;render();}else if(!tab.equals("Início")){tab="Início";render();}else super.onBackPressed();}
}
