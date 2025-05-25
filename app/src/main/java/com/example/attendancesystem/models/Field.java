package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.io.Serializable; // Import Serializable
import java.util.HashMap;
import java.util.Map;

public class Field implements Serializable { // Implement Serializable
    private String fieldId; // Can be the same as fieldName for simplicity, or a generated UID
    private String fieldName;
    private String department; // e.g., "Informatique", "Mathématiques"
    private String description;
    private Timestamp createdAt;
    private Timestamp lastUpdatedAt;

    // Required public no-argument constructor for Firebase
    public Field() {
        this.createdAt = Timestamp.now(); // Default value
        this.lastUpdatedAt = Timestamp.now(); // Default value
    }

    // Constructor for initial creation
    public Field(String fieldId, String fieldName, String department, String description) {
        this.fieldId = fieldId;
        this.fieldName = fieldName;
        this.department = department;
        this.description = description;
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
    }

    // Getters
    public String getFieldId() { return fieldId; }
    public String getFieldName() { return fieldName; }
    public String getDepartment() { return department; }
    public String getDescription() { return description; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }

    // Setters
    public void setFieldId(String fieldId) { this.fieldId = fieldId; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public void setDepartment(String department) { this.department = department; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    // Optional: toMap() method if you prefer explicit mapping over toObject/fromObject
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fieldId", fieldId);
        map.put("fieldName", fieldName);
        map.put("department", department);
        map.put("description", description);
        map.put("createdAt", createdAt);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }

    @Override
    public String toString() {
        return "Field{" +
                "fieldId='" + fieldId + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}