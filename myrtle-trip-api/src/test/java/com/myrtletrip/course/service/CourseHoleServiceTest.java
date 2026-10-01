package com.myrtletrip.course.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.model.TeeType;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseHoleServiceTest {

    @Mock private CourseHoleRepository courseHoleRepository;
    @Mock private CourseTeeRepository courseTeeRepository;
    @Mock private CourseTeeComboHoleRepository courseTeeComboHoleRepository;

    @InjectMocks
    private CourseHoleService courseHoleService;

    @Test
    void saveHolesForTee_rejectsComboTee() {
        CourseTee tee = new CourseTee();
        tee.setId(20L);
        tee.setTeeType(TeeType.COMBO);
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseHoleService.saveHolesForTee(20L, List.of()));

        assertEquals("Combo tee hole data is controlled by source tee mappings.", error.getMessage());
    }

    @Test
    void saveHolesForTee_requiresExactly18Holes() {
        CourseTee tee = regularTee();
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));

        List<SaveCourseHoleRequest> requests = new ArrayList<>();
        for (int hole = 1; hole <= 17; hole++) {
            requests.add(holeRequest(hole, 4, hole));
        }

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseHoleService.saveHolesForTee(20L, requests));

        assertEquals("Exactly 18 holes are required", error.getMessage());
    }

    @Test
    void saveHolesForTee_rejectsDuplicateMensHandicap() {
        CourseTee tee = regularTee();
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));

        List<SaveCourseHoleRequest> requests = new ArrayList<>();
        for (int hole = 1; hole <= 18; hole++) {
            int handicap = hole == 18 ? 17 : hole;
            requests.add(holeRequest(hole, 4, handicap));
        }

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> courseHoleService.saveHolesForTee(20L, requests));

        assertEquals("Duplicate men's handicap: 17", error.getMessage());
    }

    @Test
    void getHolePar_fallsBackToWomensPar() {
        CourseTee tee = regularTee();
        when(courseTeeRepository.findById(20L)).thenReturn(Optional.of(tee));

        CourseHole hole = new CourseHole();
        hole.setWomenPar(5);
        when(courseHoleRepository.findByCourseTee_IdAndHoleNumber(20L, 3))
                .thenReturn(Optional.of(hole));

        assertEquals(5, courseHoleService.getHolePar(20L, 3));
    }

    private CourseTee regularTee() {
        CourseTee tee = new CourseTee();
        tee.setId(20L);
        tee.setTeeType(TeeType.REGULAR);
        tee.setParTotal(72);
        tee.setCourseRating(new BigDecimal("72.1"));
        tee.setSlope(130);
        tee.setActive(true);
        return tee;
    }

    private SaveCourseHoleRequest holeRequest(int hole, int par, int handicap) {
        SaveCourseHoleRequest request = new SaveCourseHoleRequest();
        request.setHoleNumber(hole);
        request.setPar(par);
        request.setHandicap(handicap);
        request.setYardage(400);
        return request;
    }
}
