package com.myrtletrip.course.controller;

import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-tees")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/{courseTeeId}/holes")
    public List<CourseHole> getHolesForTee(@PathVariable Long courseTeeId) {
        return courseService.getHolesForTee(courseTeeId);
    }
}