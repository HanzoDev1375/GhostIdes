package ir.hanzodev1375.ghostide.ide.api;

import java.util.Set;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Everything the editor needs to route files to a language server and adapt its connection. Build
 * one with {@link Builder}; the app owns one shared connection per definition id and project.
 */
public final class LspServerDefinition {

  private final String id;
  private final Set<String> fileExtensions;
  private final String displayName;
  private final LspServerConnectionFactory connectionFactory;
  private final String grammarScopeName;
  private final ServerCapabilities expectedCapabilities;
  private final Object initializationOptions;
  private final Object configuration;
  private final boolean enableInlayHints;
  private final boolean enableSignatureHelp;
  private final int initializationTimeoutMillis;
  private final boolean traceIncomingMessages;
  private final String textMateGrammarLink;

  private LspServerDefinition(Builder builder) {
    if (builder.id == null || builder.id.trim().isEmpty()) {
      throw new IllegalArgumentException("LSP definition id must not be blank");
    }
    if (builder.fileExtensions == null || builder.fileExtensions.isEmpty()) {
      throw new IllegalArgumentException("LSP definition must support at least one file extension");
    }
    for (String extension : builder.fileExtensions) {
      if (extension == null || extension.trim().isEmpty()) {
        throw new IllegalArgumentException("LSP file extensions must not be blank");
      }
    }
    if (builder.displayName == null || builder.displayName.trim().isEmpty()) {
      throw new IllegalArgumentException("LSP display name must not be blank");
    }
    if (builder.connectionFactory == null) {
      throw new IllegalArgumentException("LSP connection factory must not be null");
    }
    if (builder.initializationTimeoutMillis <= 0) {
      throw new IllegalArgumentException("LSP initialization timeout must be positive");
    }
    this.id = builder.id;
    this.fileExtensions = Set.copyOf(builder.fileExtensions);
    this.displayName = builder.displayName;
    this.connectionFactory = builder.connectionFactory;
    this.grammarScopeName = builder.grammarScopeName;
    this.expectedCapabilities = builder.expectedCapabilities;
    this.initializationOptions = builder.initializationOptions;
    this.configuration = builder.configuration;
    this.enableInlayHints = builder.enableInlayHints;
    this.enableSignatureHelp = builder.enableSignatureHelp;
    this.initializationTimeoutMillis = builder.initializationTimeoutMillis;
    this.traceIncomingMessages = builder.traceIncomingMessages;
    this.textMateGrammarLink = builder.textMateGrammarLink;
  }

  public String getId() {
    return id;
  }

  public Set<String> getFileExtensions() {
    return fileExtensions;
  }

  public String getDisplayName() {
    return displayName;
  }

  public LspServerConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  public String getGrammarScopeName() {
    return grammarScopeName;
  }

  public ServerCapabilities getExpectedCapabilities() {
    return expectedCapabilities;
  }

  public Object getInitializationOptions() {
    return initializationOptions;
  }

  public Object getConfiguration() {
    return configuration;
  }

  public boolean isEnableInlayHints() {
    return enableInlayHints;
  }

  public boolean isEnableSignatureHelp() {
    return enableSignatureHelp;
  }

  public int getInitializationTimeoutMillis() {
    return initializationTimeoutMillis;
  }

  public boolean isTraceIncomingMessages() {
    return traceIncomingMessages;
  }

  public String getTextMateGrammarLink() {
    return textMateGrammarLink;
  }

  public static Builder builder(
      String id, Set<String> fileExtensions, String displayName, LspServerConnectionFactory factory) {
    return new Builder(id, fileExtensions, displayName, factory);
  }

  /** Builder for {@link LspServerDefinition}. */
  public static final class Builder {

    private final String id;
    private final Set<String> fileExtensions;
    private final String displayName;
    private final LspServerConnectionFactory connectionFactory;
    private String grammarScopeName;
    private ServerCapabilities expectedCapabilities;
    private Object initializationOptions;
    private Object configuration;
    private boolean enableInlayHints = true;
    private boolean enableSignatureHelp = true;
    private int initializationTimeoutMillis = 10_000;
    private boolean traceIncomingMessages = false;
    private String textMateGrammarLink;

    private Builder(
        String id, Set<String> fileExtensions, String displayName, LspServerConnectionFactory factory) {
      this.id = id;
      this.fileExtensions = fileExtensions;
      this.displayName = displayName;
      this.connectionFactory = factory;
    }

    public Builder grammarScopeName(String grammarScopeName) {
      this.grammarScopeName = grammarScopeName;
      return this;
    }

    public Builder expectedCapabilities(ServerCapabilities expectedCapabilities) {
      this.expectedCapabilities = expectedCapabilities;
      return this;
    }

    public Builder initializationOptions(Object initializationOptions) {
      this.initializationOptions = initializationOptions;
      return this;
    }

    public Builder configuration(Object configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder enableInlayHints(boolean enableInlayHints) {
      this.enableInlayHints = enableInlayHints;
      return this;
    }

    public Builder enableSignatureHelp(boolean enableSignatureHelp) {
      this.enableSignatureHelp = enableSignatureHelp;
      return this;
    }

    public Builder initializationTimeoutMillis(int initializationTimeoutMillis) {
      this.initializationTimeoutMillis = initializationTimeoutMillis;
      return this;
    }

    public Builder traceIncomingMessages(boolean traceIncomingMessages) {
      this.traceIncomingMessages = traceIncomingMessages;
      return this;
    }

    public Builder textMateGrammarLink(String textMateGrammarLink) {
      this.textMateGrammarLink = textMateGrammarLink;
      return this;
    }

    public LspServerDefinition build() {
      return new LspServerDefinition(this);
    }
  }
}
