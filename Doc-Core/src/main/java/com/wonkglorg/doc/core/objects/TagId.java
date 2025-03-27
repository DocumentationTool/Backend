package com.wonkglorg.doc.core.objects;

/**
 * A unique identifier for a tag
 * @param id the unique identifier
 */
public record TagId(String id) implements Identifyable{
	public static TagId of(String id) {
		return new TagId(id);
	}
	
	@Override
	public String toString() {
		return id;
	}
}
