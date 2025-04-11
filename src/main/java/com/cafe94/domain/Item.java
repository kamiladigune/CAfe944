package com.cafe94.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.util.ValidationUtils;

/**
 * Represents a single item available on the Cafe94 menu.
 * @author  Adigun Lateef
 * @version 1.0
 */
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Item.class.getName());
    private int itemID;
    private String name;
    private double price;
    private String category;
    private boolean isDailySpecial;


    /**
     * Constructs a new menu Item. Most fields are immutable after creation.
     *
     * @param itemID         The unique ID or existing ID.
     * @param name           The item name
     * @param category       The item category.
     * @param description    The item description
     * @param price          The item price
     * @param isDailySpecial True if this item is a daily special,
     * false otherwise.
     * @throws NullPointerException if name or category is null.
     * @throws IllegalArgumentException if name/category is blank, or price is
     * negative.
     */
    public Item(int itemID, String name, String category, double price,
    boolean isDailySpecial) {

        // Assign ID first
        this.itemID = itemID;

        // Validate and set final fields directly using ValidationUtils
        this.name = ValidationUtils.requireNonBlank(name, "Item name");
        this.category = ValidationUtils.requireNonBlank(category, "Item category");
        this.price = ValidationUtils.requireNonNegative(price, "Item price");
        this.isDailySpecial = isDailySpecial;

         LOGGER.log(Level.FINEST, "Created Item object: ID={0}, Name='{1}'",
         new Object[]{this.itemID, this.name});
    }

    // Getters

    /**
     * @return The item ID
     */
    public int getItemID() {
        return itemID;
    }

    /**
     * @return The item name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The item's price
    */
    public double getPrice() {
        return price;
    }

    /**
     * @return The item category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return true if the item is marked as a daily special, false otherwise.
     */
    public boolean isDailySpecial() {
        return isDailySpecial;
    }

    // Setters

    /**
     * Sets the item's persistent ID.
     * @param itemID The assigned persistent ID (must be positive).
     * @throws IllegalArgumentException if itemID is not positive.
     * @throws IllegalStateException if attempting to change an already
     * assigned positive ID.
     */
    public void setItemID(int itemID) {
        if (itemID <= 0) {
            throw new IllegalArgumentException("Item ID assigned by " +
            "persistence must be positive. Provided: " + itemID);
        }
        if (this.itemID > 0 && this.itemID != itemID) {
             // Prevent changing an already assigned ID
            LOGGER.log(Level.WARNING, "Attempting to change an already " +
            "assigned item ID from {0} to {1}",
            new Object[]{this.itemID, itemID});
            throw new IllegalStateException("Cannot change an already " +
            "assigned persistent item ID.");
        }
        this.itemID = itemID;
    }
    /**
     * Sets item name
     * @param name The item name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets item category
     * @param category The item category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Sets item price
     * @param price The item price value
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Sets special items
     * @param special The special item value
     */
    public void setDailySpecial(boolean special) {
        this.isDailySpecial = special;
    }

    // Standard Methods

    /**
     * String representaion of the objects
     * @return a string reprentation of the item objects
     */
    @Override
    public String toString() {
        return "Item[" +
               "ID=" + itemID +
               ", Name='" + name + '\'' +
               ", Cat='" + category + '\'' +
               ", Price=" + String.format("%.2f", price) +
               ", Special=" + isDailySpecial + ']';
    }

    /**
     * Compares Item objects for equality
     * @param o The object to compare with.
     * @return true if the objects are considered equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        if (itemID > 0 && item.itemID > 0) {
            return itemID == item.itemID;
        }

        // Fallback for transient objects
        return Double.compare(item.price, price) == 0 &&
               Objects.equals(name, item.name) &&
               Objects.equals(category, item.category);
    }

    /**
     * Generates a hash code for the Item object.
     * @return The hash code for this object.
     */
    @Override
    public int hashCode() {
        if (itemID > 0) {
            return Objects.hash(itemID);
        }
        return Objects.hash(name, category, price);
    }

}