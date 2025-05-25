package com.example.attendancesystem.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Course;
import com.example.attendancesystem.models.Field; // To get list of fields
import com.example.attendancesystem.services.FirebaseManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // For generating courseId

public class CreateEditCourseDialog extends DialogFragment {

    private static final String ARG_COURSE = "course_to_edit";

    private EditText etCourseName, etDepartment;
    private Spinner spinnerField, spinnerTargetYears;
    private Button btnSave;
    private FirebaseManager firebaseManager;
    private Course courseToEdit;
    private List<Field> allFields;
    private List<String> fieldNames;
    private List<String> targetYearsOptions;

    public interface OnCourseCreatedEditedListener {
        void onCourseCreatedEdited();
    }

    private OnCourseCreatedEditedListener listener;

    public void setOnCourseCreatedEditedListener(OnCourseCreatedEditedListener listener) {
        this.listener = listener;
    }

    public static CreateEditCourseDialog newInstance(Course course) {
        CreateEditCourseDialog dialog = new CreateEditCourseDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_COURSE, course);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseToEdit = (Course) getArguments().getSerializable(ARG_COURSE);
        }
        firebaseManager = FirebaseManager.getInstance();
        allFields = new ArrayList<>();
        fieldNames = new ArrayList<>();
        // Example target years, fetch from config/Firebase if dynamic
        targetYearsOptions = Arrays.asList("1ère Année", "2ème Année", "3ème Année", "4ème Année", "5ème Année");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_edit_course, null);

        etCourseName = view.findViewById(R.id.et_course_name);
        etDepartment = view.findViewById(R.id.et_course_department);
        spinnerField = view.findViewById(R.id.spinner_course_field);
        spinnerTargetYears = view.findViewById(R.id.spinner_course_target_years); // If you want multi-select, this will be more complex
        btnSave = view.findViewById(R.id.btn_save_course);

        // Setup spinners
        setupFieldSpinner();
        setupTargetYearsSpinner();

        if (courseToEdit != null) {
            etCourseName.setText(courseToEdit.getCourseName());
            etDepartment.setText(courseToEdit.getDepartment());

            // Set selected field
            int fieldIndex = fieldNames.indexOf(courseToEdit.getField());
            if (fieldIndex != -1) {
                spinnerField.setSelection(fieldIndex);
            }

            // Set selected target years (assuming single select for simplicity, or first year if multi-select)
            if (courseToEdit.getTargetYears() != null && !courseToEdit.getTargetYears().isEmpty()) {
                int yearIndex = targetYearsOptions.indexOf(courseToEdit.getTargetYears().get(0));
                if (yearIndex != -1) {
                    spinnerTargetYears.setSelection(yearIndex);
                }
            }
            builder.setTitle("Modifier le Cours");
        } else {
            builder.setTitle("Créer un Nouveau Cours");
        }

        btnSave.setOnClickListener(v -> saveCourse());

        builder.setView(view)
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void setupFieldSpinner() {
        firebaseManager.getAllFields(new FirebaseManager.DataCallback<List<Field>>() {
            @Override
            public void onSuccess(List<Field> fields) {
                allFields = fields;
                fieldNames.clear();
                for (Field field : fields) {
                    fieldNames.add(field.getFieldName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, fieldNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerField.setAdapter(adapter);

                // If editing, re-select the field after adapter is set
                if (courseToEdit != null && courseToEdit.getField() != null) {
                    int fieldIndex = fieldNames.indexOf(courseToEdit.getField());
                    if (fieldIndex != -1) {
                        spinnerField.setSelection(fieldIndex);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Erreur de chargement des filières: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTargetYearsSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, targetYearsOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTargetYears.setAdapter(adapter);
    }

    private void saveCourse() {
        String courseName = etCourseName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String selectedField = (String) spinnerField.getSelectedItem();
        String selectedTargetYear = (String) spinnerTargetYears.getSelectedItem(); // Assuming single select

        if (TextUtils.isEmpty(courseName) || TextUtils.isEmpty(department) || TextUtils.isEmpty(selectedField) || TextUtils.isEmpty(selectedTargetYear)) {
            Toast.makeText(getContext(), "Tous les champs sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> targetYearsList = new ArrayList<>();
        targetYearsList.add(selectedTargetYear); // For single select

        if (courseToEdit == null) {
            // Create new course
            String newCourseId = UUID.randomUUID().toString();
            Course newCourse = new Course(newCourseId, courseName, department, selectedField, targetYearsList);

            firebaseManager.createCourse(newCourse, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Cours créé avec succès !", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onCourseCreatedEdited();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Erreur de création du cours: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Update existing course
            Map<String, Object> updates = new HashMap<>();
            updates.put("courseName", courseName);
            updates.put("department", department);
            updates.put("field", selectedField);
            updates.put("targetYears", targetYearsList);

            firebaseManager.modifyCourse(courseToEdit.getCourseId(), updates, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Cours modifié avec succès !", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onCourseCreatedEdited();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Erreur de modification du cours: " + error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}