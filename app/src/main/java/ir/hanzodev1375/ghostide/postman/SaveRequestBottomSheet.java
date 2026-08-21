package ir.hanzodev1375.ghostide.postman;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.postman.model.RequestCollection;
import ir.hanzodev1375.ghostide.postman.model.SavedRequest;
import ir.hanzodev1375.components.sheet.BaseBlurBottomSheet;

public class SaveRequestBottomSheet extends BaseBlurBottomSheet {

  private static final String ARG_METHOD = "arg_method";
  private static final String ARG_URL = "arg_url";
  private static final String ARG_SNAPSHOT_JSON = "arg_snapshot_json";

  public interface Listener {
    void onSaved();
  }

  private Listener listener;
  private View rootView;

  public static SaveRequestBottomSheet newInstance(String method, String url, String snapshotJson) {
    SaveRequestBottomSheet fragment = new SaveRequestBottomSheet();
    Bundle args = new Bundle();
    args.putString(ARG_METHOD, method);
    args.putString(ARG_URL, url);
    args.putString(ARG_SNAPSHOT_JSON, snapshotJson);
    fragment.setArguments(args);
    return fragment;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  protected void onContentReady(ViewGroup contentContainer) {
    rootView =
        LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_save_request, contentContainer, false);
    contentContainer.addView(rootView);

    Bundle args = requireArguments();
    String method = args.getString(ARG_METHOD, "GET");
    String url = args.getString(ARG_URL, "");
    String snapshotJson = args.getString(ARG_SNAPSHOT_JSON, "");

    Context appContext = requireContext().getApplicationContext();
    AppRepository repository = new AppRepository(appContext);

    new Thread(
            () -> {
              List<RequestCollection> collections = repository.getCollections();
              List<String> names = new ArrayList<>();
              for (RequestCollection c : collections) names.add(c.name);
              requireActivity()
                  .runOnUiThread(
                      () -> {
                        if (!isAdded()) return;
                        ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                names);
                        AutoCompleteTextView auto = rootView.findViewById(R.id.collectionNameInput);
                        auto.setAdapter(adapter);
                      });
            })
        .start();

    rootView
        .findViewById(R.id.confirmSaveButton)
        .setOnClickListener(
            v -> {
              TextInputEditText nameInput = rootView.findViewById(R.id.requestNameInput);
              String name =
                  nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
              if (name.isEmpty()) {
                ((TextInputLayout) rootView.findViewById(R.id.requestNameInputLayout))
                    .setError(getString(R.string.msg_name_required));
                return;
              }
              ((TextInputLayout) rootView.findViewById(R.id.requestNameInputLayout)).setError(null);
              AutoCompleteTextView collectionInput =
                  rootView.findViewById(R.id.collectionNameInput);
              String collectionName =
                  collectionInput.getText() == null
                      ? ""
                      : collectionInput.getText().toString().trim();

              rootView.findViewById(R.id.confirmSaveButton).setEnabled(false);
              new Thread(
                      () -> {
                        long collectionId = 0;
                        if (!collectionName.isEmpty()) {
                          collectionId = repository.getOrCreateCollection(collectionName);
                        }
                        SavedRequest saved = new SavedRequest();
                        saved.collectionId = collectionId;
                        saved.name = name;
                        saved.method = method;
                        saved.url = url;
                        saved.requestJson = snapshotJson;
                        repository.insertSavedRequest(saved);

                        requireActivity()
                            .runOnUiThread(
                                () -> {
                                  if (listener != null) listener.onSaved();
                                  if (isAdded()) dismiss();
                                });
                      })
                  .start();
            });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    rootView = null;
  }
}
