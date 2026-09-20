package fr.chauffdoc.outils;

import android.app.Activity;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.*;
import android.media.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements SensorEventListener {

    static final int NAVY = Color.rgb(6,47,93);
    static final int ORANGE = Color.rgb(255,105,0);
    static final int GREEN = Color.rgb(10,170,72);
    static final int RED = Color.rgb(205,45,45);
    static final int BG = Color.rgb(244,249,252);

    SensorManager sm;
    Sensor magnetic;
    LinearLayout content;
    TextView fieldText, stateText;
    RotorView rotor;

    boolean sound = true;
    boolean vibration = true;

    final ArrayDeque<Float> samples = new ArrayDeque<>();
    float baseline = -1;
    long lastDetected = 0;
    long lastFeedback = 0;

    ToneGenerator tone;
    Handler handler = new Handler(Looper.getMainLooper());

    int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    GradientDrawable bg(int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    TextView text(String value, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(NAVY);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    Button btn(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setTextColor(NAVY);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(bg(Color.rgb(236,244,249), 14));
        b.setPadding(dp(8),0,dp(8),0);
        return b;
    }

    LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w,h);
    }

    LinearLayout.LayoutParams marginLp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h);
        p.setMargins(dp(l),dp(t),dp(r),dp(b));
        return p;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        magnetic = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION,60);

        showHome();
    }

    void base(String title) {
        if (sm != null) sm.unregisterListener(this);
        rotor = null;
        fieldText = null;
        stateText = null;

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18),dp(8),dp(18),dp(8));

        TextView brand = text("Chauff’Doc",26,true);
        brand.setTextColor(NAVY);
        header.addView(brand,new LinearLayout.LayoutParams(0,dp(54),1));

        if (!title.equals("Accueil")) {
            Button home = btn("Accueil");
            home.setOnClickListener(v -> showHome());
            header.addView(home,lp(dp(90),dp(42)));
        }

        page.addView(header,lp(-1,dp(66)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14),dp(6),dp(14),dp(24));

        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(page);
    }

    void showHome() {
        base("Accueil");

        TextView welcome = text("Outils technicien",25,true);
        welcome.setGravity(Gravity.CENTER);
        content.addView(welcome,marginLp(-1,dp(55),0,8,0,18));

        TextView sub = text(
                "Choisissez l’outil à utiliser.",
                16,false);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.DKGRAY);
        content.addView(sub,marginLp(-1,-2,0,0,0,20));

        Button magneticButton = btn("🧲  Magnetic Tool");
        magneticButton.setTextSize(20);
        magneticButton.setOnClickListener(v -> showMagnetic());
        content.addView(magneticButton,marginLp(-1,dp(80),0,8,0,14));

        TextView mDesc = text(
                "Détection de variations du champ magnétique à proximité d’un circulateur.",
                14,false);
        mDesc.setPadding(dp(12),0,dp(12),dp(12));
        mDesc.setTextColor(Color.DKGRAY);
        content.addView(mDesc);

        Button counterButton = btn("⏱  Top compteur gaz");
        counterButton.setTextSize(20);
        counterButton.setOnClickListener(v -> showCounter());
        content.addView(counterButton,marginLp(-1,dp(80),0,14,0,14));

        TextView cDesc = text(
                "Chronométrage et calcul automatique de l’écart entre deux index compteur.",
                14,false);
        cDesc.setPadding(dp(12),0,dp(12),dp(12));
        cDesc.setTextColor(Color.DKGRAY);
        content.addView(cDesc);
    }

    void showMagnetic() {
        base("Magnetic");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(16));
        card.setBackground(bg(Color.WHITE,20));

        TextView title = text("🧲  Magnetic Tool",24,true);
        card.addView(title,lp(-1,dp(48)));

        TextView sub = text(
                "Contrôle simple du circulateur chaudière",
                15,false);
        sub.setTextColor(Color.DKGRAY);
        card.addView(sub,lp(-1,dp(38)));

        rotor = new RotorView(this);
        card.addView(rotor,lp(-1,dp(190)));

        stateText = text("Approchez le téléphone du circulateur",17,true);
        stateText.setGravity(Gravity.CENTER);
        stateText.setBackground(bg(Color.rgb(235,245,251),14));
        stateText.setPadding(dp(8),dp(5),dp(8),dp(5));
        card.addView(stateText,marginLp(-1,dp(58),0,4,0,8));

        fieldText = text("Champ magnétique : -- µT",18,true);
        fieldText.setGravity(Gravity.CENTER);
        card.addView(fieldText,lp(-1,dp(48)));

        TextView help = text(
                "Approchez le haut ou l’arrière du téléphone du circulateur. " +
                "La roue s’anime lorsqu’une variation magnétique est détectée.",
                14,false);
        help.setTextColor(NAVY);
        help.setPadding(dp(14),dp(12),dp(14),dp(12));
        help.setBackground(bg(Color.rgb(230,244,252),14));
        card.addView(help,marginLp(-1,-2,0,6,0,10));

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);

        Button snd = btn("🔊 Son");
        Button vib = btn("📳 Vibration");

        options.addView(snd,new LinearLayout.LayoutParams(0,dp(52),1));
        LinearLayout.LayoutParams vibLp =
                new LinearLayout.LayoutParams(0,dp(52),1);
        vibLp.setMargins(dp(8),0,0,0);
        options.addView(vib,vibLp);

        snd.setOnClickListener(v -> {
            sound = !sound;
            snd.setText(sound ? "🔊 Son" : "🔇 Son coupé");
        });

        vib.setOnClickListener(v -> {
            vibration = !vibration;
            vib.setText(vibration ? "📳 Vibration" : "Vibration coupée");
        });

        card.addView(options);

        content.addView(card,marginLp(-1,-2,0,4,0,12));

        TextView note = text(
                "ⓘ Aide au diagnostic : la détection d’une activité magnétique " +
                "ne confirme pas à elle seule le débit hydraulique du circulateur.",
                13,false);
        note.setPadding(dp(14),dp(12),dp(14),dp(12));
        note.setBackground(bg(Color.rgb(230,244,252),14));
        content.addView(note);

        baseline = -1;
        samples.clear();
        lastDetected = 0;

        if (magnetic == null) {
            stateText.setText("Magnétomètre indisponible");
            stateText.setTextColor(RED);
            fieldText.setText("Capteur non disponible");
        } else {
            sm.registerListener(
                    this,
                    magnetic,
                    SensorManager.SENSOR_DELAY_GAME
            );
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {

        if (e.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD ||
                rotor == null ||
                fieldText == null) return;

        float x = e.values[0];
        float y = e.values[1];
        float z = e.values[2];

        float magnitude =
                (float)Math.sqrt(x*x + y*y + z*z);

        fieldText.setText(
                String.format(
                        Locale.FRANCE,
                        "Champ magnétique : %.1f µT",
                        magnitude
                )
        );

        samples.addLast(magnitude);

        if (samples.size() > 28)
            samples.removeFirst();

        if (baseline < 0)
            baseline = magnitude;
        else
            baseline = baseline * .985f + magnitude * .015f;

        float mean = 0;

        for (float f : samples)
            mean += f;

        mean /= Math.max(1,samples.size());

        float dev = 0;

        for (float f : samples)
            dev += Math.abs(f - mean);

        dev /= Math.max(1,samples.size());

        boolean active =
                samples.size() > 12 &&
                (dev > 2.2f ||
                        Math.abs(magnitude-baseline) > 8f);

        long now = System.currentTimeMillis();

        if (active) {

            lastDetected = now;

            rotor.running = true;
            rotor.invalidate();

            stateText.setText("✓ Activité magnétique détectée");
            stateText.setTextColor(GREEN);

            if (now-lastFeedback > 1800) {

                lastFeedback = now;

                if (sound)
                    tone.startTone(
                            ToneGenerator.TONE_PROP_BEEP,
                            90
                    );

                if (vibration) {

                    Vibrator vb =
                            (Vibrator)getSystemService(
                                    VIBRATOR_SERVICE
                            );

                    if (Build.VERSION.SDK_INT >= 26)
                        vb.vibrate(
                                VibrationEffect.createOneShot(
                                        80,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                )
                        );
                    else
                        vb.vibrate(80);
                }
            }

        } else if (now-lastDetected > 1200) {

            rotor.running = false;
            rotor.invalidate();

            stateText.setText("Aucune activité détectée");
            stateText.setTextColor(RED);
        }
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy
    ) {}

    void showCounter() {

        base("Compteur");

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(16),dp(12),
                dp(16),dp(18)
        );

        card.setBackground(
                bg(Color.WHITE,20)
        );

        TextView title =
                text(
                        "⏱  Top compteur gaz",
                        23,true
                );

        card.addView(
                title,
                lp(-1,dp(48))
        );

        final int[] duration = {60};
        final int[] remaining = {60};
        final boolean[] running = {false};
        final Runnable[] tick = {null};

        TextView clock =
                text("01:00",48,true);

        clock.setGravity(Gravity.CENTER);
        clock.setTextColor(NAVY);

        card.addView(
                clock,
                lp(-1,dp(85))
        );

        LinearLayout presets =
                new LinearLayout(this);

        int[] secs =
                {30,60,120,300};

        String[] names =
                {"30 s","1 min","2 min","5 min"};

        for (int i=0;i<4;i++) {

            Button b = btn(names[i]);

            final int s = secs[i];

            b.setOnClickListener(v -> {

                if (!running[0]) {

                    duration[0] = s;
                    remaining[0] = s;
                    clock.setText(time(s));
                }
            });

            LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(
                            0,dp(48),1
                    );

            if (i > 0)
                p.setMargins(dp(5),0,0,0);

            presets.addView(b,p);
        }

        card.addView(
                presets,
                marginLp(
                        -1,dp(48),
                        0,4,0,16
                )
        );

        EditText startIndex =
                new EditText(this);

        startIndex.setHint(
                "Index de départ (m³)"
        );

        startIndex.setTextSize(17);
        startIndex.setSingleLine(true);
        startIndex.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        startIndex.setPadding(
                dp(12),0,dp(12),0
        );

        startIndex.setBackground(
                bg(Color.rgb(240,246,250),12)
        );

        card.addView(
                startIndex,
                marginLp(
                        -1,dp(58),
                        0,0,0,10
                )
        );

        EditText endIndex =
                new EditText(this);

        endIndex.setHint(
                "Index de fin (m³)"
        );

        endIndex.setTextSize(17);
        endIndex.setSingleLine(true);

        endIndex.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        endIndex.setPadding(
                dp(12),0,dp(12),0
        );

        endIndex.setBackground(
                bg(Color.rgb(240,246,250),12)
        );

        card.addView(
                endIndex,
                marginLp(
                        -1,dp(58),
                        0,0,0,14
                )
        );

        Button start =
                btn("▶  Lancer le contrôle");

        start.setTextSize(16);

        card.addView(
                start,
                marginLp(
                        -1,dp(58),
                        0,0,0,10
                )
        );

        Button calc =
                btn("Calculer l’écart");

        calc.setTextSize(16);

        card.addView(
                calc,
                marginLp(
                        -1,dp(54),
                        0,0,0,12
                )
        );

        TextView result =
                text(
                        "Écart compteur : 0.000 m³",
                        20,true
                );

        result.setGravity(Gravity.CENTER);
        result.setTextColor(GREEN);
        result.setBackground(
                bg(Color.rgb(235,250,240),14)
        );

        card.addView(
                result,
                lp(-1,dp(68))
        );

        TextView safety =
                text(
                        "Outil d’aide au contrôle : " +
                        "le résultat correspond uniquement " +
                        "à la différence entre les deux index saisis.",
                        13,false
                );

        safety.setPadding(
                dp(8),dp(12),dp(8),0
        );

        safety.setTextColor(
                Color.DKGRAY
        );

        card.addView(safety);

        tick[0] = new Runnable() {

            public void run() {

                if (!running[0])
                    return;

                remaining[0]--;

                clock.setText(
                        time(remaining[0])
                );

                if (remaining[0] <= 0) {

                    running[0] = false;

                    start.setText(
                            "✓ Contrôle terminé"
                    );

                    endIndex.requestFocus();

                } else {

                    handler.postDelayed(
                            this,1000
                    );
                }
            }
        };

        start.setOnClickListener(v -> {

            if (startIndex
                    .getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                startIndex.setError(
                        "Saisissez l’index de départ"
                );

                return;
            }

            remaining[0] = duration[0];

            clock.setText(
                    time(remaining[0])
            );

            running[0] = true;

            start.setText(
                    "● Contrôle en cours…"
            );

            handler.removeCallbacks(
                    tick[0]
            );

            handler.postDelayed(
                    tick[0],1000
            );
        });

        calc.setOnClickListener(v -> {

            try {

                double a =
                        Double.parseDouble(
                                startIndex
                                        .getText()
                                        .toString()
                                        .replace(',','.')
                        );

                double z =
                        Double.parseDouble(
                                endIndex
                                        .getText()
                                        .toString()
                                        .replace(',','.')
                        );

                if (z < a) {

                    endIndex.setError(
                            "L’index final doit être supérieur ou égal"
                    );

                    return;
                }

                result.setText(
                        String.format(
                                Locale.FRANCE,
                                "Écart compteur : %.3f m³",
                                z-a
                        )
                );

            } catch(Exception ex) {

                endIndex.setError(
                        "Saisissez les deux index"
                );
            }
        });

        content.addView(
                card,
                marginLp(
                        -1,-2,
                        0,4,0,10
                )
        );
    }

    String time(int seconds) {

        return String.format(
                Locale.FRANCE,
                "%02d:%02d",
                seconds/60,
                seconds%60
        );
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (sm != null)
            sm.unregisterListener(this);
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (magnetic != null &&
                rotor != null)

            sm.registerListener(
                    this,
                    magnetic,
                    SensorManager.SENSOR_DELAY_GAME
            );
    }

    static class RotorView extends View {

        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        boolean running = false;
        float angle = 0;

        RotorView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {

            super.onDraw(c);

            float cx = getWidth()/2f;
            float cy = getHeight()/2f;

            float r =
                    Math.min(
                            getWidth(),
                            getHeight()
                    ) * .34f;

            p.setColor(NAVY);

            c.drawCircle(
                    cx,cy,r,p
            );

            c.save();

            c.rotate(
                    angle,cx,cy
            );

            p.setColor(
                    running
                            ? ORANGE
                            : Color.rgb(
                                    105,115,125
                            )
            );

            for (int i=0;i<5;i++) {

                c.save();

                c.rotate(
                        i*72,
                        cx,cy
                );

                RectF q =
                        new RectF(
                                cx-r*.12f,
                                cy-r*.72f,
                                cx+r*.30f,
                                cy-r*.12f
                        );

                c.drawOval(q,p);

                c.restore();
            }

            c.restore();

            p.setColor(Color.LTGRAY);

            c.drawCircle(
                    cx,cy,
                    r*.14f,p
            );

            p.setColor(NAVY);

            c.drawCircle(
                    cx,cy,
                    r*.055f,p
            );

            if (running) {

                angle += 12;

                postInvalidateDelayed(32);
            }
        }
    }
}
