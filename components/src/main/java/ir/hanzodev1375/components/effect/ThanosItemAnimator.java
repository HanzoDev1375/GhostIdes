package ir.hanzodev1375.components.effect;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Animator حرکتی که موقع حذف آیتم، به جای fade-out ساده، افکت GPU «تانوس» (ذره‌ای) رو اجرا می‌کنه –
 * دقیقاً مثل {@code ChatListItemAnimator} تلگرام.
 *
 * <p>استفاده: روی {@link RecyclerView} با {@code setItemAnimator} ست می‌کنی و همزمان باید یه {@link
 * ThanosEffect} (TextureView) داشته باشی که به عنوان لایه‌ی رویی به والد ری‌سایکرلر اضافه بشه.
 */
public class ThanosItemAnimator extends DefaultItemAnimator {

  public interface ThanosEffectProvider {
    @NonNull
    ThanosEffect getThanosEffect();
  }

  @Nullable private ThanosEffectProvider provider;

  private final Set<RecyclerView.ViewHolder> removingHolders = new HashSet<>();
  private boolean enabled = true;

  /**
   * وقتی {@code true} باشه، هر حذفی (تک‌تایی یا گروهی) که واقعاً از سمت کاربر بعد از تأیید حذف رخ
   * بده به‌جای fade ساده، افکت تانوس رو اجرا می‌کنه. این پرچم فقط در پنجره‌ی بارگذاری مجددِ ناشی از
   * عملیات حذف روشنه و توسط خود همین animator بعد از اتمامِ انیمیشن حذف خاموش می‌شه؛ نه در لحظه‌ی
   * همگامِ observer (چون انیمیشن حذف توسط RecyclerView به‌صورت ناهمگام روی فریم بعدی اجرا می‌شه).
   */
  private boolean snapDeletionPending = false;

  /**
   * پرچم حذف رو قبل از شروع عملیات حذف روشن می‌کنه. بعد از اتمام انیمیشن، خودِ animator خاموشش
   * می‌کنه.
   */
  public void setSnapDeletionPending(boolean pending) {
    this.snapDeletionPending = pending;
  }

  public ThanosItemAnimator(ThanosEffectProvider provider) {
    this.provider = provider;
  }

  @Override
  public boolean animateRemove(@NonNull RecyclerView.ViewHolder holder) {
    View view = holder.itemView;
    if (enabled
        && snapDeletionPending
        && provider != null
        && view != null
        && view.getWidth() > 0
        && view.getHeight() > 0) {
      ThanosEffect effect = provider.getThanosEffect();
      removingHolders.add(holder);
      dispatchRemoveStarting(holder);
      effect.animate(
          view,
          () -> {
            removingHolders.remove(holder);
            view.setVisibility(View.VISIBLE);
            dispatchRemoveFinished(holder);
            maybeResetSnapFlag();
          });
      return true;
    }
    return super.animateRemove(holder);
  }

  @Override
  public void endAnimation(@NonNull RecyclerView.ViewHolder item) {
    if (removingHolders.remove(item)) {
      if (item.itemView != null) item.itemView.setVisibility(View.VISIBLE);
      dispatchRemoveFinished(item);
      maybeResetSnapFlag();
    }
    super.endAnimation(item);
  }

  @Override
  public void endAnimations() {
    for (RecyclerView.ViewHolder holder : new ArrayList<>(removingHolders)) {
      if (holder.itemView != null) holder.itemView.setVisibility(View.VISIBLE);
      dispatchRemoveFinished(holder);
    }
    removingHolders.clear();
    snapDeletionPending = false;
    super.endAnimations();
  }

  @Override
  public boolean isRunning() {
    return !removingHolders.isEmpty() || super.isRunning();
  }

  /**
   * بعد از اتمامِ همه‌ی انیمیشن‌های حذف تانوس، پرچم رو خاموش می‌کنه تا روی ناوبری بعدی اثر نذاره.
   */
  private void maybeResetSnapFlag() {
    if (removingHolders.isEmpty()) {
      snapDeletionPending = false;
    }
  }
}
