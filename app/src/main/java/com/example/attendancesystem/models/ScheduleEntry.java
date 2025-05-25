package com.example.attendancesystem.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // For Objects.equals and Objects.hash

public class ScheduleEntry implements Serializable {
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String room;
    private boolean isRecurring; // Based on your Firebase structure

    public ScheduleEntry() {
        // Required public no-argument constructor for Firebase deserialization
    }

    public ScheduleEntry(String dayOfWeek, String startTime, String endTime, String room) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.isRecurring = true; // Default value if not specified
    }

    public ScheduleEntry(String dayOfWeek, String startTime, String endTime, String room, boolean isRecurring) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.isRecurring = isRecurring;
    }

    // Getters
    public String getDayOfWeek() { return dayOfWeek; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    // Firebase requires "getIsRecurring" for a boolean field named "isRecurring"
    public boolean getIsRecurring() { return isRecurring; }


    // Setters
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setRoom(String room) { this.room = room; }
    // Firebase requires "setIsRecurring" for a boolean field named "isRecurring"
    public void setIsRecurring(boolean recurring) { isRecurring = recurring; }


    /**
     * Provides a human-readable string for display in UI elements like Spinners.
     * This is what will be shown to the user in the dropdown.
     * @return Formatted string of the schedule entry.
     */
    public String toDisplayString() {
        return dayOfWeek + " " + startTime + " - " + endTime + " (" + room + ")";
    }

    /**
     * Converts the ScheduleEntry object to a Map for Firestore.
     * Firebase typically uses this if you're manually saving nested objects as maps.
     * @return A Map representation of the ScheduleEntry.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("dayOfWeek", dayOfWeek);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("room", room);
        map.put("isRecurring", isRecurring);
        return map;
    }

    /**
     * IMPORTANT: Overrides equals() for proper object comparison.
     * This is crucial for methods like `List.indexOf()` and `Spinner.setSelection()`
     * to correctly identify and pre-select an item based on its content, not just its memory address.
     * We compare the fields that uniquely define a schedule entry.
     * @param o The object to compare with.
     * @return true if the objects are logically equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleEntry that = (ScheduleEntry) o;
        return Objects.equals(dayOfWeek, that.dayOfWeek) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(room, that.room);
        // isRecurring is usually not part of the unique identity of a time slot,
        // so we typically don't include it in equals() for this purpose.
    }

    /**
     * IMPORTANT: Overrides hashCode() to maintain the contract with equals().
     * If two objects are equal according to the equals() method, then calling
     * the hashCode() method on each of the two objects must produce the same integer result.
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, startTime, endTime, room);
    }

    /**
     * Overrides toString() for debugging purposes. Using toDisplayString() for UI.
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return "ScheduleEntry{" +
                "dayOfWeek='" + dayOfWeek + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", room='" + room + '\'' +
                ", isRecurring=" + isRecurring +
                '}';
    }
}