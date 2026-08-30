package ir.hanzodev1375.components.sheet.customitemsheet.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import ir.hanzodev1375.components.R;

public class DialogCompat extends LiquidGlassDialogBuilderJava {
  public DialogCompat(Context c) {
    super(c);
  }

  @Override
  public DialogCompat setTitle(CharSequence title) {
    super.setTitle(title);
    return this;
  }

  @Override
  public DialogCompat setTitle(int titleId) {
    super.setTitle(titleId);
    return this;
  }

  @Override
  public DialogCompat setCustomTitle(View customTitleView) {
    super.setCustomTitle(customTitleView);
    return this;
  }

  @Override
  public DialogCompat setIcon(int iconId) {
    super.setIcon(iconId);
    return this;
  }

  @Override
  public DialogCompat setIcon(Drawable icon) {
    super.setIcon(icon);
    return this;
  }

  @Override
  public DialogCompat setIconAttribute(int attrId) {
    super.setIconAttribute(attrId);
    return this;
  }

  @Override
  public DialogCompat setMessage(CharSequence message) {
    super.setMessage(message);
    return this;
  }

  @Override
  public DialogCompat setMessage(int messageId) {
    super.setMessage(messageId);
    return this;
  }

  @Override
  public DialogCompat setView(View view) {
    super.setView(view);
    return this;
  }

  @Override
  public DialogCompat setView(int layoutResId) {
    super.setView(layoutResId);
    return this;
  }

  @Override
  public DialogCompat setView(
      View view,
      int viewSpacingLeft,
      int viewSpacingTop,
      int viewSpacingRight,
      int viewSpacingBottom) {
    super.setView(view, viewSpacingLeft, viewSpacingTop, viewSpacingRight, viewSpacingBottom);
    return this;
  }

  @Override
  public DialogCompat setPositiveButton(int textId, DialogInterface.OnClickListener listener) {
    super.setPositiveButton(textId, listener);
    return this;
  }

  @Override
  public DialogCompat setPositiveButton(
      CharSequence text, DialogInterface.OnClickListener listener) {
    super.setPositiveButton(text, listener);
    return this;
  }

  @Override
  public DialogCompat setNegativeButton(int textId, DialogInterface.OnClickListener listener) {
    super.setNegativeButton(textId, listener);
    return this;
  }

  @Override
  public DialogCompat setNegativeButton(
      CharSequence text, DialogInterface.OnClickListener listener) {
    super.setNegativeButton(text, listener);
    return this;
  }

  @Override
  public DialogCompat setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
    super.setNeutralButton(textId, listener);
    return this;
  }

  @Override
  public DialogCompat setNeutralButton(
      CharSequence text, DialogInterface.OnClickListener listener) {
    super.setNeutralButton(text, listener);
    return this;
  }

  @Override
  public DialogCompat setOnCancelListener(DialogInterface.OnCancelListener listener) {
    super.setOnCancelListener(listener);
    return this;
  }

  @Override
  public DialogCompat setOnDismissListener(DialogInterface.OnDismissListener listener) {
    super.setOnDismissListener(listener);
    return this;
  }

  @Override
  public DialogCompat setOnKeyListener(DialogInterface.OnKeyListener listener) {
    super.setOnKeyListener(listener);
    return this;
  }

  

  @Override
  public DialogCompat setItems(int itemsId, DialogInterface.OnClickListener listener) {
    super.setItems(itemsId, listener);
    return this;
  }

  @Override
  public DialogCompat setItems(CharSequence[] items, DialogInterface.OnClickListener listener) {
    super.setItems(items, listener);
    return this;
  }

  @Override
  public DialogCompat setAdapter(ListAdapter adapter, DialogInterface.OnClickListener listener) {
    super.setAdapter(adapter, listener);
    return this;
  }

  @Override
  public DialogCompat setSingleChoiceItems(
      ListAdapter adapter, int checkedItem, DialogInterface.OnClickListener listener) {
    super.setSingleChoiceItems(adapter, checkedItem, listener);
    return this;
  }

  @Override
  public DialogCompat setSingleChoiceItems(
      int itemsId, int checkedItem, DialogInterface.OnClickListener listener) {
    super.setSingleChoiceItems(itemsId, checkedItem, listener);
    return this;
  }

  @Override
  public DialogCompat setSingleChoiceItems(
      CharSequence[] items, int checkedItem, DialogInterface.OnClickListener listener) {
    super.setSingleChoiceItems(items, checkedItem, listener);
    return this;
  }

  @Override
  public DialogCompat setMultiChoiceItems(
      int itemsId, boolean[] checkedItems, DialogInterface.OnMultiChoiceClickListener listener) {
    super.setMultiChoiceItems(itemsId, checkedItems, listener);
    return this;
  }

  @Override
  public DialogCompat setMultiChoiceItems(
      CharSequence[] items,
      boolean[] checkedItems,
      DialogInterface.OnMultiChoiceClickListener listener) {
    super.setMultiChoiceItems(items, checkedItems, listener);
    return this;
  }

  @Override
  public DialogCompat setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
    super.setOnItemSelectedListener(listener);
    return this;
  }

  @Override
  public DialogCompat setCancelable(boolean cancelable) {
    super.setCancelable(cancelable);
    return this;
  }

  @Override
  public DialogCompat setBackgroundInsetStart(int backgroundInsetStart) {
    super.setBackgroundInsetStart(backgroundInsetStart);
    return this;
  }

  @Override
  public DialogCompat setBackgroundInsetTop(int backgroundInsetTop) {
    super.setBackgroundInsetTop(backgroundInsetTop);
    return this;
  }

  @Override
  public DialogCompat setBackgroundInsetEnd(int backgroundInsetEnd) {
    super.setBackgroundInsetEnd(backgroundInsetEnd);
    return this;
  }

  @Override
  public DialogCompat setBackgroundInsetBottom(int backgroundInsetBottom) {
    super.setBackgroundInsetBottom(backgroundInsetBottom);
    return this;
  }

  @Override
  public DialogCompat setMultiChoiceItems(
      Cursor arg0, String arg1, String arg2, DialogInterface.OnMultiChoiceClickListener arg3) {
    super.setMultiChoiceItems(arg0, arg1, arg2, arg3);
    return this;
  }

  @Override
  public DialogCompat setSingleChoiceItems(
      Cursor arg0, int arg1, String arg2, DialogInterface.OnClickListener arg3) {
    super.setSingleChoiceItems(arg0, arg1, arg2, arg3);
    return this;
  }
}
