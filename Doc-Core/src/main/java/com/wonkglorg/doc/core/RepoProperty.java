package com.wonkglorg.doc.core;

import com.wonkglorg.doc.core.objects.RepoId;

import java.nio.file.Path;

/**
 * Represents the properties of a single repository that is being managed by the application
 */
public class RepoProperty {
    /**
     * If no repo id is defined uses this index to generate automatic id's to ensure uniqueness
     */
    private static int idIndex = 0;
    /**
     * Name to visually display the repository with
     */
    private RepoId id = RepoId.of("Repository " + idIndex++);
    /**
     * Path to the repository
     */
    private Path path;
    /**
     * If the repository is read only
     */
    private boolean readOnly = false;
    /**
     * Name of the database file
     */
    private String dbName = "data.db";

    /**
     * defines in what repo the data.sql is stored in (if the repo is marked as readOnly
     */
    private Path dbStorage = path;

    private String github;
    private String token;

    public RepoId getId() {
        return id;
    }

    public void setId(RepoId name) {
        this.id = name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Path getDbStorage() {
        return dbStorage;
    }

    public void setDbStorage(Path dbStorage) {
        this.dbStorage = dbStorage;
    }


    public String getGithub() {
        return github;
    }

    public void setGithub(String github) {
        this.github = github;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}