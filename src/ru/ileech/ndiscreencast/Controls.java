package ru.ileech.ndiscreencast;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import android.text.TextUtils;
import java.util.Locale;

final class Controls {
 static int dp(Context c,float n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
 static GradientDrawable pill(Context c,int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,100));d.setStroke(dp(c,1),0xff344059);return d;}
 static String duration(long millis){long s=Math.max(0,millis/1000);return String.format(Locale.US,"%02d:%02d:%02d",s/3600,(s/60)%60,s%60);}
 static final class Arrow extends Drawable {
  final Paint p=new Paint(3);final Context context;
  Arrow(Context c){context=c;setBounds(0,0,dp(c,14),dp(c,14));}
  @Override public void draw(Canvas c){p.setColor(0xffaab5ce);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(context,2));p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Rect r=getBounds();Path a=new Path();a.moveTo(r.left+r.width()*.2f,r.top+r.height()*.4f);a.lineTo(r.exactCenterX(),r.top+r.height()*.7f);a.lineTo(r.right-r.width()*.2f,r.top+r.height()*.4f);c.drawPath(a,p);}
  @Override public void setAlpha(int n){p.setAlpha(n);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 static void styleSpinner(Spinner spinner,Context c,String[] values){
  spinner.setGravity(Gravity.CENTER);spinner.setBackground(pill(c,0xff1e293b));
  GradientDrawable popup=new GradientDrawable(){@Override public boolean getPadding(Rect out){out.set(dp(c,8),dp(c,10),dp(c,8),dp(c,10));return true;}};
  popup.setColor(0xff253149);popup.setCornerRadius(dp(c,24));popup.setStroke(dp(c,1),0xff344059);spinner.setPopupBackgroundDrawable(popup);
  spinner.setAdapter(new ArrayAdapter<String>(c,android.R.layout.simple_spinner_item,values){
   private TextView item(int position,boolean popup){
    TextView t=new TextView(c);t.setText(getItem(position));t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER);
    t.setMaxLines(1);t.setHorizontallyScrolling(false);t.setEllipsize(TextUtils.TruncateAt.END);
    t.setPadding(dp(c,popup?18:10),dp(c,12),dp(c,popup?18:10),dp(c,12));t.setMinHeight(dp(c,48));
    t.setAutoSizeTextTypeUniformWithConfiguration(11,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    if(!popup){Drawable balance=new ColorDrawable(Color.TRANSPARENT);balance.setBounds(0,0,dp(c,14),dp(c,14));t.setCompoundDrawables(balance,null,new Arrow(c),null);t.setCompoundDrawablePadding(dp(c,4));}
    return t;
   }
   @Override public View getView(int p,View v,android.view.ViewGroup parent){return item(p,false);}
   @Override public View getDropDownView(int p,View v,android.view.ViewGroup parent){return item(p,true);}
  });
 }
 static final class Logo extends LinearLayout {
  Logo(Context c){super(c);setGravity(Gravity.CENTER_VERTICAL);setContentDescription("NDI ScreenCast");
   ImageView icon=new ImageView(c);icon.setImageResource(R.drawable.ic_launcher);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);addView(icon,new LinearLayout.LayoutParams(dp(c,44),dp(c,44)));
   LinearLayout words=new LinearLayout(c);words.setOrientation(VERTICAL);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-2,-2);layout.leftMargin=dp(c,10);addView(words,layout);
   TextView ndi=new TextView(c);ndi.setText("NDI");ndi.setTextSize(25);ndi.setLetterSpacing(.18f);ndi.setTextColor(Color.WHITE);ndi.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));ndi.setIncludeFontPadding(false);words.addView(ndi);
   TextView cast=new TextView(c);cast.setText("SCREENCAST");cast.setTextSize(10);cast.setLetterSpacing(.16f);cast.setTextColor(0xff4ade80);cast.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));cast.setIncludeFontPadding(false);words.addView(cast);
  }
 }
 static final class SoundToggle extends View implements Checkable {
  private boolean checked;private final Paint p=new Paint(3);
  SoundToggle(Context c){super(c);setClickable(true);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setChecked(true);}
  public boolean isChecked(){return checked;}public void toggle(){setChecked(!checked);}
  public void setChecked(boolean value){checked=value;setContentDescription(value?"Звук включён. Выключить":"Звук выключен. Включить");invalidate();}
  @Override public void setEnabled(boolean value){super.setEnabled(value);setAlpha(value?1:.45f);}
  @Override public boolean performClick(){if(isEnabled())toggle();return super.performClick();}
  @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo n){super.onInitializeAccessibilityNodeInfo(n);n.setClassName(Switch.class.getName());n.setCheckable(true);n.setChecked(checked);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float h=getHeight(),w=getWidth();p.setStyle(Paint.Style.FILL);p.setColor(checked?0xff443a72:0xff283449);c.drawRoundRect(1,1,w-1,h-1,h/2,h/2,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(checked?0xff8070d4:0xff48546d);c.drawRoundRect(1,1,w-1,h-1,h/2,h/2,p);p.setStyle(Paint.Style.FILL);
   float r=h*.39f,x=checked?w-h/2:h/2,y=h/2;p.setColor(checked?0xffa696ff:0xff8290a9);c.drawCircle(x,y,r,p);
   c.save();c.translate(x-r*.7f,y-r*.7f);c.scale(r*1.4f/24,r*1.4f/24);p.setColor(0xff142038);Path speaker=new Path();speaker.moveTo(3,9);speaker.lineTo(7,9);speaker.lineTo(12,5);speaker.lineTo(12,19);speaker.lineTo(7,15);speaker.lineTo(3,15);speaker.close();c.drawPath(speaker,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);
   if(checked){c.drawArc(10,7,20,17,-55,110,false,p);c.drawArc(7,3,25,21,-53,106,false,p);}else c.drawLine(3,3,21,21,p);p.setStyle(Paint.Style.FILL);c.restore();
  }
 }
 static final class CastButton extends View {
  private final Paint p=new Paint(3);private boolean live;private long elapsed=-1;
  CastButton(Context c){super(c);setClickable(true);setFocusable(true);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setLive(false,0);}
  void setLive(boolean value,long duration){if(live==value&&elapsed>=0&&elapsed/1000==duration/1000)return;live=value;elapsed=duration;setContentDescription((value?"Остановить трансляцию. ":"Начать трансляцию. ")+Controls.duration(duration));invalidate();}
  @Override public CharSequence getAccessibilityClassName(){return Button.class.getName();}
  @Override public void setEnabled(boolean value){super.setEnabled(value);setAlpha(value?1f:.5f);}
  @Override protected void onDraw(Canvas original){super.onDraw(original);Canvas c=original;c.save();float scale=Math.min(getWidth(),getHeight())/240f;c.translate(getWidth()/2f,getHeight()/2f);c.scale(scale,scale);
   int accent=live?0xfff87171:0xff4ade80;
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f);p.setColor(accent);c.drawCircle(0,0,113,p);
   p.setStrokeWidth(19);p.setShader(new LinearGradient(-80,-80,80,80,live?new int[]{0xff56262c,0xffb4454d,0xff592b34}:new int[]{0xff174332,0xff348d60,0xff1e513c},null,Shader.TileMode.CLAMP));c.drawCircle(0,0,90,p);p.setShader(null);
   p.setStrokeWidth(1.5f);p.setColor(accent);c.drawCircle(0,0,78,p);
   p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(-22,-32,140,live?new int[]{0xff4b252c,0xff21191f}:new int[]{0xff244c37,0xff14271f},null,Shader.TileMode.CLAMP));c.drawCircle(0,0,75,p);p.setShader(null);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(accent);c.drawCircle(0,0,72,p);
   p.setStrokeWidth(2);p.setColor(accent);c.drawCircle(0,0,113,p);
   p.setStyle(Paint.Style.FILL);p.setColor(live?0xff68333a:0xff2c6544);for(int row=0;row<5;row++)for(int col=0;col<5;col++)c.drawCircle(-16+col*8,-51+row*8,1,p);
   p.setColor(accent);
   if(live)c.drawRoundRect(-9,-43,9,-25,3,3,p);else{Path a=new Path();a.moveTo(-7,-46);a.lineTo(11,-34);a.lineTo(-7,-22);a.close();c.drawPath(a,p);}
   p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(9);p.setColor(0xffaab6cc);c.drawText(live?"В ЭФИРЕ":"ГОТОВО К ЭФИРУ",0,4,p);
   p.setTextSize(18);p.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));p.setColor(Color.WHITE);c.drawText(Controls.duration(elapsed),0,29,p);
   if(isPressed()||isFocused()){p.setColor(0x20ffffff);c.drawCircle(0,0,75,p);}c.restore();
  }
  @Override protected void drawableStateChanged(){super.drawableStateChanged();invalidate();}
  @Override public boolean performClick(){return super.performClick();}
 }
}
