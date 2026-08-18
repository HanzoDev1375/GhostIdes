package ir.hanzodev1375.ghostide.codeeditors.langs.lsp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspLanguage;
import io.github.rosemoe.sora.widget.CodeEditor;
import ir.hanzodev1375.ghostide.codeeditors.langs.formatHelp.DebianBootstrap;
import ir.hanzodev1375.ghostide.codeeditors.langs.java.JavaLanguage;

public class JavaServer extends LspContentImpl {

  public static final JavaServer INSTANCE = new JavaServer();

  private static final String TAG = "JavaServer";

  private static final String[] JAVA_CANDIDATE_PATHS = {"/usr/bin/java"};
  private static final String[] JDTLS_CANDIDATE_PATHS = {
    "/root/jdtls/bin/jdtls", "/opt/jdtls/bin/jdtls", "/usr/local/bin/jdtls", "/usr/bin/jdtls"
  };

  private JavaServer() {
    super("JavaServer", "jdtls", Collections.singleton("java"), JDTLS_CANDIDATE_PATHS);
  }

  @Override
  public boolean isSupportedFile(String filePath) {
    return supportedExtensions.contains(extensionOf(filePath));
  }

  @Override
  public boolean isInstalled(Context context) {
    return findJavaExecutable(context) != null && findInstalledExecutable(context) != null;
  }

  // ──────────────────── connectFile prepares data then calls super ────────────────────

  private String pendingJavaExec;
  private File pendingRootfs;
  private File pendingProjectRootFile;
  private List<File> pendingSourceRoots;
  private List<File> pendingLibJars;

  @Override
  public LspEditor connectFile(
      Context context, String projectRoot, String filePath, CodeEditor editor) {
    String javaExec = findJavaExecutable(context);
    if (javaExec == null) {
      Log.e(TAG, "java not installed. Install a JDK inside the Debian rootfs.");
      return null;
    }
    if (findInstalledExecutable(context) == null) {
      Log.e(TAG, "jdtls not found in any known path.");
      return null;
    }

    pendingJavaExec = javaExec;
    pendingRootfs = DebianBootstrap.getRootfsDir(context);
    pendingProjectRootFile = new File(projectRoot);
    pendingSourceRoots = AndroidClasspathResolver.findJavaSourceRoots(pendingProjectRootFile);
    pendingLibJars = AndroidClasspathResolver.findLibraryJars(context, pendingProjectRootFile);

    Log.i(
        TAG,
        "jdtls invisible-project: "
            + pendingSourceRoots.size()
            + " source roots, "
            + pendingLibJars.size()
            + " jars");

    return super.connectFile(context, projectRoot, filePath, editor);
  }

  @Override
  protected LspEditor onEditorCreated(LspEditor lspEditor, CodeEditor editor) {
    Context context = editor.getContext();
    JavaLanguage java = new JavaLanguage(context);
    lspEditor.setWrapperLanguage(java);
    lspEditor.setEditor(editor);
    lspEditor.setEnableInlayHint(true);
    lspEditor.setEnableSignatureHelp(true);
    lspEditor.setEnableHover(true);
    var lang = (LspLanguage) editor.getEditorLanguage();
    lang.setFormatter(java.getFormatter());
    return lspEditor;
  }

  // ──────────────────── createDefinition (called by base class ensureDefinitionRegistered) ────────────────────

