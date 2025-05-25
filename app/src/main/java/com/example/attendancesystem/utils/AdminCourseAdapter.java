package com.example.attendancesystem.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Course;

import java.util.List;

public class AdminCourseAdapter extends RecyclerView.Adapter<AdminCourseAdapter.CourseViewHolder> {

    private List<Course> courseList;
    private OnCourseActionListener listener;

    public interface OnCourseActionListener {
        void onEditClick(Course course);
        void onDeleteClick(Course course);
        void onAssignTeacherClick(Course course); // New action for assigning teacher
    }

    public AdminCourseAdapter(List<Course> courseList, OnCourseActionListener listener) {
        this.courseList = courseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseName.setText(course.getCourseName());
        holder.tvCourseDepartment.setText("Département: " + course.getDepartment());
        holder.tvCourseField.setText("Filière: " + course.getField());
        holder.tvCourseTeacher.setText("Enseignant: " + (course.getTeacherName() != null && !course.getTeacherName().isEmpty() ? course.getTeacherName() : "Non affecté"));
        holder.tvCourseTargetYears.setText("Années cibles: " + (course.getTargetYears() != null ? String.join(", ", course.getTargetYears()) : "N/A"));

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(course));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(course));
        holder.btnAssignTeacher.setOnClickListener(v -> listener.onAssignTeacherClick(course));
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName, tvCourseDepartment, tvCourseField, tvCourseTeacher, tvCourseTargetYears;
        Button btnEdit, btnDelete, btnAssignTeacher;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name_item);
            tvCourseDepartment = itemView.findViewById(R.id.tv_course_department_item);
            tvCourseField = itemView.findViewById(R.id.tv_course_field_item);
            tvCourseTeacher = itemView.findViewById(R.id.tv_course_teacher_item);
            tvCourseTargetYears = itemView.findViewById(R.id.tv_course_target_years_item);
            btnEdit = itemView.findViewById(R.id.btn_edit_course);
            btnDelete = itemView.findViewById(R.id.btn_delete_course);
            btnAssignTeacher = itemView.findViewById(R.id.btn_assign_teacher);
        }
    }
}