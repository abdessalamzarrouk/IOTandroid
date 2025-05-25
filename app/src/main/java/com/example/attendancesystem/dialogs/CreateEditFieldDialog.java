package com.example.attendancesystem.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Field;
import com.example.attendancesystem.services.FirebaseManager;

import java.util.HashMap;
import java.util.Map;

public class CreateEditFieldDialog extends DialogFragment {

    private static final String ARG_FIELD = "field_to_edit";

    private EditText etFieldName, etDepartment, etDescription;
    private Button btnSave;
    private FirebaseManager firebaseManager;
    private Field fieldToEdit;

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
        btnSave = view.findViewById(R.id.btn_save_field);

        if (fieldToEdit != null) {
            etFieldName.setText(fieldToEdit.getFieldName());
            etDepartment.setText(fieldToEdit.getDepartment());
            etDescription.setText(fieldToEdit.getDescription());
            builder.setTitle("Modifier la Filière");
        } else {
            builder.setTitle("Créer une Nouvelle Filière");
        }

        btnSave.setOnClickListener(v -> saveField());

        builder.setView(view)
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void saveField() {
        String fieldName = etFieldName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(fieldName) || TextUtils.isEmpty(department)) {
            Toast.makeText(getContext(), "Le nom et le département sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fieldToEdit == null) {
            // Create new field
            firebaseManager.createField(fieldName, department, description, new FirebaseManager.DataCallback<Void>() {
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
                }
            });
        } else {
            // Update existing field
            Map<String, Object> updates = new HashMap<>();
            updates.put("fieldName", fieldName);
            updates.put("department", department);
            updates.put("description", description);

            firebaseManager.modifyField(fieldToEdit.getFieldId(), updates, new FirebaseManager.DataCallback<Void>() {
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
                }
            });
        }
    }
}