package com.creatorskit.cache.metadata;

/**
 * Six cache-searcher categories that can carry user renames and tag
 * assignments. Used as half of the composite key
 * ({@link CacheType}, id) that identifies a tagged or renamed entry
 * in {@link CacheMetadataStore}.
 *
 * <p>Stored as the enum's {@code name()} when serialised to JSON, so
 * the order or addition of new categories must not reshuffle the
 * existing values (append new ones at the end).
 */
public enum CacheType
{
    NPC,
    OBJECT,
    ITEM,
    SPOTANIM,
    ANIM,
    SOUND
}
