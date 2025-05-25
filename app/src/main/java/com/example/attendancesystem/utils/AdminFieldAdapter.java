package com.example.attendancesystem.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Field;

import java.util.List;

public class AdminFieldAdapter extends RecyclerView.Adapter<AdminFieldAdapter.FieldViewHolder> {

    private List<Field> fieldList;
    private OnFieldActionListener listener;

    public interface OnFieldActionListener {
        void onEditClick(Field field);
        void onDeleteClick(Field field);
    }

    public AdminFieldAdapter(List<Field> fieldList, OnFieldActionListener listener) {
        this.fieldList = fieldList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FieldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_field, parent, false);
        return new FieldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FieldViewHolder holder, int position) {
        Field field = fieldList.get(position);
        holder.tvFieldName.setText(field.getFieldName());
        holder.tvFieldDepartment.setText("Département: " + field.getDepartment());
        holder.tvFieldDescription.setText("Description: " + field.getDescription());
        // Optionally format timestamp
        // holder.tvCreatedAt.setText("Créé le: " + Utils.formatTimestamp(field.getCreatedAt()));

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(field));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(field));
    }

    @Override
    public int getItemCount() {
        return fieldList.size();
    }

    public static class FieldViewHolder extends RecyclerView.ViewHolder {
        TextView tvFieldName, tvFieldDepartment, tvFieldDescription;
        Button btnEdit, btnDelete;

        public FieldViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFieldName = itemView.findViewById(R.id.tv_field_name_item);
            tvFieldDepartment = itemView.findViewById(R.id.tv_field_department_item);
            tvFieldDescription = itemView.findViewById(R.id.tv_field_description_item);
            btnEdit = itemView.findViewById(R.id.btn_edit_field);
            btnDelete = itemView.findViewById(R.id.btn_delete_field);
        }
    }
}