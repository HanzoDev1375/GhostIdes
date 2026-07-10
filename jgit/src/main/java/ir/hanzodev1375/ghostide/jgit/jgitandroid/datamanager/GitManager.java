package ir.hanzodev1375.ghostide.jgit.jgitandroid.datamanager;

import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FileChange;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.CommitInfo;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.RemoteInfo;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.PushResult;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.PullResult;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.FetchResult;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.StashInfo;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.ConflictFile;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult;
import ir.hanzodev1375.ghostide.jgit.jgitandroid.ChangeType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.attributes.AttributesNode;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GitManager {

  private final String projectPath;
  private Git git = null;
  private Repository repository = null;
  private IgnoreNode ignoreNode = null;
  private AttributesNode attributesNode = null;

  public GitManager(String projectPath) {
    this.projectPath = projectPath;
  }

  public boolean initRepository(String initialBranchName) {
    try {
      File projectDir = new File(projectPath);
      if (!projectDir.exists()) {
        projectDir.mkdirs();
      }

      git = Git.init().setDirectory(projectDir).call();
      repository = git.getRepository();

      if (!initialBranchName.equals("master")) {
        try {
          git.branchRename().setOldName("master").setNewName(initialBranchName).call();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      loadGitIgnore();
      loadGitAttributes();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean initRepository() {
    return initRepository("main");
  }

  public boolean openRepository() {
    try {
      File gitDir = new File(projectPath, ".git");
      if (!gitDir.exists()) return false;

      repository =
          new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build();

      git = new Git(repository);
      loadGitIgnore();
      loadGitAttributes();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private void loadGitIgnore() {
    try {
      File gitignoreFile = new File(projectPath, ".gitignore");
      if (gitignoreFile.exists()) {
        ignoreNode = new IgnoreNode();
        try (FileInputStream input = new FileInputStream(gitignoreFile)) {
          ignoreNode.parse(input);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadGitAttributes() {
    try {
      File gitattributesFile = new File(projectPath, ".gitattributes");
      if (gitattributesFile.exists()) {
        attributesNode = new AttributesNode();
        try (FileInputStream input = new FileInputStream(gitattributesFile)) {
          attributesNode.parse(input);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean isIgnored(String path) {
    try {
      if (repository == null) return false;
      File workTree = repository.getWorkTree();
      File file = new File(workTree, path);
      String relativePath =
          workTree.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');

      return ignoreNode != null
          && ignoreNode.isIgnored(relativePath, file.isDirectory())
              == IgnoreNode.MatchResult.IGNORED;
    } catch (Exception e) {
      return false;
    }
  }

  public Status getStatus() {
    try {
      return git != null ? git.status().call() : null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public boolean setUserConfig(String name, String email) {
    try {
      if (repository == null) return false;
      org.eclipse.jgit.lib.StoredConfig config = repository.getConfig();
      config.setString("user", null, "name", name);
      config.setString("user", null, "email", email);
      config.save();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public String[] getUserConfig() {
    try {
      if (repository == null) return null;
      org.eclipse.jgit.lib.StoredConfig config = repository.getConfig();
      String name = config.getString("user", null, "name");
      String email = config.getString("user", null, "email");
      if (name == null) name = "User";
      if (email == null) email = "user@example.com";
      return new String[] {name, email};
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public List<FileChange> getChangedFiles() {
    List<FileChange> changes = new ArrayList<>();
    Status status = getStatus();
    if (status == null) return changes;

    for (String path : status.getAdded()) {
      if (!isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.ADDED, true));
      }
    }

    for (String path : status.getChanged()) {
      if (!isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.MODIFIED, true));
      }
    }

    for (String path : status.getRemoved()) {
      if (!isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.DELETED, true));
      }
    }

    for (String path : status.getModified()) {
      if (!status.getChanged().contains(path) && !isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.MODIFIED, false));
      }
    }

    for (String path : status.getMissing()) {
      if (!status.getRemoved().contains(path) && !isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.DELETED, false));
      }
    }

    for (String path : status.getUntracked()) {
      if (!isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.UNTRACKED, false));
      }
    }

    for (String path : status.getConflicting()) {
      if (!isIgnored(path)) {
        changes.add(new FileChange(path, ChangeType.CONFLICTING, false));
      }
    }

    return changes;
  }

  public boolean stageFile(String filePath) {
    try {
      if (isIgnored(filePath)) {
        // Force add ignored files if explicitly staged
        git.add().addFilepattern(filePath).setUpdate(false).call();
      } else {
        git.add().addFilepattern(filePath).call();
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean stageAllFiles() {
    try {
      git.add().addFilepattern(".").call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean unstageFile(String filePath) {
    try {
      git.reset().addPath(filePath).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean commit(String message, String author, String email) {
    try {
      git.commit().setMessage(message).setAuthor(new PersonIdent(author, email)).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean commit(String message) {
    return commit(message, "User", "user@example.com");
  }

  public List<CommitInfo> getCommitHistory(int maxCount) {
    List<CommitInfo> commits = new ArrayList<>();
    try {
      Iterable<org.eclipse.jgit.revwalk.RevCommit> logs = git.log().setMaxCount(maxCount).call();
      for (org.eclipse.jgit.revwalk.RevCommit commit : logs) {
        commits.add(
            new CommitInfo(
                commit.getName(),
                commit.getName().substring(0, 7),
                commit.getFullMessage(),
                commit.getAuthorIdent().getName(),
                commit.getAuthorIdent().getEmailAddress(),
                (long) commit.getCommitTime() * 1000));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return commits;
  }

  public List<CommitInfo> getCommitHistory() {
    return getCommitHistory(100);
  }

  public String getCurrentBranch() {
    try {
      return repository != null ? repository.getBranch() : null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public List<String> getAllBranches() {
    List<String> branches = new ArrayList<>();
    try {
      var refs = git.branchList().call();
      for (var ref : refs) {
        branches.add(ref.getName().replaceFirst("^refs/heads/", ""));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return branches;
  }

  public boolean createBranch(String branchName) {
    try {
      git.branchCreate().setName(branchName).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public String getFullDiff() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      git.diff().setOutputStream(out).call();
      String result = out.toString();
      if (result.isEmpty()) {
        return "No changes detected.";
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      return "Error getting diff: " + e.getMessage();
    }
  }

  public String getCommitDiff(String commitHash) {
    try {
      if (repository == null || git == null || commitHash == null) return "";
      org.eclipse.jgit.lib.ObjectId commitId = repository.resolve(commitHash);
      if (commitId == null) return "Commit not found.";

      try (RevWalk revWalk = new RevWalk(repository);
          org.eclipse.jgit.lib.ObjectReader reader = repository.newObjectReader()) {
        org.eclipse.jgit.revwalk.RevCommit commit = revWalk.parseCommit(commitId);

        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
        newTreeParser.reset(reader, commit.getTree());

        org.eclipse.jgit.treewalk.AbstractTreeIterator oldTreeParser;
        if (commit.getParentCount() > 0) {
          org.eclipse.jgit.revwalk.RevCommit parent =
              revWalk.parseCommit(commit.getParent(0).getId());
          CanonicalTreeParser parentParser = new CanonicalTreeParser();
          parentParser.reset(reader, parent.getTree());
          oldTreeParser = parentParser;
        } else {
          oldTreeParser = new org.eclipse.jgit.treewalk.EmptyTreeIterator();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser).setOutputStream(out).call();

        String result = out.toString();
        return result.isEmpty() ? "No changes in this commit." : result;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "Error getting commit diff: " + e.getMessage();
    }
  }

  public boolean checkoutBranch(String branchName) {
    try {
      git.checkout().setName(branchName).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean deleteBranch(String branchName) {
    try {
      git.branchDelete().setBranchNames(branchName).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public String getFileDiff(String filePath) {
    try {
      if (repository == null) return "";
      org.eclipse.jgit.lib.ObjectId head = repository.resolve(Constants.HEAD);

      try (RevWalk revWalk = new RevWalk(repository)) {
        org.eclipse.jgit.revwalk.RevCommit commit = revWalk.parseCommit(head);
        org.eclipse.jgit.revwalk.RevTree tree = commit.getTree();

        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        try (org.eclipse.jgit.lib.ObjectReader reader = repository.newObjectReader()) {
          oldTreeParser.reset(reader, tree);
        }

        List<org.eclipse.jgit.diff.DiffEntry> diffs =
            git.diff().setOldTree(oldTreeParser).setPathFilter(PathFilter.create(filePath)).call();

        StringBuilder diffBuilder = new StringBuilder();
        if (diffs != null) {
          for (org.eclipse.jgit.diff.DiffEntry diff : diffs) {
            diffBuilder
                .append(diff.getChangeType())
                .append(": ")
                .append(diff.getNewPath())
                .append("\n");
          }
        }
        return diffBuilder.toString();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public boolean discardChanges(String filePath) {
    try {
      git.checkout().addPath(filePath).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public void reloadGitIgnoreAndAttributes() {
    loadGitIgnore();
    loadGitAttributes();
  }

  // ─────────────────────────── STASH ───────────────────────────

  public OperationResult stashSave(String message) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      org.eclipse.jgit.revwalk.RevCommit stashCommit =
          git.stashCreate()
              .setIncludeUntracked(true)
              .setWorkingDirectoryMessage(message != null && !message.isEmpty() ? message : null)
              .call();
      if (stashCommit == null) return new OperationResult(false, "Nothing to stash");
      return new OperationResult(true, "Stash saved: " + stashCommit.getName().substring(0, 7));
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Stash failed");
    }
  }

  public List<StashInfo> getStashList() {
    List<StashInfo> list = new ArrayList<>();
    try {
      if (git == null) return list;
      Collection<org.eclipse.jgit.revwalk.RevCommit> stashes = git.stashList().call();
      int index = 0;
      for (org.eclipse.jgit.revwalk.RevCommit stash : stashes) {
        list.add(
            new StashInfo(
                index++,
                stash.getShortMessage(),
                stash.getName().substring(0, 7),
                (long) stash.getCommitTime() * 1000));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return list;
  }

  public OperationResult stashApply(int index) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      git.stashApply().setStashRef("stash@{" + index + "}").call();
      return new OperationResult(true, "Stash applied");
    } catch (org.eclipse.jgit.api.errors.StashApplyFailureException e) {
      return new OperationResult(false, "Stash apply conflict — resolve manually");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Apply failed");
    }
  }

  public OperationResult stashDrop(int index) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      git.stashDrop().setStashRef(index).call();
      return new OperationResult(true, "Stash dropped");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Drop failed");
    }
  }

  public OperationResult stashPop(int index) {
    OperationResult apply = stashApply(index);
    if (apply.isSuccess()) {
      return stashDrop(index);
    }
    return apply;
  }

  // ─────────────────────────── MERGE ───────────────────────────

  public OperationResult mergeBranch(String branchName) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      org.eclipse.jgit.lib.ObjectId branchId = repository.resolve("refs/heads/" + branchName);
      if (branchId == null) return new OperationResult(false, "Branch not found: " + branchName);

      org.eclipse.jgit.api.MergeResult result =
          git.merge()
              .include(branchId)
              .setCommit(true)
              .setFastForward(org.eclipse.jgit.api.MergeCommand.FastForwardMode.FF)
              .setMessage("Merge branch '" + branchName + "'")
              .call();

      org.eclipse.jgit.api.MergeResult.MergeStatus status = result.getMergeStatus();

      if (status == org.eclipse.jgit.api.MergeResult.MergeStatus.MERGED
          || status == org.eclipse.jgit.api.MergeResult.MergeStatus.FAST_FORWARD
          || status == org.eclipse.jgit.api.MergeResult.MergeStatus.ALREADY_UP_TO_DATE) {
        return new OperationResult(true, "Merge successful: " + status.toString());
      } else if (status == org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING) {
        String files = String.join(", ", result.getConflicts().keySet());
        return new OperationResult(false, "Merge conflict in: " + files);
      } else {
        return new OperationResult(false, "Merge failed: " + status.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Merge failed");
    }
  }

  // ─────────────────────────── REBASE ───────────────────────────

  public OperationResult rebaseBranch(String branchName) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      org.eclipse.jgit.lib.ObjectId branchId = repository.resolve("refs/heads/" + branchName);
      if (branchId == null) return new OperationResult(false, "Branch not found: " + branchName);

      org.eclipse.jgit.api.RebaseResult result = git.rebase().setUpstream(branchId).call();

      org.eclipse.jgit.api.RebaseResult.Status status = result.getStatus();

      if (status == org.eclipse.jgit.api.RebaseResult.Status.OK
          || status == org.eclipse.jgit.api.RebaseResult.Status.UP_TO_DATE
          || status == org.eclipse.jgit.api.RebaseResult.Status.FAST_FORWARD) {
        return new OperationResult(true, "Rebase successful");
      } else if (status == org.eclipse.jgit.api.RebaseResult.Status.STOPPED) {
        return new OperationResult(
            false,
            "Rebase stopped due to conflict. Resolve conflicts, then 'git rebase --continue'");
      } else {
        return new OperationResult(false, "Rebase failed: " + status.toString());
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Rebase failed");
    }
  }

  public OperationResult abortRebase() {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call();
      return new OperationResult(true, "Rebase aborted");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, "Abort failed: " + e.getMessage());
    }
  }

  // ─────────────────────────── CONFLICT RESOLVER ───────────────────────────

  public List<ConflictFile> getConflictFiles() {
    List<ConflictFile> conflicts = new ArrayList<>();
    try {
      if (repository == null) return conflicts;
      org.eclipse.jgit.api.Status status = git.status().call();
      for (String path : status.getConflicting()) {
        ConflictFile cf = readConflictFile(path);
        if (cf != null) conflicts.add(cf);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return conflicts;
  }

  private ConflictFile readConflictFile(String relativePath) {
    try {
      File file = new File(repository.getWorkTree(), relativePath);
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

      StringBuilder ours = new StringBuilder();
      StringBuilder theirs = new StringBuilder();
      StringBuilder base = new StringBuilder();
      StringBuilder current = new StringBuilder();

      // Parse conflict markers: <<<<<<< / ======= / >>>>>>>
      int state = 0; // 0=normal, 1=ours, 2=base/theirs
      for (String line : content.split("\n")) {
        if (line.startsWith("<<<<<<<")) {
          state = 1;
        } else if (line.startsWith("=======") && state == 1) {
          state = 2;
        } else if (line.startsWith(">>>>>>>") && state == 2) {
          state = 0;
        } else if (state == 0) {
          current.append(line).append("\n");
          ours.append(line).append("\n");
          theirs.append(line).append("\n");
        } else if (state == 1) {
          ours.append(line).append("\n");
        } else {
          theirs.append(line).append("\n");
        }
      }

      return new ConflictFile(relativePath, ours.toString(), theirs.toString(), current.toString());
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public OperationResult resolveConflictWithOurs(String relativePath) {
    return resolveConflict(relativePath, true);
  }

  public OperationResult resolveConflictWithTheirs(String relativePath) {
    return resolveConflict(relativePath, false);
  }

  private OperationResult resolveConflict(String relativePath, boolean useOurs) {
    try {
      if (repository == null) return new OperationResult(false, "Repository not open");
      File file = new File(repository.getWorkTree(), relativePath);
      String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

      StringBuilder resolved = new StringBuilder();
      int state = 0;
      for (String line : content.split("\n")) {
        if (line.startsWith("<<<<<<<")) {
          state = 1;
        } else if (line.startsWith("=======") && state == 1) {
          state = 2;
        } else if (line.startsWith(">>>>>>>") && state == 2) {
          state = 0;
        } else if (state == 0) {
          resolved.append(line).append("\n");
        } else if (state == 1 && useOurs) {
          resolved.append(line).append("\n");
        } else if (state == 2 && !useOurs) {
          resolved.append(line).append("\n");
        }
      }

      Files.write(file.toPath(), resolved.toString().getBytes(StandardCharsets.UTF_8));
      stageFile(relativePath);
      return new OperationResult(true, "Conflict resolved in: " + relativePath);
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, "Resolve failed: " + e.getMessage());
    }
  }

  public OperationResult resolveConflictWithCustom(String relativePath, String resolvedContent) {
    try {
      if (repository == null) return new OperationResult(false, "Repository not open");
      File file = new File(repository.getWorkTree(), relativePath);
      Files.write(file.toPath(), resolvedContent.getBytes(StandardCharsets.UTF_8));
      stageFile(relativePath);
      return new OperationResult(true, "Conflict resolved with custom content");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, "Resolve failed: " + e.getMessage());
    }
  }

  public void close() {
    if (git != null) git.close();
    if (repository != null) repository.close();
  }

  public boolean addRemote(String name, String url) {
    try {
      git.remoteAdd().setName(name).setUri(new URIish(url)).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean removeRemote(String name) {
    try {
      git.remoteRemove().setRemoteName(name).call();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public List<RemoteInfo> getRemotes() {
    List<RemoteInfo> remotes = new ArrayList<>();
    try {
      List<org.eclipse.jgit.transport.RemoteConfig> remoteConfigs = git.remoteList().call();
      for (org.eclipse.jgit.transport.RemoteConfig remote : remoteConfigs) {
        String url = remote.getURIs().isEmpty() ? "" : remote.getURIs().get(0).toString();
        remotes.add(new RemoteInfo(remote.getName(), url, url));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return remotes;
  }

  public PushResult push(String remoteName, String branchName, String username, String password) {
    try {
      org.eclipse.jgit.api.PushCommand pushCommand = git.push();
      pushCommand.setRemote(remoteName);

      if (branchName != null) {
        pushCommand.setRefSpecs(
            new RefSpec("refs/heads/" + branchName + ":refs/heads/" + branchName));
      }

      if (username != null && password != null) {
        pushCommand.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(username, password));
      }

      Iterable<org.eclipse.jgit.transport.PushResult> results = pushCommand.call();
      boolean success = true;
      for (org.eclipse.jgit.transport.PushResult result : results) {
        for (RemoteRefUpdate update : result.getRemoteUpdates()) {
          if (update.getStatus() != RemoteRefUpdate.Status.OK
              && update.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE) {
            success = false;
            break;
          }
        }
      }

      return new PushResult(success, success ? "Push successful" : "Push failed");
    } catch (Exception e) {
      e.printStackTrace();
      return new PushResult(false, e.getMessage() != null ? e.getMessage() : "Push failed");
    }
  }

  public PushResult push() {
    return push("origin", null, null, null);
  }

  public PullResult pull(String remoteName, String branchName, String username, String password) {
    try {
      org.eclipse.jgit.api.PullCommand pullCommand = git.pull();
      pullCommand.setRemote(remoteName);

      if (branchName != null) {
        pullCommand.setRemoteBranchName(branchName);
      }

      if (username != null && password != null) {
        pullCommand.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(username, password));
      }

      org.eclipse.jgit.api.PullResult result = pullCommand.call();
      boolean success = result.isSuccessful();

      // Check for merge conflicts explicitly
      if (!success) {
        org.eclipse.jgit.api.MergeResult mergeResult = result.getMergeResult();
        if (mergeResult != null
            && mergeResult.getMergeStatus()
                == org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING) {
          java.util.Set<String> conflictingFiles = mergeResult.getConflicts().keySet();
          String fileList = String.join(", ", conflictingFiles);
          return new PullResult(
              false, "Merge conflict in: " + fileList + ". Resolve conflicts and commit.");
        }
        return new PullResult(false, "Pull failed");
      }

      return new PullResult(true, "Pull successful");
    } catch (org.eclipse.jgit.api.errors.CheckoutConflictException e) {
      return new PullResult(
          false,
          "Cannot merge: local changes conflict with remote. "
              + "Commit or stash your changes first. Conflicting files: "
              + String.join(", ", e.getConflictingPaths()));
    } catch (Exception e) {
      e.printStackTrace();
      String msg = e.getMessage() != null ? e.getMessage() : "Pull failed";
      return new PullResult(false, msg);
    }
  }

  public PullResult pull() {
    return pull("origin", null, null, null);
  }

  public FetchResult fetch(String remoteName, String username, String password) {
    try {
      org.eclipse.jgit.api.FetchCommand fetchCommand = git.fetch();
      fetchCommand.setRemote(remoteName);

      if (username != null && password != null) {
        fetchCommand.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(username, password));
      }

      org.eclipse.jgit.transport.FetchResult result = fetchCommand.call();
      int updatedCount = result.getTrackingRefUpdates().size();
      String msg =
          updatedCount > 0
              ? "Fetch successful: " + updatedCount + " ref(s) updated from " + remoteName
              : "Already up to date with " + remoteName;
      return new FetchResult(true, msg);
    } catch (Exception e) {
      e.printStackTrace();
      return new FetchResult(false, e.getMessage() != null ? e.getMessage() : "Fetch failed");
    }
  }

  public FetchResult fetch() {
    return fetch("origin", null, null);
  }

  public boolean clone(String remoteUrl, String localPath, String username, String password) {
    try {
      var cloneCommand = Git.cloneRepository();
      cloneCommand.setURI(remoteUrl);
      cloneCommand.setDirectory(new File(localPath));

      if (username != null && password != null) {
        cloneCommand.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(username, password));
      }

      git = cloneCommand.call();
      repository = git.getRepository();
      loadGitIgnore();
      loadGitAttributes();
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean hasRemote() {
    try {
      return git != null && !git.remoteList().call().isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isRepositoryInitialized() {
    try {
      File gitDir = new File(projectPath, ".git");
      return gitDir.exists() && gitDir.isDirectory();
    } catch (Exception e) {
      return false;
    }
  }

  // ─────────────────────────── BLAME ───────────────────────────

  public java.util.List<ir.hanzodev1375.ghostide.jgit.jgitandroid.model.BlameInfo> getBlame(
      String filePath) {
    java.util.List<ir.hanzodev1375.ghostide.jgit.jgitandroid.model.BlameInfo> result =
        new ArrayList<>();
    try {
      if (git == null) return result;
      org.eclipse.jgit.blame.BlameResult blame =
          git.blame().setFilePath(filePath).setFollowFileRenames(true).call();
      if (blame == null) return result;
      blame.computeAll();
      int lines = blame.getResultContents().size();
      for (int i = 0; i < lines; i++) {
        org.eclipse.jgit.revwalk.RevCommit commit = blame.getSourceCommit(i);
        String hash = commit != null ? commit.getName().substring(0, 7) : "???????";
        String author = commit != null ? commit.getAuthorIdent().getName() : "Unknown";
        long ts = commit != null ? (long) commit.getCommitTime() * 1000 : 0;
        String line = blame.getResultContents().getString(i);
        result.add(
            new ir.hanzodev1375.ghostide.jgit.jgitandroid.model.BlameInfo(
                i + 1, line, hash, author, ts));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  // ─────────────────────────── RESET ───────────────────────────

  public ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult reset(
      ir.hanzodev1375.ghostide.jgit.jgitandroid.model.ResetMode mode, int stepsBack) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      org.eclipse.jgit.lib.ObjectId targetId = repository.resolve("HEAD~" + stepsBack);
      if (targetId == null)
        return new OperationResult(false, "No commit found at HEAD~" + stepsBack);
      org.eclipse.jgit.api.ResetCommand.ResetType resetType;
      switch (mode) {
        case SOFT:
          resetType = org.eclipse.jgit.api.ResetCommand.ResetType.SOFT;
          break;
        case HARD:
          resetType = org.eclipse.jgit.api.ResetCommand.ResetType.HARD;
          break;
        default:
          resetType = org.eclipse.jgit.api.ResetCommand.ResetType.MIXED;
          break;
      }
      git.reset().setMode(resetType).setRef(targetId.getName()).call();
      return new OperationResult(true, "Reset " + mode.name().toLowerCase() + " successful");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Reset failed");
    }
  }

  // ─────────────────────────── TAGS ───────────────────────────

  public ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult createTag(
      String name, String message) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      var cmd = git.tag().setName(name);
      if (message != null && !message.isEmpty()) {
        cmd.setMessage(message).setAnnotated(true);
      } else {
        cmd.setAnnotated(false);
      }
      cmd.call();
      return new OperationResult(true, "Tag '" + name + "' created");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Tag failed");
    }
  }

  public java.util.List<ir.hanzodev1375.ghostide.jgit.jgitandroid.model.TagInfo> getTags() {
    java.util.List<ir.hanzodev1375.ghostide.jgit.jgitandroid.model.TagInfo> tags =
        new ArrayList<>();
    try {
      if (git == null) return tags;
      java.util.List<org.eclipse.jgit.lib.Ref> refs = git.tagList().call();
      try (org.eclipse.jgit.revwalk.RevWalk walk =
          new org.eclipse.jgit.revwalk.RevWalk(repository)) {
        for (org.eclipse.jgit.lib.Ref ref : refs) {
          String tagName = ref.getName().replaceFirst("^refs/tags/", "");
          try {
            org.eclipse.jgit.revwalk.RevObject obj = walk.parseAny(ref.getObjectId());
            String hash = ref.getObjectId().getName().substring(0, 7);
            String msg = "";
            long ts = 0;
            if (obj instanceof org.eclipse.jgit.revwalk.RevTag) {
              org.eclipse.jgit.revwalk.RevTag tag = (org.eclipse.jgit.revwalk.RevTag) obj;
              msg = tag.getFullMessage() != null ? tag.getFullMessage() : "";
              if (tag.getTaggerIdent() != null) ts = tag.getTaggerIdent().getWhen().getTime();
            } else if (obj instanceof org.eclipse.jgit.revwalk.RevCommit) {
              org.eclipse.jgit.revwalk.RevCommit commit = (org.eclipse.jgit.revwalk.RevCommit) obj;
              ts = (long) commit.getCommitTime() * 1000;
            }
            tags.add(
                new ir.hanzodev1375.ghostide.jgit.jgitandroid.model.TagInfo(
                    tagName, hash, msg, ts));
          } catch (Exception ignored) {
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return tags;
  }

  public ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult deleteTag(String name) {
    try {
      if (git == null) return new OperationResult(false, "Repository not open");
      git.tagDelete().setTags(name).call();
      return new OperationResult(true, "Tag '" + name + "' deleted");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, e.getMessage() != null ? e.getMessage() : "Delete failed");
    }
  }

  // ─────────────────────────── GITIGNORE ───────────────────────────

  public String readGitIgnore() {
    try {
      java.io.File f = new java.io.File(projectPath, ".gitignore");
      if (!f.exists()) return "";
      return new String(
          java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  public ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult saveGitIgnore(
      String content) {
    try {
      java.io.File f = new java.io.File(projectPath, ".gitignore");
      java.nio.file.Files.write(
          f.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return new OperationResult(true, "Gitignore saved");
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, "Failed to save gitignore: " + e.getMessage());
    }
  }

  public ir.hanzodev1375.ghostide.jgit.jgitandroid.model.OperationResult addToGitIgnore(
      String pattern) {
    try {
      String current = readGitIgnore();
      if (current.contains(pattern)) return new OperationResult(true, "Pattern already exists");
      String newContent =
          current.isEmpty()
              ? pattern + "\n"
              : (current.endsWith("\n")
                  ? current + pattern + "\n"
                  : current + "\n" + pattern + "\n");
      return saveGitIgnore(newContent);
    } catch (Exception e) {
      e.printStackTrace();
      return new OperationResult(false, "Failed: " + e.getMessage());
    }
  }
}
