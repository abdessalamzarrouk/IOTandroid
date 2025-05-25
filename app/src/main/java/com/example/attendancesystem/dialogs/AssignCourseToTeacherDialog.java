package com.example.attendancesystem.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Teacher;
import com.example.attendancesystem.services.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class AssignCourseToTeacherDialog extends DialogFragment {

    private static final String ARG_COURSE_ID = "course_id";
    private static final String ARG_COURSE_NAME = "course_name";
    private static final String ARG_CURRENT_TEACHER_EMAIL = "current_teacher_email";

    private TextView tvCourseName;
    private Spinner spinnerTeachers;
    private Button btnAssign, btnUnassign;
    private FirebaseManager firebaseManager;

    private String courseId;
    private String courseName;
    private String currentTeacherEmail;
    private List<Teacher> allTeachers;
    private List<String> teacherNames; // For spinner display
    private List<String> teacherEmails; // To map back to email

    public interface OnCourseAssignedUnassignedListener {
        void onCourseAssignedUnassigned();
    }

    private OnCourseAssignedUnassignedListener listener;

    public void setOnCourseAssignedUnassignedListener(OnCourseAssignedUnassignedListener listener) {
        this.listener = listener;
    }

    public static AssignCourseToTeacherDialog newInstance(String courseId, String courseName, String currentTeacherEmail) {
        AssignCourseToTeacherDialog dialog = new AssignCourseToTeacherDialog();
        Bundle args = new Bundle();
        args.putString(ARG_COURSE_ID, courseId);
        args.putString(ARG_COURSE_NAME, courseName);
        args.putString(ARG_CURRENT_TEACHER_EMAIL, currentTeacherEmail);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            courseId = getArguments().getString(ARG_COURSE_ID);
            courseName = getArguments().getString(ARG_COURSE_NAME);
            currentTeacherEmail = getArguments().getString(ARG_CURRENT_TEACHER_EMAIL);
        }
        firebaseManager = FirebaseManager.getInstance();
        allTeachers = new ArrayList<>();
        teacherNames = new ArrayList<>();
        teacherEmails = new ArrayList<>();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_assign_course_to_teacher, null);

        tvCourseName = view.findViewById(R.id.tv_assign_course_name);
        spinnerTeachers = view.findViewById(R.id.spinner_teachers);
        btnAssign = view.findViewById(R.id.btn_assign_teacher);
        btnUnassign = view.findViewById(R.id.btn_unassign_teacher);

        tvCourseName.setText("Affecter un enseignant au cours: " + courseName);

        loadTeachers(); // Load teachers for the spinner

        btnAssign.setOnClickListener(v -> assignTeacher());
        btnUnassign.setOnClickListener(v -> unassignTeacher());

        builder.setView(view)
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    private void loadTeachers() {
        firebaseManager.getAllTeachers(new FirebaseManager.DataCallback<List<Teacher>>() {
            @Override
            public void onSuccess(List<Teacher> teachers) {
                allTeachers = teachers;
                teacherNames.clear();
                teacherEmails.clear();

                // Add a "None" or "Select Teacher" option
                teacherNames.add("Aucun enseignant");
                teacherEmails.add(null); // Corresponds to no teacher

                for (Teacher teacher : teachers) {
                    teacherNames.add(teacher.getFullName() + " (" + teacher.getDepartment() + ")");
                    teacherEmails.add(teacher.getEmail());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, teacherNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTeachers.setAdapter(adapter);

                // Select current teacher if one is assigned
                if (currentTeacherEmail != null) {
                    int index = teacherEmails.indexOf(currentTeacherEmail);
                    if (index != -1) {
                        spinnerTeachers.setSelection(index);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Erreur de chargement des enseignants: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignTeacher() {
        int selectedPosition = spinnerTeachers.getSelectedItemPosition();
        String selectedTeacherEmail = teacherEmails.get(selectedPosition);

        if (selectedTeacherEmail == null) {
            Toast.makeText(getContext(), "Veuillez sélectionner un enseignant valide.", Toast.LENGTH_SHORT).show();
            return;
        }

        Teacher selectedTeacher = null;
        for (Teacher t : allTeachers) {
            if (t.getEmail().equals(selectedTeacherEmail)) {
                selectedTeacher = t;
                break;
            }
        }

        if (selectedTeacher == null) {
            Toast.makeText(getContext(), "Enseignant introuvable. Réessayez.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String teacherName = selectedTeacher.getFullName();
        final String department = selectedTeacher.getDepartment(); // Or fetch from course directly if preferred

        firebaseManager.assignCourseToTeacher(selectedTeacherEmail, courseId, teacherName, department, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getContext(), "Cours affecté à " + teacherName + " avec succès !", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onCourseAssignedUnassigned();
                }
                dismiss();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Erreur d'affectation: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void unassignTeacher() {
        if (currentTeacherEmail == null) {
            Toast.makeText(getContext(), "Ce cours n'a pas d'enseignant affecté.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmer la Désaffectation")
                .setMessage("Êtes-vous sûr de vouloir désaffecter l'enseignant de ce cours ?")
                .setPositiveButton("Désaffecter", (dialog, which) -> {
                    firebaseManager.unassignCourseFromTeacher(currentTeacherEmail, courseId, new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getContext(), "Enseignant désaffecté avec succès !", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onCourseAssignedUnassigned();
                            }
                            dismiss();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(getContext(), "Erreur de désaffectation: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}