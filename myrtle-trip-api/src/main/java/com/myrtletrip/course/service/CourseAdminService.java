package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseAdminService {

	private final CourseService courseService;

	public CourseAdminService(CourseService courseService) {
		this.courseService = courseService;
	}

	public List<CourseSummaryResponse> getAllCourses() {
		return courseService.getAllCourseSummaries();
	}

	public CourseDetailResponse getCourse(Long courseId) {
		return courseService.getCourseDetail(courseId);
	}

	public Long createCourse(SaveCourseRequest request) {
		return courseService.createCourse(request).getCourseId();
	}

	public void updateCourse(Long courseId, SaveCourseRequest request) {
		courseService.updateCourse(courseId, request);
	}

	public List<CourseTeeResponse> getCourseTees(Long courseId) {
		return courseService.getAllTeesForCourse(courseId);
	}

	public Long createCourseTee(Long courseId, SaveCourseTeeRequest request) {
		return courseService.createTee(courseId, request).getTeeId();
	}

	public void updateCourseTee(Long teeId, SaveCourseTeeRequest request) {
		courseService.updateTee(teeId, request);
	}
}
