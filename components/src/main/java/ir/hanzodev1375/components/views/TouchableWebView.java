package ir.hanzodev1375.components.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

public class TouchableWebView extends WebView {
  public TouchableWebView(Context context) {
    super(context);
    init();
  }

  public TouchableWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    // غیرفعال کردن افکت اوراسکرول برای جلوگیری از تداخل بصری
    setOverScrollMode(View.OVER_SCROLL_NEVER);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {

    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      getParent().requestDisallowInterceptTouchEvent(true);
    }
    if (event.getAction() == MotionEvent.ACTION_MOVE) {

      int scrollY = getScrollY();
      int contentHeight = computeVerticalScrollRange(); // ارتفاع کل محتوا
      int viewHeight = getHeight(); // ارتفاع قابل مشاهده
      int maxScrollY = contentHeight - viewHeight;

      float y = event.getY();
      float dy = y - mLastY;
      mLastY = y;

      boolean atTop = scrollY <= 0;
      boolean atBottom = scrollY >= maxScrollY;
      if ((atTop && dy > 0) || (atBottom && dy < 0)) {
        getParent().requestDisallowInterceptTouchEvent(false);
      } else {
        getParent().requestDisallowInterceptTouchEvent(true);
      }
    }

    return super.onTouchEvent(event);
  }

  private float mLastY = 0f;
}
