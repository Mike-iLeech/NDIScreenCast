package ru.ileech.ndiscreencast;

import android.media.*;
import android.media.projection.MediaProjection;
import java.nio.ByteBuffer;

/** Captures eligible playback only. Never opens the microphone. */
final class PlaybackAudio {
 interface Failure { void report(Throwable error); }
 private static final int RATE=48000, CHANNELS=2, FRAME_BYTES=4, BLOCK_BYTES=960*FRAME_BYTES;
 private final AudioRecord record;
 private final Thread thread;
 private volatile boolean active=true;

 PlaybackAudio(MediaProjection projection,long sender,Failure failure){
  AudioPlaybackCaptureConfiguration config=new AudioPlaybackCaptureConfiguration.Builder(projection)
   .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
   .addMatchingUsage(AudioAttributes.USAGE_GAME)
   .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build();
  int min=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT);
  if(min<=0)throw new IllegalStateException("Системный звук 48 кГц не поддерживается");
  record=new AudioRecord.Builder().setAudioPlaybackCaptureConfig(config)
   .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_IN_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
   .setBufferSizeInBytes(Math.max(min*2,BLOCK_BYTES*4)).build();
  if(record.getState()!=AudioRecord.STATE_INITIALIZED){record.release();throw new IllegalStateException("Не удалось открыть системный звук");}
  thread=new Thread(()->{
   try{
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
    record.startRecording();
    if(record.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING)throw new IllegalStateException("Захват звука не запущен");
    ByteBuffer pcm=ByteBuffer.allocateDirect(BLOCK_BYTES);
    AudioTimestamp timestamp=new AudioTimestamp();long frames=0,anchor=0;
    while(active){
     pcm.clear();int bytes=record.read(pcm,BLOCK_BYTES,AudioRecord.READ_BLOCKING);
     if(!active)break;
     if(bytes<0)throw new IllegalStateException("Ошибка захвата звука: "+bytes);
     if(bytes==0)continue;
     if(bytes%FRAME_BYTES!=0)throw new IllegalStateException("Неполный аудиокадр");
     int samples=bytes/FRAME_BYTES;
     if(anchor==0)anchor=System.nanoTime()-(frames+samples)*1_000_000_000L/RATE;
     long time=anchor+frames*1_000_000_000L/RATE;
     if(record.getTimestamp(timestamp,AudioTimestamp.TIMEBASE_MONOTONIC)==AudioRecord.SUCCESS)
      time=timestamp.nanoTime+(frames-timestamp.framePosition)*1_000_000_000L/RATE;
     Ndi.audio(sender,pcm,samples,time/100);
     frames+=samples;
    }
   }catch(Throwable error){if(active)failure.report(error);}
   finally{active=false;try{record.stop();}catch(IllegalStateException ignored){}record.release();}
  },"NDI playback audio");
 }
 void start(){thread.start();}
 // Wait until the last native audio send has finished before destroying its sender.
 void close(){
  active=false;
  try{record.stop();}catch(IllegalStateException ignored){}
  boolean interrupted=false;
  while(thread.isAlive())try{thread.join();}catch(InterruptedException e){interrupted=true;}
  if(interrupted)Thread.currentThread().interrupt();
 }
}
