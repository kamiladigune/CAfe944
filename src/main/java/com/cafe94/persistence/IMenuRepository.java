// File: src/main/java/com/cafe94/persistence/IMenuRepository.java
package com.cafe94.persistence;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Item;

/**
 * Interface defining persistence operations for menu {@link Item} entities
 * @author Adigun LAteef
 * @version 1.0
 */
public interface IMenuRepository {

    /**
     * Saves a new menu item or updates an existing one based on its ID.
     *
     * @param item The Item object to save or update
     * @return The saved or updated Item
     * @throws NullPointerException if the item parameter is null.
     */
    Item save(Item item);

    /**
     * Deletes a menu item by its unique persistent ID.
     *
     * @param itemId The unique ID of the item to delete
     * @return {@code true} if an item with the given ID was found and
     * successfully deleted, {@code false} otherwise
     */
    boolean deleteById(int itemId);

    /**
     * Finds a menu item by its unique persistent ID.
     *
     * @param itemId The unique ID of the item to find
     * @return An {@code Optional<Item>} containing the found Item if an item
     * with the specified ID exists, otherwise an empty Optional and returns
     * empty Optional if itemId is not positive.
     */
    Optional<Item> findById(int itemId);

    /**
     * Retrieves all menu items currently stored in the repository
     * @return A {@code List<Item>} containing all menu items and returns an
     * empty list if the menu is empty.
     */
    List<Item> findAll();

    /**
     * Finds all menu items belonging to a specific category
     *
     * @param category The category name to search for
     * @return A {@code List<Item>} containing all items belonging to the
     * specified category, potentially sorted and returns an empty list if the
     * category is null/blank or no items match.
     */
    List<Item> findByCategory(String category);

    /**
     * Finds all menu items currently marked as daily specials
     *
     * @return A {@code List<Item>} containing all items currently marked as
     * daily specials and returns an empty list if no items are marked as
     * special.
     */
    List<Item> findDailySpecials();

    /**
     * Finds all distinct category names present across all menu items
     *
     * @return A {@code List<String>} of unique category names and returns an
     * empty list if no items or categories exist in the menu.
     */
    List<String> findDistinctCategories();
}