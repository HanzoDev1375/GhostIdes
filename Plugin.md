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
