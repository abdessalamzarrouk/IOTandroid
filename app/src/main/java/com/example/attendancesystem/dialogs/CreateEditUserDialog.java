package com.example.attendancesystem.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.google.firebase.Timestamp;
import com.google.gson.Gson; // IMPORTANT: Add this import

public class CreateEditUserDialog extends DialogFragment {

    private FirebaseManager firebaseManager;
    private Object userToEdit; // This will be either Student or Teacher object if editing

    private EditText etEmail, etPassword, etFullName, etId, etDepartment, etPhoneNumber, etField, etYear;
    private Spinner spinnerUserType;
    private LinearLayout layoutStudentFields, layoutTeacherFields;
    private Button btnSave;
    private ProgressBar progressBar;
    private TextView tvDialogTitle;

    public interface OnUserCreatedEditedListener {
        void onUserCreatedEdited();
    }

    private OnUserCreatedEditedListener listener;

    public void setOnUserCreatedEditedListener(OnUserCreatedEditedListener listener) {
        this.listener = listener;
    }

    // --- STATIC FACTORY METHOD ---
    private static final String ARG_USER_TO_EDIT_JSON = "user_to_edit_json"; // Key for argument
    private static final String ARG_USER_TYPE_CLASS = "user_type_class"; // Key for argument

    /**
     * Factory method to create a new instance of the dialog,
     * optionally providing a user object to edit.
     *
     * @param userToEdit An existing Student or Teacher object if editing, null if creating a new user.
     * @return A new instance of CreateEditUserDialog.
     */
    public static CreateEditUserDialog newInstance(@Nullable Object userToEdit) {
        CreateEditUserDialog dialog = new CreateEditUserDialog();
        Bundle args = new Bundle();

        if (userToEdit != null) {
            Gson gson = new Gson();
            args.putString(ARG_USER_TO_EDIT_JSON, gson.toJson(userToEdit));

            // Pass the class name to help re-create the correct object type later
            if (userToEdit instanceof Student) {
                args.putString(ARG_USER_TYPE_CLASS, Student.class.getName());
            } else if (userToEdit instanceof Teacher) {
                args.putString(ARG_USER_TYPE_CLASS, Teacher.class.getName());
            }
        }
        dialog.setArguments(args);
        return dialog;
    }
    // --- END STATIC FACTORY METHOD ---

