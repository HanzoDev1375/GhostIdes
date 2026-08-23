# GhostIDE Plugin Development

This guide explains how the plugin system fits together: which module has which piece, how a
`.gpl` package is built, and how your code talks to the running IDE. It assumes you already know
Android development.

## The three API modules

Your plugin never depends on `:app`. It depends on these three, all published as plain jars/aars:

| Module | What it's for | Contains Android types? |
| --- | --- | --- |
| `plugin-api` | Plugin lifecycle, extension/service registries | No — pure JVM |
| `ide-api` | Language server (LSP) contribution | No — pure JVM + lsp4j |
| `ide-ui-api` | Editor/file-manager access, plugin screens | Yes — needs `Context`, `Fragment` |

This split exists so a plugin that only adds a language server doesn't have to depend on Android
at all, and so `:app`'s own `EditorActivity`/`FileManagerActivity` classes never have to be
referenced from plugin code — you only ever see the small interfaces in `ide-ui-api`.

## Two contribution patterns

Every capability you add follows one of two patterns, defined in `plugin-api`:

- **Extension point** (`ExtensionPoint<T>`) — many plugins can each register one or more
  instances. Used for `LspServerProvider` (ide-api) and `PluginScreen` (ide-ui-api). Look these
  up with `context.getExtensions().extensions(SomePoint)`.
- **Service** (`ServiceKey<T>`) — exactly one instance published by the host, read-only from a
  plugin's side. Used for `EditorHost`, `FileManagerHost`, and the plugin's own scoped `Context`.
  Look these up with `context.getServices().require(SomeKey)`.

## Entry point

```java
public final class MyPlugin implements GhostPlugin {
  @Override
  public void activate(PluginContext context) {
    Disposable registration = context.getExtensions()
        .register(EditorExtensionPoints.LSP_SERVER_PROVIDER, new MyLspProvider());
    context.registerDisposable(registration);
  }
}
```

Every `Disposable` you get back from `register(...)` must be passed to
`context.registerDisposable(...)`. The host calls that on unload so your registrations don't
outlive your plugin.

## Adding a language server

Implement `LspServerProvider` (`ide-api`): `supports(LspServerRequest)` decides whether you
handle a given file, `createDefinition(...)` returns an `LspServerDefinition` built with its
`Builder`, whose `connectionFactory` lazily starts your server process and returns an
`LspServerConnection` (`start()`, `getInputStream()`, `getOutputStream()`, `isClosed()`,
`close()`). Never write your server's own logs to the output stream — only the LSP protocol
frames belong there.

## Adding a screen (a plugin's "Activity")

Android will not let a dynamically loaded class declare a new `<activity>` in the host's
manifest. Instead, implement `PluginScreen` (`ide-ui-api`) and return a `Fragment`; register it
at `PluginUiExtensionPoints.PLUGIN_SCREEN`. The host's single `PluginScreenActivity` hosts it.

Layout inflation needs your own scoped context, not the fragment's default one, or your `R`
values won't resolve:

```java
Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
LayoutInflater inflater = LayoutInflater.from(pluginContext).cloneInContext(pluginContext);
View root = inflater.inflate(R.layout.my_screen, container, false);
```

## Adding a panel inside a screen (VS Code style)

`PluginScreen` takes over the whole screen. To put UI *next to* a running screen instead — a chat
sidebar, an inspector, a live preview — implement `EditorPanel` (`ide-ui-api`) and register it at
`PluginUiExtensionPoints.EDITOR_PANEL`. The host shows it as a side sheet inside the editor screen,
one toolbar button per registered panel. The previous `PluginScreen` API is untouched.

```java
public final class ChatPanel implements EditorPanel {
  private final Context pluginContext;

  ChatPanel(PluginContext context) {
    pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
  }

  @Override
  public String getId() {
    return "com.example.myplugin.chat";
  }

  @Override
  public String getTitle() {
    return "Chat";
  }

  @Override
  public View createView() {
    // Same scoped-context inflation rule as PluginScreen, so R.layout.chat resolves.
    return LayoutInflater.from(pluginContext).cloneInContext(pluginContext)
        .inflate(R.layout.chat_panel, null, false);
  }
}

// in activate():
context.registerDisposable(
    context.getExtensions().register(PluginUiExtensionPoints.EDITOR_PANEL, new ChatPanel(context)));
```

The host calls `createView()` once and reuses the returned `View`, so build it lazily and keep the
panel's state inside it. It is invoked on the UI thread.

## Reading and writing the open editor

```java
EditorHost editor = context.getServices().require(IdeHostServices.EDITOR_HOST);
File open = editor.getOpenFile();
String text = editor.getEditorText();
editor.setEditorText(text + "\n// edited by plugin");
```

`FileManagerHost` follows the same shape for the file tree screen.

