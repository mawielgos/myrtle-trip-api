package com.myrtletrip.course.controller;

import com.myrtletrip.course.dto.CourseListResponse;
import com.myrtletrip.course.dto.CourseTeeListResponse;
import com.myrtletrip.course.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseListResponse> getCourses() {
        return courseService.getActiveCourses();
    }

    @GetMapping("/{courseId}/tees")
    public List<CourseTeeListResponse> getCourseTees(@PathVariable Long courseId) {
        return courseService.getActiveTeesForCourse(courseId);
    }
    }
