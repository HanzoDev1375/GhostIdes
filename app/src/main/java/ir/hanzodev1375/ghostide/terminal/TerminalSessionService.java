package ir.hanzodev1375.ghostide.terminal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.termux.terminal.TerminalSession;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.terminal.activity.TerminalActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service پس‌زمینه‌ای که همه‌ی {@link TerminalSession}های باز رو نگه میداره — مستقل از عمر
 * {@link TerminalActivity}. با اولین سشن foreground میشه (با نوتیفیکیشن persistent تا سیستم
 * پروسه‌ی شل رو نکشه)؛ با بسته‌شدن آخرین سشن، foreground رو ول میکنه و خودش رو متوقف میکنه.
 *
 * <p>توی AndroidManifest.xml حتماً این‌ها رو اضافه کن (خودم دسترسی به مانیفست پروژه‌ت ندارم):
 *
 * <pre>{@code
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
 * <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
 *
 * <service
 *     android:name=".terminal.TerminalSessionService"
 *     android:exported="false"
 *     android:foregroundServiceType="specialUse">
 *     <property
 *         android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
 *         android:value="Runs an embedded terminal shell session for the code editor" />
 * </service>
 * }</pre>
 */
public class TerminalSessionService extends Service {

  private static final String CHANNEL_ID = "terminal_sessions";
  private static final int NOTIFICATION_ID = 4821;

  /** رویدادهایی که فقط وقتی یه Activity واقعاً bind کرده (visible/interactive) معنی دارن. */
  public interface SessionListener {
    void onTextChanged(TerminalSession session);

    void onTitleChanged(TerminalSession session);

    void onSessionFinished(TerminalSession session);
  }

  public class LocalBinder extends Binder {
    public TerminalSessionService getService() {
      return TerminalSessionService.this;
    }
  }

  private final IBinder binder = new LocalBinder();
  private final List<TerminalTab> sessions = new ArrayList<>();
  private int nextSessionId = 1;
  @Nullable private SessionListener uiListener;

  private final GhostTerminalSessionClient.Callback internalCallback =
      new GhostTerminalSessionClient.Callback() {
        @Override
        public void onTextChanged(TerminalSession session) {
          if (uiListener != null) uiListener.onTextChanged(session);
        }

        @Override
        public void onTitleChanged(TerminalSession session) {
          updateNotification();
          if (uiListener != null) uiListener.onTitleChanged(session);
        }

        @Override
        public void onSessionFinished(TerminalSession session) {
          // خودِ شل exit شده (مثلاً کاربر "exit" زده)؛ از لیست حذفش کن و به UI (اگه وصله) بگو
          removeFromListOnly(session);
          if (uiListener != null) uiListener.onSessionFinished(session);
          if (sessions.isEmpty()) stopServiceIfIdle();
          else updateNotification();
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    createNotificationChannel();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  public void setUiListener(@Nullable SessionListener listener) {
    this.uiListener = listener;
  }

  public List<TerminalTab> getSessions() {
    return sessions;
  }

  /** یه سشنِ شل ساده‌ی خودِ اندروید میسازه (رفتار قبلی). */
  public TerminalTab createSession(@Nullable String workingDir) {
    GhostTerminalSessionClient client = new GhostTerminalSessionClient(this, internalCallback);
    TerminalSession session = TerminalSessionFactory.createSession(this, workingDir, client);
    return registerNewSession(session);
  }

  /**
   * یه سشنِ Debian (proot) میسازه؛ باید قبلش با {@link DebianBootstrap#isInstalled} چک کرده
   * باشی که rootfs واقعاً استخراج شده، وگرنه IllegalStateException میگیری.
   */
  public TerminalTab createDebianSession() {
    GhostTerminalSessionClient client = new GhostTerminalSessionClient(this, internalCallback);
    File rootfs = DebianBootstrap.getRootfsDir(this);
    TerminalSession session =
        ProotSessionFactory.createProotSession(this, rootfs, "/bin/bash", client);
    return registerNewSession(session);
  }

  private TerminalTab registerNewSession(TerminalSession session) {
    TerminalTab tab = new TerminalTab(nextSessionId++, session);
    sessions.add(tab);
    startForeground(NOTIFICATION_ID, buildNotification());
    return tab;
  }

  /** بستنِ دستیِ یه سشن (کاربر روی × تب زده). */
  public void removeSession(TerminalSession session) {
    removeFromListOnly(session);
    session.finishIfRunning();
    if (sessions.isEmpty()) stopServiceIfIdle();
    else updateNotification();
  }

  private void removeFromListOnly(TerminalSession session) {
    for (int i = 0; i < sessions.size(); i++) {
      if (sessions.get(i).session == session) {
        sessions.remove(i);
        break;
      }
    }
  }

  private void stopServiceIfIdle() {
    stopForeground(true);
    stopSelf();
  }

  private void updateNotification() {
    if (sessions.isEmpty()) return;
    NotificationManager nm = getSystemService(NotificationManager.class);
    if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
  }

  private Notification buildNotification() {
    Intent openIntent = new Intent(this, TerminalActivity.class);
    int flags =
        PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, flags);

    int count = sessions.size();
    String text = count + (count == 1 ? " session running" : " sessions running");

    return new NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.terminal_title))
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_menu_send) // بعداً با یه آیکون خودتون جایگزین کن
        .setOngoing(true)
        .setContentIntent(contentIntent)
        .build();
  }

  private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationManager nm = getSystemService(NotificationManager.class);
      if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
        NotificationChannel channel =
            new NotificationChannel(
                CHANNEL_ID, "Terminal sessions", NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
      }
    }
  }

  @Override
  public void onDestroy() {
    // فقط وقتی خودِ سیستم/ما صریح سرویس رو میکشه (نه با چرخش صفحه یا رفتن اپ به پس‌زمینه)
    for (TerminalTab tab : sessions) {
      tab.session.finishIfRunning();
    }
    sessions.clear();
    super.onDestroy();
  }
}
