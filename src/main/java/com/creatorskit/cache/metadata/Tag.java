package com.creatorskit.cache.metadata;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A user-defined tag in the Tag Manager. Tag names are unique
 * (case-sensitive) -- {@link CacheMetadataStore} relies on the name
 * as the key for assignment lookups, so {@link #equals} / {@link #hashCode}
 * use only the name. Changing a tag's colour after creation is
 * supported by deleting + re-adding with the same name.
 *
 * <p>Name is capped at 8 characters in the UI, but this class doesn't
 * enforce the cap -- {@link CacheMetadataStore#addTag} does, so saved
 * files with longer names from a future version still load.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public class Tag
{
    private final String name;
    private final TagColor color;
}
