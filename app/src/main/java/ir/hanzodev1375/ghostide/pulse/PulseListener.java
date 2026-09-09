package ir.hanzodev1375.ghostide.pulse;

/**
 * Receives one coalesced notification whenever the tracked folder tree gained, lost, or moved an
 * entry. The path points at the entry that changed; callers usually just refresh the folder they
 * currently show on screen.
 */
public interface PulseListener {

  void onPulse(String mutatedPath);
}