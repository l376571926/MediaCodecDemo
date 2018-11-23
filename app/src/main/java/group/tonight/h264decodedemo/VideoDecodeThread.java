package group.tonight.h264decodedemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecodeThread extends Thread implements Runnable {
    private static final String TAG = VideoDecodeThread.class.getSimpleName();
    private MediaCodec mVideoDecoder;
    private MediaExtractor mMediaExtractor;
    private int mVideoTrackIndex = -1;
    private SurfaceView mSurfaceView;
    private String mMp4FilePath;

    public VideoDecodeThread(String path) {
        mMp4FilePath = path;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
    }

    @Override
    public void run() {
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mMp4FilePath);
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime.contains("video")) {
                    mVideoTrackIndex = i;
                    break;
                }
            }
            if (mVideoTrackIndex == -1) {
                mMediaExtractor.release();
                return;
            }
            /*
            东风破
            视频编码器 run: {track-id=1, level=16, mime=video/avc, profile=8, language=und, display-width=288, csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9], durationUs=312960000, display-height=216, width=288, max-input-size=20658, frame-rate=25, height=216, csd-0=java.nio.HeapByteBuffer[pos=0 lim=30 cap=30]}

            东风破——单声道
            视频编码器 run: {track-id=1, level=8, mime=video/avc, profile=8, language=und, color-standard=4, display-width=320, csd-1=java.nio.HeapByteBuffer[pos=0 lim=10 cap=10], color-transfer=3, durationUs=308333333, display-height=240, width=320, color-range=2, max-input-size=11025, frame-rate=15, height=240, csd-0=java.nio.HeapByteBuffer[pos=0 lim=32 cap=32]}

            江南夜
            视频编码器 run: {max-bitrate=21337936, track-id=1, level=4096, mime=video/avc, profile=8, bitrate=6066240, language=und, display-width=1920, csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9], durationUs=281014066, display-height=1080, width=1920, max-input-size=400705, frame-rate=30, height=1080, csd-0=java.nio.HeapByteBuffer[pos=0 lim=29 cap=29]}

            逍遥叹
            视频编码器 run: {max-bitrate=1498272, track-id=1, level=1024, mime=video/avc, profile=8, bitrate=671696, language=und, display-width=565, csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9], durationUs=390880000, display-height=424, width=564, max-input-size=35678, frame-rate=25, height=424, csd-0=java.nio.HeapByteBuffer[pos=0 lim=35 cap=35]}

            六月的雨
            视频编码器 run: {max-bitrate=1972952, track-id=1, level=256, mime=video/avc, profile=2, bitrate=679184, language=chi, display-width=768, csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9], durationUs=222488933, display-height=432, width=768, max-input-size=64502, frame-rate=30, height=432, csd-0=java.nio.HeapByteBuffer[pos=0 lim=25 cap=25]}
             */
            final MediaFormat videoFormat = mMediaExtractor.getTrackFormat(mVideoTrackIndex);
            Log.e(TAG, "视频编码器 run: " + videoFormat.toString());
            String videoMime = videoFormat.getString(MediaFormat.KEY_MIME);
            int frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);//24
            int maxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);//45591


            //如果设置为SurfaceView，那就动态调整它的高度，保持原视频的宽高比
            if (mSurfaceView != null) {
                Context context = mSurfaceView.getContext();
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //按视频大小动态调整SurfaceView的高度
                            Resources resources = mSurfaceView.getResources();

                            final int videoWith = videoFormat.getInteger(MediaFormat.KEY_WIDTH);//1920
                            final int videoHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);//1080

                            int measuredWidth = mSurfaceView.getMeasuredWidth();//2064
                            int measuredHeight = mSurfaceView.getMeasuredHeight();//912

                            //纵屏，宽充满，高按比例缩放
                            int showVideoHeight = videoHeight * measuredWidth / videoWith;
                            //横屏，高充满，宽按比例缩放
                            int showVideoWidth = videoWith * measuredHeight / videoHeight;

                            if (resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, showVideoHeight));
                            } else if (resources.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(showVideoWidth, ViewGroup.LayoutParams.MATCH_PARENT);
                                params.gravity = Gravity.CENTER;
                                mSurfaceView.setLayoutParams(params);
                            }
                        }
                    });
                }
            }

            mVideoDecoder = MediaCodec.createDecoderByType(videoMime);
            mVideoDecoder.configure(videoFormat, mSurfaceView.getHolder().getSurface(), null, 0);
            mVideoDecoder.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(maxInputSize);
            int sampleSize = 0;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mMediaExtractor.selectTrack(mVideoTrackIndex);

            SpeedManager mSpeedManager = new SpeedManager();//音视频同步器

            while (sampleSize != -1) {
                sampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                //填充要解码的数据
                if (sampleSize != -1) {
                    if (sampleSize >= 0) {
                        long sampleTime = mMediaExtractor.getSampleTime();
                        if (sampleTime >= 0) {
                            int inputBufferIndex = mVideoDecoder.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = mVideoDecoder.getInputBuffer(inputBufferIndex);
                                if (inputBuffer != null) {
                                    inputBuffer.clear();
                                    inputBuffer.put(byteBuffer);
                                    mVideoDecoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0);
                                    mSpeedManager.preRender(sampleTime);
                                    mMediaExtractor.advance();
                                }
                            }
                        }
                    }
                }
                //解码已填充的数据
                int outputBufferIndex = mVideoDecoder.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferIndex >= 0) {
                    Thread.sleep(frameRate);//控制帧率在24帧左右
                    mVideoDecoder.releaseOutputBuffer(outputBufferIndex, mSurfaceView != null);
                }
            }
            mSpeedManager.reset();
            mMediaExtractor.unselectTrack(mVideoTrackIndex);
            mMediaExtractor.release();
            mVideoDecoder.stop();
            mVideoDecoder.release();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}