package ir.hanzodev1375.ghostide.ai.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

import ir.hanzodev1375.ghostide.ai.R;
import ir.hanzodev1375.ghostide.ai.model.ChatMessage;
import ir.theme.M3Theme;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private final List<ChatMessage> messages;

  public ChatAdapter(List<ChatMessage> messages) {
    this.messages = messages;
  }

  @Override
  public int getItemViewType(int position) {
    return messages.get(position).getType();
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    switch (viewType) {
      case ChatMessage.TYPE_USER:
        return new UserViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
      case ChatMessage.TYPE_AI:
        return new AiViewHolder(inflater.inflate(R.layout.item_chat_ai, parent, false));
      case ChatMessage.TYPE_LOADING:
        return new LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false));
      default:
        return new ErrorViewHolder(inflater.inflate(R.layout.item_chat_error, parent, false));
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ChatMessage msg = messages.get(position);

    if (holder instanceof UserViewHolder) {
      UserViewHolder userHolder = (UserViewHolder) holder;

      // نمایش متن
      if (!TextUtils.isEmpty(msg.getContent())) {
        userHolder.tvMessage.setText(msg.getContent());
        userHolder.tvMessage.setVisibility(View.VISIBLE);
      } else {
        userHolder.tvMessage.setVisibility(View.GONE);
      }

      // نمایش عکس
      if (msg.getImageUri() != null && !msg.getImageUri().isEmpty()) {
        userHolder.ivImage.setVisibility(View.VISIBLE);
        Glide.with(userHolder.itemView.getContext())
            .load(msg.getImageUri())
            .centerCrop()
            .into(userHolder.ivImage);
      } else {
        userHolder.ivImage.setVisibility(View.GONE);
      }

    } else if (holder instanceof AiViewHolder) {
      ((AiViewHolder) holder).tvMessage.setText(msg.getContent());
      ((AiViewHolder) holder).tvProvider.setText(msg.getProvider().toUpperCase());
    } else if (holder instanceof ErrorViewHolder) {
      ((ErrorViewHolder) holder).tvError.setText(msg.getContent());
    }
    M3Theme.listCard(holder.itemView);
  }

  @Override
  public int getItemCount() {
    return messages.size();
  }

  static class UserViewHolder extends RecyclerView.ViewHolder {
    TextView tvMessage;
    ImageView ivImage;

    UserViewHolder(@NonNull View itemView) {
      super(itemView);
      tvMessage = itemView.findViewById(R.id.tv_message_user);
      ivImage = itemView.findViewById(R.id.iv_user_image);
    }
  }

  static class AiViewHolder extends RecyclerView.ViewHolder {
    TextView tvMessage;
    TextView tvProvider;

    AiViewHolder(@NonNull View itemView) {
      super(itemView);
      tvMessage = itemView.findViewById(R.id.tv_message_ai);
      tvProvider = itemView.findViewById(R.id.tv_provider_label);
    }
  }

  static class LoadingViewHolder extends RecyclerView.ViewHolder {
    CircularProgressIndicator progressBar;

    LoadingViewHolder(@NonNull View itemView) {
      super(itemView);
      progressBar = itemView.findViewById(R.id.progress_loading);
    }
  }

  static class ErrorViewHolder extends RecyclerView.ViewHolder {
    TextView tvError;

    ErrorViewHolder(@NonNull View itemView) {
      super(itemView);
      tvError = itemView.findViewById(R.id.tv_error);
    }
  }
}
