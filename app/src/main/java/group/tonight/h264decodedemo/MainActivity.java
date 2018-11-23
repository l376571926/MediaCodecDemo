package group.tonight.h264decodedemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceView;
    private static final String[] MP4_FILE_1 = {
            "dongfengpo1_stereo.mp4"//0东风破    快速异常
            , "dongfengpo2_mono.mp4"//1东风破——单声道    快速异常

            , "jiangnanye.mp4"//2江南夜

            , "xiaoyaotan.mp4"//3逍遥叹
            , "liuyuedeyu.mp4"//4六月的雨
            , "sanguolian.mp4"//5三国恋

            , "aimei.mp4"//6暧昧
            , "aifei.mp4"//7爱妃
            , "wohaoxiangzainajianguoni.mp4"//8我好像在哪见过你
            , "gaoshang.mp4"//9高尚

            , "choubaguai.mp4"//10丑八怪
            ,"jigeni.aac"//11几个你
    };
    public static String mMp4FilePath = Environment.getExternalStorageDirectory().getPath()
            + "/zmp4mv/"
            + MP4_FILE_1[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        SurfaceHolder mHolder = mSurfaceView.getHolder();
        findViewById(R.id.play).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }
        switch (v.getId()) {
            case R.id.play:
                //开启视频解码线程
                VideoDecodeThread videoDecodeThread = new VideoDecodeThread(mMp4FilePath);
                videoDecodeThread.setSurfaceView(mSurfaceView);
                videoDecodeThread.start();

                //开启音频解码线程
                AudioDecodeThread audioDecodeThread = new AudioDecodeThread(mMp4FilePath);
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                int sessionId = audioManager.generateAudioSessionId();
                audioDecodeThread.setSessionId(sessionId);
                audioDecodeThread.start();
                break;
            default:
                break;
        }
    }

}
