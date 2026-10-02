package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseHoleResponse;
import com.myrtletrip.course.dto.CourseListResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeComboHoleResponse;
import com.myrtletrip.course.dto.CourseTeeListResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeComboHoleRequest;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.entity.CourseHole;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseCatalogService courseCatalogService;
    private final CourseTeeService courseTeeService;
    private final CourseHoleService courseHoleService;
    private final CourseComboTeeService courseComboTeeService;

    public CourseService(CourseCatalogService courseCatalogService,
                         CourseTeeService courseTeeService,
                         CourseHoleService courseHoleService,
                         CourseComboTeeService courseComboTeeService) {
        this.courseCatalogService = courseCatalogService;
        this.courseTeeService = courseTeeService;
        this.courseHoleService = courseHoleService;
        this.courseComboTeeService = courseComboTeeService;
    }

    public List<CourseListResponse> getActiveCourses() {
        return courseCatalogService.getActiveCourses();
    }

    public List<CourseSummaryResponse> getAllCourseSummaries() {
        return courseCatalogService.getAllCourseSummaries();
    }

    public CourseDetailResponse getCourseDetail(Long courseId) {
        return courseCatalogService.getCourseDetail(courseId);
    }

    @Transactional
    public CourseDetailResponse createCourse(SaveCourseRequest request) {
        return courseCatalogService.createCourse(request);
    }

    @Transactional
    public CourseDetailResponse updateCourse(Long courseId, SaveCourseRequest request) {
        return courseCatalogService.updateCourse(courseId, request);
    }

    @Transactional
    public CourseDetailResponse setCourseActive(Long courseId, boolean active) {
        return courseCatalogService.setCourseActive(courseId, active);
    }

    public List<CourseTeeListResponse> getActiveTeesForCourse(Long courseId) {
        return courseTeeService.getActiveTeesForCourse(courseId);
    }

    public List<CourseTeeResponse> getAllTeesForCourse(Long courseId) {
        return courseTeeService.getAllTeesForCourse(courseId);
    }

    public CourseTeeResponse getTeeDetail(Long teeId) {
        return courseTeeService.getTeeDetail(teeId);
    }

    @Transactional
    public CourseTeeResponse createTee(Long courseId, SaveCourseTeeRequest request) {
        return courseTeeService.createTee(courseId, request);
    }

    @Transactional
    public CourseTeeResponse updateTee(Long teeId, SaveCourseTeeRequest request) {
        return courseTeeService.updateTee(teeId, request);
    }

    @Transactional
    public CourseTeeResponse setTeeActive(Long teeId, boolean active) {
        return courseTeeService.setTeeActive(teeId, active);
    }

    public List<CourseHoleResponse> getHolesForTeeResponse(Long courseTeeId) {
        return courseHoleService.getHolesForTeeResponse(courseTeeId);
    }

    @Transactional
    public List<CourseHoleResponse> saveHolesForTee(Long courseTeeId, List<SaveCourseHoleRequest> requests) {
        return courseHoleService.saveHolesForTee(courseTeeId, requests);
    }

    public List<CourseHole> getHolesForTee(Long courseTeeId) {
        return courseHoleService.getHolesForTee(courseTeeId);
    }

    public Integer getHolePar(Long courseTeeId, int holeNumber) {
        return courseHoleService.getHolePar(courseTeeId, holeNumber);
    }

    public Integer getHoleHandicap(Long courseTeeId, int holeNumber) {
        return courseHoleService.getHoleHandicap(courseTeeId, holeNumber);
    }

    public List<CourseTeeComboHoleResponse> getComboHolesForTee(Long comboTeeId) {
        return courseComboTeeService.getComboHolesForTee(comboTeeId);
    }

    @Transactional
    public List<CourseTeeComboHoleResponse> saveComboHolesForTee(
            Long comboTeeId,
            List<SaveCourseTeeComboHoleRequest> requests) {
        return courseComboTeeService.saveComboHolesForTee(comboTeeId, requests);
    }
}
