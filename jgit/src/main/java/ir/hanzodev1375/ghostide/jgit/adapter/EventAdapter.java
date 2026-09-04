package ir.hanzodev1375.ghostide.jgit.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.listitem.ListItemCardView;
import com.google.android.material.listitem.ListItemViewHolder;
import ir.hanzodev1375.ghostide.jgit.R;
import ir.theme.M3Theme;
import ir.hanzodev1375.ghostide.jgit.model.GitHubEvent;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

  private final List<GitHubEvent> list;

  public EventAdapter(List<GitHubEvent> list) {
    this.list = list;
  }

  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH h, int position) {
    GitHubEvent event = list.get(position);
    h.type.setText(event.getTypeLabel());
    h.repo.setText(event.getRepoName());
    h.date.setText(
        event.getCreatedAt() != null && event.getCreatedAt().length() >= 10
            ? event.getCreatedAt().substring(0, 10)
            : "");

    h.icon.setImageResource(getEventIcon(event.getType()));
    h.bind(position, getItemCount());
    h.icon.setColorFilter(getEventIconColor(event.getType()));
    M3Theme.listCard(h.itemView);
  }

  private int getEventIcon(String type) {
    if (type == null) return R.drawable.ic_bolt;
    return switch (type) {
      case "PushEvent" -> R.drawable.arrow_upload_ready_24px;
      case "WatchEvent" -> R.drawable.star_24px;
      case "ForkEvent" -> R.drawable.fork_right_24px;
      case "CreateEvent" -> R.drawable.add_24px;
      case "IssuesEvent" -> R.drawable.info_24px;
      case "PullRequestEvent" -> R.drawable.merge_24px;
      case "DeleteEvent" -> R.drawable.delete_24px;
      default -> R.drawable.ic_bolt;
    };
  }

  private int getEventIconColor(String type) {
    String hex;
    if (type == null) {
      hex = "#9E9E9E";
    } else {
      switch (type) {
        case "PushEvent":
          hex = "#4CAF50";
          break;
        case "WatchEvent":
          hex = "#FFC107";
          break;
        case "ForkEvent":
          hex = "#9C27B0";
          break;
        case "CreateEvent":
          hex = "#2196F3";
          break;
        case "IssuesEvent":
          hex = "#F44336";
          break;
        case "PullRequestEvent":
          hex = "#673AB7";
          break;
        case "DeleteEvent":
          hex = "#ff0000";
          break;
        default:
          hex = "#9E9E9E";
          break;
      }
    }
    return Color.parseColor(hex);
  }

  @Override
  public int getItemCount() {
    return list.size();
  }

  static class VH extends ListItemViewHolder {
    ImageView icon;
    TextView type, repo, date;
    ListItemCardView cardev;

    VH(View v) {
      super(v);
      icon = v.findViewById(R.id.ivEventIcon);
      type = v.findViewById(R.id.tvEventType);
      repo = v.findViewById(R.id.tvEventRepo);
      date = v.findViewById(R.id.tvEventDate);
      cardev = v.findViewById(R.id.cardev);
      cardev.setCardBackgroundColor(
          ColorStateList.valueOf(
              ColorUtils.setAlphaComponent(
                  fallback(M3Theme.surfaceContainer(), 0),
                  128)));
      cardev.setClickable(true);
    }
  }

  private static int fallback(Integer value, int def) {
    return value != null ? value : def;
  }
}
