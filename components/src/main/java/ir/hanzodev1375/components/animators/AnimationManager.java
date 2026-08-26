package ir.hanzodev1375.components.animators;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;
import ir.hanzodev1375.ghostide.codeeditors.setting.PreferencesUtils;

public class AnimationManager {

  private static AnimationManager instance;
  private final PreferencesUtils prefs;
  private int currentBatteryLevel = 100;
  private boolean receiverRegistered = false;

  private final BroadcastReceiver batteryReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
          int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
          if (level >= 0 && scale > 0) {
            currentBatteryLevel = (int) ((level / (float) scale) * 100);
          }
        }
      };

  private AnimationManager(Context context) {
    prefs = new PreferencesUtils(context.getApplicationContext());
    Intent batteryStatus =
        context
            .getApplicationContext()
            .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    if (batteryStatus != null) {
      int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
      if (level >= 0 && scale > 0) {
        currentBatteryLevel = (int) ((level / (float) scale) * 100);
      }
    }
  }

  public static AnimationManager getInstance(Context context) {
    if (instance == null) {
      instance = new AnimationManager(context);
    }
    return instance;
  }

  public void registerReceiver(Context context) {
    if (!receiverRegistered) {
      IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getApplicationContext().registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
      } else {
        context.getApplicationContext().registerReceiver(batteryReceiver, filter);
      }
      receiverRegistered = true;
    }
  }

  public void unregisterReceiver(Context context) {
    if (receiverRegistered) {
      try {
        context.getApplicationContext().unregisterReceiver(batteryReceiver);
      } catch (IllegalArgumentException ignored) {
        Log.e(getClass().getName(), ignored.getMessage());
      }
      receiverRegistered = false;
    }
  }

  public boolean areAnimationsEnabled() {
    int threshold = prefs.getAnimationBatteryThreshold();
    return currentBatteryLevel > threshold;
  }

  public int getCurrentBatteryLevel() {
    return currentBatteryLevel;
  }

  public int getThreshold() {
    return prefs.getAnimationBatteryThreshold();
  }
}
