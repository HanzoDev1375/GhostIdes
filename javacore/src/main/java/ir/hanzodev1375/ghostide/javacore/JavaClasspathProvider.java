package ir.hanzodev1375.ghostide.javacore;

import java.util.List;

/**
 * Gives the in-process JDT engine the real classpath of an Android/Gradle project. The editor
 * module implements this on top of {@code AndroidClasspathResolver} (android.jar + libs + gradle
 * deps + aar extraction + source roots), so javacore itself stays Android-free.
 */
public interface JavaClasspathProvider {

  /** Absolute paths of compiled libraries usable for Java resolution (android.jar, *.jar...). */
  List<String> libraryJars(String projectRoot);

  /** Absolute paths of Java source roots inside the project (src/main/java, src/java, ...). */
  List<String> sourceRoots(String projectRoot);
}