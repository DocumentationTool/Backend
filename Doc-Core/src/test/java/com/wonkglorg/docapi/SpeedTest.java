package com.wonkglorg.docapi;

import com.wonkglorg.doc.core.FileRepository;
import com.wonkglorg.doc.core.RepoProperty;
import com.wonkglorg.doc.core.exception.CoreException;
import com.wonkglorg.doc.core.exception.client.InvalidUserException;
import com.wonkglorg.doc.core.exception.client.ReadOnlyRepoException;
import com.wonkglorg.doc.core.objects.RepoId;
import com.wonkglorg.doc.core.objects.Resource;
import com.wonkglorg.doc.core.request.ResourceRequest;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class SpeedTest {


    @Test
    void insertFtsResources() throws ReadOnlyRepoException, CoreException, GitAPIException, IOException, InvalidUserException {
        RepoProperty property = new RepoProperty();
        property.setPath(Path.of(""));
        FileRepository db = new FileRepository(property, true);
        db.initialize();

        List<Resource> resources = new ArrayList<>();


        for (int i = 0; i < 5000; i++) {
            resources.add(new Resource(Path.of("path" + i), "me", RepoId.of("this"), null, Set.of(), "content" + i));
        }


        db.getDatabase().resourceFunctions().batchInsert(resources);

        ResourceRequest request = new ResourceRequest();
        request.setSearchTerm("content");
        long start = System.currentTimeMillis();
        db.getDatabase().resourceFunctions().findByContent(request);
        long end = System.currentTimeMillis();

        if (end - start > 10) {
            Assertions.assertTrue(false, "Find request took too long " + (end - start) + "ms");
        } else {
            Assertions.assertTrue(true);
        }

    }

}
