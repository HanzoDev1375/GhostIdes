package ir.hanzodev1375.ghostide.fragments;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import ir.hanzodev1375.ghostide.R;
import com.bumptech.glide.Glide;

public class ImageViewerFragment extends Fragment {

  private static final String ARG_URI = "arg_uri";

  public static ImageViewerFragment newInstance(Uri uri) {
    ImageViewerFragment f = new ImageViewerFragment();
    Bundle args = new Bundle();
    args.putString(ARG_URI, uri.toString());
    f.setArguments(args);
    return f;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_image_viewer, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ImageView ivMain = view.findViewById(R.id.ivMainImage);

    if (getArguments() != null) {
      Uri uri = Uri.parse(getArguments().getString(ARG_URI));
      Glide.with(this).load(uri).into(ivMain);
    }
  }
}
