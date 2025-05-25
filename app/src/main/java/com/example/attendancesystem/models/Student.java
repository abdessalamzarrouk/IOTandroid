package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.HashMap;
import java.util.Map;

public class Student {
    private String email; // Identifiant unique
    // NEW fields for field-based architecture
    private String field; // "Data Science", "Software Engineering", "Cybersecurity", etc.
    private String fullName;
    private String studentId;
    private String profileImageUrl;
    private Timestamp createdAt;
    @PropertyName("isActive")
    private boolean isActive;
    private String phoneNumber;
    private String department;
    private String year; // L1, L2, L3, M1, M2
    private NotificationPreferences notificationPreferences;
    private Timestamp lastLoginAt;
    private Timestamp lastUpdatedAt;

    // Constructeur vide requis pour Firebase
    public Student() {
        this.notificationPreferences = new NotificationPreferences();
    }



    public Student(String email, String fullName, String studentId, String department, String year, String field) {
        this.email = email;
        this.fullName = fullName;
        this.studentId = studentId;
        this.department = department;
        this.year = year;
        this.field = field; // NEW
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
        this.notificationPreferences = new NotificationPreferences();
    }

    public Student(String email, String fullName, String idNumber, String department, String year) {
        this.email = email;
        this.fullName = fullName;
        this.studentId = idNumber;
        this.department = department;
        this.year = year;
    }

    public Student(String email, String fullName, String studentId, String department, String field, String year, String phoneNumber) {
        this.email = email;
        this.fullName = fullName;
        this.studentId = studentId;
        this.department = department;
        this.field = field;
        this.year = year;
        this.phoneNumber = phoneNumber; // Ensure this is set
        this.profileImageUrl = ""; // Default empty
        this.isActive = true; // Default true
        this.createdAt = Timestamp.now();
        this.lastUpdatedAt = Timestamp.now();
        this.lastLoginAt = null; // Default null
    }
    // Méthode pour convertir en Map pour Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("fullName", fullName);
        map.put("studentId", studentId);
        map.put("profileImageUrl", profileImageUrl);
        map.put("createdAt", createdAt);
        map.put("isActive", isActive);
        map.put("phoneNumber", phoneNumber);
        map.put("department", department);
        map.put("year", year);
        map.put("field", field); // NEW
        if (notificationPreferences != null) {
            map.put("notificationPreferences", notificationPreferences.toMap());
        }
        map.put("lastLoginAt", lastLoginAt);
        map.put("lastUpdatedAt", lastUpdatedAt);
        return map;
    }


    // Add getter and setter:
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    // Getters
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getStudentId() { return studentId; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public Timestamp getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getDepartment() { return department; }
    public String getYear() { return year; }
    public NotificationPreferences getNotificationPreferences() { return notificationPreferences; }
    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }

    // Setters
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setDepartment(String department) { this.department = department; }
    public void setYear(String year) { this.year = year; }
    public void setNotificationPreferences(NotificationPreferences notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    @Override
    public String toString() {
        return "Student{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", studentId='" + studentId + '\'' +
                ", department='" + department + '\'' +
                ", year='" + year + '\'' +
                '}';
    }
}