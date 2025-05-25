// app/src/main/java/com/example/attendancesystem/dialogs/CreateEditFieldDialog.java

package com.example.attendancesystem.dialogs;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.example.attendancesystem.models.Field;
import com.example.attendancesystem.models.ScheduleEntry; // NEW
import com.example.attendancesystem.services.FirebaseManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEditFieldDialog extends DialogFragment {

    private static final String ARG_FIELD = "field_to_edit";
    private static final String TAG = "CreateEditFieldDialog";

    private EditText etFieldName, etDepartment, etDescription;
    private Button btnSaveField, btnAddScheduleEntry;
    private LinearLayout scheduleEntriesContainer; // Container for dynamic schedule entries
    private FirebaseManager firebaseManager;
    private Field fieldToEdit;
    private List<ScheduleEntry> currentFieldSchedule; // List to hold schedule entries being edited

    // Days of week for the spinner
    private final List<String> DAYS_OF_WEEK = Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche");


    public interface OnFieldCreatedEditedListener {
        void onFieldCreatedEdited();
    }

    private OnFieldCreatedEditedListener listener;

    public void setOnFieldCreatedEditedListener(OnFieldCreatedEditedListener listener) {
        this.listener = listener;
    }

    public static CreateEditFieldDialog newInstance(Field field) {
        CreateEditFieldDialog dialog = new CreateEditFieldDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FIELD, field);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fieldToEdit = (Field) getArguments().getSerializable(ARG_FIELD);
        }
        firebaseManager = FirebaseManager.getInstance();
        currentFieldSchedule = new ArrayList<>(); // Initialize the list
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_edit_field, null);

        etFieldName = view.findViewById(R.id.et_field_name);
        etDepartment = view.findViewById(R.id.et_field_department);
        etDescription = view.findViewById(R.id.et_field_description);
        btnSaveField = view.findViewById(R.id.btn_save_field);
        btnAddScheduleEntry = view.findViewById(R.id.btn_add_schedule_entry);
        scheduleEntriesContainer = view.findViewById(R.id.schedule_entries_container);

        if (fieldToEdit != null) {
            etFieldName.setText(fieldToEdit.getFieldName());
            etDepartment.setText(fieldToEdit.getDepartment());
            etDescription.setText(fieldToEdit.getDescription());
            builder.setTitle("Modifier la Filière");

            // Populate existing schedule entries
            if (fieldToEdit.getWeeklySchedule() != null) {
                currentFieldSchedule.addAll(fieldToEdit.getWeeklySchedule());
                for (ScheduleEntry entry : currentFieldSchedule) {
                    addScheduleEntryView(entry);
                }
            }
        } else {
            builder.setTitle("Créer une Nouvelle Filière");
        }

        btnAddScheduleEntry.setOnClickListener(v -> addScheduleEntryView(null));
        btnSaveField.setOnClickListener(v -> saveField());

        builder.setView(view)
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void addScheduleEntryView(@Nullable ScheduleEntry entry) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View scheduleEntryView = inflater.inflate(R.layout.item_schedule_entry, scheduleEntriesContainer, false);

        Spinner spinnerDay = scheduleEntryView.findViewById(R.id.spinner_day_of_week);
        TextView tvStartTime = scheduleEntryView.findViewById(R.id.tv_start_time);
        TextView tvEndTime = scheduleEntryView.findViewById(R.id.tv_end_time);
        EditText etRoom = scheduleEntryView.findViewById(R.id.et_room);
        Button btnRemove = scheduleEntryView.findViewById(R.id.btn_remove_schedule_entry);

        // Populate Day of Week Spinner
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, DAYS_OF_WEEK);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        // Setup TimePickers
        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));

        if (entry != null) {
            int dayPosition = DAYS_OF_WEEK.indexOf(entry.getDayOfWeek());
            if (dayPosition != -1) {
                spinnerDay.setSelection(dayPosition);
            }
            tvStartTime.setText(entry.getStartTime());
            tvEndTime.setText(entry.getEndTime());
            etRoom.setText(entry.getRoom());
        } else {
            // Set default text for new entries
            tvStartTime.setText("00:00");
            tvEndTime.setText("00:00");
        }


        btnRemove.setOnClickListener(v -> {
            scheduleEntriesContainer.removeView(scheduleEntryView);
            // Optionally remove from currentFieldSchedule list if managing it here
            // (More complex: you'd need a way to link the view back to the ScheduleEntry object)
        });

        scheduleEntriesContainer.addView(scheduleEntryView);
    }

    private void showTimePicker(final TextView targetTextView) {
        int hour = 0;
        int minute = 0;
        if (!targetTextView.getText().toString().isEmpty() && !targetTextView.getText().toString().equals("00:00")) {
            try {
                String[] timeParts = targetTextView.getText().toString().split(":");
                hour = Integer.parseInt(timeParts[0]);
                minute = Integer.parseInt(timeParts[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing time for TimePicker: " + e.getMessage());
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                    targetTextView.setText(formattedTime);
                }, hour, minute, true); // true for 24-hour format
        timePickerDialog.show();
    }


    private void saveField() {
        String fieldName = etFieldName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(fieldName) || TextUtils.isEmpty(department)) {
            Toast.makeText(getContext(), "Le nom et le département sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Collect all schedule entries from the dynamically added views
        List<ScheduleEntry> newWeeklySchedule = new ArrayList<>();
        for (int i = 0; i < scheduleEntriesContainer.getChildCount(); i++) {
            View entryView = scheduleEntriesContainer.getChildAt(i);
            Spinner spinnerDay = entryView.findViewById(R.id.spinner_day_of_week);
            TextView tvStartTime = entryView.findViewById(R.id.tv_start_time);
            TextView tvEndTime = entryView.findViewById(R.id.tv_end_time);
            EditText etRoom = entryView.findViewById(R.id.et_room);

            String day = (String) spinnerDay.getSelectedItem();
            String start = tvStartTime.getText().toString();
            String end = tvEndTime.getText().toString();
            String room = etRoom.getText().toString().trim();

            if (day == null || day.isEmpty() || day.equals("Select Day") || start.equals("00:00") || end.equals("00:00")) {
                Toast.makeText(getContext(), "Veuillez compléter toutes les plages horaires ou les supprimer.", Toast.LENGTH_SHORT).show();
                return; // Stop saving if any schedule entry is incomplete
            }
            newWeeklySchedule.add(new ScheduleEntry(day, start, end, room));
        }


        if (fieldToEdit == null) {
            // Create new field
            String newFieldId = firebaseManager.getFirestore().collection("fields").document().getId(); // Generate new ID
            Field newField = new Field(newFieldId, fieldName, department, description, newWeeklySchedule);

            firebaseManager.createField(newField, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Filière créée avec succès !", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onFieldCreatedEdited();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Erreur de création de la filière: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error creating field: " + error);
                }
            });
        } else {
            // Update existing field
            fieldToEdit.setFieldName(fieldName);
            fieldToEdit.setDepartment(department);
            fieldToEdit.setDescription(description);
            fieldToEdit.setWeeklySchedule(newWeeklySchedule); // Set the updated schedule list

            firebaseManager.modifyField(fieldToEdit.getFieldId(), fieldToEdit.toMap(), new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(getContext(), "Filière modifiée avec succès !", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onFieldCreatedEdited();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(getContext(), "Erreur de modification de la filière: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error modifying field: " + error);
                }
            });
        }
    }
}