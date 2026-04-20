package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CourseAdminService {

    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;

    public CourseAdminService(
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository) {
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseSummaryResponse> getAllCourses() {
        List<Course> courses = courseRepository.findAll();
        courses.sort(Comparator.comparing(Course::getName, String.CASE_INSENSITIVE_ORDER));

        List<CourseSummaryResponse> response = new ArrayList<>();

        for (Course course : courses) {
            response.add(new CourseSummaryResponse(
                    course.getId(),
                    course.getLegacyCourseNumber(),
                    course.getName(),
                    course.getLocation(),
                    (int) courseTeeRepository.countByCourse_Id(course.getId()),
                    normalizeCourseActive(course.getActive())
            ));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

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
                normalizeCourseActive(course.getActive())
        );
    }

    public Long createCourse(SaveCourseRequest request) {
        validateCourseRequest(request);

        Course course = new Course();
        applyCourseValues(course, request);

        Course saved = courseRepository.save(course);
        return saved.getId();
    }

    public void updateCourse(Long courseId, SaveCourseRequest request) {
        validateCourseRequest(request);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        applyCourseValues(course, request);
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<CourseTeeResponse> getCourseTees(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new RuntimeException("Course not found: " + courseId);
        }

        List<CourseTee> tees = courseTeeRepository.findByCourse_IdOrderByTeeNameAsc(courseId);
        List<CourseTeeResponse> response = new ArrayList<>();

        for (CourseTee tee : tees) {
            response.add(new CourseTeeResponse(
                    tee.getId(),
                    tee.getCourse().getId(),
                    tee.getTeeName(),
                    tee.getCourseRating(),
                    tee.getSlope(),
                    tee.getParTotal(),
                    tee.isActive()
            ));
        }

        return response;
    }

    public Long createCourseTee(Long courseId, SaveCourseTeeRequest request) {
        validateCourseTeeRequest(request);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        String teeName = request.getTeeName().trim();

        if (courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCase(courseId, teeName)) {
            throw new RuntimeException("A tee with that name already exists for this course");
        }

        CourseTee tee = new CourseTee();
        tee.setCourse(course);
        tee.setTeeName(teeName);
        tee.setCourseRating(request.getCourseRating());
        tee.setSlope(request.getSlope());
        tee.setParTotal(request.getParTotal());
        tee.setActive(request.getActive() == null ? true : request.getActive());

        CourseTee saved = courseTeeRepository.save(tee);
        return saved.getId();
    }

    public void updateCourseTee(Long teeId, SaveCourseTeeRequest request) {
        validateCourseTeeRequest(request);

        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new RuntimeException("Course tee not found: " + teeId));

        String teeName = request.getTeeName().trim();

        if (courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCaseAndIdNot(
                tee.getCourse().getId(),
                teeName,
                teeId)) {
            throw new RuntimeException("A tee with that name already exists for this course");
        }

        tee.setTeeName(teeName);
        tee.setCourseRating(request.getCourseRating());
        tee.setSlope(request.getSlope());
        tee.setParTotal(request.getParTotal());
        tee.setActive(request.getActive() == null ? true : request.getActive());

        courseTeeRepository.save(tee);
    }

    private void applyCourseValues(Course course, SaveCourseRequest request) {
        course.setLegacyCourseNumber(request.getLegacyCourseNumber());
        course.setName(request.getCourseName().trim());
        course.setLocation(trimToNull(request.getLocation()));
        course.setAddressLine1(trimToNull(request.getAddressLine1()));
        course.setAddressLine2(trimToNull(request.getAddressLine2()));
        course.setCity(trimToNull(request.getCity()));
        course.setState(trimToNull(request.getState()));
        course.setPostalCode(trimToNull(request.getPostalCode()));
        course.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        course.setWebsiteUrl(trimToNull(request.getWebsiteUrl()));
        course.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
    }

    private void validateCourseRequest(SaveCourseRequest request) {
        if (request == null) {
            throw new RuntimeException("Course request is required");
        }

        if (request.getCourseName() == null || request.getCourseName().trim().isEmpty()) {
            throw new RuntimeException("Course name is required");
        }
    }

    private void validateCourseTeeRequest(SaveCourseTeeRequest request) {
        if (request == null) {
            throw new RuntimeException("Course tee request is required");
        }

        if (request.getTeeName() == null || request.getTeeName().trim().isEmpty()) {
            throw new RuntimeException("Tee name is required");
        }

        if (request.getCourseRating() == null) {
            throw new RuntimeException("Course rating is required");
        }

        if (request.getSlope() == null) {
            throw new RuntimeException("Slope is required");
        }

        if (request.getParTotal() == null) {
            throw new RuntimeException("Par total is required");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Boolean normalizeCourseActive(Boolean active) {
        return active == null ? Boolean.TRUE : active;
    }
}
