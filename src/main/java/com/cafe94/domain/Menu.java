package com.cafe94.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents the restaurant's menu, holding a collection of
 * {@link Item} objects.
 */
public class Menu implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
    Logger.getLogger(Menu.class.getName());
    private final Map<Integer, Item> items;

    /**
     * Default constructor initializes an empty, thread-safe menu.
     */
    public Menu() {
        this.items = new ConcurrentHashMap<>();
    }

    /**
     * Constructor to initialize the menu with a list of items.
     * @param initialItems The initial list of items
     */
    public Menu(List<Item> initialItems) {
        // Calls default constructor first to initialize the map
        this();
        if (initialItems != null) {
            LOGGER.log(Level.CONFIG, "Initializing Menu with {0} potential " +
            "items.", initialItems.size());
            int addedCount = 0;
            for (Item item : initialItems) {
                // Ensure item and its ID are valid before adding
                if (item != null && item.getItemID() > 0) {
                    this.items.put(item.getItemID(), item);
                    addedCount++;
                } else {
                    LOGGER.log(Level.WARNING, "Skipping invalid item during " +
                    "Menu initialization: {0}", item);
                }
            }
            LOGGER.log(Level.CONFIG, "Successfully added {0} valid items " +
            "during Menu initialization.", addedCount);
        } else {
            LOGGER.log(Level.CONFIG, "Initializing Menu with no initial " +
            "items provided.");
        }
    }

    /**
     * Adds a new item or updates an existing item in the menu map, keyed by
     * item ID.
     * @param item The item to add or update
     */
    public void addOrUpdateItem(Item item) {
        if (item != null && item.getItemID() > 0) {
            Item previous = this.items.put(item.getItemID(), item);
            if (previous == null) {
                LOGGER.log(Level.FINE, "Added new item to menu: ID={0}, " +
                "Name='{1}'", new Object[]{item.getItemID(), item.getName()});
            } else {
                LOGGER.log(Level.FINE, "Updated existing item in menu: " +
                "ID={0}, Name='{1}'", new Object[]{item.getItemID(),
                    item.getName()});
            }
        } else {
            // Log a warning for invalid items
            LOGGER.log(Level.WARNING, "Attempted to add invalid item " +
            "(null or ID <= 0) to menu: {0}", item);
            throw new IllegalArgumentException("Cannot add invalid item " +
            "(null or ID <= 0) to menu.");
        }
    }

    /**
     * Removes an item from the menu map by its ID.
     * @param itemId The ID of the item to remove (must be positive).
     * @return The removed item if found, or null otherwise.
     */
    public Item removeItem(int itemId) {
        if (itemId <= 0) {
            LOGGER.log(Level.WARNING, "Attempted to remove item with " +
            "invalid ID: {0}", itemId);
            return null;
        }
        Item removedItem = this.items.remove(itemId);
        if (removedItem != null) {
             LOGGER.log(Level.FINE, "Removed item from menu: ID={0}, Name='{1}'", new Object[]{itemId, removedItem.getName()});
        } else {
             LOGGER.log(Level.FINE, "Attempted to remove item with ID {0}, but it was not found in the menu.", itemId);
        }
        return removedItem;
    }

    /**
     * Gets a specific item from the menu by its ID.
     * @param itemId The ID of the item to retrieve (must be positive).
     * @return The Item object if found, or null if not found or the ID is invalid.
     */
    public Item getItemById(int itemId) {
         if (itemId <= 0) {
            LOGGER.log(Level.FINER, "Attempted to get item with invalid ID: {0}", itemId);
            return null; // Invalid ID
        }
        return this.items.get(itemId);
    }

    /**
     * Gets an unmodifiable list view of all items currently on the menu.
     * @return An unmodifiable list of all items.
     * Returns an empty list if the menu is empty.
     */
    public List<Item> getAllItems() {
        if (this.items.isEmpty()) {
            return Collections.emptyList();
        }
        // Create a list from the map's values for sorting
        List<Item> sortedItems = new ArrayList<>(this.items.values());
        // Sort by category, then by name
        sortedItems.sort(Comparator.comparing(Item::getCategory,
        String.CASE_INSENSITIVE_ORDER)
                                  .thenComparing(Item::getName,
                                  String.CASE_INSENSITIVE_ORDER));
        // Return an unmodifiable view of the sorted list
        return Collections.unmodifiableList(sortedItems);
    }

    /**
     * Gets an unmodifiable list view of all items currently marked as
     * daily specials.
     * @return An unmodifiable list of daily special items. Returns an
     * empty list if none exist.
     */
    public List<Item> getDailySpecials() {
        return this.items.values().stream()
                .filter(Objects::nonNull)
                .filter(Item::isDailySpecial)
                .sorted(Comparator.comparing(Item::getName,
                String.CASE_INSENSITIVE_ORDER))
                // Collect into a new list and then make it unmodifiable
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                         Collections::unmodifiableList));
    }

    /**
     * Gets an unmodifiable list of all unique category names present
     * in the menu.
     * @return An unmodifiable, sorted list of unique category names.
     * Returns an empty list if no items exist.
     */
    public List<String> getAllCategories() {
        return this.items.values().stream()
                .filter(Objects::nonNull)
                .map(Item::getCategory)
                .filter(Objects::nonNull)
                .filter(category -> !category.trim().isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                // Collect into a new list and then make it unmodifiable
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                         Collections::unmodifiableList));
    }

     /**
     * Gets an unmodifiable list view of all items belonging to a
     * specific category.
     * @param category The category name to filter
     * @return An unmodifiable list of items in the specified category.
     * Returns an empty list if the category is null, blank, or not found.
     */
    public List<Item> getItemsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            LOGGER.log(Level.FINER, "getItemsByCategory called with null or " +
            "blank category.");
            // Return immutable empty list for invalid input
            return Collections.emptyList();
        }
        return this.items.values().stream()
                .filter(Objects::nonNull)
                .filter(item -> category.equalsIgnoreCase(item.getCategory()))
                // Sort by name
                .sorted(Comparator.comparing(Item::getName,
                String.CASE_INSENSITIVE_ORDER))
                 // Collect into a new list and then make it unmodifiable
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                         Collections::unmodifiableList));
    }
}
