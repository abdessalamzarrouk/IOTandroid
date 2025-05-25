package com.example.attendancesystem.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Course;
import com.example.attendancesystem.models.Field;
import com.example.attendancesystem.models.ScheduleEntry;
import com.example.attendancesystem.services.FirebaseManager;
import com.google.firebase.Timestamp; // Import Firebase Timestamp for createdAt field

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap; // For .toMap() if you use it for ScheduleEntry
import java.util.List;
import java.util.UUID;
import java.util.Map; // For .toMap() if you use it for ScheduleEntry

public class CreateEditCourseDialog extends DialogFragment {

    private static final String ARG_COURSE = "course_to_edit";
    private static final String TAG = "CreateEditCourseDialog";

    private EditText etCourseName, etDepartment;
    private Spinner spinnerField, spinnerCourseScheduleEntry;
    private LinearLayout targetYearsCheckboxContainer;
    private TextView tvSelectedScheduleDetails;
    private Button btnSave;

    private FirebaseManager firebaseManager;
    private Course courseToEdit;
    private List<Field> allFields; // List of all fields loaded from Firebase
    private List<String> fieldNames; // Names for the field spinner
    private List<String> targetYearsOptions; // Fixed list of target years

    private List<ScheduleEntry> currentFieldScheduleEntries; // The schedule entries for the CURRENTLY selected field

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
        currentFieldScheduleEntries = new ArrayList<>();
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
        targetYearsCheckboxContainer = view.findViewById(R.id.target_years_checkbox_container);
        spinnerCourseScheduleEntry = view.findViewById(R.id.spinner_course_schedule_entry);
        tvSelectedScheduleDetails = view.findViewById(R.id.tv_selected_schedule_details);
        btnSave = view.findViewById(R.id.btn_save_course);

        // Setup dynamic checkboxes for target years
        setupTargetYearsCheckboxes();

        // Load fields for spinner (this will also trigger initial setup for schedule spinner)
        setupFieldSpinner();

