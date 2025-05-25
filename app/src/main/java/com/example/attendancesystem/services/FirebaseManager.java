package com.example.attendancesystem.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.attendancesystem.models.Course;
import com.example.attendancesystem.models.Field;
import com.example.attendancesystem.models.Justification;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.models.Attendance;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    // Instances Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Collections Firestore selon la nouvelle architecture
    private static final String STUDENTS_COLLECTION = "students";
    private static final String FIELDS_COLLECTION = "fields";
    private static final String TEACHERS_COLLECTION = "teachers";
    private static final String ADMINS_COLLECTION = "admins";
    private static final String ATTENDANCE_COLLECTION = "attendance";
    private static final String COURSES_COLLECTION = "courses";
    private static final String SESSIONS_COLLECTION = "sessions";
    private static final String JUSTIFICATIONS_COLLECTION = "justifications";
    private static final String REPORTS_COLLECTION = "reports";

    // Singleton pattern
    private static FirebaseManager instance;

    public FirebaseFirestore getFirestore() {
        return db;
    }

    public static class AttendanceStats {
        private int totalSessions;
        private int attendedSessions;
        private double attendanceRate;

        public AttendanceStats(int totalSessions, int attendedSessions) {
            this.totalSessions = totalSessions;
            this.attendedSessions = attendedSessions;
            this.attendanceRate = totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0.0;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getAttendedSessions() { return attendedSessions; }
        public double getAttendanceRate() { return attendanceRate; }
    }

    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // =================== AUTHENTIFICATION ===================

    /**
     * Interface pour les callbacks d'authentification
     */
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }

    /**
     * Connexion avec email/mot de passe
     */
    public void signInWithEmailPassword(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Connexion réussie: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Erreur de connexion";
                            Log.w(TAG, "Échec de connexion", task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Inscription avec email/mot de passe
     */
    public void createUserWithEmailPassword(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Inscription réussie: " + user.getEmail());
                            callback.onSuccess(user);
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Erreur d'inscription";
                            Log.w(TAG, "Échec d'inscription", task.getException());
                            callback.onFailure(error);
                        }
                    }
                });
    }

    /**
     * Déconnexion
     */
    public void signOut() {
        mAuth.signOut();
        Log.d(TAG, "Utilisateur déconnecté");
    }

    /**
     * Obtenir l'utilisateur actuellement connecté
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /**
     * Vérifier si un utilisateur est connecté
     */
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    // =================== INTERFACE GÉNÉRIQUE POUR LES CALLBACKS ===================

    /**
     * Interface pour les callbacks de données
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    // =================== GESTION DES ÉTUDIANTS ===================

    /**
     * Sauvegarder un étudiant dans Firestore
     */
    public void saveStudent(Student student, DataCallback<Void> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(student.getEmail())
                .set(student.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Étudiant sauvegardé: " + student.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde étudiant", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /*
     * Mettre à jour le profil étudiant
     */
    public void updateStudent(Student student, DataCallback<Void> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(student.getEmail())
                .update(student.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Étudiant mis à jour: " + student.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour étudiant", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== GESTION DES ENSEIGNANTS ===================

    /**
     * Sauvegarder un enseignant dans Firestore
     */
    public void saveTeacher(Teacher teacher, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(teacher.getEmail())
                .set(teacher.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Enseignant sauvegardé: " + teacher.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde enseignant", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Récupérer un enseignant par son email
     */
    public void getTeacherByEmail(String email, DataCallback<Teacher> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Teacher teacher = document.toObject(Teacher.class);
                                if (teacher != null) {
                                    callback.onSuccess(teacher);
                                } else {
                                    callback.onFailure("Erreur de conversion des données");
                                }
                            } else {
                                callback.onFailure("Enseignant non trouvé");
                            }
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Mettre à jour le profil enseignant
     */
    public void updateTeacher(Teacher teacher, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION)
                .document(teacher.getEmail())
                .update(teacher.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Enseignant mis à jour: " + teacher.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour enseignant", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== GESTION DES ADMINISTRATEURS ===================

    /**
     * Sauvegarder un administrateur dans Firestore
     */
    public void saveAdmin(Admin admin, DataCallback<Void> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(admin.getEmail())
                .set(admin.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Administrateur sauvegardé: " + admin.getEmail());
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Erreur sauvegarde administrateur", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Récupérer un administrateur par son email
     */
    public void getAdminByEmail(String email, DataCallback<Admin> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Admin admin = document.toObject(Admin.class);
                                if (admin != null) {
                                    callback.onSuccess(admin);
                                } else {
                                    callback.onFailure("Erreur de conversion des données");
                                }
                            } else {
                                callback.onFailure("Administrateur non trouvé");
                            }
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Mettre à jour le profil administrateur
     */
    public void updateAdmin(Admin admin, DataCallback<Void> callback) {
        db.collection(ADMINS_COLLECTION)
                .document(admin.getEmail())
                .update(admin.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Administrateur mis à jour: " + admin.getEmail());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour administrateur", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // =================== DÉTECTION AUTOMATIQUE DU TYPE D'UTILISATEUR ===================

    /**
     * Déterminer le type d'utilisateur par email et récupérer ses données
     */
    public void getUserByEmail(String email, DataCallback<Object> callback) {
        // Essayer d'abord dans la collection students
        getStudentByEmail(email, new DataCallback<Student>() {
            @Override
            public void onSuccess(Student student) {
                callback.onSuccess(student);
            }

            @Override
            public void onFailure(String error) {
                // Si pas trouvé dans students, essayer teachers
                getTeacherByEmail(email, new DataCallback<Teacher>() {
                    @Override
                    public void onSuccess(Teacher teacher) {
                        callback.onSuccess(teacher);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Si pas trouvé dans teachers, essayer admins
                        getAdminByEmail(email, new DataCallback<Admin>() {
                            @Override
                            public void onSuccess(Admin admin) {
                                callback.onSuccess(admin);
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Utilisateur non trouvé dans aucune collection");
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Obtenir le rôle d'un utilisateur par email
     */
    public void getUserRole(String email, DataCallback<String> callback) {
        getUserByEmail(email, new DataCallback<Object>() {
            @Override
            public void onSuccess(Object user) {
                if (user instanceof Student) {
                    callback.onSuccess("student");
                } else if (user instanceof Teacher) {
                    callback.onSuccess("teacher");
                } else if (user instanceof Admin) {
                    callback.onSuccess("admin");
                } else {
                    callback.onFailure("Type d'utilisateur non reconnu");
                }
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    // =================== GESTION DES PRÉSENCES ===================

    /**
     * Enregistrer une présence
     */
    public void saveAttendance(Attendance attendance, DataCallback<String> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .add(attendance.toMap())
                .addOnSuccessListener(documentReference -> {
                    String attendanceId = documentReference.getId();
                    attendance.setAttendanceId(attendanceId);

                    // Mettre à jour avec l'ID généré
                    documentReference.update("attendanceId", attendanceId);

                    Log.d(TAG, "Présence enregistrée: " + attendanceId);
                    callback.onSuccess(attendanceId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur enregistrement présence", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Récupérer l'historique de présence d'un étudiant
     */
    public void getStudentAttendanceHistory(String studentEmail, DataCallback<List<Attendance>> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Attendance> attendanceList = new ArrayList<>();
                            for (DocumentSnapshot document : task.getResult()) {
                                Attendance attendance = document.toObject(Attendance.class);
                                if (attendance != null) {
                                    attendance.setAttendanceId(document.getId());
                                    attendanceList.add(attendance);
                                }
                            }
                            Log.d(TAG, "Historique récupéré: " + attendanceList.size() + " entrées");
                            callback.onSuccess(attendanceList);
                        } else {
                            callback.onFailure(task.getException().getMessage());
                        }
                    }
                });
    }

    /**
     * Récupérer les présences d'un cours
     */
    public void getCourseAttendance(String courseId, DataCallback<List<Attendance>> callback) {
        db.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Attendance> attendanceList = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Attendance attendance = document.toObject(Attendance.class);
                            if (attendance != null) {
                                attendance.setAttendanceId(document.getId());
                                attendanceList.add(attendance);
                            }
                        }
                        callback.onSuccess(attendanceList);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // =================== GESTION DES FICHIERS AVEC GOOGLE DRIVE ===================

    /**
     * Interface pour l'upload de fichiers vers Google Drive
     */
    public interface GoogleDriveUploadCallback {
        void onProgress(int progress);
        void onSuccess(String fileUrl);
        void onFailure(String error);
    }

    /**
     * Upload d'une photo de profil vers Google Drive et mise à jour du profil utilisateur
     */
    public void uploadProfileImageToGoogleDrive(String userEmail, String userType,
                                                android.net.Uri imageUri, Context context,
                                                GoogleDriveUploadCallback callback) {

        GoogleDriveService driveService = GoogleDriveService.getInstance(context);

        if (!driveService.isSignedIn()) {
            callback.onFailure("Non connecté à Google Drive. Veuillez vous connecter d'abord.");
            return;
        }

        driveService.uploadProfilePhoto(userEmail, userType, imageUri,
                new GoogleDriveService.UploadCallback() {
                    @Override
                    public void onProgress(int progress) {
                        callback.onProgress(progress);
                    }

                    @Override
                    public void onSuccess(String fileUrl) {
                        // Mettre à jour le profil utilisateur avec l'URL de la photo
                        updateUserProfileImage(userEmail, userType, fileUrl, new DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                callback.onSuccess(fileUrl);
                            }

                            @Override
                            public void onFailure(String error) {
                                callback.onFailure("Photo uploadée mais erreur de mise à jour du profil: " + error);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Mettre à jour l'URL de l'image de profil dans Firestore
     */
    private void updateUserProfileImage(String userEmail, String userType, String imageUrl, DataCallback<Void> callback) {
        String collection = getCollectionForUserType(userType);
        if (collection == null) {
            callback.onFailure("Type d'utilisateur non reconnu: " + userType);
            return;
        }

        db.collection(collection)
                .document(userEmail)
                .update("profileImageUrl", imageUrl, "lastUpdatedAt", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "URL de l'image mise à jour pour " + userEmail);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour URL image", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Obtenir le nom de collection selon le type d'utilisateur
     */
    private String getCollectionForUserType(String userType) {
        switch (userType) {
            case "student": return STUDENTS_COLLECTION;
            case "teacher": return TEACHERS_COLLECTION;
            case "admin": return ADMINS_COLLECTION;
            default: return null;
        }
    }

    /**
     * Supprimer une photo de profil de Google Drive
     */
    public void deleteProfileImageFromGoogleDrive(String userEmail, String userType,
                                                  Context context, DataCallback<Void> callback) {

        GoogleDriveService driveService = GoogleDriveService.getInstance(context);

        if (!driveService.isSignedIn()) {
            callback.onFailure("Non connecté à Google Drive");
            return;
        }

        driveService.deleteProfilePhoto(userEmail, userType, new GoogleDriveService.DriveCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Mettre à jour le profil utilisateur (vider l'URL)
                updateUserProfileImage(userEmail, userType, "", callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void saveSession(Session session, DataCallback<String> callback) {
        db.collection(SESSIONS_COLLECTION)
                .add(session.toMap())
                .addOnSuccessListener(documentReference -> {
                    String sessionId = documentReference.getId();
                    session.setSessionId(sessionId);

                    // Mettre à jour avec l'ID généré
                    documentReference.update("sessionId", sessionId);

                    Log.d(TAG, "Session sauvegardée: " + sessionId);
                    callback.onSuccess(sessionId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur sauvegarde session", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Mettre à jour une session
     */
    public void updateSession(Session session, DataCallback<Void> callback) {
        if (session.getSessionId() == null) {
            callback.onFailure("ID de session manquant");
            return;
        }

        db.collection(SESSIONS_COLLECTION)
                .document(session.getSessionId())
                .update(session.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Session mise à jour: " + session.getSessionId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erreur mise à jour session", e);
                    callback.onFailure(e.getMessage());
                });
    }

    // Add these NEW methods to your FirebaseManager.java (replace the existing session methods)

// =================== FIELD-BASED SESSION MANAGEMENT ===================

    /**
     * Obtenir les sessions d'aujourd'hui pour un étudiant (par département, filière et année)
     */
    public void getTodaySessionsForStudent(String studentEmail, String department, String field, String year, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        // Rechercher les sessions pour ce département, filière et année
        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        Log.d(TAG, "Found " + sessions.size() + " sessions today for " + field + " " + year);
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting today's sessions", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir la prochaine session pour un étudiant (par département, filière et année)
     */
    public void getNextSessionForStudent(String studentEmail, String department, String field, String year, DataCallback<Session> callback) {
        Timestamp now = Timestamp.now();

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereIn("status", Arrays.asList("scheduled", "active"))
                .whereGreaterThanOrEqualTo("startTime", now)
                .orderBy("startTime")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Next session found: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No future sessions found for " + field + " " + year);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting next session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les sessions d'aujourd'hui pour un enseignant
     */
    public void getTodaySessionsForTeacher(String teacherEmail, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        Log.d(TAG, "Found " + sessions.size() + " sessions today for teacher: " + teacherEmail);
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting teacher's today sessions", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir la prochaine session pour un enseignant
     */
    public void getNextSessionForTeacher(String teacherEmail, DataCallback<Session> callback) {
        Timestamp now = Timestamp.now();

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereIn("status", Arrays.asList("scheduled", "active"))
                .whereGreaterThanOrEqualTo("startTime", now)
                .orderBy("startTime")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Next session found for teacher: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No future sessions found for teacher: " + teacherEmail);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting teacher's next session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les sessions programmées aujourd'hui pour un enseignant
     */
    public void getTodayScheduledSessionsForTeacher(String teacherEmail, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp startOfNextDay = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereEqualTo("status", "scheduled")
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .whereLessThan("startTime", startOfNextDay)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        Log.d(TAG, "Found " + sessions.size() + " scheduled sessions today for teacher: " + teacherEmail);
                        callback.onSuccess(sessions);
                    } else {
                        Log.e(TAG, "Error getting teacher's scheduled sessions", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les statistiques d'assiduité d'un étudiant (par département et filière)
     */
    public void getStudentAttendanceStatistics(String studentEmail, String department, String field, String year, DataCallback<AttendanceStats> callback) {
        // Calculer les statistiques des 30 derniers jours
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Timestamp thirtyDaysAgo = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("endTime", thirtyDaysAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalSessions = 0;
                        int attendedSessions = 0;

                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null && session.getEnrolledStudentEmails().contains(studentEmail)) {
                                totalSessions++;
                                if (session.getPresentStudentEmails().contains(studentEmail)) {
                                    attendedSessions++;
                                }
                            }
                        }

                        AttendanceStats stats = new AttendanceStats(totalSessions, attendedSessions);
                        Log.d(TAG, "Attendance stats for " + studentEmail + ": " + attendedSessions + "/" + totalSessions);
                        callback.onSuccess(stats);
                    } else {
                        Log.e(TAG, "Error getting attendance statistics", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Obtenir les sessions d'une filière pour une semaine donnée
     */
    public void getWeeklySessionsForField(String department, String field, String year, Timestamp weekStart, DataCallback<List<Session>> callback) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(weekStart.toDate());

        // End of week (7 days later)
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        Timestamp weekEnd = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year)
                .whereGreaterThanOrEqualTo("startTime", weekStart)
                .whereLessThan("startTime", weekEnd)
                .orderBy("startTime")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Session> sessions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                                sessions.add(session);
                            }
                        }
                        callback.onSuccess(sessions);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    /**
     * Auto-enroll students in sessions based on their field and year
     */
    public void autoEnrollStudentsInSession(Session session, DataCallback<Void> callback) {
        // Get all students matching the session's criteria
        db.collection(STUDENTS_COLLECTION)
                .whereEqualTo("department", session.getDepartment())
                .whereEqualTo("field", session.getField())
                .whereEqualTo("year", session.getTargetYears().get(0)) // For simplicity, taking first target year
                .whereEqualTo("isActive", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> studentEmails = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            if (student != null) {
                                studentEmails.add(student.getEmail());
                            }
                        }

                        // Update session with enrolled students
                        session.setEnrolledStudentEmails(studentEmails);

                        // Save updated session
                        updateSession(session, callback);

                        Log.d(TAG, "Auto-enrolled " + studentEmails.size() + " students in session: " + session.getCourseName());
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }
    // Add this method to your existing FirebaseManager.java class

    /**
     * Obtenir une session active pour un enseignant
     */
    public void getActiveSessionForTeacher(String teacherEmail, DataCallback<Session> callback) {
        Log.d(TAG, "Getting active session for teacher: " + teacherEmail);

        db.collection(SESSIONS_COLLECTION)
                .whereEqualTo("teacherEmail", teacherEmail)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                session.setSessionId(document.getId());
                            }
                            Log.d(TAG, "Active session found: " + (session != null ? session.getCourseName() : "null"));
                            callback.onSuccess(session);
                        } else {
                            Log.d(TAG, "No active session found for teacher: " + teacherEmail);
                            callback.onSuccess(null);
                        }
                    } else {
                        Log.e(TAG, "Error getting active session", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }
    public void getStudentAttendanceStatistics(String studentEmail, DataCallback<AttendanceStats> callback) {
        // Calculer les statistiques des 30 derniers jours
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Timestamp thirtyDaysAgo = new Timestamp(calendar.getTime());

        db.collection(SESSIONS_COLLECTION)
                .whereArrayContains("enrolledStudentEmails", studentEmail)
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("endTime", thirtyDaysAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalSessions = 0;
                        int attendedSessions = 0;

                        for (DocumentSnapshot document : task.getResult()) {
                            Session session = document.toObject(Session.class);
                            if (session != null) {
                                totalSessions++;
                                if (session.getPresentStudentEmails().contains(studentEmail)) {
                                    attendedSessions++;
                                }
                            }
                        }

                        AttendanceStats stats = new AttendanceStats(totalSessions, attendedSessions);
                        callback.onSuccess(stats);
                    } else {
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    // ---------------------------------------------- JUSTIFS
    public void getStudentByEmail(String email, DataCallback<Student> callback) {
        db.collection(STUDENTS_COLLECTION)
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Student student = documentSnapshot.toObject(Student.class);
                        if (student != null) { // Added null check for robustness
                            callback.onSuccess(student);
                        } else {
                            callback.onFailure("Erreur de conversion des données"); // More specific error
                        }
                    } else {
                        callback.onFailure("Étudiant non trouvé");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Erreur de récupération de l'étudiant: " + e.getMessage()));
    }

    // This method needs to be implemented to fetch courses associated with the student
    // based on department, field, and year.
    public void getStudentCourses(String studentEmail, String department, String field, String year, DataCallback<List<Map<String, String>>> callback) {
        db.collection("courses")
                .whereEqualTo("department", department)
                .whereEqualTo("field", field)
                .whereArrayContains("targetYears", year) // Query if the student's year is in the course's targetYears
                // Optional: You might also want to combine with enrolledStudentEmails if that's still a hard requirement
                // .whereArrayContains("enrolledStudentEmails", studentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, String>> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, String> course = new HashMap<>();
                        course.put("id", document.getId());
                        course.put("name", document.getString("courseName")); // Use "courseName"
                        // Add other relevant course data if needed
                        courses.add(course);
                    }
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> callback.onFailure("Error getting student courses: " + e.getMessage()));
    }


    // --- Justification Operations ---

    /**
     * Get all justifications from Firestore for admin review.
     * Can be extended with filtering if needed.
     */
    public void getAllJustifications(DataCallback<List<Justification>> callback) {
        db.collection("justifications")
                .orderBy("submittedAt", Query.Direction.DESCENDING) // Order by latest submitted
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Justification> justifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Justification justification = document.toObject(Justification.class);
                            // Assign the Firestore document ID to the Justification object
                            justification.setJustificationId(document.getId());
                            justifications.add(justification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing justification document: " + document.getId(), e);
                            // Handle corrupted documents gracefully, perhaps skip them
                        }
                    }
                    callback.onSuccess(justifications);
                })
                .addOnFailureListener(e -> callback.onFailure("Error getting all justifications: " + e.getMessage()));
    }

    /**
     * Update an existing justification in Firestore.
     */
    public void updateJustification(Justification justification, DataCallback<Void> callback) {
        if (justification.getJustificationId() == null || justification.getJustificationId().isEmpty()) {
            callback.onFailure("Justification ID is missing for update.");
            return;
        }

        db.collection("justifications")
                .document(justification.getJustificationId())
                .set(justification.toMap()) // Use toMap() to ensure all fields are set correctly
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Justification updated: " + justification.getJustificationId());
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating justification", e);
                    callback.onFailure("Error updating justification: " + e.getMessage());
                });
    }

    // Save a new justification (modified to use Justification model with justificationDate)
    /**
     * Sauvegarder une nouvelle justification dans Firestore.
     * Définit également l'ID du document généré sur l'objet Justification.
     */
    public void saveJustification(Justification justification, DataCallback<String> callback) {
        db.collection("justifications")
                .add(justification.toMap()) // Use toMap() for explicit control
                .addOnSuccessListener(documentReference -> {
                    // IMPORTANT: Set the generated Firestore document ID back to the Justification object
                    justification.setJustificationId(documentReference.getId());
                    Log.d(TAG, "Justification saved with ID: " + documentReference.getId());

                    // Now, update the document to include its own ID as a field (optional but good practice)
                    // This makes it easier to query by ID directly from the document itself if needed.
                    db.collection("justifications")
                            .document(documentReference.getId())
                            .update("justificationId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Justification ID field updated in Firestore for: " + documentReference.getId());
                                callback.onSuccess(documentReference.getId());
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error updating justificationId field in Firestore for: " + documentReference.getId(), e);
                                // Even if this nested update fails, the document was added,
                                // and the ID was set on the Java object, so we still call success.
                                callback.onSuccess(documentReference.getId());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding justification", e);
                    callback.onFailure("Error adding justification: " + e.getMessage());
                });
    }

    // Get all justifications for a specific student (modified to use studentEmail)
    public void getStudentJustifications(String studentEmail, DataCallback<List<Justification>> callback) {
        db.collection("justifications")
                .whereEqualTo("studentEmail", studentEmail)
                .orderBy("submittedAt", Query.Direction.DESCENDING) // Order by latest submitted
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Justification> justifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        justifications.add(document.toObject(Justification.class));
                    }
                    callback.onSuccess(justifications);
                })
                .addOnFailureListener(e -> callback.onFailure("Error getting student justifications: " + e.getMessage()));
    }

    // --- New/Modified: Attendance Operations (to be used by admin after justification approval) ---

    // This method will be used by an ADMIN to update attendance status
    // after a justification is approved.
    public void updateAttendanceStatusForJustifiedAbsence(
            String studentEmail, String courseId, Date justificationDate, DataCallback<Void> callback) {

        // To handle potential time differences and ensure date-based matching,
        // we'll query for attendance records within a specific day range.
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(justificationDate);
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(justificationDate);
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);


        db.collection("attendance")
                .whereEqualTo("studentEmail", studentEmail)
                .whereEqualTo("courseId", courseId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay.getTime())
                .whereLessThanOrEqualTo("timestamp", endOfDay.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure("No attendance record found for this student, course, and date.");
                        return;
                    }

                    // Batch update to ensure all matching records are updated
                    // This is crucial if a student had multiple sessions/attendances for the same course on one day
                    // and was absent from all.
                    FirebaseFirestore.getInstance().runBatch(batch -> {
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    // Only update if current status is "absent". If it's already "justified" or "present",
                                    // it means it was handled or not an absence.
                                    if ("absent".equalsIgnoreCase(document.getString("status"))) {
                                        batch.update(document.getReference(), "status", "justified");
                                    }
                                }
                            })
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Error updating attendance status in batch: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Error querying attendance for update: " + e.getMessage()));
    }


    // =================== GESTION DES COMPTES (ADMIN) ===================

    /**
     * Crée un nouveau compte utilisateur (étudiant ou enseignant) dans Firebase Auth
     * et sauvegarde son profil dans Firestore.
     * Le type d'utilisateur est déterminé par le 'role' fourni.
     * Un mot de passe temporaire est généré. L'utilisateur devra le réinitialiser.
     */
    public void createNewUserAccount(String email, String password, String role,
                                     Student studentProfile, Teacher teacherProfile,
                                     DataCallback<String> callback) {
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            callback.onFailure("Email ou mot de passe invalide.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        Log.d(TAG, "Auth user created: " + user.getEmail() + ", UID: " + user.getUid());

                        // Now save the profile to Firestore based on role
                        if ("student".equalsIgnoreCase(role) && studentProfile != null) {
                            studentProfile.setEmail(email); // Ensure email is set
                            studentProfile.setCreatedAt(Timestamp.now());
                            studentProfile.setLastUpdatedAt(Timestamp.now());
                            db.collection(STUDENTS_COLLECTION)
                                    .document(email) // Use email as doc ID
                                    .set(studentProfile.toMap())
                                    .addOnSuccessListener(aVoid -> callback.onSuccess("Compte étudiant créé avec succès!"))
                                    .addOnFailureListener(e -> {
                                        // If Firestore save fails, try to delete Auth user
                                        user.delete();
                                        callback.onFailure("Échec de la sauvegarde du profil étudiant: " + e.getMessage());
                                    });
                        } else if ("teacher".equalsIgnoreCase(role) && teacherProfile != null) {
                            teacherProfile.setEmail(email); // Ensure email is set
                            teacherProfile.setCreatedAt(Timestamp.now());
                            teacherProfile.setLastUpdatedAt(Timestamp.now());
                            db.collection(TEACHERS_COLLECTION)
                                    .document(email) // Use email as doc ID
                                    .set(teacherProfile.toMap())
                                    .addOnSuccessListener(aVoid -> callback.onSuccess("Compte enseignant créé avec succès!"))
                                    .addOnFailureListener(e -> {
                                        // If Firestore save fails, try to delete Auth user
                                        user.delete();
                                        callback.onFailure("Échec de la sauvegarde du profil enseignant: " + e.getMessage());
                                    });
                        } else {
                            // Role mismatch or profile missing, delete Auth user
                            user.delete();
                            callback.onFailure("Rôle invalide ou profil manquant pour la création du compte.");
                        }
                    } else {
                        callback.onFailure("Erreur inattendue lors de la création de l'utilisateur Auth.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Échec de la création du compte Auth: " + e.getMessage()));
    }

    public void deleteUserAccount(String email, String role, DataCallback<Void> callback) {
        String collectionPath = (role.equals("student")) ? "students" : "teachers";

        db.collection(collectionPath)
                .whereEqualTo("email", email) // Find the user document by email
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // FIX: Declare userDocument as DocumentSnapshot as getDocuments().get(0) returns DocumentSnapshot
                        DocumentSnapshot userDocument = queryDocumentSnapshots.getDocuments().get(0);
                        String userId = userDocument.getId(); // Get the Firestore document ID

                        // Delete the document
                        db.collection(collectionPath).document(userId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User document deleted from Firestore: " + email + " (ID: " + userId + ")");
                                    callback.onSuccess(aVoid);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting user document from Firestore: " + email, e);
                                    callback.onFailure("Erreur de suppression du document utilisateur: " + e.getMessage());
                                });
                    } else {
                        Log.w(TAG, "User document not found for deletion: " + email + " in collection: " + collectionPath);
                        callback.onFailure("Utilisateur non trouvé.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying for user document to delete: " + email, e);
                    callback.onFailure("Erreur de recherche de l'utilisateur pour suppression: " + e.getMessage());
                });
    }
    /**
     * Récupère tous les comptes étudiants.
     */
    public void getAllStudents(DataCallback<List<Student>> callback) {
        db.collection(STUDENTS_COLLECTION)
                .orderBy("fullName") // Order by name for display
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Student> students = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Student student = document.toObject(Student.class);
                            Log.d("FirebaseDebug", "Fetched Student: " + student.getEmail() + ", isActive: " + student.isActive());
                            students.add(student);
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing student document: " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(students);
                })
                .addOnFailureListener(e -> callback.onFailure("Erreur lors de la récupération des étudiants: " + e.getMessage()));
    }

    /**
     * Récupère tous les comptes enseignants.
     */
    public void getAllTeachers(DataCallback<List<Teacher>> callback) {
        db.collection(TEACHERS_COLLECTION)
                .orderBy("fullName") // Order by name for display
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Teacher> teachers = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Teacher teacher = document.toObject(Teacher.class);
                            Log.d("FirebaseDebug", "Fetched Teacher: " + teacher.getEmail() + ", isActive: " + teacher.isActive());
                            teachers.add(teacher);
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing teacher document: " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(teachers);
                })
                .addOnFailureListener(e -> callback.onFailure("Erreur lors de la récupération des enseignants: " + e.getMessage()));
    }

    /**
     * Met à jour le profil d'un étudiant.
     */
    public void updateStudentProfile(Student student, DataCallback<Void> callback) {
        if (student.getEmail() == null || student.getEmail().isEmpty()) {
            callback.onFailure("L'email de l'étudiant est manquant pour la mise à jour.");
            return;
        }
        db.collection(STUDENTS_COLLECTION)
                .document(student.getEmail())
                .update(student.toMap()) // Use toMap for specific field updates if needed
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Erreur lors de la mise à jour du profil étudiant: " + e.getMessage()));
    }

    /**
     * Met à jour le profil d'un enseignant.
     */
    public void updateTeacherProfile(Teacher teacher, DataCallback<Void> callback) {
        if (teacher.getEmail() == null || teacher.getEmail().isEmpty()) {
            callback.onFailure("L'email de l'enseignant est manquant pour la mise à jour.");
            return;
        }
        db.collection(TEACHERS_COLLECTION)
                .document(teacher.getEmail())
                .update(teacher.toMap()) // Use toMap for specific field updates if needed
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Erreur lors de la mise à jour du profil enseignant: " + e.getMessage()));
    }

    /**
     * Désactive/active un compte utilisateur dans Firestore (isActive field).
     * Note: La désactivation/réactivation dans Firebase Auth est complexe côté client (requiert ré-authentification).
     * Il est plus sûr de gérer l'accès via le champ isActive dans Firestore et des règles de sécurité.
     */
    public void setUserActiveStatus(String email, String role, boolean isActive, DataCallback<Void> callback) {
        String collectionPath;
        if ("student".equalsIgnoreCase(role)) {
            collectionPath = STUDENTS_COLLECTION;
        } else if ("teacher".equalsIgnoreCase(role)) {
            collectionPath = TEACHERS_COLLECTION;
        } else {
            callback.onFailure("Rôle non reconnu pour la mise à jour du statut d'activité.");
            return;
        }

        db.collection(collectionPath)
                .document(email)
                .update("isActive", isActive, "lastUpdatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + email + " isActive status set to " + isActive);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onFailure("Erreur lors de la mise à jour du statut d'activité: " + e.getMessage()));
    }


    // =====================================================================
    // --- NEW: Field (Filière) Management Methods ---
    // =====================================================================

    /**
     * Creates a new field (filière) document in Firestore.
     * @param field The Field object to save. Its fieldId will be used as the document ID.
     * @param callback Callback for success or failure.
     */

    // --- Field Operations (UPDATED to handle weeklySchedule) ---

    public void createField(Field field, DataCallback<Void> callback) {
        db.collection("fields").document(field.getFieldId())
                .set(field.toMap()) // Use toMap() which now includes weeklySchedule
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void modifyField(String fieldId, Map<String, Object> updates, DataCallback<Void> callback) {
        // IMPORTANT: If you pass the whole `Field` object via `toMap()` then `updates` should be `field.toMap()`
        // If you are selectively updating, ensure `weeklySchedule` updates are handled correctly (e.g., replace the whole list)
        db.collection("fields").document(fieldId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Modified to accept a Field object that includes the schedule
    public void modifyField(String fieldId, Field updatedField, DataCallback<Void> callback) {
        db.collection("fields").document(fieldId)
                .set(updatedField.toMap()) // Use set with merge if you want to only update provided fields, or full set for replacement
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Deletes a field.
     * @param fieldId The ID of the field to delete.
     * @param callback Callback for success/failure.
     */
    public void deleteField(String fieldId, DataCallback<Void> callback) { // THIS WAS MISSING OR HAD WRONG SIGNATURE
        db.collection(FIELDS_COLLECTION).document(fieldId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void getAllFields(DataCallback<List<Field>> callback) {
        db.collection("fields")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Field> fields = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Field field = document.toObject(Field.class);
                                fields.add(field);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing field document: " + document.getId(), e);
                            }
                        }
                        callback.onSuccess(fields);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    // --- Course Operations (UPDATED to handle courseScheduleEntry) ---

    public void createCourse(Course course, DataCallback<Void> callback) {
        db.collection("courses").document(course.getCourseId())
                .set(course.toMap()) // Use toMap() which now includes courseScheduleEntry
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Changed to accept a Course object for full replacement/update
    public void modifyCourse(String courseId, Map<String, Object> updates, DataCallback<Void> callback) {
        // If using map, ensure 'courseScheduleEntry' is passed as a nested map
        db.collection("courses").document(courseId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void modifyCourse(String courseId, Course updatedCourse, DataCallback<Void> callback) {
        db.collection("courses").document(courseId)
                .set(updatedCourse.toMap()) // Overwrite the document with the new course object
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void deleteCourse(String courseId, DataCallback<Void> callback) {
        db.collection("courses").document(courseId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getAllCourses(DataCallback<List<Course>> callback) {
        db.collection("courses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Course> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Course course = document.toObject(Course.class);
                                courses.add(course);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing course document: " + document.getId(), e);
                            }
                        }
                        callback.onSuccess(courses);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }
    /**
     * Assigns a course to a teacher using a transaction for atomicity.
     * Updates both the teacher's `assignedCourseIds` and the course's `teacherEmail`/`teacherName`/`department`.
     *
     * @param teacherEmail The email of the teacher.
     * @param courseId The ID of the course.
     * @param teacherName The name of the teacher.
     * @param department The department associated with the course.
     * @param callback Callback for success/failure.
     */
    public void assignCourseToTeacher(String teacherEmail, String courseId, String teacherName, String department, DataCallback<Void> callback) {
        DocumentReference teacherRef = db.collection(TEACHERS_COLLECTION).document(teacherEmail);
        DocumentReference courseRef = db.collection(COURSES_COLLECTION).document(courseId);

        db.runTransaction(transaction -> {
                    // Update teacher's assigned courses
                    transaction.update(teacherRef, "assignedCourseIds", FieldValue.arrayUnion(courseId));

                    // Update course with teacher info
                    Map<String, Object> courseUpdates = new HashMap<>();
                    courseUpdates.put("teacherEmail", teacherEmail);
                    courseUpdates.put("teacherName", teacherName);
                    courseUpdates.put("department", department);
                    transaction.update(courseRef, courseUpdates);

                    return null;
                })
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Unassigns a course from a teacher using a transaction for atomicity.
     * Updates both the teacher's `assignedCourseIds` and the course's `teacherEmail`/`teacherName`.
     *
     * @param teacherEmail The email of the teacher.
     * @param courseId The ID of the course.
     * @param callback Callback for success/failure.
     */
    public void unassignCourseFromTeacher(String teacherEmail, String courseId, DataCallback<Void> callback) {
        DocumentReference teacherRef = db.collection(TEACHERS_COLLECTION).document(teacherEmail);
        DocumentReference courseRef = db.collection(COURSES_COLLECTION).document(courseId);

        db.runTransaction(transaction -> {
                    // Remove course from teacher's assigned courses
                    transaction.update(teacherRef, "assignedCourseIds", FieldValue.arrayRemove(courseId));

                    // Clear teacher info from course
                    Map<String, Object> courseUpdates = new HashMap<>();
                    courseUpdates.put("teacherEmail", null);
                    courseUpdates.put("teacherName", null);
                    transaction.update(courseRef, courseUpdates);

                    return null;
                })
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Assigns a field to a teacher.
     * @param teacherEmail The email of the teacher.
     * @param fieldId The ID of the field.
     * @param callback Callback for success/failure.
     */
    public void assignFieldToTeacher(String teacherEmail, String fieldId, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION).document(teacherEmail)
                .update("assignedFieldIds", FieldValue.arrayUnion(fieldId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Unassigns a field from a teacher.
     * @param teacherEmail The email of the teacher.
     * @param fieldId The ID of the field.
     * @param callback Callback for success/failure.
     */
    public void unassignFieldFromTeacher(String teacherEmail, String fieldId, DataCallback<Void> callback) {
        db.collection(TEACHERS_COLLECTION).document(teacherEmail)
                .update("assignedFieldIds", FieldValue.arrayRemove(fieldId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // =================== MÉTHODES UTILITAIRES ===================

    /**
     * Vérifier la connexion à Firebase
     */
    public boolean isFirebaseConnected() {
        return mAuth != null && db != null && storage != null;
    }

    /**
     * Obtenir l'email de l'utilisateur connecté
     */
    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
}