package ir.hanzodev1375.components.sheet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import com.bumptech.glide.Glide;
import ir.hanzodev1375.components.R;
import ir.hanzodev1375.components.databinding.SheetPluginSetupBinding;
import ir.hanzodev1375.components.store.model.PluginSetupActionData;
import ir.theme.M3Theme;
import java.util.List;

public class PluginSetupSheet extends BaseSheet {

  public interface OnRunListener {
    void onRun(String combinedCommand);
  }

  private final SheetPluginSetupBinding binding;
  private final List<PluginSetupActionData> actions;
  private final OnRunListener runListener;

  public PluginSetupSheet(
      Context context,
      String name,
      String iconUrl,
      List<PluginSetupActionData> actions,
      OnRunListener runListener) {
    super(context);
    this.actions = actions != null ? actions : List.of();
    this.runListener = runListener;
    binding = SheetPluginSetupBinding.inflate(LayoutInflater.from(context));
    setContentView(binding.getRoot());
    setHasPeekMod(false);
    build(name, iconUrl);
    M3Theme.applyTopLevel(binding.getRoot());
  }

  private void build(String name, String iconUrl) {
    binding.pluginSetupName.setText(name != null ? name : "");
    renderDescription();
    renderCommand();
    renderIcon(iconUrl);
    M3Theme.text(binding.pluginSetupName, binding.pluginSetupDesc, binding.pluginSetupCommand);
    M3Theme.card(binding.pluginSetupCard);
    M3Theme.button(binding.pluginSetupRun, binding.pluginSetupCancel);
    binding.pluginSetupRun.setOnClickListener(this::onRunClick);
    binding.pluginSetupCancel.setOnClickListener(v -> dismiss());
  }

  private void renderDescription() {
    StringBuilder desc = new StringBuilder();
    for (PluginSetupActionData action : actions) {
      if (!action.label().isEmpty()) {
        if (desc.length() > 0) desc.append(" · ");
        desc.append(action.label());
      }
    }
    binding.pluginSetupDesc.setText(desc.toString());
    binding.pluginSetupDesc.setVisibility(desc.length() > 0 ? View.VISIBLE : View.GONE);
  }

  private void renderCommand() {
    StringBuilder command = new StringBuilder();
    for (PluginSetupActionData action : actions) {
      if (command.length() > 0) command.append('\n').append('\n');
      if (!action.label().isEmpty()) command.append(action.label()).append('\n');
      if (!action.description().isEmpty()) command.append(action.description()).append('\n');
      command.append(action.command());
    }
    binding.pluginSetupCommand.setText(command.toString());
  }

  private void renderIcon(String iconUrl) {
    if (iconUrl == null || iconUrl.isEmpty()) {
      binding.pluginSetupIcon.setImageResource(R.drawable.ic_outline_extension);
      return;
    }
    Glide.with(binding.pluginSetupIcon)
        .load(iconUrl)
        .placeholder(R.drawable.ic_outline_extension)
        .error(R.drawable.ic_outline_extension)
        .into(binding.pluginSetupIcon);
  }

  private void onRunClick(View view) {
    StringBuilder combinedCommand = new StringBuilder();
    for (PluginSetupActionData action : actions) {
      if (combinedCommand.length() > 0) combinedCommand.append(" && ");
      combinedCommand.append(action.command());
    }
    if (runListener != null && combinedCommand.length() > 0) {
      runListener.onRun(combinedCommand.toString());
    }
    dismiss();
  }
}