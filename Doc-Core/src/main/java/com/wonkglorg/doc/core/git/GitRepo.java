package com.wonkglorg.doc.core.git;

import com.wonkglorg.doc.core.RepoProperty;
import com.wonkglorg.doc.core.exception.client.ReadOnlyRepoException;
import com.wonkglorg.doc.core.objects.UserId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.ServiceUnavailableException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Backing representation of a git repository held by the {@link com.wonkglorg.doc.core.FileRepository}
 */
public class GitRepo {
    /**
     * The different stages a file can be in
     */
    public enum GitStage {
        UNTRACKED(Status::getUntracked),
        MODIFIED(Status::getModified),
        ADDED(Status::getAdded);
        private final Function<Status, Set<String>> getFiles;

        GitStage(Function<Status, Set<String>> getFiles) {
            this.getFiles = getFiles;
        }

        public Set<String> getFiles(Status stage) {
            return getFiles.apply(stage);
        }
    }

    /**
     * All current branches active for users
     */
    private final Map<UserId, UserBranch> currentUserBranches = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(GitRepo.class);
    /**
     * The Plumbing view of the backing git repo
     * (<a href="https://git-scm.com/book/en/v2/Appendix-B:-Embedding-Git-in-your-Applications-JGit">Docs</a>)
     */
    private Repository repository;
    /**
     * The Porcelain view of the backing git repo
     * (<a href="https://git-scm.com/book/en/v2/Appendix-B:-Embedding-Git-in-your-Applications-JGit">Docs</a>)
     */
    private Git git;

    /**
     * Is the same as {@link #repository} unless {@link RepoProperty#isReadOnly()} is defined, in
     * that case this repo points specifically to the path defined by
     * {@link RepoProperty#getDbStorage()}
     */
    private Repository databaseRepository;

    /**
     * Behaves the same as {@link #databaseRepository}
     */
    private Git databaseGit;
    /**
     * The properties of the repository this is a part of
     */
    private final RepoProperty properties;

    /**
     * Creates a new GitRepo object
     *
     * @param properties of the owning {@link com.wonkglorg.doc.core.FileRepository}
     * @throws GitAPIException
     */
    public GitRepo(RepoProperty properties) throws GitAPIException, ReadOnlyRepoException {
        this.properties = properties;
        Path pathToLocalRepo = properties.getPath();
        Path gitFile = pathToLocalRepo.resolve(".git");
        if (!Files.exists(pathToLocalRepo) || !Files.exists(gitFile)) {
            handleMissingRepository(pathToLocalRepo);
        } else {
            openRepository(pathToLocalRepo);
        }

        Path pathToDB = properties.getDbStorage();

        if (properties.isReadOnly() && pathToDB == null) {
            throw new ServiceUnavailableException("Read-only repository with no valid database reference for: '%s' this should be specified in the repository properties".formatted(properties.getDbName()));
        }

        if (pathToDB == null) {
            log.info("No unique Database Repo specified using Documentation Repo instead: '{}'", pathToLocalRepo);
            pathToDB = pathToLocalRepo; //its not read only so not forced to be a seperate repo and no explicit repo is specified (db gets moved into same repo as docs)
        }

        if (!Files.exists(pathToDB)) {
            handleMissingDatabaseRepository(pathToDB);
        } else {
            openDatabaseRepository(pathToDB);
        }

        //needs to at least contain 1 commit otherwise jgit complains
        ensureInitialCommit();
    }

    /**
     * Adds a file to the git repo (does not call any io operations and only stages the exiting file)
     *
     * @param file the file to add
     */
    public void add(Path file) {
        try {
            git.add().addFilepattern(file.toString()).call();
        } catch (GitAPIException e) {
            log.error("Error while adding file: " + file, e);
        }
    }

    /**
     * Removes a file from the git repo (does not call any io operations and only stages the exiting file)
     *
     * @param file the file to remove
     */
    public void remove(Path file) {
        try {
            git.rm().addFilepattern(file.toString()).call();
        } catch (GitAPIException e) {
            log.error("Error while removing file: " + file, e);
        }
    }

    /**
     * Commits the current changes to the git repo
     *
     * @param message the message to commit with
     */
    public void commit(String message) {
        try {
            git.commit().setMessage(message).call();
        } catch (GitAPIException e) {
            log.error("Error while committing", e);
        }
    }

