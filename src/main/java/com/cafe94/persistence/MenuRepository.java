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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.cafe94.domain.Item;

/**
 * Concrete implementation of {@link IMenuRepository} using Java Serialization
 * for persistence
 */
public class MenuRepository implements IMenuRepository {

    private static final Logger LOGGER =
    Logger.getLogger(MenuRepository.class.getName());
    private final Map<Integer, Item> items = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final String storageFilePath;

    /**
     * Constructs the repository, loading existing data from the specified
     * file path
     *
     * @param storageFilePath The path to the file for storing menu item data
     * @throws NullPointerException if storageFilePath is null.
     */
    public MenuRepository(String storageFilePath) {
        this.storageFilePath = Objects.requireNonNull(storageFilePath,
        "Storage file path cannot be null.");
        // Load data on initialisation
        loadData();
        // Initialize nextId based on the maximum ID found in loaded data
        int maxId =
        items.keySet().stream().max(Integer::compare).orElse(0);
        nextId.set(maxId + 1);
        LOGGER.log(Level.INFO,
        "MenuRepository initialized. Loaded {0} items from {1}. Next ID: {2}",
        new Object[]{items.size(), this.storageFilePath, nextId.get()});
    }

    /**
     * Saves a new item or updates an existing one
     *
     * @param item The Item to save or update
     * @return The saved Item object (the same instance passed in
     * @throws NullPointerException if item is null.
     * @throws RuntimeException if assigning new ID fails
     */
    @Override
    public synchronized Item save(Item item) {
        Objects.requireNonNull(item,
        "Item to save cannot be null.");
        int itemId = item.getItemID();

        if (itemId <= 0) {
            itemId = nextId.getAndIncrement();
            try {
                
                item.setItemID(itemId);
                LOGGER.log(Level.FINE,
                "Assigned new ID {0} to item: {1}",
                new Object[]{itemId, item.getName()});
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                "Failed to set new ID on item object. ID generated: " +
                itemId, e);
                throw new RuntimeException(
                    "Failed to assign ID to new item", e);
            }
        } else {
            // Ensure nextId is correctly positioned if saving an existing item
            nextId.accumulateAndGet(itemId + 1, Math::max);
            LOGGER.log(Level.FINEST, "Saving existing item with ID {0}",
            itemId);
        }

        items.put(itemId, item);
        // Persist the changes
        saveData();
        LOGGER.log(Level.INFO,
        "Saved item: ID={0}, Name='{1}', Category='{2}'",
        new Object[]{itemId, item.getName(), item.getCategory()});

