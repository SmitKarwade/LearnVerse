package com.example.learnverse.tutor.dto;

import com.example.learnverse.activity.model.Activity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorCourseDetailResponse {
    private Activity activity;
    private List<EnrolledStudentDto> enrolledStudents;
    private CourseStatsDto stats;
}
