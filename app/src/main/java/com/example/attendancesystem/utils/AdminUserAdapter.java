package com.example.attendancesystem.utils;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.models.Teacher;
import android.content.res.ColorStateList;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<Object> users; // Can hold Student or Teacher
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEditClick(Object user);
        void onToggleStatusClick(Object user);
        void onRemoveClick(Object user); // Added: Method for removing a user
    }

    public AdminUserAdapter(List<Object> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_account, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Object user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvUserTypeId, tvUserDepartment, tvUserStatus;
        AppCompatButton btnEditUser, btnToggleStatus, btnRemoveUser; // Added: Reference for remove button

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            tvUserTypeId = itemView.findViewById(R.id.tv_user_type_id);
            tvUserDepartment = itemView.findViewById(R.id.tv_user_department);
            tvUserStatus = itemView.findViewById(R.id.tv_user_status);
            btnEditUser = itemView.findViewById(R.id.btn_edit_user);
            btnToggleStatus = itemView.findViewById(R.id.btn_toggle_status);
            btnRemoveUser = itemView.findViewById(R.id.btn_remove_user); // Initialized: The remove button
        }

        public void bind(Object user) {
            String statusText;
            int statusColor;
            String typeIdText = "";

            // The Log.d for AdapterDebug is kept as it's useful for troubleshooting the isActive issue
            if (user instanceof Student) {
                Student student = (Student) user;
                Log.d("AdapterDebug", "Binding Student: " + student.getEmail() + ", isActive: " + student.isActive());
                tvUserName.setText("Nom: " + student.getFullName());
                tvUserEmail.setText("Email: " + student.getEmail());
                tvUserDepartment.setText("Département: " + student.getDepartment() + ", Filière: " + student.getField() + ", Année: " + student.getYear());
                typeIdText = "Étudiant (ID: " + student.getStudentId() + ")";
                statusText = student.isActive() ? "Actif" : "Désactivé";
                statusColor = student.isActive() ? itemView.getContext().getColor(R.color.green_700) : itemView.getContext().getColor(R.color.red_700);
                btnToggleStatus.setText(student.isActive() ? "Désactiver" : "Activer");
                btnToggleStatus.setBackgroundTintList(
                        ColorStateList.valueOf(itemView.getContext().getColor(student.isActive() ? R.color.red_700 : R.color.green_700))
                );

            } else if (user instanceof Teacher) {
                Teacher teacher = (Teacher) user;
                Log.d("AdapterDebug", "Binding Teacher: " + teacher.getEmail() + ", isActive: " + teacher.isActive());
                tvUserName.setText("Nom: " + teacher.getFullName());
                tvUserEmail.setText("Email: " + teacher.getEmail());
                tvUserDepartment.setText("Département: " + teacher.getDepartment());
                typeIdText = "Enseignant (ID: " + teacher.getEmployeeId() + ")";
                statusText = teacher.isActive() ? "Actif" : "Désactivé";
                statusColor = teacher.isActive() ? itemView.getContext().getColor(R.color.green_700) : itemView.getContext().getColor(R.color.red_700);
                btnToggleStatus.setText(teacher.isActive() ? "Désactiver" : "Activer");
                btnToggleStatus.setBackgroundTintList(
                        ColorStateList.valueOf(itemView.getContext().getColor(teacher.isActive() ? R.color.red_700 : R.color.green_700))
                );

            } else {
                // Fallback for unknown types (shouldn't happen with proper filtering)
                tvUserName.setText("Utilisateur Inconnu");
                tvUserEmail.setText("");
                tvUserDepartment.setText("");
                typeIdText = "";
                statusText = "";
                statusColor = Color.BLACK;
            }

            tvUserTypeId.setText("Type: " + typeIdText);
            tvUserStatus.setText("Statut: " + statusText);
            tvUserStatus.setTextColor(statusColor);

            btnEditUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(user);
                }
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onToggleStatusClick(user);
                }
            });

            // New: Set OnClickListener for the remove button
            btnRemoveUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(user);
                }
            });
        }
    }
}