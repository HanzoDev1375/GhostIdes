package ir.hanzodev1375.ghostide.activity.pluginmanager;

import java.io.File;

import ir.hanzodev1375.ghostide.plugin.gpl.GplManifest;

/** One row in the plugin manager list: an installed {@code .gpl} file plus its manifest. */
public record InstalledPluginInfo(File file, GplManifest manifest, boolean loaded) {}
