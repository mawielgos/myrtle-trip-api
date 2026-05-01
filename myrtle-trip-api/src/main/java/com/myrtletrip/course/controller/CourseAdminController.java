package com.myrtletrip.course.controller;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseHoleResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeComboHoleResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeComboHoleRequest;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@CrossOrigin
public class CourseAdminController {

    private final CourseService courseService;

    public CourseAdminController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseSummaryResponse> getAllCourses() {
        return courseService.getAllCourseSummaries();
    }

    @GetMapping("/{courseId}")
    public CourseDetailResponse getCourse(@PathVariable Long courseId) {
        return courseService.getCourseDetail(courseId);
    }

    @PostMapping
    public CourseDetailResponse createCourse(@RequestBody SaveCourseRequest request) {
        return courseService.createCourse(request);
    }

    @PutMapping("/{courseId}")
    public CourseDetailResponse updateCourse(@PathVariable Long courseId,
                                             @RequestBody SaveCourseRequest request) {
        return courseService.updateCourse(courseId, request);
    }

    @PutMapping("/{courseId}/active")
    public CourseDetailResponse setCourseActive(@PathVariable Long courseId,
                                               @RequestParam boolean active) {
        return courseService.setCourseActive(courseId, active);
    }

    @GetMapping("/{courseId}/tees")
    public List<CourseTeeResponse> getTees(@PathVariable Long courseId) {
        return courseService.getAllTeesForCourse(courseId);
    }

    @GetMapping("/tees/{teeId}")
    public CourseTeeResponse getTee(@PathVariable Long teeId) {
        return courseService.getTeeDetail(teeId);
    }

    @PostMapping("/{courseId}/tees")
    public CourseTeeResponse createTee(@PathVariable Long courseId,
                                      @RequestBody SaveCourseTeeRequest request) {
        return courseService.createTee(courseId, request);
    }

    @PutMapping("/tees/{teeId}")
    public CourseTeeResponse updateTee(@PathVariable Long teeId,
                                      @RequestBody SaveCourseTeeRequest request) {
        return courseService.updateTee(teeId, request);
    }

    @PutMapping("/tees/{teeId}/active")
    public CourseTeeResponse setTeeActive(@PathVariable Long teeId,
                                         @RequestParam boolean active) {
        return courseService.setTeeActive(teeId, active);
    }

    @GetMapping("/tees/{teeId}/holes")
    public List<CourseHoleResponse> getHoles(@PathVariable Long teeId) {
        return courseService.getHolesForTeeResponse(teeId);
    }

    @PutMapping("/tees/{teeId}/holes")
    public List<CourseHoleResponse> saveHoles(@PathVariable Long teeId,
                                             @RequestBody List<SaveCourseHoleRequest> request) {
        return courseService.saveHolesForTee(teeId, request);
    }

    @GetMapping("/tees/{teeId}/combo-holes")
    public List<CourseTeeComboHoleResponse> getComboHoles(@PathVariable Long teeId) {
        return courseService.getComboHolesForTee(teeId);
    }

    @PutMapping("/tees/{teeId}/combo-holes")
    public List<CourseTeeComboHoleResponse> saveComboHoles(@PathVariable Long teeId,
                                                           @RequestBody List<SaveCourseTeeComboHoleRequest> request) {
        return courseService.saveComboHolesForTee(teeId, request);
    }
}
