package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.model.TeeType;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseTeeServiceTest {

    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;

    @InjectMocks
    private CourseTeeService courseTeeService;

    @Test
    void createTee_requiresAtLeastOneCompleteRatingSlopeSet() {
        Course course = new Course();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        SaveCourseTeeRequest request = validTeeRequest();
        request.setCourseRating(null);
        request.setSlope(null);
        request.setWomenCourseRating(null);
        request.setWomenSlope(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseTeeService.createTee(10L, request));

        assertEquals("At least one complete rating/slope set is required.", error.getMessage());
        verify(courseTeeRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createTee_rejectsRetiredDateBeforeEffectiveDate() {
        Course course = new Course();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        SaveCourseTeeRequest request = validTeeRequest();
        request.setEffectiveDate(LocalDate.of(2026, 4, 1));
        request.setRetiredDate(LocalDate.of(2026, 3, 31));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseTeeService.createTee(10L, request));

        assertEquals("Retired date cannot be before effective date", error.getMessage());
    }

    @Test
    void createTee_rejectsDuplicateVersionForCourseNameAndEffectiveDate() {
        Course course = new Course();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        SaveCourseTeeRequest request = validTeeRequest();
        when(courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCaseAndEffectiveDate(
                10L, "Blue", LocalDate.of(2026, 1, 1))).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseTeeService.createTee(10L, request));

        assertTrue(error.getMessage().contains("already exists"));
    }

    @Test
    void updateTee_toCombo_removesRegularHoleRows() {
        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getId()).thenReturn(10L);

        CourseTee tee = new CourseTee();
        tee.setId(20L);
        tee.setCourse(course);
        tee.setTeeType(TeeType.REGULAR);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));
        when(courseTeeRepository.saveAndFlush(tee)).thenReturn(tee);

        SaveCourseTeeRequest request = validTeeRequest();
        request.setTeeType("COMBO");

        courseTeeService.updateTee(20L, request);

        verify(courseHoleRepository).deleteByCourseTee_Id(20L);
        verify(courseTeeComboHoleRepository, never()).deleteByComboTee_Id(20L);
    }

    @Test
    void updateTee_toRegular_removesComboMappings() {
        Course course = org.mockito.Mockito.mock(Course.class);
        when(course.getId()).thenReturn(10L);

        CourseTee tee = new CourseTee();
        tee.setId(20L);
        tee.setCourse(course);
        tee.setTeeType(TeeType.COMBO);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));
        when(courseTeeRepository.saveAndFlush(tee)).thenReturn(tee);

        SaveCourseTeeRequest request = validTeeRequest();
        request.setTeeType("REGULAR");

        courseTeeService.updateTee(20L, request);

        verify(courseTeeComboHoleRepository).deleteByComboTee_Id(20L);
        verify(courseHoleRepository, never()).deleteByCourseTee_Id(20L);
    }

    private SaveCourseTeeRequest validTeeRequest() {
        SaveCourseTeeRequest request = new SaveCourseTeeRequest();
        request.setTeeName("Blue");
        request.setTeeType("REGULAR");
        request.setEffectiveDate(LocalDate.of(2026, 1, 1));
        request.setCourseRating(new BigDecimal("72.1"));
        request.setSlope(130);
        request.setParTotal(72);
        request.setActive(Boolean.TRUE);
        return request;
    }
}
