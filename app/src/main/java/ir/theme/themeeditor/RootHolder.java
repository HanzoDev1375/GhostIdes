package ir.theme.themeeditor;

import android.view.View;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.R;

public class RootHolder extends RecyclerView.ViewHolder {
  public RootHolder(@NonNull View v) {
    super(v);
    var animator = AnimationUtils.loadAnimation(v.getContext(), R.anim.abc_fade_in);
    v.setAnimation(animator);
  }
}
