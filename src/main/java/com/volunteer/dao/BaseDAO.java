package com.volunteer.dao;

import com.volunteer.exception.DatabaseException;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface demonstrating Collections and Generics.
 * Establishes standard CRUD contracts for database entities.
 *
 * @param <T> the entity type
 * @param <ID> the primary key identifier type (e.g., Long, String)
 */
public interface BaseDAO<T, ID> {

    /**
     * Persists a new entity into the database.
     *
     * @param entity the entity to create
     * @return the created entity with its generated identifier
     * @throws DatabaseException if persistence fails
     */
    T create(T entity) throws DatabaseException;

    /**
     * Finds an entity by its primary key identifier.
     *
     * @param id the primary key
     * @return an {@link Optional} containing the entity if found
     * @throws DatabaseException if a database access error occurs
     */
    Optional<T> findById(ID id) throws DatabaseException;

    /**
     * Retrieves all entities of type {@link T}.
     * Demonstrates type-safe {@link List} collection usage.
     *
     * @return a list of all entities
     * @throws DatabaseException if retrieval fails
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing entity in the database.
     *
     * @param entity the entity with modified fields
     * @return true if updated successfully; false if record was not found
     * @throws DatabaseException if update fails
     */
    boolean update(T entity) throws DatabaseException;

    /**
     * Deletes an entity by its identifier.
     *
     * @param id the primary key
     * @return true if deleted; false if record was not found
     * @throws DatabaseException if deletion fails
     */
    boolean deleteById(ID id) throws DatabaseException;
}
