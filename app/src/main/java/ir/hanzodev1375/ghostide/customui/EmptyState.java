package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.util.AttributeSet;

import ir.hanzodev1375.ghostide.databinding.LayoutEmptystateBinding;

public class EmptyState extends LinearLayout {
  private LayoutEmptystateBinding bind;

  public EmptyState(Context context) {
    super(context);
    init();
  }

  public EmptyState(Context context, AttributeSet set) {
    super(context, set);
    init();
  }

  void init() {
    bind = LayoutEmptystateBinding.inflate(LayoutInflater.from(getContext()));
    if (bind != null) {
      removeAllViews();
      addView(bind.getRoot());
    }
  }
}