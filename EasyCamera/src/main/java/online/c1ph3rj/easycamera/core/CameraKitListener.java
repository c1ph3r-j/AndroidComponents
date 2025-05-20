package online.c1ph3rj.easycamera.core;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageCaptureException;

/** Java-friendly listener mirroring the one you already use. */
public interface CameraKitListener {
    void onLensSwitch(int lensId);
    void onTimerUpdate(long millis);
    void onTimerStart();
    void onTimerPause();
    void onTimerResume();
    void onTimerStop();
    void onTimerDone();
    void onVideoStateChanged(boolean paused);
    void onImageSaved(@NonNull String absolutePath);
    void onImageError(@NonNull ImageCaptureException e);

    void onImageError(@NonNull Exception e);

    void onRecordingInit();
    void onRecordingStart();
    void onRecordingError(int errorCode);
    void onRecordingComplete(@NonNull String videoPath);
    void onFlashStateChange(boolean enabled);
    void onCameraReady();
}

