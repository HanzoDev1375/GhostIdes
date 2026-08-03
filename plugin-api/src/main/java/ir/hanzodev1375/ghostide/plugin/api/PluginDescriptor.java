package ir.hanzodev1375.ghostide.plugin.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stable, parsed identity of an installed plugin. Instances are immutable; build one with {@link
 * Builder}.
 */
public final class PluginDescriptor {

  private static final Pattern PLUGIN_ID = Pattern.compile("[A-Za-z0-9_.-]+");

  private final String id;
  private final String name;
  private final String version;
  private final String entryClass;
  private final String description;
  private final String author;
  private final String source;
  private final List<String> classPath;
  private final List<PluginDependency> dependencies;
  private final Set<String> capabilities;
  private final boolean enabledByDefault;

  private PluginDescriptor(Builder builder) {
    if (!PLUGIN_ID.matcher(builder.id).matches()) {
      throw new IllegalArgumentException(
          "Plugin id '" + builder.id + "' must use letters, numbers, '.', '_' or '-'");
    }
    if (builder.name == null || builder.name.isBlank()) {
      throw new IllegalArgumentException("Plugin name must not be blank");
    }
    if (builder.version == null || builder.version.isBlank()) {
      throw new IllegalArgumentException("Plugin version must not be blank");
    }
    if (builder.entryClass == null || builder.entryClass.isBlank()) {
      throw new IllegalArgumentException("Plugin entry class must not be blank");
    }
    this.id = builder.id;
    this.name = builder.name;
    this.version = builder.version;
    this.entryClass = builder.entryClass;
    this.description = builder.description;
    this.author = builder.author;
    this.source = builder.source;
    this.classPath = List.copyOf(builder.classPath);
    this.dependencies = List.copyOf(builder.dependencies);
    this.capabilities = Set.copyOf(builder.capabilities);
    this.enabledByDefault = builder.enabledByDefault;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getEntryClass() {
    return entryClass;
  }

  public String getDescription() {
    return description;
  }

  public String getAuthor() {
    return author;
  }

  public String getSource() {
    return source;
  }

  public List<String> getClassPath() {
    return classPath;
  }

  public List<PluginDependency> getDependencies() {
    return dependencies;
  }

  public Set<String> getCapabilities() {
    return capabilities;
  }

  public boolean isEnabledByDefault() {
    return enabledByDefault;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PluginDescriptor that)) {
      return false;
    }
    return id.equals(that.id) && version.equals(that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, version);
  }

  @Override
  public String toString() {
    return "PluginDescriptor{id=" + id + ", version=" + version + "}";
  }

  public static Builder builder(String id, String name, String version, String entryClass) {
    return new Builder(id, name, version, entryClass);
  }

  /** Builder for {@link PluginDescriptor}. */
  public static final class Builder {

    private final String id;
    private final String name;
    private final String version;
    private final String entryClass;
    private String description = "";
    private String author = "";
    private String source = "";
    private List<String> classPath = Collections.emptyList();
    private List<PluginDependency> dependencies = Collections.emptyList();
    private Set<String> capabilities = Collections.emptySet();
    private boolean enabledByDefault = true;

    private Builder(String id, String name, String version, String entryClass) {
      this.id = id;
      this.name = name;
      this.version = version;
      this.entryClass = entryClass;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder classPath(List<String> classPath) {
      this.classPath = classPath;
      return this;
    }

    public Builder dependencies(List<PluginDependency> dependencies) {
      this.dependencies = dependencies;
      return this;
    }

    public Builder capabilities(Set<String> capabilities) {
      this.capabilities = capabilities;
      return this;
    }

    public Builder enabledByDefault(boolean enabledByDefault) {
      this.enabledByDefault = enabledByDefault;
      return this;
    }

    public PluginDescriptor build() {
      return new PluginDescriptor(this);
    }
  }
}
