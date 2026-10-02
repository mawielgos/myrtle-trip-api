package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseHoleResponse;
import com.myrtletrip.course.dto.CourseListResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeComboHoleResponse;
import com.myrtletrip.course.dto.CourseTeeListResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeComboHoleRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleService courseHoleService;
    private final CourseComboTeeService courseComboTeeService;
    private final CourseTeeService courseTeeService;

    public CourseService(CourseRepository courseRepository,
                         CourseTeeRepository courseTeeRepository,
                         CourseHoleService courseHoleService,
                         CourseComboTeeService courseComboTeeService,
                         CourseTeeService courseTeeService) {
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleService = courseHoleService;
        this.courseComboTeeService = courseComboTeeService;
        this.courseTeeService = courseTeeService;
    }

    public List<CourseListResponse> getActiveCourses() {
        List<CourseListResponse> responses = new ArrayList<>();
        List<Course> courses = courseRepository.findByActiveTrueOrderByNameAsc();

        for (Course course : courses) {
            responses.add(toCourseListResponse(course));
        }

        return responses;
    }

    public List<CourseSummaryResponse> getAllCourseSummaries() {
        List<Course> courses = courseRepository.findAll();
        List<CourseSummaryResponse> responses = new ArrayList<>();

        courses.sort((a, b) -> {
            String left = a.getName() == null ? "" : a.getName().toLowerCase();
            String right = b.getName() == null ? "" : b.getName().toLowerCase();
            return left.compareTo(right);
        });

        for (Course course : courses) {
            int teeCount = (int) courseTeeRepository.countByCourse_Id(course.getId());
            responses.add(new CourseSummaryResponse(
                    course.getId(),
                    course.getLegacyCourseNumber(),
                    course.getName(),
                    course.getLocation(),
                    teeCount,
                    course.getActive()
            ));
        }

        return responses;
    }

    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        return new CourseDetailResponse(
                course.getId(),
                course.getLegacyCourseNumber(),
                course.getName(),
                course.getLocation(),
                course.getAddressLine1(),
                course.getAddressLine2(),
                course.getCity(),
                course.getState(),
                course.getPostalCode(),
                course.getPhoneNumber(),
                course.getWebsiteUrl(),
                course.getActive()
        );
    }

    @Transactional
    public CourseDetailResponse createCourse(SaveCourseRequest request) {
        validateCourseRequest(request, null);

        Course course = new Course();
        applyCourseValues(course, request);

        if (course.getActive() == null) {
            course.setActive(Boolean.TRUE);
        }

        Course saved = courseRepository.save(course);
        return getCourseDetail(saved.getId());
    }

    @Transactional
    public CourseDetailResponse updateCourse(Long courseId, SaveCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        validateCourseRequest(request, courseId);
        applyCourseValues(course, request);

        Course saved = courseRepository.save(course);
        return getCourseDetail(saved.getId());
    }

    @Transactional
    public CourseDetailResponse setCourseActive(Long courseId, boolean active) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        course.setActive(active);
        courseRepository.save(course);

        return getCourseDetail(courseId);
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

    @Transactional
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

    private void validateCourseRequest(SaveCourseRequest request, Long existingCourseId) {
        if (request == null) {
            throw new IllegalArgumentException("Course request is required");
        }

        String courseName = trimToNull(request.getCourseName());
        if (courseName == null) {
            throw new IllegalArgumentException("Course name is required");
        }

        boolean duplicateName;
        if (existingCourseId == null) {
            duplicateName = courseRepository.existsByNameIgnoreCase(courseName);
        } else {
            duplicateName = courseRepository.existsByNameIgnoreCaseAndIdNot(courseName, existingCourseId);
        }

        if (duplicateName) {
            throw new IllegalArgumentException("Course name already exists");
        }
    }

    private void applyCourseValues(Course course, SaveCourseRequest request) {
        course.setLegacyCourseNumber(request.getLegacyCourseNumber());
        course.setName(trimToNull(request.getCourseName()));
        course.setLocation(trimToNull(request.getLocation()));
        course.setAddressLine1(trimToNull(request.getAddressLine1()));
        course.setAddressLine2(trimToNull(request.getAddressLine2()));
        course.setCity(trimToNull(request.getCity()));
        course.setState(trimToNull(request.getState()));
        course.setPostalCode(trimToNull(request.getPostalCode()));
        course.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        course.setWebsiteUrl(trimToNull(request.getWebsiteUrl()));

        if (request.getActive() == null) {
            if (course.getActive() == null) {
                course.setActive(Boolean.TRUE);
            }
        } else {
            course.setActive(request.getActive());
        }
    }
    private CourseListResponse toCourseListResponse(Course course) {
        CourseListResponse response = new CourseListResponse();
        response.setCourseId(course.getId());
        response.setCourseName(course.getName());
        response.setLocation(course.getLocation());
        return response;
    }
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
