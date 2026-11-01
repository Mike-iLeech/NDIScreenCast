package ru.ileech.ndiscreencast;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.net.wifi.WifiManager;
import android.net.TrafficStats;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
public class CastService extends Service {
 static volatile boolean running=false;
 static volatile String status="Готово к трансляции";
 static volatile long startedAt=0,durationMs=0;
 static volatile double bitrateMbps=-1;
 private long previousBytes,previousSample;
 private final Runnable telemetry=new Runnable(){public void run(){
  if(closing||startedAt==0)return;
  long now=SystemClock.elapsedRealtime(),bytes=TrafficStats.getUidTxBytes(android.os.Process.myUid());
  bitrateMbps=bytes<0||previousBytes<0||now<=previousSample?-1:Math.max(0,(bytes-previousBytes)*8.0/((now-previousSample)*1000.0));
  previousBytes=bytes;previousSample=now;worker.postDelayed(this,1000);
 }};
 private HandlerThread thread; private Handler worker;
 private MediaProjection projection; private VirtualDisplay display; private ImageReader reader;
 private long sender,nextFrame,lastStatus; private int maxEdge,density,fps; private boolean closing;
 private PlaybackAudio playback; private boolean audioEnabled;
 private WifiManager.MulticastLock multicast; private WifiManager.WifiLock wifi; private PowerManager.WakeLock wake;
 private final MediaProjection.Callback callback=new MediaProjection.Callback(){
  @Override public void onStop(){stopSelf();}
  @Override public void onCapturedContentResize(int w,int h){if(!closing&&display!=null)try{resize(w,h);}catch(Exception e){fail(e);}}
 };
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onCreate(){super.onCreate();thread=new HandlerThread("NDI sender");thread.start();worker=new Handler(thread.getLooper());}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i==null||"stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;} if(running)return START_NOT_STICKY;
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("cast","Трансляция NDI",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,CastService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE);
  if("stop".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  Notification n=new Notification.Builder(this,"cast").setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("NDI ScreenCast").setContentText("Передача экрана по локальной сети").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Остановить",stop).build()).build();
  startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);durationMs=0;startedAt=0;bitrateMbps=-1;running=true;status="Запуск NDI…";
  worker.post(()->{try{
   maxEdge=i.getIntExtra("edge",1280);fps=i.getIntExtra("fps",60)==30?30:60;audioEnabled=i.getBooleanExtra("audio",false);
   sender=Ndi.create(i.getStringExtra("name"));if(sender==0)throw new IllegalStateException("Не удалось создать NDI-источник");
   WifiManager wm=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wm!=null){multicast=wm.createMulticastLock("NDI discovery");multicast.acquire();wifi=wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,"NDI stream");wifi.acquire();}
   wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ndicast:stream");wake.acquire();
   projection=((MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(i.getIntExtra("code",-1),i.getParcelableExtra("data"));
   projection.registerCallback(callback,worker);
   DisplayMetrics m=new DisplayMetrics();((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(m);density=m.densityDpi;
   reader=makeReader(m.widthPixels,m.heightPixels);
   display=projection.createVirtualDisplay("NDI Screen",reader.getWidth(),reader.getHeight(),density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,worker);
   if(audioEnabled){playback=new PlaybackAudio(projection,sender,error->worker.post(()->{if(!closing)fail(error);}));playback.start();}
   startedAt=SystemClock.elapsedRealtime();previousSample=startedAt;previousBytes=TrafficStats.getUidTxBytes(android.os.Process.myUid());worker.postDelayed(telemetry,1000);
   status="В эфире · ожидание NDI-приёмника";
  }catch(Throwable e){status="Ошибка: "+e.getMessage();stopSelf();}});return START_NOT_STICKY;
 }
 private ImageReader makeReader(int w,int h){float scale=Math.min(1f,(float)maxEdge/Math.max(w,h));int width=Math.max(2,((int)(w*scale)/2)*2),height=Math.max(2,((int)(h*scale)/2)*2);
  ImageReader r=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,3);
  r.setOnImageAvailableListener(source->{if(closing)return;try(Image image=source.acquireLatestImage()){
   if(image==null||sender==0)return;long now=System.nanoTime();if(now+1_000_000L<nextFrame)return;
   long interval=1_000_000_000L/fps;nextFrame=(nextFrame==0||now-nextFrame>interval)?now+interval:nextFrame+interval;
   Image.Plane p=image.getPlanes()[0];if(p.getPixelStride()!=4)throw new IllegalStateException("Неподдерживаемый формат экрана");
   Ndi.frame(sender,p.getBuffer(),image.getWidth(),image.getHeight(),p.getRowStride(),fps,now/100);
   if(now-lastStatus>1_000_000_000L){lastStatus=now;status="В эфире · "+width+" × "+height+" · до "+fps+" кадров/с · "+(audioEnabled?"звук: стерео 48 кГц":"без звука")+" · приёмников: "+Ndi.connections(sender);}
  }catch(Exception e){if(!closing)fail(e);}},worker);return r;
 }
 private void resize(int w,int h){if(w<=0||h<=0)return;ImageReader next=makeReader(w,h);if(reader.getWidth()==next.getWidth()&&reader.getHeight()==next.getHeight()){next.close();return;}display.setSurface(null);display.resize(next.getWidth(),next.getHeight(),density);display.setSurface(next.getSurface());ImageReader old=reader;reader=next;old.close();}
 private void fail(Throwable e){status="Ошибка: "+e.getMessage();stopSelf();}
 @Override public void onDestroy(){worker.post(()->{closing=true;worker.removeCallbacks(telemetry);durationMs=startedAt>0?SystemClock.elapsedRealtime()-startedAt:0;startedAt=0;bitrateMbps=-1;if(playback!=null)playback.close();if(display!=null)display.release();if(reader!=null)reader.close();if(projection!=null){projection.unregisterCallback(callback);projection.stop();}if(sender!=0){Ndi.destroy(sender);sender=0;}if(multicast!=null&&multicast.isHeld())multicast.release();if(wifi!=null&&wifi.isHeld())wifi.release();if(wake!=null&&wake.isHeld())wake.release();running=false;if(!status.startsWith("Ошибка"))status="Трансляция остановлена";thread.quitSafely();});super.onDestroy();}
}
