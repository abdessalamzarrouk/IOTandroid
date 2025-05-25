// app/src/main/java/com/example/attendancesystem/models/Course.java

package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Course implements Serializable {
    private String courseId;
    private String courseName;
    private String department;
    private String field; // Name of the associated field (Filière)
    private List<String> targetYears;
    private String teacherEmail; // Email of the assigned teacher
    private String teacherName;  // Name of the assigned teacher
    private boolean isActive;
    private Timestamp createdAt;

    private ScheduleEntry courseScheduleEntry; // NEW: Reference to a specific schedule entry from the field

    private Map<String, Object> statistics; // Example: { "totalLectures": 10, "avgAttendance": 0.85 }

    // Required public no-argument constructor for Firebase
    public Course() {}

    public Course(String courseId, String courseName, String department, String field, List<String> targetYears) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.department = department;
        this.field = field;
        this.targetYears = targetYears;
        this.isActive = true; // Default to active
        this.createdAt = Timestamp.now();
        this.statistics = new HashMap<>(); // Initialize empty statistics
        this.teacherEmail = null;
        this.teacherName = null;
    }

    // Getters
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getDepartment() { return department; }
    public String getField() { return field; }
    public List<String> getTargetYears() { return targetYears; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getTeacherName() { return teacherName; }
    public boolean isActive() { return isActive; }
    public Timestamp getCreatedAt() { return createdAt; }
    public ScheduleEntry getCourseScheduleEntry() { return courseScheduleEntry; } // NEW GETTER
    public Map<String, Object> getStatistics() { return statistics; }


    // Setters
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setDepartment(String department) { this.department = department; }
    public void setField(String field) { this.field = field; }
    public void setTargetYears(List<String> targetYears) { this.targetYears = targetYears; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setCourseScheduleEntry(ScheduleEntry courseScheduleEntry) { this.courseScheduleEntry = courseScheduleEntry; } // NEW SETTER
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }


    // For saving to Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("department", department);
        map.put("field", field);
        map.put("targetYears", targetYears);
        map.put("teacherEmail", teacherEmail);
        map.put("teacherName", teacherName);
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        map.put("statistics", statistics);

        // Convert ScheduleEntry to a map for Firestore
        if (courseScheduleEntry != null) {
            map.put("courseScheduleEntry", courseScheduleEntry.toMap()); // Using toMap() of ScheduleEntry
        } else {
            map.put("courseScheduleEntry", null);
        }

        return map;
    }
}