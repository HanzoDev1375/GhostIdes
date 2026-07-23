package ir.hanzodev1375.ghostide.postman.util;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

/** A TextWatcher that only cares about the final text, for RecyclerView rows with EditTexts. */
public class SimpleTextWatcher implements TextWatcher {

    private final Consumer<String> onChanged;

    public SimpleTextWatcher(Consumer<String> onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        onChanged.accept(s.toString());
    }
}