         // Return the  item object
        return item;
    }

    /**
     * Deletes an item by its unique persistent ID
     *
     * @param itemId The ID of the item to delete
     * @return true if an item was found and deleted, false otherwise.
     */
    @Override
    public synchronized boolean deleteById(int itemId) {
        if (itemId <= 0) {
             LOGGER.log(Level.WARNING,
             "Attempted to delete item with invalid ID: {0}", itemId);
            return false;
        }
        Item removedItem = items.remove(itemId);
        if (removedItem != null) {
            saveData();
            LOGGER.log(Level.INFO, "Deleted item ID: {0}", itemId);
            return true;
        } else {
            LOGGER.log(Level.WARNING, "Item ID {0} not found for deletion.",
            itemId);
            return false;
        }
    }

    /**
     * Finds an item by its unique persistent ID.
     *
     * @param itemId The ID of the item
     * @return An Optional containing the Item if found, otherwise empty.
     */
    @Override
    public Optional<Item> findById(int itemId) {
        if (itemId <= 0) {
             LOGGER.log(Level.FINER,
             "findById called with non-positive ID: {0}", itemId);
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(itemId));
    }

    /**
     * Retrieves all items, sorted by category then name
     * @return An unmodifiable List of all Items.
     */
    @Override
    public List<Item> findAll() {
        List<Item> sortedItems = new ArrayList<>(items.values());
        sortedItems.sort(Comparator.comparing(Item::getCategory,
        String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(sortedItems);
    }

    /**
     * Finds items by category
     * @param category The category name (must not be null or blank).
     * @return An unmodifiable List of Items in the category.
     */
    @Override
    public List<Item> findByCategory(String category) {
        Objects.requireNonNull(category,
        "Category cannot be null for findByCategory.");
        if (category.trim().isEmpty()){
             LOGGER.log(Level.FINER,
             "findByCategory called with blank category.");
             return Collections.emptyList();
        }
        String lowerCaseCategory = category.toLowerCase();
        return items.values().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getCategory() != null
                && item.getCategory().toLowerCase().equals(lowerCaseCategory))
                .sorted(Comparator.comparing(Item::getName,
                String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all daily special items, sorted by name
     * @return An unmodifiable List of daily special Items.
     */
    @Override
    public List<Item> findDailySpecials() {
        return items.values().stream()
                .filter(Objects::nonNull)
                .filter(Item::isDailySpecial)
                .sorted(Comparator.comparing(Item::getName,
                String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList));
    }

    /**
     * Finds all distinct category names
     * @return An unmodifiable List of unique category names.
     */
    @Override
    public List<String> findDistinctCategories() {
        return items.values().stream()
               .filter(Objects::nonNull)
               .map(Item::getCategory)
               .filter(Objects::nonNull)
               .filter(cat -> !cat.trim().isEmpty())
               .distinct()
               .sorted(String.CASE_INSENSITIVE_ORDER)
               .collect(Collectors.collectingAndThen(Collectors.toList(),
               Collections::unmodifiableList));
    }


    /** Loads item data from the storage file. Synchronized.
     * 
    */
    @SuppressWarnings("unchecked")
    private synchronized void loadData() {
        File file = new File(storageFilePath);
        if (!file.exists() || file.length() == 0) {
             LOGGER.log(Level.INFO,
             "Item data file not found or empty ({0}). Starting empty.",
             storageFilePath);
             ensureDirectoryExists(file);
             return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ObjectInputStream ois = new ObjectInputStream(bis)) {

            Object readObject = ois.readObject();
             if (readObject instanceof Map) {
                items.clear();
                Map<?,?> rawMap = (Map<?,?>) readObject;
                rawMap.forEach((key, value) -> {
                    if (key instanceof Integer && value instanceof Item) {
                         items.put((Integer)key, (Item)value);
                    } else {
                         LOGGER.log(Level.WARNING,
                         "Skipping invalid entry during load: Key type {0}, " +
                         "Value type {1}",
                         new Object[]{key != null ?
                            key.getClass().getName() : "null",
                            value != null ? value.getClass().getName() :
                            "null"});
                    }
                });
                LOGGER.log(Level.INFO,
                "Loaded {0} item entries from: {1}",
                new Object[]{items.size(), storageFilePath});
            } else {
                LOGGER.log(Level.SEVERE,
                "Item data file ({0}) does not contain a valid Map.",
                storageFilePath);
            }
        } catch (FileNotFoundException e) {
             LOGGER.log(Level.SEVERE, "Item data file not found for loading: "
             + storageFilePath, e);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
             LOGGER.log(Level.SEVERE, "Failed to load item data from file ("
             + storageFilePath + "). Data might be corrupted or class " +
             "versions incompatible.", e);
        }
    }

    /** Saves the current item map to the storage file. Synchronized.
    */
    private synchronized void saveData() {
        File file = new File(storageFilePath);
        ensureDirectoryExists(file);

        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(new ConcurrentHashMap<>(items));
            LOGGER.log(Level.FINE, "Item data saved successfully to {0}",
            storageFilePath);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
            "CRITICAL: Failed to save item data to file ({0}). Data loss may "
            + " occur.", new Object[]{storageFilePath, e});
        }
    }

    /** Ensures the parent directory for the storage file exists.
     * 
    */
     private void ensureDirectoryExists(File file) {
        Objects.requireNonNull(file, "File cannot be null");
        File parentDir = file.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
             LOGGER.log(Level.INFO,
             "Attempting to create directory for item data: {0}",
             parentDir.getAbsolutePath());
             if (parentDir.mkdirs()) {
                 LOGGER.log(Level.INFO,
                 "Successfully created directory: {0}",
                 parentDir.getAbsolutePath());
             } else {
                 LOGGER.log(Level.SEVERE,
                 "Failed to create directory for item data: {0}",
                 parentDir.getAbsolutePath());
             }
         }
     }
}
