package ir.hanzodev1375.ghostide.models;

public class ToolbarModel {
  private int icon;
  private String tag;
  private boolean showVisblityItem;

  public ToolbarModel(int icon, String tag, boolean showVisblityItem) {
    this.icon = icon;
    this.tag = tag;
    this.showVisblityItem = showVisblityItem;
  }

  public ToolbarModel(int icon, String tag) {
    this.icon = icon;
    this.tag = tag;
    this.showVisblityItem = true;
  }

  public int getIcon() {
    return this.icon;
  }

  public String getTag() {
    return this.tag;
  }

  public boolean isShowVisblityItem() {
    return this.showVisblityItem;
  }

  public void setShowVisblityItem(boolean showVisblityItem) {
    this.showVisblityItem = showVisblityItem;
  }
}
