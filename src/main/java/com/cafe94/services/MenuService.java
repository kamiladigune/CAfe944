package com.cafe94.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.cafe94.domain.Item;
import com.cafe94.domain.User;
import static com.cafe94.enums.Permission.MANAGE_MENU_ITEMS;
import static com.cafe94.enums.Permission.SET_DAILY_SPECIAL;
import com.cafe94.persistence.IMenuRepository;
import com.cafe94.util.ValidationUtils;

/**
 * Implementation of IMenuService for managing menu items.
 * @author Adigun Lateef
 * @version 1.1
 */
public class MenuService implements IMenuService {

    private static final Logger LOGGER =
        Logger.getLogger(MenuService.class.getName());

    private final IMenuRepository menuRepository;
    private final AuthorizationService authService;

    private Item transientDailySpecial = null;

    /** Constructor for Dependency Injection. */
    public MenuService(IMenuRepository menuRepository,
                       AuthorizationService authService) {
        this.menuRepository = Objects.requireNonNull(menuRepository);
        this.authService = Objects.requireNonNull(authService);
    }

    @Override
    public Item addItem(String name, String category, double price,
    User staffMember) {
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);
        Item newItem = new Item(0, name, category, price,
        false);
        Item savedItem = menuRepository.save(newItem);
        LOGGER.log(Level.INFO, "Staff {0} added new item: {1}",
                   new Object[]{staffMember.getUserID(), savedItem});
        if (savedItem == null || savedItem.getItemID() <= 0) {
            LOGGER.log(Level.SEVERE,
                       "Failed to save new item or retrieve valid ID.");
            throw new RuntimeException("Failed save new menu item: " + name);
        }
        return savedItem;
    }

    @Override
    public Item updateItem(int itemId, String name, String category,
    double price, boolean isSpecial, User staffMember) {
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);
        Item itemToUpdate = findItemByIdOrThrow(itemId);
        // Update properties
        itemToUpdate.setName(name);
        itemToUpdate.setCategory(category);
        itemToUpdate.setPrice(price);
        itemToUpdate.setDailySpecial(isSpecial);
        Item updatedItem = menuRepository.save(itemToUpdate);
        LOGGER.log(Level.INFO, "Staff {0} updated item: {1}",
                   new Object[]{staffMember.getUserID(), updatedItem});
        return updatedItem;
    }

    @Override
    public boolean removeItem(int itemId, User staffMember) {
        authService.checkPermission(staffMember, MANAGE_MENU_ITEMS);
        boolean deleted = menuRepository.deleteById(itemId);
        if (deleted) {
            LOGGER.log(Level.INFO, "Staff {0} removed item ID: {1}",
                       new Object[]{staffMember.getUserID(), itemId});
            if (transientDailySpecial != null &&
                transientDailySpecial.getItemID() == itemId) {
                transientDailySpecial = null;
            }
        } else {
             LOGGER.log(Level.WARNING, "Staff {0} failed remove item {1}",
                        new Object[]{staffMember.getUserID(), itemId});
        }
        return deleted;
    }

    private void clearAllExistingSpecials(User staffMember) {
         LOGGER.log(Level.FINE, "Clearing existing daily specials.");
         // Clear persisted specials
         List<Item> currentSpecials = menuRepository.findDailySpecials();
         for (Item item : currentSpecials) {
             if (item.isDailySpecial()) {
                 item.setDailySpecial(false);
                 // Persist the change
                 menuRepository.save(item);
                 LOGGER.log(Level.FINER, "Cleared special status for " +
                     "persisted item ID: {0}", item.getItemID());
             }
         }l;
    }

    @Override
    public Item setDailySpecial(int itemId, User staffMember) {
        authService.checkPermission(staffMember, SET_DAILY_SPECIAL);
        Item item = findItemByIdOrThrow(itemId);
        clearAllExistingSpecials(staffMember);
        item.setDailySpecial(true);
        Item updatedItem = menuRepository.save(item);
        LOGGER.log(Level.INFO, "Staff {0} set existing item as special: {1}",
                   new Object[]{staffMember.getUserID(), updatedItem});
        return updatedItem;
    }

    @Override
    public Item clearDailySpecial(int itemId, User staffMember) {
        authService.checkPermission(staffMember, SET_DAILY_SPECIAL);
        Item item = findItemByIdOrThrow(itemId);
        if (item.isDailySpecial()) {
            item.setDailySpecial(false);
            Item updatedItem = menuRepository.save(item);
             LOGGER.log(Level.INFO, "Staff {0} cleared special status " +
                        "for item: {1}",
                        new Object[]{staffMember.getUserID(), updatedItem});
            return updatedItem;
        }
         if (transientDailySpecial != null &&
             transientDailySpecial.getItemID() == itemId) {
             transientDailySpecial = null;
             LOGGER.log(Level.INFO, "Cleared transient daily special.");
             return item;
         }

        LOGGER.log(Level.INFO, "Item ID {0} was not marked as special.",
                   itemId);
        return item;
    }

    // New Placeholder Method Implementation
    @Override
    public Item setNewCreationAsSpecial(String name, String description,
                                        double price, User staffMember) {
        authService.checkPermission(staffMember, SET_DAILY_SPECIAL);
        ValidationUtils.requireNonBlank(name, "Special name");
        ValidationUtils.requireNonNegative(price, "Special price");

        // Clear any existing specials first
        clearAllExistingSpecials(staffMember);

        Item newSpecial = new Item(0, name, "Daily Special",
        price, true);

        transientDailySpecial = newSpecial;

        LOGGER.log(Level.INFO, "Staff {0} set NEW CREATION as " +
                   "transient special: {1}",
                   new Object[]{staffMember.getUserID(), newSpecial});
        LOGGER.log(Level.WARNING, "Newly created special '{0}' is " +
                   "TRANSIENT - it is NOT saved permanently in the menu " +
                   "and may not appear correctly in all views.", name);

        return transientDailySpecial;
    }


    // Retrieval Methods
    @Override
    public Optional<Item> getItemById(int itemId) {
         if (itemId <= 0) {
             if (transientDailySpecial != null &&
                 transientDailySpecial.getItemID() == itemId) {
                 return Optional.of(transientDailySpecial);
             }
             LOGGER.log(Level.FINER,
                 "getItemById called with invalid/temp ID: {0}", itemId);
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
             return Collections.emptyList();
        }
        return menuRepository.findByCategory(category);
    }

    @Override
    public List<String> getAllCategories() {
        return menuRepository.findDistinctCategories();
    }

    @Override
    public List<Item> getDailySpecials() {
        List<Item> specials = new ArrayList<>(
            menuRepository.findDailySpecials()
        );
        if (transientDailySpecial != null) {
             if (specials.stream().noneMatch(
                 item -> item.getItemID() == transientDailySpecial.getItemID()
                )) {
                 specials.add(transientDailySpecial);
             }
        }
        // Return combined list
        return specials;
    }

    /** Finds item by ID or throws NoSuchElementException. */
    private Item findItemByIdOrThrow(int itemId) {
        return getItemById(itemId)
               .orElseThrow(() -> new NoSuchElementException(
                   "Item not found with ID: " + itemId));
    }

}