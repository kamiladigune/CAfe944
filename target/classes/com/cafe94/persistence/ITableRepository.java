// File: src/main/java/com/cafe94/persistence/ITableRepository.java
package com.cafe94.persistence;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Table;

/**
 * Interface defining persistence operations for restaurant {@link Table}
 * entities
 * @author Adigun Lateef
 * @version 1.0
 */
public interface ITableRepository {

    /**
     * Saves a new table configuration or updates an existing table
     * @param table The Table object to save or update
     * @return The saved or updated Table object as persisted
     * @throws NullPointerException if the table parameter is null.
     * @throws IllegalArgumentException if the table object contains invalid
     * data
     */
    Table save(Table table);

    /**
     * Retrieves a specific table based on its unique table number.
     *
     * @param tableNumber The unique number identifying the table
     * @return An {@code Optional<Table>} containing the found Table if a table
     * with the specified number exists otherwise ,an empty Optional and
     * returns empty Optional if tableNumber is not positive.
     */
    Optional<Table> findByTableNumber(int tableNumber);

    /**
     * Retrieves all tables defined for the restaurant
     *
     * @return A {@code List<Table>} containing all configured Table objects and
     * returns an empty list if none are configured.
     */
    List<Table> findAll();

    /**
     * Retrieves all tables that have a capacity greater than or equal to the
     * specified minimum requirement
     *
     * @param requiredCapacity The minimum number of seats the table must have
     * @return A {@code List<Table>} containing all Table objects that meet or
     * exceed the capacity requirement and eturns an empty list if
     * requiredCapacity is not positive or no tables meet the criteria.
     */
    List<Table> findWithCapacityGreaterThanOrEqual(int requiredCapacity);

    /**
     * Deletes a table configuration based on its unique table number
     * @param tableNumber The unique number identifying the table to delete
     * @return {@code true} if a table with the given number was found and
     * successfully deleted, {@code false} otherwise ).
     */

}