package ru.ileech.ndiscreencast;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
public class SettingsActivity extends Activity {
 private TextView result;private final Handler handler=new Handler();
 private final Runnable refresh=new Runnable(){public void run(){result.setText(Updates.status);Updates.offer(SettingsActivity.this);handler.postDelayed(this,1000);}};
 private int dp(int n){return Controls.dp(this,n);}
 private TextView text(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);return v;}
 private void gap(LinearLayout l,int n){l.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
 private void toggle(LinearLayout root,String title,String key,boolean fallback){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(18),dp(12),dp(12),dp(12));row.setBackground(Controls.pill(this,0xff1e293b));root.addView(row);
  TextView label=text(title,16);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));Switch control=new Switch(this);control.setShowText(false);control.setChecked(Updates.prefs(this).getBoolean(key,fallback));row.addView(control,new LinearLayout.LayoutParams(dp(64),dp(48)));control.setContentDescription(title);
  control.setOnCheckedChangeListener((v,on)->{Updates.prefs(this).edit().putBoolean(key,on).apply();if(key.equals("autoUpdate")){Updates.schedule(this);if(on)Updates.check(this,false,null);}});gap(root,14);
 }
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff0f172a);getWindow().setNavigationBarColor(0xff0f172a);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff0f172a);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(20),dp(24),dp(24));scroll.addView(root);setContentView(scroll);
  if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});
  else scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  TextView back=text("‹  Настройки",26);back.setPadding(0,dp(8),0,dp(16));back.setOnClickListener(v->finish());root.addView(back);gap(root,20);
  toggle(root,"Отображать битрейт потока","showBitrate",false);toggle(root,"Включить автообновление","autoUpdate",true);
  root.addView(text("Автообновление проверяет GitHub Releases при открытии приложения и в фоне с интервалом 24 часа. Новая версия загружается автоматически. Установку подтверждает Android. Во время эфира установка не предлагается.",14));gap(root,20);
  TextView check=text("Проверить обновления",16);check.setGravity(Gravity.CENTER);check.setPadding(dp(18),dp(14),dp(18),dp(14));check.setBackground(Controls.pill(this,0xff254b3a));root.addView(check);check.setOnClickListener(v->{Updates.check(this,true,null);result.setText(Updates.status);});gap(root,16);
  result=text(Updates.status,14);root.addView(result);gap(root,16);
  try{root.addView(text("Версия "+getPackageManager().getPackageInfo(getPackageName(),0).versionName,14));}catch(Exception ignored){}
 }
 @Override protected void onResume(){super.onResume();handler.post(refresh);}
 @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
