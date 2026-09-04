package ir.hanzodev1375.components.sheet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.slider.Slider;
import ir.hanzodev1375.components.databinding.SliderSheetBinding;
import ir.theme.M3Theme;

public class SliderSheet extends BaseSheet {
  private SliderSheetBinding binding;

  public SliderSheet(Context context) {
    super(context);
    binding = SliderSheetBinding.inflate(LayoutInflater.from(context));
    setContentView(binding.getRoot());
    setHasPeekMod(false);
    M3Theme.applyTopLevel(binding.getRoot());
  }

  public void setButtonOk(View.OnClickListener listener, int textRes) {
    binding.btnok.setOnClickListener(listener);
    binding.btnok.setText(textRes);
  }

  public void setButtonNo(View.OnClickListener listener, int textRes) {
    binding.btnno.setOnClickListener(listener);
    binding.btnno.setText(textRes);
  }

  public void setButtonOk(View.OnClickListener listener, String text) {
    binding.btnok.setOnClickListener(listener);
    binding.btnok.setText(text);
  }

  public void setButtonNo(View.OnClickListener listener, String text) {
    binding.btnno.setOnClickListener(listener);
    binding.btnno.setText(text);
  }

  public void setTitle(CharSequence title) {
    binding.title.setText(title);
  }

  public void setLable(CharSequence label) {
    binding.lable.setText(label);
  }

  public TextView getLable() {
    return binding.lable;
  }

  public Slider getSlider() {
    return binding.slider;
  }
}
