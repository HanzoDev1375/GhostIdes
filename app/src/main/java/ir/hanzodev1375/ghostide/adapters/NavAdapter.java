package ir.hanzodev1375.ghostide.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import ir.hanzodev1375.ghostide.databinding.LayoutNavItemBinding;
import ir.hanzodev1375.ghostide.models.NavModel;
import ir.theme.M3Theme;
import java.util.List;

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class NavAdapter extends RecyclerView.Adapter<NavAdapter.ViewHolder> {

  public final DiffUtil.ItemCallback<NavModel> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<NavModel>() {
        @Override
        public boolean areItemsTheSame(@NonNull NavModel oldItem, @NonNull NavModel newItem) {
          return oldItem.hashCode() == newItem.hashCode();
        }

        @Override
        public boolean areContentsTheSame(@NonNull NavModel oldItem, @NonNull NavModel newItem) {
          return oldItem.equals(newItem);
        }
      };
  private final AsyncListDiffer<NavModel> mDiffer =
      new AsyncListDiffer<NavModel>(this, DIFF_CALLBACK);
  protected OnItemClickListener<NavModel> itemClickListener;
  protected OnItemLongClickListener<NavModel> itemLongClickListener;
  private EditorColorScheme colorScheme;

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    var binding =
        LayoutNavItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
    return new ViewHolder(binding);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    var nav = mDiffer.getCurrentList().get(position);
    holder.bind(nav);

    if (position == getItemCount() - 1) {
      holder.nav_icon.setVisibility(View.GONE);
    } else {
      holder.itemView.setOnClickListener(
          v -> {
            if (itemClickListener != null) {
              if (position != RecyclerView.NO_POSITION) {
                itemClickListener.onItemClick(v, nav, position);
              }
            }
          });
      holder.itemView.setOnLongClickListener(
          v -> {
            if (itemLongClickListener != null) {
              if (position != RecyclerView.NO_POSITION) {
                return itemLongClickListener.onItemLongClick(v, nav, position);
              }
            }
            return false;
          });
    }
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return mDiffer.getCurrentList().size();
  }

  public void setColorScheme(EditorColorScheme colorScheme) {
    this.colorScheme = colorScheme;
  }

  public void setOnItemClickListener(OnItemClickListener<NavModel> listener) {
    this.itemClickListener = listener;
  }

  public void setOnItemLongClickListener(OnItemLongClickListener<NavModel> listener) {
    this.itemLongClickListener = listener;
  }

  public void submitList(List<NavModel> newItems) {
    mDiffer.submitList(newItems);
    notifyDataSetChanged();
  }

  public interface OnItemClickListener<NavModel> {
    void onItemClick(View view, NavModel item, int position);
  }

  public interface OnItemLongClickListener<NavModel> {
    boolean onItemLongClick(View view, NavModel item, int position);
  }

  public class ViewHolder extends RecyclerView.ViewHolder {

    private final ImageView nav_icon;
    private final TextView nav_text;

    public ViewHolder(LayoutNavItemBinding binding) {
      super(binding.getRoot());
      nav_icon = binding.navIcon;
      nav_text = binding.navText;
    }

    public void bind(NavModel nav) {
      if (nav.getFile().isFile()) {
        nav_icon.setVisibility(View.INVISIBLE);
      } else {
        nav_icon.setVisibility(View.VISIBLE);
      }
      nav_text.setText(nav.getName());
    }
  }
}
