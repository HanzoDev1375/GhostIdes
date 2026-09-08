package com.termux.view.completion.providers;

import com.termux.view.completion.TerminalCompletionItem;
import com.termux.view.completion.TerminalCompletionItemKind;

import java.util.ArrayList;
import java.util.List;

/** Provides completions for common shell commands, built-in keywords and per-command options. */
public class CommandCompletionProvider implements TerminalCompletionProvider {

  private static final String[] COMMON_COMMANDS = {
    "ls",
    "cd",
    "pwd",
    "mkdir",
    "rmdir",
    "rm",
    "cp",
    "mv",
    "touch",
    "cat",
    "less",
    "more",
    "head",
    "tail",
    "grep",
    "find",
    "locate",
    "echo",
    "printf",
    "read",
    "test",
    "true",
    "false",
    "chmod",
    "chown",
    "chgrp",
    "ln",
    "unlink",
    "readlink",
    "df",
    "du",
    "free",
    "top",
    "htop",
    "ps",
    "kill",
    "killall",
    "pkill",
    "jobs",
    "bg",
    "fg",
    "nohup",
    "screen",
    "tmux",
    "ssh",
    "scp",
    "rsync",
    "wget",
    "curl",
    "ping",
    "traceroute",
    "ifconfig",
    "ip",
    "netstat",
    "ss",
    "dig",
    "nslookup",
    "host",
    "git",
    "svn",
    "hg",
    "python",
    "python3",
    "pip",
    "pip3",
    "node",
    "npm",
    "yarn",
    "java",
    "javac",
    "gradle",
    "mvn",
    "ant",
    "gcc",
    "g++",
    "make",
    "cmake",
    "gdb",
    "vim",
    "vi",
    "nano",
    "emacs",
    "tar",
    "zip",
    "unzip",
    "gzip",
    "gunzip",
    "bzip2",
    "sed",
    "awk",
    "cut",
    "sort",
    "uniq",
    "wc",
    "tr",
    "tee",
    "xargs",
    "yes",
    "sleep",
    "date",
    "time",
    "watch",
    "man",
    "info",
    "which",
    "whereis",
    "type",
    "alias",
    "unalias",
    "export",
    "env",
    "set",
    "unset",
    "source",
    "exec",
    "eval",
    "exit",
    "logout",
    "reboot",
    "shutdown",
    "apt",
    "apt-get",
    "dpkg",
    "yum",
    "dnf",
    "pacman",
    "brew",
    "docker",
    "podman",
    "kubectl",
    "mount",
    "umount",
    "fdisk",
    "lsblk",
    "blkid",
    "crontab",
    "at",
    "batch",
    "su",
    "sudo",
    "doas"
  };

  private static final String[][] COMMAND_OPTIONS = {
    {
      "ls", "-l", "-a", "-la", "-al", "-h", "-lh", "-ltr", "-lt", "-1", "-d", "-i", "-R", "-S",
      "-r", "-t"
    },
    {
      "git",
      "add",
      "commit",
      "push",
      "pull",
      "clone",
      "checkout",
      "branch",
      "status",
      "diff",
      "log",
      "merge",
      "rebase",
      "stash",
      "remote",
      "tag",
      "fetch",
      "reset",
      "revert",
      "blame",
      "show",
      "init",
      "config",
      "clean",
      "mv",
      "rm"
    },
    {
      "apt",
      "install",
      "remove",
      "purge",
      "update",
      "upgrade",
      "search",
      "show",
      "list",
      "autoremove",
      "clean",
      "full-upgrade"
    },
    {
      "apt-get",
      "install",
      "remove",
      "purge",
      "update",
      "upgrade",
      "dist-upgrade",
      "autoremove",
      "clean"
    },
    {"python", "-c", "-m", "-i", "-h", "--help", "--version", "-V", "-B", "-E", "-O", "-u", "-v"},
    {"python3", "-c", "-m", "-i", "-h", "--help", "--version", "-V", "-B", "-E", "-O", "-u", "-v"},
    {
      "npm",
      "install",
      "run",
      "start",
      "build",
      "test",
      "publish",
      "init",
      "update",
      "uninstall",
      "list"
    },
    {
      "docker", "run", "ps", "images", "build", "pull", "push", "stop", "start", "restart", "logs",
      "exec", "rm", "rmi"
    },
    {
      "systemctl",
      "start",
      "stop",
      "restart",
      "status",
      "enable",
      "disable",
      "is-active",
      "is-enabled",
      "list-units"
    }
  };

  @Override
  public List<TerminalCompletionItem> getCompletions(String currentLine, int cursorPosition) {
    List<TerminalCompletionItem> items = new ArrayList<>();

    if (currentLine == null || currentLine.isEmpty()) {
      return items;
    }

    String textBeforeCursor = currentLine.substring(0, clamp(cursorPosition, currentLine.length()));
    String currentWord = extractCurrentWord(textBeforeCursor);

    if (currentWord.isEmpty()) {
      return items;
    }

    String[] parts = textBeforeCursor.trim().split("\\s+");

    if (parts.length <= 1) {
      // Completing a command name.
      for (String cmd : COMMON_COMMANDS) {
        if (cmd.startsWith(currentWord)) {
          items.add(
              new TerminalCompletionItem(
                  cmd, cmd, "command", currentWord, TerminalCompletionItemKind.Function));
        }
      }
    } else {
      // Completing arguments for a known command.
      String command = parts[0];
      for (String[] optionEntry : COMMAND_OPTIONS) {
        if (optionEntry[0].equals(command)) {
          for (int i = 1; i < optionEntry.length; i++) {
            String flag = optionEntry[i];
            if (flag.startsWith(currentWord)) {
              items.add(
                  new TerminalCompletionItem(
                      flag,
                      flag,
                      command + " option",
                      currentWord,
                      TerminalCompletionItemKind.Keyword));
            }
          }
          break;
        }
      }
    }

    return items;
  }

  @Override
  public String getProviderName() {
    return "CommandCompletionProvider";
  }

  /** Extract the current word being typed after the last space. */
  private String extractCurrentWord(String textBeforeCursor) {
    int start = lastSeparator(textBeforeCursor) + 1;
    return textBeforeCursor.substring(start);
  }

  private static int lastSeparator(String text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\t') {
        return i;
      }
    }
    return -1;
  }

  private static int clamp(int value, int max) {
    return Math.max(0, Math.min(value, max));
  }
}
