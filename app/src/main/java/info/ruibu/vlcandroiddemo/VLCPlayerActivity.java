package info.ruibu.vlcandroiddemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import info.ruibu.util.LibVLCUtil;
import info.ruibu.util.SystemUtil;

public class VLCPlayerActivity extends AppCompatActivity {
    private IVLCVout vlcVout;
    private MediaPlayer mediaPlayer;

    private SurfaceView surfaceView;
    private RelativeLayout rlHub;
    private SeekBar seekBarTime;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private ImageView imgPlay;
    private SeekBar seekBarVolume;
    private TextView tvFullScreen;
    private boolean isFullScreen = false;
    private long totalTime = 0;

    private SeekBar.OnSeekBarChangeListener onTimeSeekBarChangeListener;
    private SeekBar.OnSeekBarChangeListener onVolumeSeekBarChangeListener;
    private IVLCVout.Callback callback;
    private MediaPlayer.EventListener eventListener;

    private int videoWidth;
    private int videoHight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_vlc_player);

            Intent intent = getIntent();
            if (intent == null) {
                return;
            }

            RelativeLayout rlPlayer = (RelativeLayout) findViewById(R.id.rlPlayer);
            rlPlayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isFullScreen) {
                        rlHub.setVisibility(View.VISIBLE);
                    }
                }
            });

            surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            rlHub = (RelativeLayout) findViewById(R.id.rlHub);
            seekBarTime = (SeekBar) findViewById(R.id.seekBarTime);
            onTimeSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    try {
                        if (!mediaPlayer.isSeekable() || totalTime == 0) {
                            return;
                        }

                        if (progress > totalTime) {
                            progress = (int) totalTime;
                        }

                        if (fromUser) {
                            mediaPlayer.setTime((long) progress);
                            tvCurrentTime.setText(SystemUtil.getMediaTime(progress));
                        }
                    } catch (Exception e) {
                        Log.d("vlc-time", e.toString());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

            seekBarTime.setOnSeekBarChangeListener(onTimeSeekBarChangeListener);
            tvCurrentTime = (TextView) findViewById(R.id.tvCurrentTime);
            tvTotalTime = (TextView) findViewById(R.id.tvTotalTime);
            imgPlay = (ImageView) findViewById(R.id.imgPlay);
            imgPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        imgPlay.setBackgroundResource(R.drawable.videoviewx_play);
                    } else {
                        mediaPlayer.play();
                        imgPlay.setBackgroundResource(R.drawable.videoviewx_pause);
                    }
                }
            });

            LibVLC libvlc = LibVLCUtil.getLibVLC(null);
            surfaceHolder.setKeepScreenOn(true);
            mediaPlayer = new MediaPlayer(libvlc);
            vlcVout = mediaPlayer.getVLCVout();
            callback = new IVLCVout.Callback() {
                @Override
                public void onNewLayout(IVLCVout ivlcVout, int i, int i1, int i2, int i3, int i4, int i5) {
                    try {
                        totalTime = mediaPlayer.getLength();
                        seekBarTime.setMax((int) totalTime);
                        tvTotalTime.setText(SystemUtil.getMediaTime((int) totalTime));

                        videoWidth = i;
                        videoHight = i1;

                        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                        Display display = windowManager.getDefaultDisplay();
                        Point point = new Point();
                        display.getSize(point);

                        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
                        layoutParams.width = point.x;
                        layoutParams.height = (int) Math.ceil((float) videoHight * (float) point.x / (float) videoWidth);
                        surfaceView.setLayoutParams(layoutParams);
                    } catch (Exception e) {
                        Log.d("vlc-newlayout", e.toString());
                    }
                }

                @Override
                public void onSurfacesCreated(IVLCVout ivlcVout) {

                }

                @Override
                public void onSurfacesDestroyed(IVLCVout ivlcVout) {

                }
            };
            vlcVout.addCallback(callback);
            vlcVout.setVideoView(surfaceView);
            vlcVout.attachViews();

            Media media;
            if (intent.getStringExtra("VideoType").equals("Local")) {
                media = new Media(libvlc, intent.getStringExtra("VideoUrl"));
            } else {
                media = new Media(libvlc, Uri.parse(intent.getStringExtra("VideoUrl")));
            }

            mediaPlayer.setMedia(media);

            eventListener = new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    try {
                        if (event.getTimeChanged() == 0 || totalTime == 0 || event.getTimeChanged() > totalTime) {
                            return;
                        }

                        seekBarTime.setProgress((int) event.getTimeChanged());
                        tvCurrentTime.setText(SystemUtil.getMediaTime((int) event.getTimeChanged()));

                        //播放结束
                        if (mediaPlayer.getPlayerState() == Media.State.Ended) {
                            seekBarTime.setProgress(0);
                            mediaPlayer.setTime(0);
                            tvTotalTime.setText(SystemUtil.getMediaTime((int) totalTime));
                            mediaPlayer.stop();
                            imgPlay.setBackgroundResource(R.drawable.videoviewx_play);
                        }
                    } catch (Exception e) {
                        Log.d("vlc-event", e.toString());
                    }
                }
            };
            mediaPlayer.setEventListener(eventListener);

            seekBarVolume = (SeekBar) findViewById(R.id.seekBarVolume);
            onVolumeSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.setVolume(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };
            seekBarVolume.setOnSeekBarChangeListener(onVolumeSeekBarChangeListener);

            tvFullScreen = (TextView) findViewById(R.id.tvFullScreen);
            tvFullScreen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isFullScreen = !isFullScreen;
                    if (isFullScreen) {
                        tvFullScreen.setText("退出全屏");
                        rlHub.setVisibility(View.GONE);
                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                        getWindow().setAttributes(params);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                    } else {
                        tvFullScreen.setText("全屏");
                        rlHub.setVisibility(View.VISIBLE);
                        WindowManager.LayoutParams params = getWindow().getAttributes();
                        params.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        getWindow().setAttributes(params);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                    }
                }
            });

            mediaPlayer.play();
        } catch (Exception e) {
            Log.d("VideoPlayer", e.toString());
        }
    }

    private void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            imgPlay.setBackgroundResource(R.drawable.videoviewx_play);
        }

        vlcVout.detachViews();

        seekBarTime.setOnSeekBarChangeListener(null);
        seekBarVolume.setOnSeekBarChangeListener(null);
        vlcVout.removeCallback(callback);
        mediaPlayer.setEventListener(null);
    }

    private void resumePlay() {
        vlcVout.setVideoView(surfaceView);
        vlcVout.attachViews();

        seekBarTime.setOnSeekBarChangeListener(onTimeSeekBarChangeListener);
        seekBarVolume.setOnSeekBarChangeListener(onVolumeSeekBarChangeListener);
        vlcVout.addCallback(callback);
        mediaPlayer.setEventListener(eventListener);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();

            pausePlay();
            mediaPlayer.release();
        } catch (Exception e) {
            Log.d("vlc-destroy", e.toString());
        }
    }

    @Override
    protected void onStop() {
        try {
            super.onStop();

            pausePlay();
        } catch (Exception e) {
            Log.d("vlc-stop", e.toString());
        }
    }

    @Override
    protected void onPause() {
        try {
            super.onPause();

            pausePlay();
        } catch (Exception e) {
            Log.d("vlc-pause", e.toString());
        }
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();

            resumePlay();
        } catch (Exception e) {
            Log.d("vlc-resume", e.toString());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            super.onNewIntent(intent);
        } catch (Exception e) {
            Log.d("vlc-newintent", e.toString());
        }
    }

    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();

            finish();
        } catch (Exception e) {
            Log.d("vlc-back", e.toString());
        }
    }
}