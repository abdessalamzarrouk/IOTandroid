package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.dialogs.AssignCourseToTeacherDialog;
import com.example.attendancesystem.dialogs.CreateEditCourseDialog;
import com.example.attendancesystem.models.Course;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.AdminCourseAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCourseManagementActivity extends AppCompatActivity implements
        AdminCourseAdapter.OnCourseActionListener,
        CreateEditCourseDialog.OnCourseCreatedEditedListener,
        AssignCourseToTeacherDialog.OnCourseAssignedUnassignedListener {

    private static final String TAG = "AdminCourseManagement";

    private FirebaseManager firebaseManager;
    private RecyclerView rvCourses;
    private AdminCourseAdapter adapter;
    private List<Course> courseList;
    private ProgressBar progressBar;
    private Button btnAddNewCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_course_management);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Cours");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        courseList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCourses();
    }

    private void initViews() {
        rvCourses = findViewById(R.id.rv_courses);
        progressBar = findViewById(R.id.progress_bar_course_management);
        btnAddNewCourse = findViewById(R.id.btn_add_new_course);
    }

    private void setupRecyclerView() {
        adapter = new AdminCourseAdapter(courseList, this);
        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        rvCourses.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddNewCourse.setOnClickListener(v -> showCreateEditCourseDialog(null));
    }

    private void loadCourses() {
        showLoading(true);
        firebaseManager.getAllCourses(new FirebaseManager.DataCallback<List<Course>>() {
            @Override
            public void onSuccess(List<Course> courses) {
                courseList.clear();
                courseList.addAll(courses);
                adapter.notifyDataSetChanged();
                showLoading(false);
                Log.d(TAG, "Loaded " + courses.size() + " courses.");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminCourseManagementActivity.this, "Erreur de chargement des cours: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading courses: " + error);
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvCourses.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // --- AdminCourseAdapter.OnCourseActionListener Callbacks ---

    @Override
    public void onEditClick(Course course) {
        showCreateEditCourseDialog(course);
    }

    @Override
    public void onDeleteClick(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la Suppression")
                .setMessage("Êtes-vous sûr de vouloir supprimer le cours '" + course.getCourseName() + "' ?")
                .setPositiveButton("Supprimer", (dialog, which) -> deleteCourse(course.getCourseId()))
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onAssignTeacherClick(Course course) {
        // Open dialog to select a teacher and assign the course
        AssignCourseToTeacherDialog dialog = AssignCourseToTeacherDialog.newInstance(course.getCourseId(), course.getCourseName(), course.getTeacherEmail());
        dialog.setOnCourseAssignedUnassignedListener(this);
        dialog.show(getSupportFragmentManager(), "AssignTeacherDialog");
    }

    private void deleteCourse(String courseId) {
        firebaseManager.deleteCourse(courseId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AdminCourseManagementActivity.this, "Cours supprimé avec succès.", Toast.LENGTH_SHORT).show();
                loadCourses(); // Reload list
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminCourseManagementActivity.this, "Erreur de suppression du cours: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // --- CreateEditCourseDialog.OnCourseCreatedEditedListener Callback ---
    @Override
    public void onCourseCreatedEdited() {
        loadCourses(); // Reload list after creation/edit
        Utils.showToast(this, "Liste des cours mise à jour.");
    }

    // --- AssignCourseToTeacherDialog.OnCourseAssignedUnassignedListener Callback ---
    @Override
    public void onCourseAssignedUnassigned() {
        loadCourses(); // Reload list after assignment/unassignment
        Utils.showToast(this, "Affectation du cours mise à jour.");
    }

    // --- Dialog helper methods ---
    private void showCreateEditCourseDialog(Course courseToEdit) {
        CreateEditCourseDialog dialog = CreateEditCourseDialog.newInstance(courseToEdit);
        dialog.setOnCourseCreatedEditedListener(this);
        dialog.show(getSupportFragmentManager(), "CreateEditCourseDialog");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.refresh_menu, menu); // Assuming you have a refresh icon
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadCourses();
            Utils.showToast(this, "Données actualisées");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}