package com.cafe94.services;

import java.util.List;
import java.util.Optional;

import com.cafe94.domain.Item;
import com.cafe94.domain.User;

/**
 * Interface defining business logic operations related to managing the
 * restaurant menu.
 * @author Adigun Lateef
 * @version 1.1
 */
public interface IMenuService {

    /**
     * Adds a new permanent item to the menu
     * @param category Item category.
     * @param description Item description.
     * @param price Item price.
     * @param allergens List of allergens
     * @param staffMember Staff performing action.
     * @return The saved Item.
     */
    Item addItem(String name, String category,
                 double price, User staffMember);

    /**
     * Updates an existing menu item
     * @param itemId ID of item to update.
     * @param name Updated name.
     * @param category Updated category
     * @param price Updated price
     * @param isSpecial Updated special status.
     * @param staffMember Staff performing action.
     * @return The updated Item.
     */
    Item updateItem(int itemId, String name, String category, double price,
    boolean isSpecial, User staffMember);

    /**
     * Removes an item permanently from the menu
     * @param itemId ID of item to remove.
     * @param staffMember Staff performing action.
     * @return true if successful, false otherwise.
     */
    boolean removeItem(int itemId, User staffMember);

    /**
     * Sets an existing menu item as a daily special.
     * @param itemId ID of item to mark as special.
     * @param staffMember Staff performing action.
     * @return The updated Item.
     */
    Item setDailySpecial(int itemId, User staffMember);

    /**
     * Clears the daily special status for an existing menu item
     * @param itemId ID of item to clear special status from.
     * @param staffMember Staff performing action.
     * @return The updated Item.
     */
    Item clearDailySpecial(int itemId, User staffMember);

    /**
     * Handles setting a new creation
     * as the daily special
     * @param name Name of the new special.
     * @param description Description of the new special.
     * @param price Price of the new special.
     * @param staffMember Staff performing the action (Chef).
     * @return A transient Item object representing the new special.
     */
    Item setNewCreationAsSpecial(String name, String description,
                                 double price, User staffMember);


    // Retrieval Methods
    Optional<Item> getItemById(int itemId);
    List<Item> getAllItems();
    List<Item> getItemsByCategory(String category);
    List<String> getAllCategories();
    List<Item> getDailySpecials();

}