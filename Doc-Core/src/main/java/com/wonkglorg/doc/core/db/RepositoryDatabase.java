package com.wonkglorg.doc.core.db;

import com.wonkglorg.doc.core.FileRepository;
import com.wonkglorg.doc.core.RepoProperty;
import com.wonkglorg.doc.core.db.dbs.Database;
import static com.wonkglorg.doc.core.db.dbs.DatabaseType.MEMORY_SQLITE;
import static com.wonkglorg.doc.core.db.dbs.DatabaseType.SQLITE;
import com.wonkglorg.doc.core.db.functions.DatabaseFunctions;
import com.wonkglorg.doc.core.db.functions.PermissionFunctions;
import com.wonkglorg.doc.core.db.functions.ResourceFunctions;
import com.wonkglorg.doc.core.exception.CoreSqlException;
import com.wonkglorg.doc.core.objects.RepoId;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Represents the database object for a defined repository
 */
@SuppressWarnings("UnusedReturnValue")
public class RepositoryDatabase extends Database<HikariDataSource>{
	
	private static final Logger log = LoggerFactory.getLogger(RepositoryDatabase.class);
	/**
	 * Permission functions related to this database
	 */
	private final PermissionFunctions permissionFunctions;
	/**
	 * Resource functions related to this database
	 */
	private final ResourceFunctions resourceFunctions;
	/**
	 * The file repository this database is part of
	 */
	private final FileRepository fileRepository;
	/**
	 * The properties of the repository
	 */
	private final RepoProperty repoProperties;
	
	public RepositoryDatabase(RepoProperty repoProperties, Path openInPath, FileRepository fileRepository, boolean inMemory) {
		super(inMemory ? MEMORY_SQLITE : SQLITE, inMemory ? getMemoryDataSource() : getDataSource(openInPath));
		if(inMemory){
			log.info("Using in memory database for repo '{}'", repoProperties.getId());
		} else {
			log.info("Using database at '{}' for repo '{}'", openInPath, repoProperties.getId());
		}
		
		this.fileRepository = fileRepository;
		this.repoProperties = repoProperties;
		this.resourceFunctions = new ResourceFunctions(this);
		this.permissionFunctions = new PermissionFunctions(this);
	}
	
	/**
	 * Retrieves the data source for the current sql connection
	 *
	 * @param openInPath the path to open the data source in
	 * @return the created data source
	 */
	private static HikariDataSource getDataSource(Path openInPath) {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setLeakDetectionThreshold(1000);
		hikariConfig.setJdbcUrl(SQLITE.driver() + openInPath.toString());
		hikariConfig.setDriverClassName(SQLITE.classLoader());
		return new HikariDataSource(hikariConfig);
	}
	
	private static HikariDataSource getMemoryDataSource() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setLeakDetectionThreshold(1000);
		hikariConfig.setJdbcUrl(MEMORY_SQLITE.driver());
		hikariConfig.setDriverClassName(MEMORY_SQLITE.classLoader());
		return new HikariDataSource(hikariConfig);
	}
	
	/**
	 * Initializes the database for the current repo (creating tables, triggers, etc.)
	 */
	public void initialize() throws CoreSqlException {
		log.info("Initialising Database for repo '{}'", repoProperties.getId());
		try{
			DatabaseFunctions.initializeDatabase(this);
			log.info("Creating triggers");
			DatabaseFunctions.initializeTriggers(this);
		} catch(RuntimeException e){
			log.error("Error while initializing Database for repo '{}'", repoProperties.getId(), e);
		}
		log.info("Database initialized for repo '{}'", repoProperties.getId());
		resourceFunctions.initialize();
		permissionFunctions.initialize();
	}
	
	/**
	 * Rebuilds the entire FTS table to remove any unused records
	 */
	public void rebuildFts() throws CoreSqlException {
		DatabaseFunctions.rebuildFts(this);
	}
	
	public RepoId getRepoId() {
		return repoProperties.getId();
	}
	
	public RepoProperty getRepoProperties() {
		return repoProperties;
	}
	
	public ResourceFunctions resourceFunctions() {
		return resourceFunctions;
	}
	
	public PermissionFunctions permissionFunctions() {
		return permissionFunctions;
	}
	
	public FileRepository getFileRepository() {
		return fileRepository;
	}
	
	@Override
	public void close() throws Exception {
		getDataSource().close();
	}
}
