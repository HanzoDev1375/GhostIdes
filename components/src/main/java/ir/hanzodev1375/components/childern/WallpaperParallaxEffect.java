package ir.hanzodev1375.components.childern;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Moves the background when the phone is tilted, exactly like Telegram's "moving wallpaper".
 *
 * <p>Uses the accelerometer, normalises by {@link SensorManager#GRAVITY_EARTH}, computes pitch/roll
 * with {@code atan2}, smooths with a small rolling buffer and finally reports the offset in pixels
 * via {@link Callback#onOffsetsChanged(int, int)}.
 */
public class WallpaperParallaxEffect implements SensorEventListener {

  private static final int SAMPLE_COUNT = 3;
  private static final float MAX_TILT_RANGE = 0.45f;

  private final float[] rollBuffer = new float[SAMPLE_COUNT];
  private final float[] pitchBuffer = new float[SAMPLE_COUNT];
  private int bufferOffset;
  private final WindowManager wm;
  private final SensorManager sensorManager;
  private Sensor accelerometer;
  private boolean enabled;
  private Callback callback;
  private final float maxOffsetPx;

  public WallpaperParallaxEffect(Context context) {
    wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    if (sensorManager != null) {
      accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    maxOffsetPx = 16f * dm.density;
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled == enabled) {
      return;
    }
    this.enabled = enabled;
    if (accelerometer == null || sensorManager == null) {
      return;
    }
    if (enabled) {
      sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    } else {
      sensorManager.unregisterListener(this);
    }
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  /** Scale so the background is larger than its bounds, giving the parallax room to move. */
  public float getScale(int boundsWidth, int boundsHeight) {
    int offset = Math.round(maxOffsetPx);
    int safeW = Math.max(boundsWidth, 1);
    int safeH = Math.max(boundsHeight, 1);
    return Math.max(((float) safeW + offset * 2f) / safeW, ((float) safeH + offset * 2f) / safeH);
  }

  public float getMaxOffsetPx() {
    return maxOffsetPx;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    int rotation = wm != null ? wm.getDefaultDisplay().getRotation() : Surface.ROTATION_0;

    float x = event.values[0] / SensorManager.GRAVITY_EARTH;
    float y = event.values[1] / SensorManager.GRAVITY_EARTH;
    float z = event.values[2] / SensorManager.GRAVITY_EARTH;

    float pitch = (float) (Math.atan2(x, Math.sqrt(y * y + z * z)) / Math.PI * 2.0);
    float roll = (float) (Math.atan2(y, Math.sqrt(x * x + z * z)) / Math.PI * 2.0);

    switch (rotation) {
      case Surface.ROTATION_90:
        float tmp90 = pitch;
        pitch = roll;
        roll = tmp90;
        break;
      case Surface.ROTATION_180:
        roll = -roll;
        pitch = -pitch;
        break;
      case Surface.ROTATION_270:
        float tmp270 = -pitch;
        pitch = roll;
        roll = tmp270;
        break;
      case Surface.ROTATION_0:
      default:
        break;
    }

    rollBuffer[bufferOffset] = roll;
    pitchBuffer[bufferOffset] = pitch;
    bufferOffset = (bufferOffset + 1) % SAMPLE_COUNT;

    roll = 0f;
    pitch = 0f;
    for (int i = 0; i < SAMPLE_COUNT; i++) {
      roll += rollBuffer[i];
      pitch += pitchBuffer[i];
    }
    roll /= SAMPLE_COUNT;
    pitch /= SAMPLE_COUNT;

    if (roll > 1f) {
      roll = 2f - roll;
    } else if (roll < -1f) {
      roll = -2f - roll;
    }

    float vx = Math.max(-1f, Math.min(1f, -pitch / MAX_TILT_RANGE));
    float vy = Math.max(-1f, Math.min(1f, -roll / MAX_TILT_RANGE));

    int offsetX = Math.round(vx * maxOffsetPx);
    int offsetY = Math.round(vy * maxOffsetPx);
    if (callback != null) {
      callback.onOffsetsChanged(offsetX, offsetY);
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}

  public interface Callback {
    void onOffsetsChanged(int offsetX, int offsetY);
  }
}
