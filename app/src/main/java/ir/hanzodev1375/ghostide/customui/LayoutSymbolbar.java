package ir.hanzodev1375.ghostide.customui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.util.Supplier;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ir.hanzodev1375.ghostide.adapters.SysmbolbarAdapter;
import ir.hanzodev1375.ghostide.codeeditors.IdeEditor;
import ir.theme.ThemeManager;
import ir.theme.ThemeUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class LayoutSymbolbar extends LinearLayoutCompat {

  private Supplier<IdeEditor> editorSupplier;
  private boolean isShowing = false;
  private ArrayList<HashMap<String, Object>> staticSymbiolPiare = new ArrayList<>();

  public LayoutSymbolbar(Context c) {
    super(c);
    init();
  }

  public LayoutSymbolbar(Context c, AttributeSet s) {
    super(c, s);
    init();
  }

  public void bindEditor(Supplier<IdeEditor> supplier) {
    this.editorSupplier = supplier;
  }

  private IdeEditor getEditor() {
    return editorSupplier != null ? editorSupplier.get() : null;
  }

  void init() {

    RecyclerView rv = new RecyclerView(getContext());
    rv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    removeAllViews();
    addView(rv);

    try {
      InputStream inputstream5 = getContext().getAssets().open("data/symbol.json");

      staticSymbiolPiare =
          new Gson()
              .fromJson(
                  copyFromInputStream(inputstream5),
                  new TypeToken<ArrayList<HashMap<String, Object>>>() {}.getType());

    } catch (Exception ignored) {
    }

    SysmbolbarAdapter syspiarAdapter =
        new SysmbolbarAdapter(
            staticSymbiolPiare,
            new SysmbolbarAdapter.OnTabView() {

              @Override
              public void TAB(String tab) {
                IdeEditor editor = getEditor();
                if (editor != null) {
                  editor.commitText("  ");
                }
              }

              @Override
              public void POST(String post) {
                IdeEditor editor = getEditor();
                if (editor != null) {
                  editor.insertText(post, post.length());
                }
              }
            },
            null);

    rv.setAdapter(syspiarAdapter);
    rv.setLayoutManager(
        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

    ThemeManager manager = new ThemeManager(getContext());
    ThemeUtils themeUtils = new ThemeUtils(manager);
    themeUtils.applySymbolBarLayout(this);

    setVisibility(View.GONE);
    isShowing = false;
  }

  protected String copyFromInputStream(InputStream inputStream) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];

    try {
      int i;
      while ((i = inputStream.read(buf)) != -1) {
        outputStream.write(buf, 0, i);
      }
      outputStream.close();
      inputStream.close();
    } catch (IOException ignored) {
    }

    return outputStream.toString();
  }

  public void show() {
    if (!isShowing) {
      Transition sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.Z, true);
      TransitionManager.beginDelayedTransition(this, sharedAxis);
      setVisibility(View.VISIBLE);
      isShowing = true;
    }
  }

  public void hide() {
    if (isShowing) {
      Transition sharedAxis = new MaterialSharedAxis(MaterialSharedAxis.Z, false);
      TransitionManager.beginDelayedTransition(this, sharedAxis);
      setVisibility(View.GONE);
      isShowing = false;
    }
  }

  public boolean isShowing() {
    return isShowing;
  }
}