You can also get the raw editor widget behind the current tab. `IdeEditor` is not part of this
API, so it is returned as `Object` and is meant as an optional access point — add the host editor
module as `compileOnly` in your `.gpl` if you want to cast:

```java
Object raw = editorHost.getEditor();
if (raw instanceof IdeEditor ide) {   // compileOnly ':editor' to cast
  ide.getCurrentFilePath();
  ide.getLspEditor();
}
```

## Running code from a plugin (Code Runner)

The host publishes a `CodeRunnerHost` service under `IdeHostServices.CODE_RUNNER_HOST`. It runs
shell commands and source files exactly like the editor's run (FAB) button — output goes to the
IDE terminal, either as a bottom sheet or a full screen:

```java
CodeRunnerHost runner = context.getServices().require(IdeHostServices.CODE_RUNNER_HOST);

runner.runShell("python3 main.py", true);        // arbitrary shell command, bottom sheet
runner.runCurrentFile(false);                     // the file open in the editor, full screen
runner.runFile("/sdcard/Project/main.py", true);  // any file by path
boolean ok = runner.isSupported("/sdcard/a.py");  // does the runner know this language?
```

`runCurrentFile()` does not read the path itself — it asks the registered editor panels for
`EditorPanel#getLastPath()` (first non-blank wins) and only falls back to the editor's current
file when no panel answers. Overriding `getLastPath()` in your panel lets your plugin decide
which file gets run; the host supplies the current open file as the default.

An LSP plugin can use the same runner without writing any client code: have your language server
return a code action whose `command` is one of these built-ins, and the editor runs it locally:

| command | arguments |
| --- | --- |
| `ghostide.runShell` | `["echo hi", true]` — shell text, optional `asBottomSheet` |
| `ghostide.runFile` | `["/path/file.py", true]` — file path, optional `asBottomSheet` |
| `ghostide.runCurrentFile` | `[true]` — optional `asBottomSheet` |

### Headless execution with output capture

When you need a command's output programmatically instead of showing it in the terminal, use
`exec()`. It runs headlessly (`sh -c`, no UI) and blocks until the process exits — call it from a
background thread:

```java
CodeRunnerHost.ExecResult result =
    runner.exec("git -C /sdcard/Project status --porcelain", null);
if (result.success()) {
  String listing = result.output();
}
```

Pass a `Consumer<String>` as the second argument to receive each merged stdout/stderr line as it
arrives. Note this runs in the app's own shell environment; binaries that live inside the proot
rootfs should go through `ProotProcessLauncher` instead.

## Subscribing to IDE events

Register a `FileEventListener` at `IdeEvents.FILE_EVENT` (`ide-ui-api`) to observe file
lifecycle changes everywhere in the IDE:

```java
context.registerDisposable(
    context.getExtensions().register(
        IdeEvents.FILE_EVENT,
        event -> {
          switch (event.type()) {
            case SAVED -> lint(event.path());
            case RENAMED -> updateIndex(event.previousPath(), event.path());
            case OPENED, CLOSED, DELETED -> {}
          }
        }));
```

Event types: `OPENED`, `SAVED`, `CLOSED`, `DELETED`, `RENAMED`. Paths are absolute; for
`RENAMED`, `path()` is the new location and `previousPath()` the old one. Listeners may be called
from any thread (saves and deletes happen on background threads) — hop to the main thread before
touching UI.

## Showing user feedback (toast / input / confirm)

The host publishes a `UiFeedbackHost` service under `IdeHostServices.UI_FEEDBACK`. All methods are
safe from any thread; dialog callbacks fire with `null`/`false` when no Activity is available:

```java
UiFeedbackHost ui = context.getServices().require(IdeHostServices.UI_FEEDBACK);

ui.toast("Indexed 42 files");

ui.confirm("Format all?", "This rewrites 12 files.", confirmed -> {
  if (confirmed) formatAll();
});

ui.promptInput("Project name", "my-app", name -> {
  if (name == null) return; // cancelled
  createProject(name);
});
```

## Persistent per-plugin storage

Every plugin gets its own isolated key-value store, published under `CoreServices.PLUGIN_STORAGE`
(defined in `plugin-api`, so LSP-only plugins can use it too). Data survives restarts and reloads;
keep values small and use your private directory for large blobs:

```java
PluginStorage storage = context.getServices().require(CoreServices.PLUGIN_STORAGE);

int runs = storage.getInt("runCount", 0);
storage.putInt("runCount", runs + 1);
storage.putString("lastPath", "/sdcard/Project/main.py");
```

## Registering commands

Implement `PluginCommand` (`plugin-api`) and register it at `CoreExtensionPoints.PLUGIN_COMMAND`
so the host can list and invoke your action (e.g. from a command palette):

