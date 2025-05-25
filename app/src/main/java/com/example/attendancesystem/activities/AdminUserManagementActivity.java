package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog; // Import for AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.dialogs.CreateEditUserDialog;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.AdminUserAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AdminUserManagementActivity extends AppCompatActivity implements
        AdminUserAdapter.OnUserActionListener,
        CreateEditUserDialog.OnUserCreatedEditedListener {

    private static final String TAG = "AdminUserManagementAct";

    private FirebaseManager firebaseManager;
    private RecyclerView rvUsers;
    private AdminUserAdapter adapter;
    private List<Object> allUsers;
    private List<Object> displayedUsers;
    private ProgressBar progressBar;
    private Spinner userTypeFilterSpinner;
    private Button btnAddNewUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Comptes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        allUsers = new ArrayList<>();
        displayedUsers = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setupFilterSpinner();
        setupListeners();
        loadAllUsers();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);
        progressBar = findViewById(R.id.progress_bar_user_management);
        userTypeFilterSpinner = findViewById(R.id.spinner_user_type_filter);
        btnAddNewUser = findViewById(R.id.btn_add_new_user);
    }

    private void setupRecyclerView() {
        adapter = new AdminUserAdapter(displayedUsers, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.user_types_filter,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeFilterSpinner.setAdapter(spinnerAdapter);

        userTypeFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterUsers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupListeners() {
        btnAddNewUser.setOnClickListener(v -> showCreateEditUserDialog(null));
    }

    private void loadAllUsers() {
        showLoading(true);
        allUsers.clear();

        firebaseManager.getAllStudents(new FirebaseManager.DataCallback<List<Student>>() {
            @Override
            public void onSuccess(List<Student> students) {
                allUsers.addAll(students);
                firebaseManager.getAllTeachers(new FirebaseManager.DataCallback<List<Teacher>>() {
                    @Override
                    public void onSuccess(List<Teacher> teachers) {
                        allUsers.addAll(teachers);
                        filterUsers();
                        showLoading(false);
                        Log.d(TAG, "Loaded " + allUsers.size() + " total users.");
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(AdminUserManagementActivity.this, "Erreur de chargement des enseignants: " + error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erreur de chargement des enseignants: " + error);
                        showLoading(false);
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminUserManagementActivity.this, "Erreur de chargement des étudiants: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Erreur de chargement des étudiants: " + error);
                showLoading(false);
            }
        });
    }

    private void filterUsers() {
        String selectedType = userTypeFilterSpinner.getSelectedItem().toString();
        displayedUsers.clear();

        for (Object user : allUsers) {
            if (selectedType.equals("Tous")) {
                displayedUsers.add(user);
            } else if (selectedType.equals("Étudiants") && user instanceof Student) {
                displayedUsers.add(user);
            } else if (selectedType.equals("Enseignants") && user instanceof Teacher) {
                displayedUsers.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // --- AdminUserAdapter.OnUserActionListener Callbacks ---

    @Override
    public void onEditClick(Object user) {
        showCreateEditUserDialog(user);
    }

    @Override
    public void onToggleStatusClick(Object user) {
        final String email;
        final String role;
        final boolean currentStatus;

        if (user instanceof Student) {
            Student student = (Student) user;
            email = student.getEmail();
            role = "student";
            currentStatus = student.isActive();
        } else if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            email = teacher.getEmail();
            role = "teacher";
            currentStatus = teacher.isActive();
        } else {
            Toast.makeText(this, "Type d'utilisateur inconnu.", Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean newStatus = !currentStatus;

        firebaseManager.setUserActiveStatus(email, role, newStatus, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                String statusText = newStatus ? "activé" : "désactivé";
                Toast.makeText(AdminUserManagementActivity.this, "Compte " + email + " " + statusText, Toast.LENGTH_SHORT).show();
                loadAllUsers();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminUserManagementActivity.this, "Erreur de mise à jour du statut: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRemoveClick(Object user) {
        final String email;
        final String role;

        if (user instanceof Student) {
            Student student = (Student) user;
            email = student.getEmail();
            role = "student";
        } else if (user instanceof Teacher) {
            Teacher teacher = (Teacher) user;
            email = teacher.getEmail();
            role = "teacher";
        } else {
            Toast.makeText(this, "Type d'utilisateur inconnu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a confirmation dialog before permanent deletion
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la Suppression")
                .setMessage("Êtes-vous sûr de vouloir supprimer le compte de " + email + " ? Cette action est irréversible.")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    deleteUser(email, role); // Proceed with deletion if confirmed
                })
                .setNegativeButton("Annuler", null) // Do nothing on cancel
                .show();
    }

    private void deleteUser(String email, String role) {
        firebaseManager.deleteUserAccount(email, role, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AdminUserManagementActivity.this, "Compte " + email + " supprimé avec succès.", Toast.LENGTH_SHORT).show();
                loadAllUsers(); // Reload the list to update UI
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminUserManagementActivity.this, "Erreur lors de la suppression du compte: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to delete user account: " + error);
            }
        });
    }

    // --- CreateEditUserDialog.OnUserCreatedEditedListener Callback ---
    @Override
    public void onUserCreatedEdited() {
        loadAllUsers();
        Utils.showToast(this, "Liste des utilisateurs mise à jour.");
    }

    // --- Dialog helper method ---
    private void showCreateEditUserDialog(Object userToEdit) {
        CreateEditUserDialog dialog = CreateEditUserDialog.newInstance(userToEdit);
        dialog.setOnUserCreatedEditedListener(this);
        dialog.show(getSupportFragmentManager(), "CreateEditUserDialog");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}