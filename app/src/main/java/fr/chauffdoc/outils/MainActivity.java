package fr.chauffdoc.outils;

import android.app.Activity;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.media.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {
    static final int NAVY=Color.rgb(6,47,93), ORANGE=Color.rgb(255,105,0);
    static final int GREEN=Color.rgb(10,170,72), RED=Color.rgb(205,45,45);
    SensorManager sm; Sensor magnetic;
    LinearLayout root, body;
    TextView fieldText, stateText;
    RotorView rotor;
    boolean sound=true, vibration=true;
    final ArrayDeque<Float> samples=new ArrayDeque<>();
    float baseline=-1;
    long lastDetected=0, lastFeedback=0;
    ToneGenerator tone;
    Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sm=(SensorManager)getSystemService(SENSOR_SERVICE);
        magnetic=sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,60);
        showMagnetic();
    }

    GradientDrawable rounded(int color,float radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    TextView label(String s,int size,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(NAVY);
        v.setPadding(12,10,12,10); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextColor(NAVY); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(rounded(Color.rgb(241,247,251),24)); return b;
    }
    LinearLayout card(){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(20,20,20,20);
        c.setBackground(rounded(Color.WHITE,34));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(14,10,14,10); c.setLayoutParams(lp); return c;
    }
    void shell(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(5,10,5,24);
        root.setBackgroundColor(Color.rgb(241,248,252)); scroll.addView(root);
        TextView brand=label("Chauff’Doc",34,true); brand.setGravity(Gravity.CENTER); brand.setTextColor(NAVY);
        root.addView(brand,new LinearLayout.LayoutParams(-1,88));
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); root.addView(body);
        LinearLayout nav=new LinearLayout(this); nav.setPadding(8,10,8,10); nav.setBackgroundColor(NAVY);
        Button mag=button("🧲 Magnetic Tool"); Button top=button("⏱ Top Compteur");
        nav.addView(mag,new LinearLayout.LayoutParams(0,66,1)); nav.addView(top,new LinearLayout.LayoutParams(0,66,1));
        root.addView(nav);
        mag.setOnClickListener(v->showMagnetic()); top.setOnClickListener(v->showCounter());
        setContentView(scroll);
    }

    void showMagnetic(){
        if(sm!=null) sm.unregisterListener(this);
        shell(); body.removeAllViews();
        LinearLayout c=card();
        TextView title=label("🧲  Magnetic Tool",27,true); c.addView(title);
        TextView sub=label("Contrôle simple du circulateur chaudière",17,false); sub.setTextColor(Color.DKGRAY); c.addView(sub);
        rotor=new RotorView(this); c.addView(rotor,new LinearLayout.LayoutParams(-1,390));
        stateText=label("Approchez le téléphone du circulateur",19,true); stateText.setGravity(Gravity.CENTER);
        stateText.setBackground(rounded(Color.rgb(236,246,252),26)); c.addView(stateText,new LinearLayout.LayoutParams(-1,85));
        fieldText=label("Champ magnétique : -- µT",21,true); fieldText.setGravity(Gravity.CENTER);
        c.addView(fieldText,new LinearLayout.LayoutParams(-1,68));

        TextView help=label("Approchez le haut ou l’arrière du téléphone du circulateur. La roue s’anime lorsqu’une variation magnétique est détectée.",14,false);
        help.setBackground(rounded(Color.rgb(231,246,255),24)); help.setPadding(18,16,18,16); c.addView(help);

        LinearLayout options=new LinearLayout(this);
        Button snd=button("🔊 Son activé"); Button vib=button("📳 Vibration activée");
        options.addView(snd,new LinearLayout.LayoutParams(0,68,1)); options.addView(vib,new LinearLayout.LayoutParams(0,68,1)); c.addView(options);
        snd.setOnClickListener(v->{sound=!sound;snd.setText(sound?"🔊 Son activé":"🔇 Son coupé");});
        vib.setOnClickListener(v->{vibration=!vibration;vib.setText(vibration?"📳 Vibration activée":"Vibration coupée");});
        body.addView(c);

        TextView note=label("ⓘ Aide au diagnostic : une activité magnétique détectée ne confirme pas à elle seule le débit hydraulique du circulateur.",13,true);
        note.setBackground(rounded(Color.rgb(231,246,255),24)); note.setPadding(18,16,18,16); body.addView(note);

        baseline=-1; samples.clear(); lastDetected=0;
        if(magnetic==null){
            stateText.setText("Magnétomètre indisponible sur ce téléphone"); stateText.setTextColor(RED);
            fieldText.setText("Capteur non disponible");
        } else sm.registerListener(this,magnetic,SensorManager.SENSOR_DELAY_GAME);
    }

    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()!=Sensor.TYPE_MAGNETIC_FIELD || rotor==null || fieldText==null)return;
        float x=e.values[0],y=e.values[1],z=e.values[2];
        float magnitude=(float)Math.sqrt(x*x+y*y+z*z);
        fieldText.setText(String.format(Locale.FRANCE,"Champ magnétique : %.1f µT",magnitude));
        samples.addLast(magnitude); if(samples.size()>28)samples.removeFirst();
        if(baseline<0) baseline=magnitude; else baseline=baseline*.985f+magnitude*.015f;
        float mean=0; for(float f:samples)mean+=f; mean/=Math.max(1,samples.size());
        float dev=0; for(float f:samples)dev+=Math.abs(f-mean); dev/=Math.max(1,samples.size());
        boolean active=samples.size()>12 && (dev>2.2f || Math.abs(magnitude-baseline)>8f);
        long now=System.currentTimeMillis();
        if(active){
            lastDetected=now; rotor.running=true; rotor.invalidate();
            stateText.setText("✓ Activité magnétique détectée"); stateText.setTextColor(GREEN);
            if(now-lastFeedback>1800){
                lastFeedback=now;
                if(sound)tone.startTone(ToneGenerator.TONE_PROP_BEEP,90);
                if(vibration){
                    Vibrator vb=(Vibrator)getSystemService(VIBRATOR_SERVICE);
                    if(Build.VERSION.SDK_INT>=26) vb.vibrate(VibrationEffect.createOneShot(80,VibrationEffect.DEFAULT_AMPLITUDE));
                    else vb.vibrate(80);
                }
            }
        } else if(now-lastDetected>1200){
            rotor.running=false; rotor.invalidate();
            stateText.setText("Aucune activité détectée"); stateText.setTextColor(RED);
        }
    }
    @Override public void onAccuracyChanged(Sensor s,int accuracy){}

    void showCounter(){
        if(sm!=null)sm.unregisterListener(this);
        shell(); body.removeAllViews();
        LinearLayout c=card(); c.addView(label("⏱  Top compteur gaz",27,true));
        TextView clock=label("01:00",64,true); clock.setGravity(Gravity.CENTER); clock.setTextColor(NAVY);
        c.addView(clock,new LinearLayout.LayoutParams(-1,125));

        int[] duration={60}, remaining={60}; boolean[] running={false}; Runnable[] tick={null};
        LinearLayout presets=new LinearLayout(this);
        int[] secs={30,60,120,300}; String[] names={"30 s","1 min","2 min","5 min"};
        for(int i=0;i<4;i++){
            Button b=button(names[i]); final int s=secs[i];
            b.setOnClickListener(v->{if(!running[0]){duration[0]=s;remaining[0]=s;clock.setText(time(s));}});
            presets.addView(b,new LinearLayout.LayoutParams(0,60,1));
        }
        c.addView(presets);

        EditText startIndex=new EditText(this), endIndex=new EditText(this);
        startIndex.setHint("Index de départ (m³)"); endIndex.setHint("Index de fin (m³)");
        startIndex.setInputType(2|8192); endIndex.setInputType(2|8192);
        c.addView(startIndex,new LinearLayout.LayoutParams(-1,72)); c.addView(endIndex,new LinearLayout.LayoutParams(-1,72));

        Button start=button("▶ Lancer le contrôle"), calc=button("Calculer l’écart");
        TextView result=label("Écart compteur : 0.000 m³",24,true); result.setGravity(Gravity.CENTER); result.setTextColor(GREEN);
        c.addView(start,new LinearLayout.LayoutParams(-1,70)); c.addView(calc,new LinearLayout.LayoutParams(-1,70)); c.addView(result,new LinearLayout.LayoutParams(-1,90));

        tick[0]=new Runnable(){public void run(){
            if(!running[0])return; remaining[0]--; clock.setText(time(remaining[0]));
            if(remaining[0]<=0){running[0]=false;start.setText("✓ Contrôle terminé");endIndex.requestFocus();}
            else handler.postDelayed(this,1000);
        }};
        start.setOnClickListener(v->{
            if(startIndex.getText().toString().trim().isEmpty()){startIndex.setError("Saisissez l’index de départ");return;}
            remaining[0]=duration[0];clock.setText(time(remaining[0]));running[0]=true;start.setText("● Contrôle en cours…");
            handler.removeCallbacks(tick[0]);handler.postDelayed(tick[0],1000);
        });
        calc.setOnClickListener(v->{
            try{
                double a=Double.parseDouble(startIndex.getText().toString().replace(',','.'));
                double z=Double.parseDouble(endIndex.getText().toString().replace(',','.'));
                if(z<a){endIndex.setError("L’index final doit être supérieur ou égal");return;}
                result.setText(String.format(Locale.FRANCE,"Écart compteur : %.3f m³",z-a));
            }catch(Exception ex){endIndex.setError("Saisissez les deux index");}
        });
        body.addView(c);
    }

    String time(int s){return String.format(Locale.FRANCE,"%02d:%02d",s/60,s%60);}

    @Override protected void onPause(){super.onPause();if(sm!=null)sm.unregisterListener(this);}
    @Override protected void onResume(){super.onResume();if(magnetic!=null&&rotor!=null)sm.registerListener(this,magnetic,SensorManager.SENSOR_DELAY_GAME);}

    static class RotorView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); boolean running=false; float angle=0;
        RotorView(Context c){super(c);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.36f;
            p.setColor(NAVY);c.drawCircle(cx,cy,r,p);c.save();c.rotate(angle,cx,cy);p.setColor(running?ORANGE:Color.rgb(95,108,120));
            for(int i=0;i<5;i++){c.save();c.rotate(i*72,cx,cy);RectF q=new RectF(cx-r*.12f,cy-r*.72f,cx+r*.30f,cy-r*.12f);c.drawOval(q,p);c.restore();}
            c.restore();p.setColor(Color.LTGRAY);c.drawCircle(cx,cy,r*.14f,p);p.setColor(NAVY);c.drawCircle(cx,cy,r*.055f,p);
            if(running){angle+=12;postInvalidateDelayed(32);}
        }
    }
}