```java
public final class FormatCommand implements PluginCommand {
  @Override public String getId() { return "com.example.myplugin.format"; }
  @Override public String getTitle() { return "Format project"; }
  @Override public void execute() { /* may be called off the main thread */ }
}

// in activate():
context.registerDisposable(
    context.getExtensions().register(CoreExtensionPoints.PLUGIN_COMMAND, new FormatCommand()));
```

Ids must be unique and reverse-domain namespaced.

## Handling your own LSP actions (optional editor access)

Commands that are not `ghostide.*` are still forwarded to your server via
`workspace/executeCommand`. To handle a command on the client instead — with the raw editor
available — implement `EditorActionHandler` (`ide-ui-api`) and register it at
`PluginUiExtensionPoints.EDITOR_ACTION_HANDLER`. The `editor` argument is an optional output: the
host's `IdeEditor` behind the action, or `null` when no editor is attached. `IdeEditor` is not
part of the API modules, so add the host editor module as `compileOnly` to cast:

```java
public final class RunMyPluginAction implements EditorActionHandler {
  @Override public String getCommandId() { return "com.example.myplugin.run"; }

  @Override public boolean execute(Object editor, String command, List<Object> arguments) {
    if (editor instanceof IdeEditor ide) {       // compileOnly ':editor' to cast
      String file = ide.getCurrentFilePath();
      // ...
    }
    return true; // handled — do not forward to the language server
  }
}

// in activate():
context.registerDisposable(
    context.getExtensions().register(PluginUiExtensionPoints.EDITOR_ACTION_HANDLER,
        new RunMyPluginAction()));
```

The handler may be called on a background thread, so hop to the UI thread before touching the
editor.

## Contributing file icons

Implement `FileIconContributor` (`ide-ui-api`) and register it at
`PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR`. Your contributor is consulted for every icon
shown in the file manager, editor tabs, history and bookmarks *before* the built-in
`file_icons.json` set; return `null` for paths you don't handle so the next contributor (or the
default set) takes over.

- Return a full URI (`file://`, `content://`, ...) to load artwork shipped inside your `.gpl`.
  Copy it out of your assets into private storage once during `activate()` — Glide cannot open
  plugin assets through `android_asset`.
- Return a bare name (`file_type_kotlin`) to reuse any icon of the built-in `vscode_icons` set.
- For bulk mappings, ship a JSON file with the same schema as `data/file_icons.json`
  (`asset_dir`, `extensions`, `filenames`, `folders`, `defaults`) plus your SVGs, then register
  the ready-made `JsonFileIconContributor`. It extracts only the SVGs actually present in your
  plugin, serves them as `file://` URIs, and passes unknown names through to the built-in set:

```java
public void activate(PluginContext context) {
  Context pluginContext = context.getServices().require(IdeHostServices.PLUGIN_ANDROID_CONTEXT);
  context.registerDisposable(context.getExtensions().register(
      PluginUiExtensionPoints.FILE_ICON_CONTRIBUTOR,
      new JsonFileIconContributor(pluginContext, "myicons.json")));
}
```

Multiple icon plugins can be active at once: they are queried in descending priority order and
the first non-null answer wins. Unloading a plugin removes its contributions automatically.

## Building the `.gpl` package

A `.gpl` is a normal Android **application** module — build it in Android Studio like any app,
just never install the resulting APK normally. `assembleRelease` produces the APK; renaming its
extension to `.gpl` is the whole packaging step.

`build.gradle`:

```groovy
plugins {
  id 'com.android.application'
}

android {
  namespace 'com.example.myplugin'
  defaultConfig {
    applicationId 'com.example.myplugin'
    minSdk 26
  }
}

dependencies {
  compileOnly project(':plugin-api')   // or the published jar
  compileOnly project(':ide-api')
  compileOnly project(':ide-ui-api')
}
```

`compileOnly` matters: these classes already exist inside the running host app, so your plugin
must not bundle its own copies.

`src/main/assets/plugin.json`:

```json
{
  "id": "com.example.myplugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "entryClass": "com.example.myplugin.MyPlugin",
  "description": "One line description shown in the plugin manager."
}
```

`entryClass` must have a public no-argument constructor and implement `GhostPlugin`.

## What the host does when it loads your `.gpl`

1. Reads `assets/plugin.json` straight out of the zip, before touching any code.
2. Opens a `DexClassLoader` on the `.gpl` file itself and instantiates `entryClass`.
3. Wraps the host `Context` so your `getResources()`/`getAssets()` resolve against your own
   package (via the same `AssetManager.addAssetPath` technique long used by Android plugin
   frameworks — not a public API, so if a future Android version blocks it your plugin still
   loads, just without your custom layouts/drawables).
4. Calls `activate(context)`.

On unload, the host calls your `deactivate()`, disposes everything you registered, and removes
your extension registrations by plugin id — you don't need to track that yourself as long as you
registered every `Disposable`.