        // Listener for field selection to populate schedule entries spinner
        spinnerField.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Adjust for the "Sélectionner une Filière" hint at position 0
                if (position > 0 && position <= allFields.size()) {
                    Field selectedField = allFields.get(position - 1); // Get actual Field object
                    populateScheduleEntrySpinner(selectedField.getWeeklySchedule());
                    etDepartment.setText(selectedField.getDepartment()); // Auto-fill department based on field
                } else {
                    populateScheduleEntrySpinner(null); // Clear if hint or invalid selection
                    etDepartment.setText(""); // Clear department if no field selected
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                populateScheduleEntrySpinner(null); // Clear schedule entries if nothing selected
                etDepartment.setText("");
            }
        });

        // Listener for schedule entry selection to update details TextView
        spinnerCourseScheduleEntry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Adjust for the "Sélectionner une Plage Horaire" hint at position 0
                if (position > 0 && position <= currentFieldScheduleEntries.size()) {
                    ScheduleEntry selectedEntry = currentFieldScheduleEntries.get(position - 1);
                    tvSelectedScheduleDetails.setText("Jour: " + selectedEntry.getDayOfWeek() + "\n" +
                            "Heure: " + selectedEntry.getStartTime() + " - " + selectedEntry.getEndTime() + "\n" +
                            "Salle: " + (TextUtils.isEmpty(selectedEntry.getRoom()) ? "N/A" : selectedEntry.getRoom()));
                } else {
                    tvSelectedScheduleDetails.setText("Détails de la plage horaire sélectionnée: (Non sélectionné)");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvSelectedScheduleDetails.setText("Détails de la plage horaire sélectionnée: (Non sélectionné)");
            }
        });


        if (courseToEdit != null) {
            etCourseName.setText(courseToEdit.getCourseName());
            etDepartment.setText(courseToEdit.getDepartment()); // This will be overwritten by field selection if you enable auto-fill
            builder.setTitle("Modifier le Cours");

            // Populate target years checkboxes
            if (courseToEdit.getTargetYears() != null) {
                for (int i = 0; i < targetYearsCheckboxContainer.getChildCount(); i++) {
                    View child = targetYearsCheckboxContainer.getChildAt(i);
                    if (child instanceof CheckBox) {
                        CheckBox cb = (CheckBox) child;
                        if (courseToEdit.getTargetYears().contains(cb.getText().toString())) {
                            cb.setChecked(true);
                        }
                    }
                }
            }
        } else {
            builder.setTitle("Créer un Nouveau Cours");
        }

        btnSave.setOnClickListener(v -> saveCourse());

        builder.setView(view)
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void setupTargetYearsCheckboxes() {
        targetYearsCheckboxContainer.removeAllViews();
        for (String year : targetYearsOptions) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(year);
            targetYearsCheckboxContainer.addView(cb);
        }
    }

    private void setupFieldSpinner() {
        firebaseManager.getAllFields(new FirebaseManager.DataCallback<List<Field>>() {
            @Override
            public void onSuccess(List<Field> fields) {
                allFields = fields;
                fieldNames.clear();
                fieldNames.add("Sélectionner une Filière"); // Hint
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
                        // Use .post() to ensure the listener runs AFTER the adapter is fully set and ready
                        spinnerField.post(() -> {
                            spinnerField.setSelection(fieldIndex);

                            // The spinnerField.setOnItemSelectedListener should now trigger
                            // and populate currentFieldScheduleEntries.
                            // We need to wait for that to happen before setting schedule selection.
                            // A slight delay or checking `currentFieldScheduleEntries` size might be needed,
                            // but often `post()` is sufficient if the listener is quick.

                            if (courseToEdit.getCourseScheduleEntry() != null) {
                                // Find the index of the course's schedule entry in the newly populated list
                                int scheduleIndex = -1;
                                for (int i = 0; i < currentFieldScheduleEntries.size(); i++) {
                                    if (currentFieldScheduleEntries.get(i).equals(courseToEdit.getCourseScheduleEntry())) {
                                        scheduleIndex = i;
                                        break;
                                    }
                                }
                                if (scheduleIndex != -1) {
                                    spinnerCourseScheduleEntry.setSelection(scheduleIndex + 1); // +1 for hint
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Erreur de chargement des filières: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading fields: " + error);
            }
        });
    }

    private void populateScheduleEntrySpinner(@Nullable List<ScheduleEntry> scheduleEntries) {
        currentFieldScheduleEntries.clear();
        List<String> scheduleDisplayNames = new ArrayList<>();
        scheduleDisplayNames.add("Sélectionner une Plage Horaire"); // Default hint

        if (scheduleEntries != null) {
            currentFieldScheduleEntries.addAll(scheduleEntries);
            for (ScheduleEntry entry : scheduleEntries) {
                scheduleDisplayNames.add(entry.toDisplayString()); // *** CHANGED: Use toDisplayString() ***
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, scheduleDisplayNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseScheduleEntry.setAdapter(adapter);

        // Reset details display
        tvSelectedScheduleDetails.setText("Détails de la plage horaire sélectionnée: (Non sélectionné)");
    }

    private void saveCourse() {
        String courseName = etCourseName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim(); // Now potentially auto-filled
        String selectedFieldName = (String) spinnerField.getSelectedItem();
        int selectedFieldPosition = spinnerField.getSelectedItemPosition();

        if (TextUtils.isEmpty(courseName) || TextUtils.isEmpty(department) || selectedFieldPosition == 0) {
            Toast.makeText(getContext(), "Nom du cours, Département et Filière sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedTargetYears = new ArrayList<>();
        for (int i = 0; i < targetYearsCheckboxContainer.getChildCount(); i++) {
            View child = targetYearsCheckboxContainer.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox cb = (CheckBox) child;
                if (cb.isChecked()) {
                    selectedTargetYears.add(cb.getText().toString());
                }
            }
        }
        if (selectedTargetYears.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez sélectionner au moins une Année Cible.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected ScheduleEntry
        int selectedSchedulePosition = spinnerCourseScheduleEntry.getSelectedItemPosition();
        ScheduleEntry selectedCourseScheduleEntry = null;
        if (selectedSchedulePosition > 0) { // If a valid entry is selected (not the hint)
            selectedCourseScheduleEntry = currentFieldScheduleEntries.get(selectedSchedulePosition - 1);
        } else {
            Toast.makeText(getContext(), "Veuillez sélectionner une Plage Horaire pour le cours.", Toast.LENGTH_SHORT).show();
            return;
        }


        if (courseToEdit == null) {
            // Create new course
            String newCourseId = firebaseManager.getFirestore().collection("courses").document().getId(); // Get a new Firestore ID
            Course newCourse = new Course(newCourseId, courseName, department, selectedFieldName, selectedTargetYears);
            newCourse.setCourseScheduleEntry(selectedCourseScheduleEntry); // Set the selected schedule entry
            newCourse.setCreatedAt(Timestamp.now()); // Set current timestamp for new course
            newCourse.setActive(true); // Assuming new courses are active by default

            // Initialize statistics map
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("averageAttendanceRate", 0);
            statistics.put("totalEnrolledStudents", 0); // Will be updated as students enroll
            statistics.put("totalSessions", 0);
            newCourse.setStatistics(statistics);


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
                    Log.e(TAG, "Error creating course: " + error);
                }
            });
        } else {
            // Update existing course
            courseToEdit.setCourseName(courseName);
            courseToEdit.setDepartment(department);
            courseToEdit.setField(selectedFieldName);
            courseToEdit.setTargetYears(selectedTargetYears);
            courseToEdit.setCourseScheduleEntry(selectedCourseScheduleEntry); // Update the schedule entry
            // Keep existing createdAt, teacherEmail, teacherName, isActive, statistics as they are not edited here

            firebaseManager.modifyCourse(courseToEdit.getCourseId(), courseToEdit, new FirebaseManager.DataCallback<Void>() { // Pass Course object
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
                    Log.e(TAG, "Error modifying course: " + error);
                }
            });
        }
    }
}