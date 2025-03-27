package com.wonkglorg.doc.core.objects;

/**
 * Representation of a tag for a resource
 * @param tagId The unique identifier for the tag
 * @param tagName The name of the tag
 */
public record Tag(TagId tagId, String tagName){}
