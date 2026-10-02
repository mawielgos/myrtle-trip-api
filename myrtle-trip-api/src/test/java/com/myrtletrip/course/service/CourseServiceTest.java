package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseListResponse;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseCatalogService courseCatalogService;
    @Mock private CourseTeeService courseTeeService;
    @Mock private CourseHoleService courseHoleService;
    @Mock private CourseComboTeeService courseComboTeeService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getActiveCourses_delegatesToCatalogService() {
        List<CourseListResponse> expected = List.of(new CourseListResponse());
        when(courseCatalogService.getActiveCourses()).thenReturn(expected);

        assertSame(expected, courseService.getActiveCourses());
        verify(courseCatalogService).getActiveCourses();
    }

    @Test
    void getCourseDetail_delegatesToCatalogService() {
        CourseDetailResponse expected = org.mockito.Mockito.mock(CourseDetailResponse.class);
        when(courseCatalogService.getCourseDetail(10L)).thenReturn(expected);

        assertSame(expected, courseService.getCourseDetail(10L));
        verify(courseCatalogService).getCourseDetail(10L);
    }
}