  @Override
  protected LanguageServerDefinition createDefinition(
      Context context, String jdtlsExecutable, String ext) {

    String workspaceId = sanitize(pendingProjectRootFile.getAbsolutePath());
    File dataDir = new File(context.getCacheDir(), "jdtls-workspace/" + workspaceId);
    dataDir.mkdirs();

    List<String> args = new ArrayList<>();
    args.add("--java-executable");
    args.add(pendingJavaExec);
    args.add("--jvm-arg=-Djdk.lang.Process.launchMechanism=FORK");
    args.add("--jvm-arg=-Djdk.xml.maxGeneralEntitySizeLimit=0");
    args.add("--jvm-arg=-Djdk.xml.totalEntitySizeLimit=0");
    args.add("--jvm-arg=-Dlog.level=WARNING");
    args.add("--jvm-arg=-Xms256m");
    args.add("--jvm-arg=-Xmx1G");
    args.add("--jvm-arg=-XX:+UseG1GC");
    args.add("--jvm-arg=-XX:+TieredCompilation");
    args.add("--jvm-arg=-XX:TieredStopAtLevel=1");
    args.add("--jvm-arg=-Dorg.eclipse.jdt.ls.lombok.support=false");
    args.add("--jvm-arg=--add-modules=ALL-SYSTEM");
    args.add("--jvm-arg=--add-opens");
    args.add("--jvm-arg=java.base/java.util=ALL-UNNAMED");
    args.add("--jvm-arg=--add-opens");
    args.add("--jvm-arg=java.base/java.lang=ALL-UNNAMED");
    args.add("-data");
    args.add(dataDir.getAbsolutePath());

    Map<String, Object> initOptions =
        buildInvisibleProjectInitOptions(pendingRootfs, pendingProjectRootFile, pendingSourceRoots, pendingLibJars);

    return new CustomLanguageServerDefinition(
        "java",
        workingDir -> new ProotStdioConnectionProvider(context, workingDir, jdtlsExecutable, args),
        serverName,
        null,
        null) {
      @Override
      public Object getInitializationOptions(URI uri) {
        return initOptions;
      }
    };
  }

  // ──────────────────── jdtls invisible-project init options ────────────────────

  private static Map<String, Object> buildInvisibleProjectInitOptions(
      File rootfs, File projectRoot, List<File> sourceRoots, List<File> libJars) {

    List<String> relativeSourcePaths = new ArrayList<>();
    for (File src : sourceRoots) {
      relativeSourcePaths.add(AndroidClasspathResolver.relativize(projectRoot, src));
    }

    List<String> libraryPaths = new ArrayList<>();
    for (File jar : libJars) {
      libraryPaths.add(AndroidClasspathResolver.toGuestLibraryPath(rootfs, jar));
    }

    Map<String, Object> project = new HashMap<>();
    project.put("sourcePaths", relativeSourcePaths);
    project.put("referencedLibraries", libraryPaths);

    Map<String, Object> gradleEnabled = new HashMap<>();
    gradleEnabled.put("enabled", false);
    Map<String, Object> mavenEnabled = new HashMap<>();
    mavenEnabled.put("enabled", false);
    Map<String, Object> importSettings = new HashMap<>();
    importSettings.put("gradle", gradleEnabled);
    importSettings.put("maven", mavenEnabled);

    Map<String, Object> java = new HashMap<>();
    java.put("project", project);
    java.put("import", importSettings);

    Map<String, Object> settings = new HashMap<>();
    settings.put("java", java);

    Map<String, Object> initializationOptions = new HashMap<>();
    initializationOptions.put("settings", settings);
    return initializationOptions;
  }

  // ──────────────────── Helpers ────────────────────

  public static String findJavaExecutable(Context context) {
    File rootfs = DebianBootstrap.getRootfsDir(context);
    if (rootfs == null || !rootfs.exists()) return null;

    for (String candidate : JAVA_CANDIDATE_PATHS) {
      if (new File(rootfs, candidate.substring(1)).exists()) return candidate;
    }

    File jvmDir = new File(rootfs, "usr/lib/jvm");
    File[] installs = jvmDir.exists() ? jvmDir.listFiles() : null;
    if (installs != null) {
      for (File install : installs) {
        if (new File(install, "bin/java").exists()) {
          return "/usr/lib/jvm/" + install.getName() + "/bin/java";
        }
      }
    }
    return null;
  }

  private static String sanitize(String value) {
    return (value == null || value.isEmpty()) ? "root" : value.replaceAll("[^a-zA-Z0-9]+", "_");
  }
}
