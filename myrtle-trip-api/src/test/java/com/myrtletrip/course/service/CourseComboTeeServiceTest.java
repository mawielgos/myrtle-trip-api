package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.SaveCourseTeeComboHoleRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.model.TeeType;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseComboTeeServiceTest {

    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    @Mock private CourseHoleService courseHoleService;

    @InjectMocks
    private CourseComboTeeService courseComboTeeService;

    @Test
    void getComboHolesForTee_rejectsRegularTee() {
        CourseTee tee = tee(20L, TeeType.REGULAR);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseComboTeeService.getComboHolesForTee(20L));

        assertEquals("Tee is not a combo tee", error.getMessage());
    }

    @Test
    void saveComboHolesForTee_requiresExactly18Mappings() {
        CourseTee combo = tee(20L, TeeType.COMBO);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(combo));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseComboTeeService.saveComboHolesForTee(20L, List.of()));

        assertEquals("Exactly 18 combo tee hole mappings are required", error.getMessage());
    }

    @Test
    void saveComboHolesForTee_rejectsSelfSource() {
        CourseTee combo = tee(20L, TeeType.COMBO);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(combo));

        List<SaveCourseTeeComboHoleRequest> requests = requests(30L);
        requests.get(0).setSourceTeeId(20L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseComboTeeService.saveComboHolesForTee(20L, requests));

        assertEquals("Combo tee cannot source holes from itself", error.getMessage());
    }

    @Test
    void saveComboHolesForTee_rejectsSourceFromDifferentCourse() {
        CourseTee combo = teeWithCourseId(20L, TeeType.COMBO, 1L);
        CourseTee source = teeWithCourseId(30L, TeeType.REGULAR, 2L);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(courseTeeRepository.findById(30L)).thenReturn(Optional.of(source));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseComboTeeService.saveComboHolesForTee(20L, requests(30L)));

        assertEquals("Source tees must belong to the same course as the combo tee", error.getMessage());
    }

    @Test
    void saveComboHolesForTee_rejectsComboSource() {
        CourseTee combo = teeWithCourseId(20L, TeeType.COMBO, 1L);
        CourseTee source = teeWithCourseId(30L, TeeType.COMBO, 1L);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(combo));
        when(courseTeeRepository.findById(30L)).thenReturn(Optional.of(source));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseComboTeeService.saveComboHolesForTee(20L, requests(30L)));

        assertEquals("Combo tees cannot use another combo tee as a source tee", error.getMessage());
    }

    private CourseTee tee(Long teeId, TeeType type) {
        CourseTee tee = new CourseTee();
        tee.setId(teeId);
        tee.setTeeType(type);
        tee.setCourse(mock(Course.class));
        tee.setTeeName("Tee " + teeId);
        return tee;
    }

    private CourseTee teeWithCourseId(Long teeId, TeeType type, Long courseId) {
        CourseTee tee = tee(teeId, type);
        when(tee.getCourse().getId()).thenReturn(courseId);
        return tee;
    }

    private List<SaveCourseTeeComboHoleRequest> requests(Long sourceTeeId) {
        List<SaveCourseTeeComboHoleRequest> requests = new ArrayList<>();
        for (int hole = 1; hole <= 18; hole++) {
            SaveCourseTeeComboHoleRequest request = new SaveCourseTeeComboHoleRequest();
            request.setHoleNumber(hole);
            request.setSourceTeeId(sourceTeeId);
            requests.add(request);
        }
        return requests;
    }
}
