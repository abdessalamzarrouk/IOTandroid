package com.example.attendancesystem.models;

import com.google.firebase.Timestamp;
import java.io.Serializable; // Import Serializable
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Course implements Serializable { // Implement Serializable
    private String courseId;
    private String courseName;
    private String teacherEmail; // Email of the assigned teacher
    private String teacherName;  // Name of the assigned teacher
    private String department;
    private boolean isActive;
    private Timestamp createdAt;
    private String field; // The field/filière this course belongs to
    private List<String> targetYears; // e.g., ["1ère Année", "2ème Année"]

    // Schedule details (nested object, or separate collection depending on complexity)
    private Schedule schedule;

    // Statistics (nested object)
    private Statistics statistics;

    // Required public no-argument constructor for Firebase
    public Course() {
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.targetYears = new ArrayList<>();
        this.schedule = new Schedule(); // Initialize nested objects
        this.statistics = new Statistics();
    }

    // Constructor for initial creation
    public Course(String courseId, String courseName, String department, String field, List<String> targetYears) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.department = department;
        this.field = field;
        this.targetYears = targetYears != null ? new ArrayList<>(targetYears) : new ArrayList<>();
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.schedule = new Schedule();
        this.statistics = new Statistics();
        // teacherEmail and teacherName are set when assigned
        this.teacherEmail = null;
        this.teacherName = null;
    }

    // Getters
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public String getTeacherEmail() { return teacherEmail; }
    public String getTeacherName() { return teacherName; }
    public String getDepartment() { return department; }
    public boolean isActive() { return isActive; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getField() { return field; }
    public List<String> getTargetYears() { return targetYears; }
    public Schedule getSchedule() { return schedule; }
    public Statistics getStatistics() { return statistics; }

    // Setters
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public void setDepartment(String department) { this.department = department; }
    public void setActive(boolean active) { isActive = active; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setField(String field) { this.field = field; }
    public void setTargetYears(List<String> targetYears) { this.targetYears = targetYears; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    public void setStatistics(Statistics statistics) { this.statistics = statistics; }

    // Nested classes for Schedule and Statistics
    public static class Schedule implements Serializable { // Implement Serializable
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private String room;
        private boolean isRecurring;

        public Schedule() {
            // Default constructor
        }

        public Schedule(String dayOfWeek, String startTime, String endTime, String room, boolean isRecurring) {
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
        public boolean isRecurring() { return isRecurring; }

        // Setters
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public void setRoom(String room) { this.room = room; }
        public void setRecurring(boolean recurring) { isRecurring = recurring; }

        // Convert to Map for Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("dayOfWeek", dayOfWeek);
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            map.put("room", room);
            map.put("isRecurring", isRecurring);
            return map;
        }
    }

    public static class Statistics implements Serializable { // Implement Serializable
        private int totalSessions;
        private double averageAttendanceRate;
        private int totalEnrolledStudents;

        public Statistics() {
            // Default values
            this.totalSessions = 0;
            this.averageAttendanceRate = 0.0;
            this.totalEnrolledStudents = 0;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public double getAverageAttendanceRate() { return averageAttendanceRate; }
        public int getTotalEnrolledStudents() { return totalEnrolledStudents; }

        // Setters
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        public void setAverageAttendanceRate(double averageAttendanceRate) { this.averageAttendanceRate = averageAttendanceRate; }
        public void setTotalEnrolledStudents(int totalEnrolledStudents) { this.totalEnrolledStudents = totalEnrolledStudents; }

        // Convert to Map for Firebase
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalSessions", totalSessions);
            map.put("averageAttendanceRate", averageAttendanceRate);
            map.put("totalEnrolledStudents", totalEnrolledStudents);
            return map;
        }
    }

    // Method to convert Course object to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", courseId);
        map.put("courseName", courseName);
        map.put("teacherEmail", teacherEmail);
        map.put("teacherName", teacherName);
        map.put("department", department);
        map.put("isActive", isActive);
        map.put("createdAt", createdAt);
        map.put("field", field);
        map.put("targetYears", targetYears);
        if (schedule != null) {
            map.put("schedule", schedule.toMap());
        }
        if (statistics != null) {
            map.put("statistics", statistics.toMap());
        }
        return map;
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", teacherEmail='" + teacherEmail + '\'' +
                ", teacherName='" + teacherName + '\'' +
                ", department='" + department + '\'' +
                ", field='" + field + '\'' +
                '}';
    }
}