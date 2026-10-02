package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    @Mock private CourseTeeService courseTeeService;
    @Mock private CourseHoleService courseHoleService;
    @Mock private CourseComboTeeService courseComboTeeService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourse_requiresRequest() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(null));

        assertEquals("Course request is required", error.getMessage());
        verify(courseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createCourse_requiresName() {
        SaveCourseRequest request = new SaveCourseRequest();
        request.setCourseName("   ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(request));

        assertEquals("Course name is required", error.getMessage());
    }

    @Test
    void createCourse_rejectsDuplicateNameIgnoringCase() {
        SaveCourseRequest request = new SaveCourseRequest();
        request.setCourseName("Pine Lakes");
        when(courseRepository.existsByNameIgnoreCase("Pine Lakes")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseService.createCourse(request));

        assertEquals("Course name already exists", error.getMessage());
    }
}
