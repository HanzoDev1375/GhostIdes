package ir.theme;

import com.google.gson.annotations.SerializedName;

public class ActivityTheme {

  @SerializedName("background")
  private String background;

  @SerializedName("statusBar")
  private String statusBar;

  @SerializedName("navigationBar")
  private String navigationBar;

  @SerializedName("background")
  public String getBackground() {
    return background;
  }
  

  public void setBackground(String background) {
    this.background = background;
  }

  public String getStatusBar() {
    return statusBar;
  }

  public void setStatusBar(String statusBar) {
    this.statusBar = statusBar;
  }

  public String getNavigationBar() {
    return navigationBar;
  }

  public void setNavigationBar(String navigationBar) {
    this.navigationBar = navigationBar;
  }
}
