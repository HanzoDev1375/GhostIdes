package ir.hanzodev1375.ghostide.refactor.rename;

import ir.hanzodev1375.ghostide.refactor.rename.model.RollbackEntry;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class RollbackManager {

  private final List<RollbackEntry> entries = new ArrayList<>();

  public void writeFile(File file, String newContent) throws IOException {
    byte[] originalContent = file.exists() ? Files.readAllBytes(file.toPath()) : null;
    entries.add(
        new RollbackEntry(RollbackEntry.Action.RESTORE_FILE_CONTENT, file, null, originalContent));
    Files.write(file.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
  }

  public void moveDirectory(File from, File to) throws IOException {
    if (!from.exists()) {
      return;
    }
    createDirectoriesTracked(to.getParentFile());
    entries.add(new RollbackEntry(RollbackEntry.Action.MOVE_FILE_BACK, from, to, null));
    try {
      Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException atomicFailure) {
      try {
        Files.move(from.toPath(), to.toPath());
      } catch (IOException moveFailure) {
        copyDirectoryRecursively(from, to);
        deleteDirectoryRecursively(from);
      }
    }
  }

  public void deleteEmptyDirectory(File directory) throws IOException {
    if (!directory.isDirectory()) {
      return;
    }
    File[] children = directory.listFiles();
    if (children != null && children.length > 0) {
      return;
    }
    entries.add(
        new RollbackEntry(RollbackEntry.Action.RECREATE_DELETED_DIRECTORY, directory, null, null));
    Files.deleteIfExists(directory.toPath());
  }

  public void rollbackAll() {
    for (int i = entries.size() - 1; i >= 0; i--) {
      RollbackEntry entry = entries.get(i);
      try {
        switch (entry.getAction()) {
          case RESTORE_FILE_CONTENT -> restoreFileContent(entry);
          case MOVE_FILE_BACK -> restoreMovedDirectory(entry);
          case RECREATE_DELETED_DIRECTORY -> recreateDirectory(entry);
          case DELETE_CREATED_DIRECTORY -> deleteCreatedDirectory(entry);
        }
      } catch (IOException ignored) {
      }
    }
    entries.clear();
  }

  public void commit() {
    entries.clear();
  }

  private void createDirectoriesTracked(File directory) throws IOException {
    if (directory == null || directory.exists()) {
      return;
    }
    createDirectoriesTracked(directory.getParentFile());
    if (!directory.exists()) {
      Files.createDirectory(directory.toPath());
      entries.add(
          new RollbackEntry(RollbackEntry.Action.DELETE_CREATED_DIRECTORY, directory, null, null));
    }
  }

  private void restoreFileContent(RollbackEntry entry) throws IOException {
    File file = entry.getTarget();
    byte[] originalContent = entry.getOriginalContent();
    if (originalContent == null) {
      Files.deleteIfExists(file.toPath());
    } else {
      Files.write(file.toPath(), originalContent);
    }
  }

  private void restoreMovedDirectory(RollbackEntry entry) throws IOException {
    File originalLocation = entry.getTarget();
    File movedLocation = entry.getBackup();
    if (movedLocation == null || !movedLocation.exists()) {
      return;
    }
    File parent = originalLocation.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    try {
      Files.move(movedLocation.toPath(), originalLocation.toPath(), StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException atomicFailure) {
      Files.move(movedLocation.toPath(), originalLocation.toPath());
    }
  }

  private void recreateDirectory(RollbackEntry entry) throws IOException {
    Files.createDirectories(entry.getTarget().toPath());
  }

  private void deleteCreatedDirectory(RollbackEntry entry) throws IOException {
    Files.deleteIfExists(entry.getTarget().toPath());
  }

  private void copyDirectoryRecursively(File source, File destination) throws IOException {
    if (source.isDirectory()) {
      Files.createDirectories(destination.toPath());
      File[] children = source.listFiles();
      if (children != null) {
        for (File child : children) {
          copyDirectoryRecursively(child, new File(destination, child.getName()));
        }
      }
    } else {
      Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private void deleteDirectoryRecursively(File file) throws IOException {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          deleteDirectoryRecursively(child);
        }
      }
    }
    Files.deleteIfExists(file.toPath());
  }
}
