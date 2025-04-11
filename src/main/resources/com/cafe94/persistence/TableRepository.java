// File: src/main/java/com/cafe94/persistence/TableRepository.java
package com.cafe94.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Table;

/**
 * Concrete implementation of {@link ITableRepository} using Java Serialization
 * for persistence
 * @author Adigun Lateef
 * @version 1.0
 */
public class TableRepository implements ITableRepository {

    private static final Logger LOGGER =
    Logger.getLogger(TableRepository.class.getName());
    private final Map<Integer, Table> tables = new ConcurrentHashMap<>();
    private final String storageFilePath;

    /**
     * Constructs the repository, loading existing table data from the storage
     * file
     * @param storageFilePath Path to the data file
     * @param initialTables   A list of tables to populate if the data file is
     * empty or not found
     * @throws NullPointerException if storageFilePath is null.
     */
    public TableRepository(String storageFilePath,
    List<Table> initialTables) {
        this.storageFilePath = Objects.requireNonNull(storageFilePath,
        "Storage file path cannot be null.");
        // Attempt loading existing data first
        loadData();

        // If the map is empty after loading, populate with initial tables
        if (this.tables.isEmpty()) {
            if (initialTables != null && !initialTables.isEmpty()) {
                LOGGER.log(Level.INFO,
                "Table data file empty or not found. Populating repository " +
                "with {0} initial tables.", initialTables.size());
                int addedCount = 0;
                for (Table table : initialTables) {
                    if (table != null && table.getTableNumber() > 0) {
                        this.tables.put(table.getTableNumber(), table);
                        addedCount++;
                    } else {
                        LOGGER.log(Level.WARNING,
                        "Skipping invalid initial table configuration: {0}",
                        table);
                    }
                }
                if (addedCount > 0) {
                    // Save the initial state
                    saveData();
                }
            } else {
                // Log warning if file was empty and no initial tables
                // were provided
                LOGGER.log(Level.WARNING,
                "Table repository initialised empty. No initial tables " +
                "provided or loaded.");
            }
        }
        LOGGER.log(Level.INFO,
        "TableRepository initialised. Contains {0} tables.", tables.size());
    }

    /**
     * Saves a table configuration
     * @param table The Table object to save or update
     * @return The saved or updated Table object.
     * @throws NullPointerException if table is null.
     * @throws IllegalArgumentException if table number is not positive.
     */
    @Override
    public synchronized Table save(Table table) {
        Objects.requireNonNull(table, "Table to save cannot be null.");
        int tableNumber = table.getTableNumber();
        if (tableNumber <= 0) {
            throw new IllegalArgumentException(
                "Table must have a valid positive table number to be saved. " +
                "Provided: " + tableNumber);
        }


        Table previousValue = tables.put(tableNumber, table);
        // Persist the tate
        saveData();

        if (previousValue == null) {
             LOGGER.log(Level.INFO,
             "Added new table: Number={0}, Cap={1}, Status={2}",
             new Object[]{tableNumber, table.getCapacity()});
        } else {
             LOGGER.log(Level.INFO,
             "Updated table: Number={0}, Cap={1}, Status={2}",
             new Object[]{tableNumber, table.getCapacity()});
        }

        // Return the saved table object
        return table;
    }

    /**
     * Retrieves a specific table by its unique table number.
     *
     * @param tableNumber The unique number identifying the table
     * @return An Optional containing the Table if found, otherwise empty.
     */
    @Override
    public Optional<Table> findByTableNumber(int tableNumber) {
        if (tableNumber <= 0) {
            LOGGER.log(Level.FINER,
            "findByTableNumber called with non-positive number: {0}",
            tableNumber);
            return Optional.empty();
        }
        return Optional.ofNullable(tables.get(tableNumber));
    }

    /**
     * Retrieves all tables defined for the restaurant
     *
     * @return An unmodifiable List containing all Table objects
     */
    @Override
    public List<Table> findAll() {
        // Create a new list from values for sorting
        List<Table> sortedTables = new ArrayList<>(tables.values());
        sortedTables.sort(Comparator.comparingInt(Table::getTableNumber));
        return Collections.unmodifiableList(sortedTables);
    }


    /**
     * Retrieves all tables with capacity greater than or equal to the
     * specified requirement
     *
     * @param requiredCapacity The minimum number of seats required
     * @return An unmodifiable List containing Table objects meeting
     * the capacity requirement.
     */
    @Override
    public List<Table> findWithCapacityGreaterThanOrEqual(
        int requiredCapacity) {
        if (requiredCapacity <= 0) {
            LOGGER.log(Level.FINER,
            "findWithCapacityGreaterThanOrEqual called with non-positive " +
            "capacity: {0}", requiredCapacity);
            // Return empty list for non-positive capacity
            return Collections.emptyList();
        }
        return tables.values().stream()
                .filter(table -> table.getCapacity() >= requiredCapacity)
                .sorted(Comparator.comparingInt(Table::getTableNumber))
                // Collect to an unmodifiable list
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }


    /**
     * Loads table data from the specified storage file
     */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        File file = new File(storageFilePath);
        if (!file.exists() || file.length() == 0) {
            LOGGER.log(Level.INFO, "Table data file not found or empty " +
            "({0}). Will use initial tables if provided upon initialization.",
            storageFilePath);
            // Ensure directory exists for future saves
            ensureDirectoryExists(file);
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object readObject = ois.readObject();
            if (readObject instanceof Map) {
                tables.clear();
                Map<?, ?> rawMap = (Map<?, ?>) readObject;
                rawMap.forEach((key, value) -> {
                    if (key instanceof Integer && value instanceof Table) {
                        tables.put((Integer) key, (Table) value);
                    } else {
                        LOGGER.log(Level.WARNING,
                        "Skipping invalid entry during load: Key type {0}, " +
                        "Value type {1}",
                        new Object[]{key != null ? key.getClass().getName() :
                            "null",
                            value != null ? value.getClass().getName() :
                            "null"});
                    }
                });
                LOGGER.log(Level.INFO,
                "Successfully loaded {0} table entries from: {1}",
                new Object[]{tables.size(), storageFilePath});
            } else {
                LOGGER.log(Level.SEVERE,
                "Table data file ({0}) does not contain a valid Map.",
                storageFilePath);
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Table data file not found for loading: "
            + storageFilePath, e);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOGGER.log(Level.SEVERE, "Failed to load table data from file ("
            + storageFilePath + "). Data might be corrupted or class " +
            "versions incompatible.", e);
        }
    }

    /**
     * Saves the current state of the tables map to the storage file
     */
    private synchronized void saveData() {
        File file = new File(storageFilePath);
        ensureDirectoryExists(file);

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(new ConcurrentHashMap<>(tables));
            LOGGER.log(Level.FINE, "Table data saved successfully to {0}",
            storageFilePath);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
            "CRITICAL: Failed to save table data to file (" +
            storageFilePath + "). Data loss may occur.", e);
        }
    }

    /**
     * Ensures the parent directory for the storage file exists and
     * creates it if necessary.
     * @param file The storage file.
     */
    private void ensureDirectoryExists(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            LOGGER.log(Level.INFO,
            "Attempting to create directory for table data: {0}",
            parentDir.getAbsolutePath());
            if (parentDir.mkdirs()) {
                 LOGGER.log(Level.INFO,
                 "Successfully created directory: {0}",
                 parentDir.getAbsolutePath());
            } else {
                 LOGGER.log(Level.SEVERE,
                 "Failed to create directory for table data: {0}",
                 parentDir.getAbsolutePath());
            }
        }
    }
}