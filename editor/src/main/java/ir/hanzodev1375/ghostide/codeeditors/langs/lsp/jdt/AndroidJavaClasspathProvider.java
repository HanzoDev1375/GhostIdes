package ir.hanzodev1375.ghostide.codeeditors.langs.lsp.jdt;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.lsp.AndroidClasspathResolver;
import ir.hanzodev1375.ghostide.javacore.JavaClasspathProvider;

/**
 * Bridges the real Android/Gradle classpath into the in-process JDT engine. Delegates to {@link
 * AndroidClasspathResolver} so the LSP uses exactly the same classpath (android.jar + libs +
 * gradle deps + aar extraction + source roots) as javac collaborating runners. In addition the
 * JDK installed in the terminal is exposed via its jmods, giving ECJ the real system library it
 * needs for Java 9+ binding resolution (the same role a running JVM plays for JDT-LS).
 */
public final class AndroidJavaClasspathProvider implements JavaClasspathProvider {

  private final Context context;

  public AndroidJavaClasspathProvider(Context context) {
    this.context = context;
  }

  @Override
  public List<String> libraryJars(String projectRoot) {
    try {
      File root = projectRoot == null ? null : new File(projectRoot);
      if (root == null || !root.isDirectory()) return Collections.emptyList();
      List<File> jars = AndroidClasspathResolver.findLibraryJars(context, root);
      List<String> out = new ArrayList<>(jars.size());
      for (File f : jars) out.add(f.getAbsolutePath());
      File jdkJrt = AndroidClasspathResolver.findJdkJrt(DebianBootstrap.getRootfsDir(context));
      if (jdkJrt != null) out.add(jdkJrt.getAbsolutePath());
      return out;
    } catch (Throwable t) {
      return Collections.emptyList();
    }
  }

  @Override
  public List<String> sourceRoots(String projectRoot) {
    try {
      File root = projectRoot == null ? null : new File(projectRoot);
      if (root == null || !root.isDirectory()) return Collections.emptyList();
      List<File> roots = AndroidClasspathResolver.findJavaSourceRoots(root);
      List<String> out = new ArrayList<>(roots.size());
      for (File f : roots) out.add(f.getAbsolutePath());
      return out;
    } catch (Throwable t) {
      return Collections.emptyList();
    }
  }
}