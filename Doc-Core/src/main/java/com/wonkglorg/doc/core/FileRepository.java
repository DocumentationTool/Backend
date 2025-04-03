package com.wonkglorg.doc.core;

import com.wonkglorg.doc.core.db.RepositoryDatabase;
import com.wonkglorg.doc.core.db.UserDatabase;
import com.wonkglorg.doc.core.exception.CoreException;
import com.wonkglorg.doc.core.exception.CoreSqlException;
import com.wonkglorg.doc.core.exception.client.InvalidTagException;
import com.wonkglorg.doc.core.exception.client.InvalidUserException;
import com.wonkglorg.doc.core.exception.client.ReadOnlyRepoException;
import com.wonkglorg.doc.core.git.GitRepo;
import com.wonkglorg.doc.core.git.UserBranch;
import com.wonkglorg.doc.core.objects.Resource;
import com.wonkglorg.doc.core.objects.TagId;
import com.wonkglorg.doc.core.objects.UserId;
import com.wonkglorg.doc.core.path.TargetPath;
import com.wonkglorg.doc.core.request.ResourceRequest;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.wonkglorg.doc.core.git.GitRepo.GitStage.*;

/**
 * Represents a managed repository defined in the application.yml file
 */
public class FileRepository implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(FileRepository.class);

    /**
     * THe properties of the repository
     */
    private final RepoProperty repoProperty;
    /**
     * The backing repo
     */
    private GitRepo gitRepo;
    /**
     * Represents the backing database of a repo
     */
    private RepositoryDatabase dataDB;
    /**
     * A global user database
     */
    private static UserDatabase userDB;

    private final boolean inMemory;

    /**
     * The executor service for checking for changes in the repo every {@link #checkInterval}
     */
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
    /**
     * The interval to check for changes in the repo
     */
    private final Duration checkInterval = Duration.ofMinutes(10);

    public FileRepository(RepoProperty repoProperty, boolean inMemory) throws GitAPIException, ReadOnlyRepoException {
        this.repoProperty = repoProperty;
        this.inMemory = inMemory;
        gitRepo = new GitRepo(repoProperty, inMemory);

        try {
            if (userDB == null) {
                userDB = new UserDatabase(inMemory ? null : Path.of("users.db"));
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RepositoryDatabase getDatabase() {
        return dataDB;
    }

    public GitRepo getGitRepo() {
        return gitRepo;
    }

    /**
     * Initializes the repository by checking for the database file and updating the database
     *
     * @throws GitAPIException if there is an error with the git repo
     */
    public void initialize() throws GitAPIException, CoreException, InvalidUserException, ReadOnlyRepoException, IOException {
        log.info("Looking for repo in: '{}'", repoProperty.getPath());

        dataDB = new RepositoryDatabase(repoProperty, repoProperty.getPath().resolve(repoProperty.getDbName()), this, inMemory);
        dataDB.initialize();

        if (!inMemory) {
            Optional<Path> file = gitRepo.getSingleFile(s -> s.equalsIgnoreCase(repoProperty.getDbName()), UNTRACKED, MODIFIED, ADDED);
            if (file.isEmpty()) {
                log.info("No Database in '{}'. Creating new Database.", repoProperty.getDbName());
            }

            Set<Path> foundFiles = gitRepo.getFiles(s -> s.toLowerCase().endsWith(".md"), UNTRACKED, MODIFIED, ADDED);

            Files.walk(repoProperty.getPath())
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(foundFiles::add);

            Set<Path> filesToCheck = foundFiles.stream().map(this::relativizePathToRepo).collect(Collectors.toSet());


            checkFileChanges(filesToCheck);

            log.info("Scheduling check for changes in '{}'", repoProperty.getId());
            executorService.scheduleAtFixedRate(() -> {
                try {
                    log.info("Update task for repo '{}'", repoProperty.getId());
                    checkFileChanges(gitRepo.getFiles(s -> s.toLowerCase().endsWith(".md"), UNTRACKED, MODIFIED, ADDED));
                } catch (GitAPIException | CoreException | InvalidUserException | ReadOnlyRepoException e) {
                    log.error("Error while checking for changes", e);
                }
            }, 10, checkInterval.toMinutes(), TimeUnit.MINUTES);
        }

    }

    /**
     * Checks for changes in the files and updates the database
     *
     * @param foundFiles the files to check
     * @throws CoreException         if there is an error with the core
     * @throws InvalidUserException  if the user is invalid
     * @throws ReadOnlyRepoException if the repo is read only
     */
    private void checkFileChanges(Set<Path> foundFiles) throws CoreException, InvalidUserException, ReadOnlyRepoException {
        log.info("Checking for changes in {} files", foundFiles.size());

        ResourceRequest request = new ResourceRequest();
        request.targetPath(new TargetPath(null));
        request.repoId(repoProperty.getId());
        request.userId(null);

        List<Resource> resources = dataDB.resourceFunctions().getResources(request);

        Map<Path, Resource> resourceMap = resources.stream().collect(HashMap::new, (m, r) -> m.put(r.resourcePath(), r), Map::putAll);

        List<Path> newResources = foundFiles.stream().filter(f -> resources.stream().noneMatch(r -> r.resourcePath().equals(f))).toList();

        List<Path> deletedResources = resources.stream()
                .map(Resource::resourcePath)
                .filter(path -> foundFiles.stream().noneMatch(path::equals))
                .toList();

        List<Path> matchingResources = resources.stream().map(Resource::resourcePath).filter(foundFiles::contains).toList();

        //pull any changes from the remote
        gitRepo.pull();

        int existingFilesChanged = updateMatchingResources(matchingResources, resourceMap);
        addNewFiles(newResources);
        deleteOldResources(deletedResources);
        log.info("--------Report for repo '{}--------", repoProperty.getId());
        if (newResources.isEmpty() && deletedResources.isEmpty() && existingFilesChanged == 0) {
            log.info("No changes detected in repo '{}'", repoProperty.getId());
            log.info("--------End of report--------");
            return;
        } else {
            log.info("New resources: {}", newResources.size());
            log.info("Deleted resources: {}", deletedResources.size());
            log.info("Updated resources: {}", existingFilesChanged);
            log.info("--------End of report--------");
        }


        addDataDb();
        gitRepo.commit("Startup: Updated resources info: New: %s, Deleted: %s, Updated: %s".formatted(newResources.size(),
                deletedResources.size(),
                existingFilesChanged));

        gitRepo.push();
    }

    public Path relativizePathToRepo(Path path) {
        return gitRepo.getRepoPath().relativize(path);
    }

    public void addDataDb() {
        Path repoRoot = gitRepo.getRepoPath();
        Path fileToAdd = dataDB.getOpenInPath();

        if (!fileToAdd.startsWith(repoRoot)) {
            log.warn("The file {} is not inside the Git repo {}", fileToAdd, repoRoot);
        }


        Path absolutePath = dataDB.getOpenInPath();
        Path relativePath = gitRepo.getRepoPath().relativize(absolutePath);
        gitRepo.add(relativePath);
        gitRepo.add(dataDB.getOpenInPath());
    }

    /**
     * Checks if all tag exist
     *
     * @param tags the tags to check
     * @throws InvalidTagException if the tag does not exist
     */
    public void checkTags(Set<TagId> tags) throws InvalidTagException {
        if (tags == null) {
            return;
        }
        for (var tag : tags) {
            if (!getDatabase().resourceFunctions().tagExists(repoProperty.getId(), tag)) {
                throw new InvalidTagException("Tag '%s' does not exist in '%s'".formatted(tag, repoProperty.getId()));
            }
        }
    }

    /**
     * Adds a file to the database
     *
     * @param resource the resource to add
     */
    public void addResourceAndCommit(Resource resource) {

        if (gitRepo.isMemory()) {
            return;
        }

        try {
            gitRepo.add(resource.resourcePath());
            gitRepo.commit("Added resource %s".formatted(resource.resourcePath()));
            gitRepo.push();
            //UserBranch branch = gitRepo.createBranch(UserId.of(resource.createdBy()));
            Path file = gitRepo.getRepoPath().resolve(resource.resourcePath());
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.createFile(file);
            }

            if (resource.data() != null) {
                Files.write(file, resource.data().getBytes());
            }
            /*
            branch.addFile(file);
            branch.commit("Added resource %s".formatted(resource.resourcePath()));
            branch.closeBranch();
             */
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes a file from the database
     *
     * @param resourcePath the path to the resource
     */
    public void removeResourceAndCommit(UserId userId, Path resourcePath) throws IOException {

        if (gitRepo.isMemory()) {
            return;
        }
        try {
            UserBranch branch = gitRepo.createBranch(userId);
            Files.deleteIfExists(getGitRepo().getRepoPath().resolve(resourcePath));
            branch.updateFileDeleted(resourcePath);
            branch.commit("Deleted resource %s".formatted(resourcePath));
            branch.closeBranch();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds new files to the database
     *
     * @param newFiles the files to add
     */
    private void addNewFiles(List<Path> newFiles) throws CoreSqlException, ReadOnlyRepoException {

        if (gitRepo.isMemory()) {
            return;
        }
        List<Resource> resources = new ArrayList<>();
        for (Path file : newFiles) {
            if (dataDB.resourceFunctions().resourceExists(repoProperty.getId(), file)) {
                log.info("Skipping resource '{}' because it already exists", file);
                continue;
            }

            RevCommit lastCommitDetailsForFile = gitRepo.getLastCommitDetailsForFile(file.toString());
            Resource newResource;
            String content = readData(gitRepo, file);
            if (lastCommitDetailsForFile == null) {
                log.error("File '{}' was not added by git", file);
                newResource = new Resource(file, "system", repoProperty.getId(), null, new HashSet<>(), content);
            } else {

                newResource = new Resource(file,
                        lastCommitDetailsForFile.getAuthorIdent().getName(),
                        repoProperty.getId(),
                        null,
                        new HashSet<>(),
                        content);
            }
            resources.add(newResource);
            gitRepo.add(file);
        }

        dataDB.resourceFunctions().batchInsert(resources);
    }

    /**
     * Updates the resources in the database
     *
     * @param matchingResources the resources to update
     * @return true if the resources have changed
     */
    private int updateMatchingResources(List<Path> matchingResources, Map<Path, Resource> existingResources)
            throws CoreSqlException, ReadOnlyRepoException {

        if (gitRepo.isMemory()) {
            return 0;
        }
        List<Resource> resources = new ArrayList<>();
        for (Path file : matchingResources) {
            RevCommit fileCommit = gitRepo.getLastCommitDetailsForFile(file.toString());
            if (fileCommit == null) {
                continue;
            }

            String authorName = fileCommit.getAuthorIdent().getName();
            Instant instant = Instant.ofEpochSecond(fileCommit.getCommitTime());
            LocalDateTime commitTime = LocalDateTime.ofInstant(instant, fileCommit.getAuthorIdent().getTimeZone().toZoneId());
            Resource existingResource = existingResources.get(file);

            //If the file has not been modified since the last commit, skip it
            if (existingResource.modifiedAt().isEqual(commitTime)) {
                continue;
            }

            Resource newResource = new Resource(file,
                    existingResource.createdAt(),
                    existingResource.createdBy(),
                    commitTime,
                    authorName,
                    repoProperty.getId(),
                    existingResource.getResourceTags(),
                    existingResource.category(),
                    readData(gitRepo, file));
            resources.add(newResource);
            gitRepo.add(file);
        }

        dataDB.resourceFunctions().batchUpdate(resources);
        return resources.size();
    }

    /**
     * Reads the data from a file
     *
     * @param gitRepo the repo to read from
     * @param file    the file to read
     * @return the data in the file
     */
    private String readData(GitRepo gitRepo, Path file) {
        Path repoContextFile = gitRepo.getRepoPath().resolve(file);
        try {
            return Files.readString(repoContextFile);
        } catch (IOException e) {
            log.error("Error while reading file data from '{}'", repoContextFile, e);
            return "";
        }
    }

    /**
     * Deletes old resources no longer in the repository, this is a cleanup operation and manually happens on startup and every {@link #checkInterval}
     *
     * @param deletedResources the resources to delete
     * @throws CoreSqlException if there is an error with the core
     */
    private void deleteOldResources(List<Path> deletedResources) throws CoreSqlException, ReadOnlyRepoException {

        if (gitRepo.isMemory()) {
            return;
        }
        for (Path file : deletedResources) {
            log.info("Deleting resource '{}'", file);
            gitRepo.remove(file);
        }
        dataDB.resourceFunctions().batchDelete(deletedResources);
    }

    public RepoProperty getRepoProperty() {
        return repoProperty;
    }

    public static UserDatabase getUserDB() {
        return userDB;
    }

    @Override
    public void close() throws Exception {

        if (gitRepo.isMemory()) {
            return;
        }
        gitRepo.getGit().close();
        dataDB.close();

    }
}
