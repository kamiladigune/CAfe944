// File: src/main/java/com/cafe94/services/IMenuService.java
package com.cafe94.services;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Item;
import com.cafe94.domain.User;

/**
 * Interface defining business logic operations related to managing the
 * restaurant menu
 * @author Adigun Lateef
 */
public interface IMenuService {

   
    /**
     * Adds a new item to the menu
     *
     * @param name        The name of the new item
     * @param category    The category of the new item
     * @param description A description of the item
     * @param price       The price of the item
     * @param staffMember The staff member performing the action
     * @return The newly created and persisted {@link Item}.
     * @throws SecurityException if staffMember lacks the required permission.
     * @throws IllegalArgumentException if input parameters are invalid
     */
    Item addItem(String name, String category, String description,
    double price, List<String> allergens, User staffMember);

    /**
     * Updates an existing menu item identified by its ID
     *
     * @param itemId      The unique ID of the item to update
     * @param name        The updated name
     * @param category    The updated category
     * @param description The updated description
     * @param price       The updated price
     * @param isSpecial   The updated daily special status (true if special,
     * false otherwise).
     * @param staffMember The staff member performing the action
     * @return The updated and persisted {@link Item}
     * @throws SecurityException if staffMember lacks the required permission
     * @throws java.util.NoSuchElementException if no item is found with the
     * given itemId
     * @throws IllegalArgumentException if other input parameters are invalid
     */
    Item updateItem(int itemId, String name, String category,
    String description, double price, List<String> allergens,
    boolean isSpecial, User staffMember);

    /**
     * Removes an item from the menu based on its ID
     *
     * @param itemId      The unique ID of the item to remove
     * @param staffMember The staff member performing the action
     * @return {@code true} if the item was successfully found and removed,
     * {@code false} otherwise
     * @throws SecurityException if staffMember lacks the required permission
     */
    boolean removeItem(int itemId, User staffMember);

    /**
     * Sets a specific menu item as a daily special
     *
     * @param itemId      The unique ID of the item to mark as special
     * @param staffMember The staff member performing the action
     * @return The updated {@link Item} marked as special. If the item was
     * already special, returns the unchanged item.
     * @throws SecurityException if staffMember lacks the required permission.
     * @throws java.util.NoSuchElementException if no item is found with the
     * given itemId
     */
    Item setDailySpecial(int itemId, User staffMember);

    /**
     * Clears the daily special status for a specific menu item
     * @param itemId      The unique ID of the item to mark as special
     * @param staffMember The staff member performing the action
     * @return The updated {@link Item} with the special status cleared
     * @throws SecurityException if staffMember lacks the required permission.
     * @throws java.util.NoSuchElementException if no item is found with the
     * given itemId
     */
    Item clearDailySpecial(int itemId, User staffMember);

    /**
     * Retrieves a specific menu item by its unique ID.
     *
     * @param itemId The unique ID of the item to retrieve
     * @return An {@code Optional<Item>} containing the Item if found,
     * otherwise an empty Optional snd returns empty Optional if
     * itemId is not positive.
     */
    Optional<Item> getItemById(int itemId);

    /**
     * Retrieves a list of all items currently available on the menu
     * @return A {@code List<Item>} containing all menu items and
     * returns an empty list if the menu is empty.
     */
    List<Item> getAllItems();

    /**
     * Retrieves all menu items belonging to a specific category
     *
     * @param category The category name to filter by
     * @return A {@code List<Item>} containing items matching the category
     * Returns an empty list if the category is null/blank or no items match.
     */
    List<Item> getItemsByCategory(String category);

    /**
     * Retrieves a distinct list of all category names currently represented
     * in the menu
     * @return A sorted {@code List<String>} of unique category names abd
     * returns an empty list if the menu is empty or has no categories
     */
    List<String> getAllCategories();

    /**
     * Retrieves a list of all menu items currently marked as daily specials
     * @return A {@code List<Item>} containing items marked as daily specials
     * and returns an empty list if no items are marked as special.
     */
    List<Item> getDailySpecials();

}