    /**
     * Checks if the repository has any commits
     *
     * @return true if the repository has commits, false otherwise
     */
    private boolean repoHasCommits() {
        try {
            // Try to get the HEAD reference. If HEAD doesn't exist, there are no commits yet.
            Ref head = git.getRepository().exactRef("HEAD");
            if (head == null) {
                return false;
            }
            //head can exist without an actual commit, so commit specific check too
            ObjectId masterCommit = git.getRepository().resolve("refs/heads/master");
            return masterCommit != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Ensures that the repository has at least one commit
     */
    private void ensureInitialCommit() {
        // Check if the repository has any commits
        if (!repoHasCommits()) {
            createInitialCommit();
        }
    }

    /**
     * Creates an initial commit in the repository (this is needed for git to properly obtain the head and create branches)
     */
    private void createInitialCommit() {
        try {
            //make sure branch exists
            if (git.getRepository().findRef("HEAD") == null) {
                //create a branch (e.g., 'master' or 'main') if it doesn't exist
                git.branchCreate().setName("master").call();
                git.checkout().setName("master").call();
            }

            // Now, we can safely commit
            git.commit().setMessage("Initial commit").setAllowEmpty(true).call();
        } catch (GitAPIException e) {
            log.error("Failed to create initial commit", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Pulls the latest changes from the remote if it exists otherwise does nothing
     */
    public void pull() {
        if (!remoteExists("origin")) {
            log.warn("No remote found, skipping pull");
            return;
        }
        try {
            git.pull().call();
        } catch (GitAPIException e) {
            log.error("Error while pulling from remote", e);
        }
    }

    /**
     * Pushes the current branch to the remote if it exists otherwise does nothing
     */
    public void push() {
        if (!remoteExists("origin")) {
            log.warn("No remote found, skipping push");
            return;
        }
        try {
            git.push().call();
        } catch (GitAPIException e) {
            log.error("Error while pushing to remote", e);
        }
    }

    /**
     * Checks if a remote exists and is reachable
     *
     * @param remoteName the name of the remote
     * @return true if the remote exists and is reachable, false otherwise
     */
    private boolean remoteExists(String remoteName) {
        try {
            List<RemoteConfig> remotes = git.remoteList().call();
            for (RemoteConfig remote : remotes) {
                if (remote.getName().equals(remoteName) && !remote.getURIs().isEmpty()) {
                    // Try to connect to the remote
                    URIish uri = remote.getURIs().get(0);
                    try (Transport transport = Transport.open(git.getRepository(), uri)) {
                        transport.close();
                        return true; // Connection successful
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error checking remote existence", e);
        }
        return false;
    }

    /**
     * Creates a new branch for a user
     *
     * @param userId the user to create the branch for
     * @return the created branch
     */
    public UserBranch createBranch(UserId userId) {
        UserBranch branch = null;
        try {
            branch = new UserBranch(this, userId);
        } catch (GitAPIException e) {
            log.error("Error while creating branch for user: " + userId, e);
            return null;
        }
        currentUserBranches.put(userId, branch);
        return branch;
    }

    /**
     * Gets the branch for a user
     *
     * @param userId the user to get the branch for
     * @return the branch for the user
     */
    public UserBranch getBranch(String userId) {
        return currentUserBranches.get(userId);
    }

    /**
     * Closes the branch for a user
     *
     * @param path the path to the branch
     */
    private void handleMissingRepository(Path path) throws ReadOnlyRepoException {
        if (properties.isReadOnly()) {
            throw new ReadOnlyRepoException("Unable to locate Repository Marked as Read-Only! at path: " + path);
        }
        log.info("No local repository found. Creating a new one...");
        createRepoFromPath(path);
        openRepository(path);
        log.info("Created a new Git repository");
    }

    /**
     * Opens a repository from the given path
     * @param path the path to the repository
     */
    private void openRepository(Path path) {
        repository = openRepoFromPath(path).orElseThrow();
        git = new Git(repository);
        log.info("com.wonkglorg.doc.core.git.GitRepo initialized");
    }

    /**
     * Creates a git repository from the given path when non was found (this does not check if this is really the case and should be done before by
     * verifying the path
     * @param path the path to the repository
     */
    private void handleMissingDatabaseRepository(Path path) {
        log.info("No database repository found. Creating a new one...");
        createRepoFromPath(path);
        openDatabaseRepository(path);
        log.info("Created a new database Git repository");
    }

    private void openDatabaseRepository(Path path) {
        databaseRepository = openRepoFromPath(path).orElseThrow();
        databaseGit = new Git(databaseRepository);
        log.info("Database com.wonkglorg.doc.core.git.GitRepo opened");
    }

    /**
     * Creates a git repository from the given path
     *
     * @param pathToRepo the path to the root of the repo
     */
    public static void createRepoFromPath(Path pathToRepo) {
        File repoDir = pathToRepo.toFile();
        //creates the repo if it doesn't exist, FileRepository doesn't handle empty projects
        try (Git ignored = Git.init().setDirectory(repoDir).call()) {
            log.info("Initialized new Git repository at: " + repoDir.getAbsolutePath());
        } catch (GitAPIException e) {
            log.error("Failed to initialize Git repository", e);
        }

    }

    /**
     * Opens an existing repo
     *
     * @param pathToRepo the path to the root of the repo
     * @return an empty optional if no valid repo was fond, otherwise the loaded repo
     */
    public static Optional<Repository> openRepoFromPath(Path pathToRepo) {

        try {
            return Optional.of(new FileRepositoryBuilder().setGitDir(pathToRepo.resolve(".git").toFile()) // Ensure it points to .git directory
                    .readEnvironment().findGitDir().setMustExist(true).build());
        } catch (IOException e) {
            log.error("IO Exception while accessing repository at: " + pathToRepo, e);
        }
        return Optional.empty();
    }

    /**
     * @param filter
     * @param stages
     * @return the relative path to the repo
     * @throws GitAPIException
     */
    private HashSet<Path> get(Predicate<String> filter, boolean quitSingle, GitStage... stages) throws GitAPIException {
        HashSet<Path> files = new HashSet<>();
        Status status = git.status().call();
        Set<String> allFiles = new HashSet<>();
        for (GitStage stage : stages) {
            allFiles.addAll(stage.getFiles(status));
        }

        for (String filePath : allFiles) {
            if (filter.test(filePath)) {
                files.add(Path.of(filePath));
                if (quitSingle) {
                    return files;
                }
            }
        }

        return files;
    }

    /**
     * Gets the last commit details of a file, including commit ID, author, and timestamp.
     *
     * @param filePath the path to the file
     * @return a string containing commit ID, author, and timestamp, or null if no commit was found
     */
    public RevCommit getLastCommitDetailsForFile(String filePath) {
        try {
            Iterable<RevCommit> logs = git.log().addPath(filePath).setMaxCount(1).call();
            return logs.iterator().next();
        } catch (GitAPIException e) {
            log.error("Error while getting last commit details for file: " + filePath, e);
        }
        return null; // No commit found
    }

    /**
     * Retrieves files from git repo
     *
     * @param filter the filter the file path should match
     * @param stages the stages the file could be in
     * @return the relative path to the repo
     * @throws GitAPIException
     */
    public Set<Path> getFiles(Predicate<String> filter, GitStage... stages) throws GitAPIException {
        return get(filter, false, stages);
    }

    public Git getGit() {
        return git;
    }

    public RepoProperty getProperties() {
        return properties;
    }

    /**
     * @return the working directory of the git repo
     */
    public Path getRepoPath() {
        return Path.of(repository.getWorkTree().getAbsolutePath());
    }

    public Path getDatabaseRepoPath() {
        return Path.of(databaseRepository.getWorkTree().getAbsolutePath());
    }

    public Repository getRepository() {
        return repository;
    }

    /**
     * @param filter the filter the file path should match
     * @param stages the stages the file could be in
     * @return the relative path to the repo
     * @throws GitAPIException if an error occurs while getting the files
     */
    public Optional<Path> getSingleFile(Predicate<String> filter, GitStage... stages) throws GitAPIException {
        return get(filter, true, stages).stream().findFirst();
    }

    //or trow away the branch if its not needed anymore and they quit the changes

    public Repository getDatabaseRepository() {
        return databaseRepository;
    }

    public Git getDatabaseGit() {
        return databaseGit;
    }

    /**
     * @return the name of the master branch
     */
    public String getMasterBranchName() {
        try {
            return git.getRepository().findRef("refs/heads/master").getName();
        } catch (IOException e) {
            log.error("Error while getting master branch name", e);
            return null;
        }
    }

}
