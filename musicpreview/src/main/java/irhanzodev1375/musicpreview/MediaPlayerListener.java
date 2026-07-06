package irhanzodev1375.musicpreview;

public interface MediaPlayerListener {

  void isPlaying(int currentDuration);

  void onPause();

  void onStart();

  void onComplete();
}
