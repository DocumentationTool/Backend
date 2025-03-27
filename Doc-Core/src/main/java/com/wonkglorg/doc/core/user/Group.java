package com.wonkglorg.doc.core.user;

import com.wonkglorg.doc.core.objects.DateHelper;
import com.wonkglorg.doc.core.objects.GroupId;
import com.wonkglorg.doc.core.objects.UserId;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group in the database
 */
public class Group {
    /**
     * The unique identifier for this group
     */
    private final GroupId id;
    /**
     * The name of the group
     */
    private String name;

    /**
     * The user who created the group
     */
    private String createdBy;
    /**
     * The date the group was created
     */
    private LocalDateTime creationDate;

    /**
     * The users in the group
     */
    private final Set<UserId> userIds = new HashSet<>();

    public Group(GroupId id, String name, String createdBy, LocalDateTime creationDate) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.creationDate = creationDate;
    }

    public Group(GroupId id, String name, String createdBy, String creationDate) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.creationDate = DateHelper.parseDateTime(creationDate);
    }

    public GroupId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Set<UserId> getUserIds() {
        return userIds;
    }

    public void setName(String name) {
        this.name = name;
    }
}
