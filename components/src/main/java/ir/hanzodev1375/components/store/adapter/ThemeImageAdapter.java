package ir.hanzodev1375.components.store.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import ir.hanzodev1375.components.R;

public class ThemeImageAdapter extends RecyclerView.Adapter<ThemeImageAdapter.VH> {

  private String[] urls = new String[0];

  public void setUrls(String[] newUrls) {
    urls = newUrls != null ? newUrls : new String[0];
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_theme_preview, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    String url = urls[position];
    ImageView image = holder.image;
    Glide.with(image.getContext())
        .load(url)
        .placeholder(R.drawable.ic_outline_palette)
        .error(R.drawable.ic_outline_palette)
        .into(image);
    image.startAnimation(AnimationUtils.loadAnimation(image.getContext(), android.R.anim.fade_in));
  }

  @Override
  public int getItemCount() {
    return urls.length;
  }

  static class VH extends RecyclerView.ViewHolder {
    final ImageView image;

    VH(@NonNull View itemView) {
      super(itemView);
      image = itemView.findViewById(R.id.previewImage);
    }
  }
}
