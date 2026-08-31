package ir.hanzodev1375.ghostide.codeeditors.dependencychecker;

/**
 * A parsed dependency reference found in a source line, together with the character span of the
 * version token so the editor can highlight and replace it.
 *
 * @param group Maven group id
 * @param name Maven artifact id
 * @param version currently declared version
 * @param versionStart column where the version token starts (inclusive)
 * @param versionEnd column where the version token ends (exclusive)
 * @param fullStart column where the dependency text starts
 * @param fullEnd column where the dependency text ends
 */
public record DependencyMatch(
    String group,
    String name,
    String version,
    int versionStart,
    int versionEnd,
    int fullStart,
    int fullEnd) {

  public String coordinates() {
    return group + ":" + name;
  }
}
