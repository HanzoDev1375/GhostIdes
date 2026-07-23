package ir.hanzodev1375.ghostide.postman;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import ir.hanzodev1375.ghostide.R;
import ir.hanzodev1375.ghostide.postman.data.AppRepository;
import ir.hanzodev1375.ghostide.databinding.BottomSheetSaveRequestBinding;
import ir.hanzodev1375.ghostide.postman.model.RequestCollection;
import ir.hanzodev1375.ghostide.postman.model.SavedRequest;
import ir.hanzodev1375.ghostide.postman.util.BlurUtils;

/**
 * "Save request" modal — picks (or creates) a collection and stores a snapshot of the current
 * request so it can be reloaded later.
 */
public class SaveRequestBottomSheet extends BottomSheetDialogFragment {

  private static final String ARG_METHOD = "arg_method";
  private static final String ARG_URL = "arg_url";
  private static final String ARG_SNAPSHOT_JSON = "arg_snapshot_json";

  public interface Listener {
    void onSaved();
  }

  private Listener listener;
  private BottomSheetSaveRequestBinding binding;

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

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = BottomSheetSaveRequestBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    ViewGroup blurRoot = requireActivity().findViewById(android.R.id.content);
    BlurUtils.applyBlur(requireActivity(), binding.saveSheetBlurView, blurRoot, 18f);

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
                        binding.collectionNameInput.setAdapter(adapter);
                        binding.collectionNameInput.setThreshold(1);
                      });
            })
        .start();

    binding.confirmSaveButton.setOnClickListener(
        v -> {
          String name =
              binding.requestNameInput.getText() == null
                  ? ""
                  : binding.requestNameInput.getText().toString().trim();
          if (name.isEmpty()) {
            binding.requestNameInputLayout.setError(getString(R.string.msg_name_required));
            return;
          }
          binding.requestNameInputLayout.setError(null);
          String collectionName =
              binding.collectionNameInput.getText() == null
                  ? ""
                  : binding.collectionNameInput.getText().toString().trim();

          binding.confirmSaveButton.setEnabled(false);
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
    binding = null;
  }
}
