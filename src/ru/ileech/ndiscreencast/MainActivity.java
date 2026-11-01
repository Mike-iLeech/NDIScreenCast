package ru.ileech.ndiscreencast;
import android.app.*;
import android.os.*;
import android.content.*;
import android.media.projection.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
public class MainActivity extends Activity {
 private static final int BG=0xff0f172a,MUTED=0xff94a3b8;
 private TextView status,label,hint,bitrate,soundState;private EditText name;private Spinner quality,framerate;
 private RadioGroup modes;private RadioButton screen,app;private Controls.SoundToggle audio;private Controls.CastButton button;
 private boolean pending,waiting,stopping;private long requested;
 private final Handler handler=new Handler();
 private final Runnable refresh=new Runnable(){public void run(){update();handler.postDelayed(this,250);}};
 private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 private void gap(LinearLayout l,int n){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
 private GradientDrawable pill(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(100));return d;}
 private RadioButton mode(String s){RadioButton r=new RadioButton(this);r.setId(View.generateViewId());r.setText(s);r.setTextSize(16);r.setGravity(Gravity.CENTER);r.setButtonDrawable(null);modes.addView(r,new RadioGroup.LayoutParams(0,dp(48),1));return r;}
 @Override public void onCreate(Bundle b){super.onCreate(b);
  if(b!=null){pending=b.getBoolean("pending");waiting=b.getBoolean("waiting");requested=b.getLong("requested");}
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
  LinearLayout root=column();root.setPadding(dp(24),dp(16),dp(24),dp(16));scroll.addView(root);setContentView(scroll);
  if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});
  else scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);header.addView(new Controls.Logo(this),new LinearLayout.LayoutParams(0,-2,1));TextView settings=text("⚙",28,0xffb7c6dd);settings.setGravity(Gravity.CENTER);settings.setContentDescription("Настройки");settings.setBackground(Controls.pill(this,0xff1e293b));header.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));gap(root,12);
  root.addView(text("Имя источника",13,MUTED));name=new EditText(this);name.setSingleLine();name.setTextSize(16);name.setBackground(pill(0xff1e293b));name.setPadding(dp(18),dp(12),dp(18),dp(12));name.setMinHeight(dp(48));name.setTextColor(Color.WHITE);name.setText(getPreferences(0).getString("name","Android Screen"));gap(root,6);root.addView(name);gap(root,12);
  root.addView(text("Что передавать",13,MUTED));gap(root,6);modes=new RadioGroup(this);modes.setOrientation(0);modes.setPadding(dp(4),dp(4),dp(4),dp(4));modes.setBackground(pill(0xff1e293b));root.addView(modes);
  screen=mode("Экран");app=mode("Приложение");modes.check(getPreferences(0).getBoolean("appMode",false)&&Build.VERSION.SDK_INT>=34?app.getId():screen.getId());
  hint=text("",12,MUTED);gap(root,6);root.addView(hint);modes.setOnCheckedChangeListener((g,id)->{save();updateMode();});updateMode();gap(root,10);
  LinearLayout options=new LinearLayout(this);root.addView(options);LinearLayout left=column(),right=column();options.addView(left,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams rightParams=new LinearLayout.LayoutParams(0,-2,1);rightParams.leftMargin=dp(10);options.addView(right,rightParams);
  left.addView(text("Разрешение",13,MUTED));quality=new Spinner(this,Spinner.MODE_DROPDOWN);Controls.styleSpinner(quality,this,new String[]{"До 720p","До 1080p"});quality.setSelection(getPreferences(0).getInt("quality",0));gap(left,6);left.addView(quality,new LinearLayout.LayoutParams(-1,dp(48)));
  right.addView(text("Частота кадров",13,MUTED));framerate=new Spinner(this,Spinner.MODE_DROPDOWN);Controls.styleSpinner(framerate,this,new String[]{"30 кадров/с","60 кадров/с"});framerate.setSelection(getPreferences(0).getInt("fps",1));gap(right,6);right.addView(framerate,new LinearLayout.LayoutParams(-1,dp(48)));
  gap(root,14);LinearLayout soundRow=new LinearLayout(this);soundRow.setGravity(Gravity.CENTER_VERTICAL);soundRow.setPadding(dp(18),dp(10),dp(12),dp(10));soundRow.setBackground(pill(0xff1e293b));root.addView(soundRow);
  LinearLayout soundText=column();soundRow.addView(soundText,new LinearLayout.LayoutParams(0,-2,1));soundText.addView(text("Системный звук",16,Color.WHITE));soundState=text("",12,MUTED);soundText.addView(soundState);
  audio=new Controls.SoundToggle(this);audio.setChecked(getPreferences(0).getBoolean("audio",true));soundRow.addView(audio,new LinearLayout.LayoutParams(dp(88),dp(44)));soundState.setText(audio.isChecked()?"Включён · стерео 48 кГц":"Выключен");audio.setOnClickListener(v->{soundState.setText(audio.isChecked()?"Включён · стерео 48 кГц":"Выключен");save();});
  LinearLayout center=column();center.setGravity(Gravity.CENTER);center.setMinimumHeight(dp(300));root.addView(center,new LinearLayout.LayoutParams(-1,-2,1));
  button=new Controls.CastButton(this);center.addView(button,new LinearLayout.LayoutParams(dp(224),dp(224)));gap(center,6);label=text("Начать трансляцию",15,Color.WHITE);label.setGravity(Gravity.CENTER);label.setPadding(dp(22),dp(12),dp(22),dp(12));label.setBackground(Controls.pill(this,0xff2b334b));center.addView(label,new LinearLayout.LayoutParams(-2,-2));gap(center,10);bitrate=text("Битрейт · — Мбит/с",13,MUTED);bitrate.setGravity(Gravity.CENTER);bitrate.setPadding(dp(16),dp(8),dp(16),dp(8));bitrate.setBackground(Controls.pill(this,0xff192338));center.addView(bitrate,new LinearLayout.LayoutParams(-2,-2));
  button.setOnClickListener(v->{if(CastService.running){stopping=true;stopService(new Intent(this,CastService.class));update();}else begin();});
  label.setOnClickListener(v->button.performClick());
  status=text(CastService.status,13,MUTED);status.setGravity(Gravity.CENTER);root.addView(status);gap(root,10);
  root.addView(text("Одна локальная сеть с NDI-приёмником. Звук: стерео 48 кГц, без микрофона. Захватывается звук всех приложений, разрешивших запись. Для 1080p/60 нужна быстрая сеть. Настройки меняются до запуска.",12,MUTED));gap(root,10);
  TextView link=text("NDI® · ndi.video",13,0xff38bdf8);link.setGravity(Gravity.CENTER);root.addView(link);link.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://ndi.video"))));update();
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=0)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7);
 }
 private void save(){if(name==null||quality==null||framerate==null||audio==null)return;getPreferences(0).edit().putString("name",name.getText().toString().trim()).putInt("quality",quality.getSelectedItemPosition()).putInt("fps",framerate.getSelectedItemPosition()).putBoolean("audio",audio.isChecked()).putBoolean("appMode",modes.getCheckedRadioButtonId()==app.getId()).apply();}
 private void updateMode(){boolean a=modes.getCheckedRadioButtonId()==app.getId();screen.setBackground(pill(a?0xff1e293b:0xff334155));app.setBackground(pill(a?0xff334155:0xff1e293b));screen.setTextColor(a?MUTED:Color.WHITE);app.setTextColor(a?Color.WHITE:MUTED);hint.setText(Build.VERSION.SDK_INT<34?"Android 10–13: доступен только весь экран.":a?"После запуска трансляции выберите приложение в системном диалоге Android.":"После запуска трансляции подтвердите захват всего экрана.");}
 private void putText(TextView view,String value){if(!view.getText().toString().equals(value))view.setText(value);}
 private void update(){if(button==null)return;bitrate.setVisibility(getSharedPreferences("settings",0).getBoolean("showBitrate",false)?View.VISIBLE:View.GONE);Updates.offer(this);boolean live=CastService.running;if(live){pending=false;waiting=false;}if(waiting&&(CastService.status.startsWith("Ошибка")||SystemClock.elapsedRealtime()-requested>10000)){pending=false;waiting=false;}if(!live)stopping=false;boolean edit=!live&&!pending;
  name.setEnabled(edit);quality.setEnabled(edit);framerate.setEnabled(edit);audio.setEnabled(edit);screen.setEnabled(edit);app.setEnabled(edit&&Build.VERSION.SDK_INT>=34);long elapsed=CastService.startedAt>0?SystemClock.elapsedRealtime()-CastService.startedAt:CastService.durationMs;button.setLive(live,elapsed);button.setEnabled(!pending&&!stopping);label.setEnabled(!pending&&!stopping);putText(bitrate,CastService.bitrateMbps<0?"Битрейт · — Мбит/с":String.format(java.util.Locale.US,"Битрейт · ≈ %.1f Мбит/с",CastService.bitrateMbps));putText(label,stopping?"Остановка…":pending?"Ожидание разрешения…":live?"Остановить трансляцию":"Начать трансляцию");putText(status,CastService.status);
 }
 private void begin(){if(CastService.running||pending)return;if(name.getText().toString().trim().isEmpty()){name.setError("Введите имя источника");return;}save();pending=true;update();if(audio.isChecked()&&checkSelfPermission("android.permission.RECORD_AUDIO")!=0){requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},8);return;}requestCapture();}
 private void requestCapture(){MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);Intent i;if(Build.VERSION.SDK_INT>=34)i=m.createScreenCaptureIntent(modes.getCheckedRadioButtonId()==app.getId()?MediaProjectionConfig.createConfigForUserChoice():MediaProjectionConfig.createConfigForDefaultDisplay());else i=m.createScreenCaptureIntent();startActivityForResult(i,42);}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] results){super.onRequestPermissionsResult(r,p,results);if(r==8){if(results.length>0&&results[0]==0)requestCapture();else{pending=false;update();Toast.makeText(this,"Разрешите запись аудио или выключите звук.",Toast.LENGTH_LONG).show();}}}
 @Override protected void onActivityResult(int r,int code,Intent data){super.onActivityResult(r,code,data);if(r!=42)return;if(code==RESULT_OK&&data!=null){Intent i=new Intent(this,CastService.class);i.putExtra("code",code);i.putExtra("data",data);i.putExtra("name",name.getText().toString().trim());i.putExtra("edge",quality.getSelectedItemPosition()==0?1280:1920);i.putExtra("fps",framerate.getSelectedItemPosition()==0?30:60);i.putExtra("audio",audio.isChecked());waiting=true;requested=SystemClock.elapsedRealtime();startForegroundService(i);}else pending=false;update();}
 @Override protected void onSaveInstanceState(Bundle b){save();b.putBoolean("pending",pending);b.putBoolean("waiting",waiting);b.putLong("requested",requested);super.onSaveInstanceState(b);}
 @Override protected void onResume(){super.onResume();handler.removeCallbacks(refresh);handler.post(refresh);Updates.schedule(this);Updates.check(this,false,null);}
 @Override protected void onPause(){save();handler.removeCallbacks(refresh);super.onPause();}
}
