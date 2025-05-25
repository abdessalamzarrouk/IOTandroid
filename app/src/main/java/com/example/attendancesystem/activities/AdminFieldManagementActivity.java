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
import com.example.attendancesystem.dialogs.CreateEditFieldDialog;
import com.example.attendancesystem.models.Field;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.AdminFieldAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminFieldManagementActivity extends AppCompatActivity implements
        AdminFieldAdapter.OnFieldActionListener,
        CreateEditFieldDialog.OnFieldCreatedEditedListener {

    private static final String TAG = "AdminFieldManagement";

    private FirebaseManager firebaseManager;
    private RecyclerView rvFields;
    private AdminFieldAdapter adapter;
    private List<Field> fieldList;
    private ProgressBar progressBar;
    private Button btnAddNewField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_field_management);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Filières");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        fieldList = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadFields();
    }

    private void initViews() {
        rvFields = findViewById(R.id.rv_fields);
        progressBar = findViewById(R.id.progress_bar_field_management);
        btnAddNewField = findViewById(R.id.btn_add_new_field);
    }

    private void setupRecyclerView() {
        adapter = new AdminFieldAdapter(fieldList, this);
        rvFields.setLayoutManager(new LinearLayoutManager(this));
        rvFields.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAddNewField.setOnClickListener(v -> showCreateEditFieldDialog(null));
    }

    private void loadFields() {
        showLoading(true);
        firebaseManager.getAllFields(new FirebaseManager.DataCallback<List<Field>>() {
            @Override
            public void onSuccess(List<Field> fields) {
                fieldList.clear();
                fieldList.addAll(fields);
                adapter.notifyDataSetChanged();
                showLoading(false);
                Log.d(TAG, "Loaded " + fields.size() + " fields.");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminFieldManagementActivity.this, "Erreur de chargement des filières: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading fields: " + error);
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvFields.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // --- AdminFieldAdapter.OnFieldActionListener Callbacks ---

    @Override
    public void onEditClick(Field field) {
        showCreateEditFieldDialog(field);
    }

    @Override
    public void onDeleteClick(Field field) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la Suppression")
                .setMessage("Êtes-vous sûr de vouloir supprimer la filière '" + field.getFieldName() + "' ?")
                .setPositiveButton("Supprimer", (dialog, which) -> deleteField(field.getFieldId()))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteField(String fieldId) {
        firebaseManager.deleteField(fieldId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AdminFieldManagementActivity.this, "Filière supprimée avec succès.", Toast.LENGTH_SHORT).show();
                loadFields(); // Reload list
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminFieldManagementActivity.this, "Erreur de suppression de la filière: " + error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to delete field: " + error);
            }
        });
    }

    // --- CreateEditFieldDialog.OnFieldCreatedEditedListener Callback ---
    @Override
    public void onFieldCreatedEdited() {
        loadFields(); // Reload list after creation/edit
        Utils.showToast(this, "Liste des filières mise à jour.");
    }

    // --- Dialog helper method ---
    private void showCreateEditFieldDialog(Field fieldToEdit) {
        CreateEditFieldDialog dialog = CreateEditFieldDialog.newInstance(fieldToEdit);
        dialog.setOnFieldCreatedEditedListener(this);
        dialog.show(getSupportFragmentManager(), "CreateEditFieldDialog");
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
            loadFields();
            Utils.showToast(this, "Données actualisées");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}