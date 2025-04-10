// File: src/main/java/com/cafe94/services/MenuService.java
package com.cafe94.services;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cafe94.domain.Item;
import com.cafe94.domain.User;
import static com.cafe94.enums.Permission.MANAGE_MENU_ITEMS;
import static com.cafe94.enums.Permission.SET_DAILY_SPECIAL;
import com.cafe94.persistence.IMenuRepository;

/**
 * Implementation of IMenuService for managing menu items
 *@author Adigun LAteef
 @version 1.o
 */
public class MenuService implements IMenuService {

    private final IMenuRepository menuRepository;
    private final AuthorizationService authService;

    /**
     * Constructor for Dependency Injection.
     * @param menuRepository Repository for accessing item data
     * @param authService    Service for checking user permissions
     */
    public MenuService(IMenuRepository menuRepository,
    AuthorizationService authService) {
        this.menuRepository = Objects.requireNonNull(menuRepository,
        "MenuRepository cannot be null.");
        this.authService = Objects.requireNonNull(authService,
        "AuthorizationService cannot be null.");
    }


    @Override
    public Item addItem(String name, String category, String description,
    double price, List<String> allergens, User staffMember) {
        // Authorization Check
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);

        // Create new Item object
        Item newItem = new Item(0, name, category, price,
        false);
        // Persist via Repository
        Item savedItem = menuRepository.save(newItem);
        System.out.println("INFO: Staff " + staffMember.getUserID() +
        " added new item: " + savedItem);
        // Check if save was successful
        if (savedItem == null || savedItem.getItemID() <= 0) {
            System.err.println(
                "ERROR: Failed to save new item or retrieve valid ID.");
            // Throw exception or handle error
            throw new RuntimeException("Failed to save new menu item: "
            + name);
        }

        return savedItem;
    }

    @Override
    public Item updateItem(int itemId, String name, String category,
    String description, double price, List<String> allergens,
    boolean isSpecial, User staffMember) {
        // Authorization Check
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);

        // Find existing item or throw error
        Item itemToUpdate = findItemByIdOrThrow(itemId);

        // Update item properties using setters
        itemToUpdate.setName(name);
        itemToUpdate.setCategory(category);
        itemToUpdate.setPrice(price);
        itemToUpdate.setDailySpecial(isSpecial);

        // Persist changes
        Item updatedItem = menuRepository.save(itemToUpdate);
        System.out.println("INFO: Staff " + staffMember.getUserID()
        + " updated item: " + updatedItem);

        return updatedItem;
    }

    @Override
    public boolean removeItem(int itemId, User staffMember) {
        // Authorization Check
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);

        // Attempt deletion via repository
        boolean deleted = menuRepository.deleteById(itemId);

        if (deleted) {
            System.out.println("INFO: Staff " + staffMember.getUserID() +
            " removed item with ID: " + itemId);
        } else {
             System.err.println("WARN: Staff " + staffMember.getUserID() +
             " - Failed to remove item ID: " + itemId +
             " (Not found or DB error?).");
        }
        return deleted;
    }

    @Override
    public Item setDailySpecial(int itemId, User staffMember) {
        // Authorization Check
        authService.checkPermission(staffMember, SET_DAILY_SPECIAL);

        // Find item
        Item item = findItemByIdOrThrow(itemId);

        // Set special status and save (if not already set)
        if (!item.isDailySpecial()) {
            item.setDailySpecial(true);
            Item updatedItem = menuRepository.save(item);
            System.out.println("INFO: Staff " + staffMember.getUserID() +
            " set item as daily special: " + updatedItem);
            
            return updatedItem;
        } else {
            System.out.println("INFO: Item ID " + itemId +
            " was already marked as special.");
            return item;
        }
    }

    @Override
    public Item clearDailySpecial(int itemId, User staffMember) {
        // Authorization Check
         authService.checkPermission(staffMember, SET_DAILY_SPECIAL);

        // Find item
        Item item = findItemByIdOrThrow(itemId);

        // Clear status if set and save
        if (item.isDailySpecial()) {
            item.setDailySpecial(false);
            Item updatedItem = menuRepository.save(item);
            System.out.println("INFO: Staff " + staffMember.getUserID() +
            " cleared daily special status for item: " + updatedItem);
            return updatedItem;
        } else {
            System.out.println("INFO: Item ID " + itemId +
            " was already not marked as special.");
            return item;
        }
    }


    @Override
    public Optional<Item> getItemById(int itemId) {
        if (itemId <= 0) {
             System.err.println("WARN: getItemById called with invalid ID: "
             + itemId);
             return Optional.empty();
        }
        return menuRepository.findById(itemId);
    }

    @Override
    public List<Item> getAllItems() {
        return menuRepository.findAll();
    }

    @Override
    public List<Item> getItemsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            System.out.println(
                "WARN: getItemsByCategory called with null or blank " +
                "category.");
            return Arrays.asList();
        }
        return menuRepository.findByCategory(category);
    }

    @Override
    public List<String> getAllCategories() {
        List<String> categories = menuRepository.findDistinctCategories();
        if (categories != null && !categories.isEmpty()) {
            return categories;
        } else {
            // Fallback if repo method doesn't exist or fails
            System.out.println(
                "WARN: Falling back to retrieving all items to " +
                "determine categories.");
            List<Item> allItems = menuRepository.findAll();
            if (allItems == null || allItems.isEmpty()) {
                return Arrays.asList();
            }
            return allItems.stream()
                    .map(Item::getCategory)
                    .filter(Objects::nonNull)
                    .filter(cat -> !cat.trim().isEmpty())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Item> getDailySpecials() {
        return menuRepository.findDailySpecials();
    }

    /** Finds an item by ID or throws NoSuchElementException if not found. */
    private Item findItemByIdOrThrow(int itemId) {
        if (itemId <= 0) throw new IllegalArgumentException(
            "Item ID must be positive.");
        // Calls repo findById
        return menuRepository.findById(itemId)
               .orElseThrow(() -> new NoSuchElementException(
                "Item not found: " + itemId));
    }
}