    // Default empty constructor (REQUIRED for Fragment recreation by Android system)
    public CreateEditUserDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve arguments here, when the Fragment is being created
        if (getArguments() != null) {
            String userJson = getArguments().getString(ARG_USER_TO_EDIT_JSON);
            String userTypeClassName = getArguments().getString(ARG_USER_TYPE_CLASS);

            if (userJson != null && userTypeClassName != null) {
                Gson gson = new Gson();
                try {
                    // Use Class.forName to get the actual class type from the string name
                    Class<?> userClass = Class.forName(userTypeClassName);
                    userToEdit = gson.fromJson(userJson, userClass);
                } catch (ClassNotFoundException e) {
                    // This should ideally not happen if userTypeClassName is correctly passed
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Erreur: type d'utilisateur inconnu.", Toast.LENGTH_SHORT).show();
                    userToEdit = null; // Ensure it's null if type cannot be resolved
                }
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_edit_user, null);

        firebaseManager = FirebaseManager.getInstance();

        initViews(view);
        setupSpinner();
        setupListeners();
        populateFieldsForEdit(); // Populate fields if userToEdit is not null

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View view) {
        tvDialogTitle = view.findViewById(R.id.tv_dialog_title);
        etEmail = view.findViewById(R.id.et_user_email);
        etPassword = view.findViewById(R.id.et_user_password);
        etFullName = view.findViewById(R.id.et_user_full_name);
        etId = view.findViewById(R.id.et_user_id); // Student ID or Employee ID
        etDepartment = view.findViewById(R.id.et_user_department);
        etPhoneNumber = view.findViewById(R.id.et_user_phone_number);
        etField = view.findViewById(R.id.et_student_field);
        etYear = view.findViewById(R.id.et_student_year);
        spinnerUserType = view.findViewById(R.id.spinner_dialog_user_type);
        layoutStudentFields = view.findViewById(R.id.layout_student_specific_fields);
        layoutTeacherFields = view.findViewById(R.id.layout_teacher_specific_fields);
        btnSave = view.findViewById(R.id.btn_save_user);
        progressBar = view.findViewById(R.id.progress_bar_dialog);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.create_user_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                if (selectedType.equals("Étudiant")) {
                    layoutStudentFields.setVisibility(View.VISIBLE);
                    layoutTeacherFields.setVisibility(View.GONE);
                    etId.setHint("ID Étudiant");
                } else if (selectedType.equals("Enseignant")) {
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.VISIBLE);
                    etId.setHint("ID Employé");
                } else { // "Sélectionner le type" or any other default
                    layoutStudentFields.setVisibility(View.GONE);
                    layoutTeacherFields.setVisibility(View.GONE);
                    etId.setHint("ID"); // General hint
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveUser());
    }

    private void populateFieldsForEdit() {
        if (userToEdit != null) {
            tvDialogTitle.setText("Modifier Compte Utilisateur");
            etEmail.setEnabled(false); // Email cannot be changed for existing Firebase Auth users
            etPassword.setVisibility(View.GONE); // Password not directly editable here
            spinnerUserType.setEnabled(false); // User type cannot be changed

            if (userToEdit instanceof Student) {
                Student student = (Student) userToEdit;
                // Find index of "Étudiant"
                int studentIndex = ((ArrayAdapter) spinnerUserType.getAdapter()).getPosition("Étudiant");
                if (studentIndex >= 0) {
                    spinnerUserType.setSelection(studentIndex);
                }
                etEmail.setText(student.getEmail());
                etFullName.setText(student.getFullName());
                etId.setText(student.getStudentId());
                etDepartment.setText(student.getDepartment());
                etPhoneNumber.setText(student.getPhoneNumber());
                etField.setText(student.getField());
                etYear.setText(student.getYear());
                layoutStudentFields.setVisibility(View.VISIBLE); // Ensure student fields are visible
                layoutTeacherFields.setVisibility(View.GONE); // Ensure teacher fields are hidden
            } else if (userToEdit instanceof Teacher) {
                Teacher teacher = (Teacher) userToEdit;
                // Find index of "Enseignant"
                int teacherIndex = ((ArrayAdapter) spinnerUserType.getAdapter()).getPosition("Enseignant");
                if (teacherIndex >= 0) {
                    spinnerUserType.setSelection(teacherIndex);
                }
                etEmail.setText(teacher.getEmail());
                etFullName.setText(teacher.getFullName());
                etId.setText(teacher.getEmployeeId());
                etDepartment.setText(teacher.getDepartment());
                etPhoneNumber.setText(teacher.getPhoneNumber());
                layoutStudentFields.setVisibility(View.GONE); // Ensure student fields are hidden
                layoutTeacherFields.setVisibility(View.VISIBLE); // Ensure teacher fields are visible
            }
        } else {
            tvDialogTitle.setText("Créer Nouveau Compte Utilisateur");
            etPassword.setVisibility(View.VISIBLE);
            // Ensure fields are reset if creating a new user (prevents pre-filling from previous edit)
            etEmail.setText("");
            etFullName.setText("");
            etId.setText("");
            etDepartment.setText("");
            etPhoneNumber.setText("");
            etField.setText("");
            etYear.setText("");
            spinnerUserType.setSelection(0); // "Sélectionner le type"
        }
    }

    private void saveUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String id = etId.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String selectedType = spinnerUserType.getSelectedItem().toString();

        if (email.isEmpty() || fullName.isEmpty() || id.isEmpty() || department.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez remplir tous les champs obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userToEdit == null && password.isEmpty()) { // Only require password for new user
            Toast.makeText(getContext(), "Veuillez définir un mot de passe pour le nouveau compte.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedType.equals("Sélectionner le type")) {
            Toast.makeText(getContext(), "Veuillez sélectionner un type d'utilisateur.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        if (userToEdit == null) { // Create new user
            if (selectedType.equals("Étudiant")) {
                String field = etField.getText().toString().trim();
                String year = etYear.getText().toString().trim();
                if (field.isEmpty() || year.isEmpty()) {
                    Toast.makeText(getContext(), "Veuillez remplir les champs spécifiques de l'étudiant.", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    return;
                }
                Student newStudent = new Student(email, fullName, id, department, field, year, phoneNumber);
                firebaseManager.createNewUserAccount(email, password, "student", newStudent, null, new FirebaseManager.DataCallback<String>() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        if (listener != null) listener.onUserCreatedEdited();
                        dismiss();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Erreur création étudiant: " + error, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
            } else if (selectedType.equals("Enseignant")) {
                Teacher newTeacher = new Teacher(email, fullName, id, department, phoneNumber);
                firebaseManager.createNewUserAccount(email, password, "teacher", null, newTeacher, new FirebaseManager.DataCallback<String>() {
                    @Override
                    public void onSuccess(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        if (listener != null) listener.onUserCreatedEdited();
                        dismiss();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Erreur création enseignant: " + error, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
            }
        } else { // Edit existing user
            // The type of user (Student/Teacher) should not change during edit.
            // We use the already determined type from userToEdit
            if (userToEdit instanceof Student) {
                Student student = (Student) userToEdit;
                String field = etField.getText().toString().trim();
                String year = etYear.getText().toString().trim();

                if (field.isEmpty() || year.isEmpty()) { // Specific validation for student edit
                    Toast.makeText(getContext(), "Veuillez remplir les champs spécifiques de l'étudiant.", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    return;
                }

                // Update fields
                student.setFullName(fullName);
                student.setStudentId(id);
                student.setDepartment(department);
                student.setPhoneNumber(phoneNumber);
                student.setField(field);
                student.setYear(year);
                student.setLastUpdatedAt(Timestamp.now());

                firebaseManager.updateStudentProfile(student, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "Profil étudiant mis à jour.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        if (listener != null) listener.onUserCreatedEdited();
                        dismiss();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Erreur mise à jour étudiant: " + error, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
            } else if (userToEdit instanceof Teacher) {
                Teacher teacher = (Teacher) userToEdit;

                // Update fields
                teacher.setFullName(fullName);
                teacher.setEmployeeId(id);
                teacher.setDepartment(department);
                teacher.setPhoneNumber(phoneNumber);
                teacher.setLastUpdatedAt(Timestamp.now());

                firebaseManager.updateTeacherProfile(teacher, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getContext(), "Profil enseignant mis à jour.", Toast.LENGTH_SHORT).show();
                        showLoading(false);
                        if (listener != null) listener.onUserCreatedEdited();
                        dismiss();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(getContext(), "Erreur mise à jour enseignant: " + error, Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                });
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        // Enable/disable email and spinner based on whether it's a new user or an edit
        etEmail.setEnabled(!show && userToEdit == null);
        spinnerUserType.setEnabled(!show && userToEdit == null);

        // Also disable/enable other input fields when loading
        etFullName.setEnabled(!show);
        etId.setEnabled(!show);
        etDepartment.setEnabled(!show);
        etPhoneNumber.setEnabled(!show);
        etField.setEnabled(!show);
        etYear.setEnabled(!show);
        if (userToEdit == null) { // Only enable password field for new user creation
            etPassword.setEnabled(!show);
        }
    }
}