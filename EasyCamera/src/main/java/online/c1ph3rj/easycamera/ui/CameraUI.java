package online.c1ph3rj.easycamera.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.video.Quality;
import androidx.cardview.widget.CardView;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

import online.c1ph3rj.easycamera.R;
import online.c1ph3rj.easycamera.core.CameraKit;
import online.c1ph3rj.easycamera.core.CameraKitListener;
import online.c1ph3rj.easycamera.core.MediaStoreStrategy;

/**
 * Sample screen that shows how to use CameraKit with the UI you provided.
 * One-tap => photo, long-press => start / stop video, live timer overlay,
 * flash toggle, lens flip, and automatic permission flow.
 */
public class CameraUI extends AppCompatActivity implements CameraKitListener {

    // ------------------- view refs -------------------
    private PreviewView previewView;
    private CardView    timerCard;
    private TextView    timerText;
    private ImageView   flashIcon;
    private ImageView   captureIcon;
    private CardView        flashBtn;
    private CardView   flipBtn;
    private ImageView flipIcon;
    private boolean     hasFlashFeature;
    private boolean     isVideoPaused = false;

    // ------------------- camera ----------------------
    private CameraKit cameraKit;
    private boolean   recording = false;

    // ------------------- long-press ------------------
    private static final int LONG_PRESS_THRESHOLD = 350; // ms
    private long downTime;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera_ui);

        hasFlashFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        // handle system bars padding (template code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return insets;
        });

        bindViews();
        initCameraKit();
        wireControls();
    }

    // --------------------------------------------------
    // binding helpers
    // --------------------------------------------------
    private void bindViews() {
        previewView  = findViewById(R.id.cameraPreview);
        timerCard    = findViewById(R.id.timerTextView);
        timerText    = findViewById(R.id.timerText);
        flashIcon    = findViewById(R.id.flashIcon);
        flashBtn      = findViewById(R.id.flashBtn);
        if (!hasFlashFeature) {
            flashBtn.setVisibility(View.GONE);
        }
        captureIcon  = findViewById(R.id.captureIcon);
        flipBtn = findViewById(R.id.flipCamBtn);
        flipIcon = findViewById(R.id.flipCamIcon);

        timerCard.setVisibility(View.INVISIBLE);
    }

    private void initCameraKit() {
        cameraKit = CameraKit.Builder
                .with(this, previewView)
                .defaultLens(CameraKit.LENS_BACK)
                .enablePinchToZoom(true)
                .enableTapToFocus(true)
                .listener(this)
                .build();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void wireControls() {
        // flash toggle
        findViewById(R.id.flashBtn).setOnClickListener(v -> cameraKit.toggleFlash());

        // lens flip or pause/resume during recording
        flipBtn.setOnClickListener(v -> {
            if (recording) {
                if (isVideoPaused) {
                    cameraKit.resumeVideo();
                } else {
                    cameraKit.pauseVideo();
                }
            } else {
                cameraKit.switchLens();
            }
        });

        // capture vs record (tap vs long-press, and stop on tap when recording)
        View captureBtn = findViewById(R.id.captureBtn);

        captureBtn.setOnClickListener(v -> {
            if (recording) {
                cameraKit.stopVideo();
            } else {
                cameraKit.captureImage();
            }
        });

        captureBtn.setOnLongClickListener(v -> {
            if (!recording && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                cameraKit.captureVideo();
                return true;
            }
            return false;
        });
    }

    // --------------------------------------------------
    // lifecycle
    // --------------------------------------------------
    @Override protected void onStart() {
        super.onStart();
        cameraKit.start();
    }

    @Override protected void onStop() {
        super.onStop();
        cameraKit.release();
    }

    // --------------------------------------------------
    // CameraKitListener implementation
    // --------------------------------------------------

    /* Camera ready */
    @Override public void onCameraReady() { }

    /* Flash toggle */
    @Override public void onFlashStateChange(boolean enabled) {
        runOnUiThread(() -> {
            flashIcon.setImageResource(enabled ? R.drawable.flash_on_ic : R.drawable.flash_off_ic);
        });
    }

    /* Photos */
    @Override public void onImageSaved(@NonNull String path) {
        runOnUiThread(() -> {
            Toast.makeText(CameraUI.this, "Saved photo: " + path, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onImageError(@NonNull ImageCaptureException e) {

    }

    @Override
    public void onImageError(@NonNull Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(CameraUI.this, "Photo error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /* Recording lifecycle */
    @Override public void onRecordingInit() { }
    @Override public void onRecordingStart() {
        runOnUiThread(() -> {
            recording = true;
            captureIcon.setImageResource(R.drawable.stop_ic);
            flipIcon.setImageResource(R.drawable.pause_ic);
        });
    }
    @Override public void onRecordingComplete(@NonNull String path) {
        runOnUiThread(() -> {
            recording = false;
            captureIcon.setImageResource(R.drawable.camera_ic);
            flipIcon.setImageResource(R.drawable.flip_cam_ic);
            Toast.makeText(CameraUI.this, "Saved video: " + path, Toast.LENGTH_SHORT).show();
        });
    }
    @Override public void onRecordingError(int code) {
        runOnUiThread(() -> {
            Toast.makeText(CameraUI.this, "Recording error: " + code, Toast.LENGTH_SHORT).show();
            captureIcon.setImageResource(R.drawable.camera_ic);
        });
    }

    /* Timer handling */
    @Override public void onTimerStart() {
        runOnUiThread(() -> {
            timerCard.setVisibility(View.VISIBLE);
        });
    }
    @Override public void onTimerStop() {
        runOnUiThread(() -> {
            timerCard.setVisibility(View.INVISIBLE);
        });
    }
    @Override public void onTimerPause() {
        runOnUiThread(() -> {
            timerText.setTextColor(Color.YELLOW);
            // Blink animation
            AlphaAnimation blink = new AlphaAnimation(0.0f, 1.0f);
            blink.setDuration(500);
            blink.setInterpolator(new LinearInterpolator());
            blink.setRepeatCount(Animation.INFINITE);
            blink.setRepeatMode(Animation.REVERSE);
            timerText.startAnimation(blink);
        });
    }
    @Override public void onTimerResume() {
        runOnUiThread(() -> {
            timerText.setAlpha(1f);
            // Stop blink and reset color
            timerText.clearAnimation();
            timerText.setTextColor(Color.WHITE);
        });
    }

    @Override public void onTimerUpdate(long ms) {
        runOnUiThread(() -> {
            timerText.setText(format(ms));
        });
    }
    @Override public void onTimerDone()  {
        isVideoPaused = false;
    }

    /* Lens + pause state */
    @Override
    public void onLensSwitch(int lensId) {
        runOnUiThread(() -> {
            if (lensId == CameraKit.LENS_FRONT) {
                flashBtn.setVisibility(View.GONE);
            } else {
                flashBtn.setVisibility(hasFlashFeature ? View.VISIBLE : View.GONE);
            }
        });
    }
    @Override
    public void onVideoStateChanged(boolean paused) {
        runOnUiThread(() -> {
            isVideoPaused = paused;
            flipIcon.setImageResource(paused ? R.drawable.resume_ic : R.drawable.pause_ic);
        });
    }

    // --------------------------------------------------
    // utility
    // --------------------------------------------------
    private static String format(long ms) {
        long s = ms / 1000, m = s / 60, h = m / 60;
        s %= 60; m %= 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
    }
}