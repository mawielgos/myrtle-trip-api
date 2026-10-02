package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.CourseListResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;

    @InjectMocks
    private CourseCatalogService courseCatalogService;

    @Test
    void createCourse_requiresRequest() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseCatalogService.createCourse(null));

        assertEquals("Course request is required", error.getMessage());
        verify(courseRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createCourse_requiresName() {
        SaveCourseRequest request = new SaveCourseRequest();
        request.setCourseName("   ");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseCatalogService.createCourse(request));

        assertEquals("Course name is required", error.getMessage());
    }

    @Test
    void createCourse_rejectsDuplicateNameIgnoringCase() {
        SaveCourseRequest request = new SaveCourseRequest();
        request.setCourseName("Pine Lakes");
        when(courseRepository.existsByNameIgnoreCase("Pine Lakes")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseCatalogService.createCourse(request));

        assertEquals("Course name already exists", error.getMessage());
    }

    @Test
    void getActiveCourses_mapsRepositoryResults() {
        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getId()).thenReturn(7L);
        when(course.getName()).thenReturn("Pine Lakes");
        when(course.getLocation()).thenReturn("Myrtle Beach");
        when(courseRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(course));

        List<CourseListResponse> responses = courseCatalogService.getActiveCourses();

        assertEquals(1, responses.size());
        assertEquals(7L, responses.get(0).getCourseId());
        assertEquals("Pine Lakes", responses.get(0).getCourseName());
        assertEquals("Myrtle Beach", responses.get(0).getLocation());
    }

    @Test
    void getAllCourseSummaries_sortsByNameAndCountsTees() {
        Course zeta = org.mockito.Mockito.mock(Course.class);
        when(zeta.getId()).thenReturn(2L);
        when(zeta.getName()).thenReturn("Zeta");
        when(zeta.getActive()).thenReturn(true);

        Course alpha = org.mockito.Mockito.mock(Course.class);
        when(alpha.getId()).thenReturn(1L);
        when(alpha.getName()).thenReturn("Alpha");
        when(alpha.getActive()).thenReturn(true);

        when(courseRepository.findAll()).thenReturn(List.of(zeta, alpha));
        when(courseTeeRepository.countByCourse_Id(1L)).thenReturn(3L);
        when(courseTeeRepository.countByCourse_Id(2L)).thenReturn(1L);

        List<CourseSummaryResponse> responses = courseCatalogService.getAllCourseSummaries();

        assertEquals(2, responses.size());
        assertEquals("Alpha", responses.get(0).getCourseName());
        assertEquals(3, responses.get(0).getTeeCount());
        assertEquals("Zeta", responses.get(1).getCourseName());
        assertEquals(1, responses.get(1).getTeeCount());
    }
}
