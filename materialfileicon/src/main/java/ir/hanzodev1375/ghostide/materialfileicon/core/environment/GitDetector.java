package ir.hanzodev1375.ghostide.materialfileicon.core.environment;

import java.io.File;

class GitDetector {

    protected static boolean isGitDirectory(String dirPath) {
        /*
        File file = new File(dirPath);
        if (!file.isDirectory()) {
            return false;
        }
        File gitDir = new File(file, ".git");
        return gitDir.exists() && gitDir.isDirectory();
        */
        return false;
    }

    protected static boolean isGitFile(String filePath) {
        /*
             File file = new File(filePath);
             if (!file.exists() || file.isDirectory()) {
                 return false;
             }
             File parentDir = new File(file.getParent());
             return isGitDirectory(parentDir.getAbsolutePath());
        */
        return false;
    }

    protected static boolean isGitIgnoreFile(String filePath) {
        return false;
        // return new FileEnvironmentHelper(filePath).isFileHasKeyName(filePath, ".gitignore", true,
        // true);
    }

}