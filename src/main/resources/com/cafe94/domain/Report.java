// File: src/main/java/com/cafe94/domain/Report.java
package com.cafe94.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cafe94.enums.ReportType;
import com.cafe94.util.ValidationUtils;


/**
 * Represents a generated report containing specific system data analysis results.
 * @author Adigun Lateef
 * @version 1.0
 */
public final class Report implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
    Logger.getLogger(Report.class.getName());
    private final ReportType type;
    private final LocalDateTime generatedTimestamp;
    private final String title;
    private final Map<String, Object> data;

    /**
     * Constructs a new, immutable Report object.
     *
     * @param type  The type of the report
     * @param title A descriptive title for the report
     * @param data  The report data content
     * @throws NullPointerException if type or title is null.
     * @throws IllegalArgumentException if title is blank.
     */
    public Report(ReportType type, String title, Map<String, Object> data) {
        this.type = Objects.requireNonNull(type,
        "Report type cannot be null.");
        this.title = ValidationUtils.requireNonBlank(title,
        "Report title");
        // Create a defensive copy and make it unmodifiable
        Map<String, Object> dataCopy = (data == null) ?
        new HashMap<>() : new HashMap<>(data);
        this.data = Collections.unmodifiableMap(dataCopy);
         // Set generation time
        this.generatedTimestamp = LocalDateTime.now();

        LOGGER.log(Level.FINE, "Created Report: Type={0}, Title='{1}', " +
        "DataEntries={2}",
        new Object[]{this.type, this.title, this.data.size()});
    }

    // Getters

    /**
     * @return The {@link ReportType} enum constant indicating the type of
     * report.
     * */
    public ReportType getType() {
        return type;
    }

    /**
     * @return The exact {@link LocalDateTime} when the report object is
     * created.
     */
    public LocalDateTime getGeneratedTimestamp() {
        return generatedTimestamp;
    }

    /**
     * @return The descriptive title of the report content.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the report data as an unmodifiable Map.
     * @return An unmodifiable {@code Map<String, Object>} containing the
     * report data.
     */
    public Map<String, Object> getData() {
        return data;
    }

    // Standard Methods

    /** String representaion of the objects
     * @return a string reprentation of the Report objects
     */
    @Override
    public String toString() {
        return "Report[" +
               "Type=" + type +
               ", Title='" + title + '\'' +
               ", Generated=" + generatedTimestamp.toLocalDate() +
               "T" + generatedTimestamp.toLocalTime() +
               ", DataEntries=" + data.size() + ']';
    }

    /**
     * Checks equality based on report type, title, generation timestamp,
     * and the content of the data map.
     * @param o The object to compare with.
     * @return true if the objects represent the same report content,
     * false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return type == report.type &&
               Objects.equals(generatedTimestamp, report.generatedTimestamp) &&
               Objects.equals(title, report.title) &&
               Objects.equals(data, report.data);
    }

    /**
     * Generates a hash code based on report type,
     * @return The hash code for this Report object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, generatedTimestamp, title, data);
    }
}