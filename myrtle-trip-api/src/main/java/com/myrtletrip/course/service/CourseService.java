package com.myrtletrip.course.service;

import com.myrtletrip.course.dto.CourseDetailResponse;
import com.myrtletrip.course.dto.CourseHoleResponse;
import com.myrtletrip.course.dto.CourseListResponse;
import com.myrtletrip.course.dto.CourseSummaryResponse;
import com.myrtletrip.course.dto.CourseTeeListResponse;
import com.myrtletrip.course.dto.CourseTeeResponse;
import com.myrtletrip.course.dto.SaveCourseHoleRequest;
import com.myrtletrip.course.dto.SaveCourseRequest;
import com.myrtletrip.course.dto.SaveCourseTeeRequest;
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseService {

    private final CourseHoleRepository courseHoleRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;

    public CourseService(CourseHoleRepository courseHoleRepository,
                         CourseRepository courseRepository,
                         CourseTeeRepository courseTeeRepository) {
        this.courseHoleRepository = courseHoleRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
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
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found");
        }

        List<CourseTee> tees = courseTeeRepository.findByCourse_IdAndActiveTrueOrderByTeeNameAsc(courseId);
        List<CourseTeeListResponse> responses = new ArrayList<>();

        for (CourseTee tee : tees) {
            responses.add(toCourseTeeListResponse(tee));
        }

        return responses;
    }

    public List<CourseTeeResponse> getAllTeesForCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found");
        }

        List<CourseTee> tees = courseTeeRepository.findByCourse_IdOrderByTeeNameAsc(courseId);
        List<CourseTeeResponse> responses = new ArrayList<>();

        for (CourseTee tee : tees) {
            responses.add(toCourseTeeResponse(tee));
        }

        return responses;
    }

    public CourseTeeResponse getTeeDetail(Long teeId) {
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        return toCourseTeeResponse(tee);
    }

    @Transactional
    public CourseTeeResponse createTee(Long courseId, SaveCourseTeeRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        validateTeeRequest(courseId, request, null);

        CourseTee tee = new CourseTee();
        tee.setCourse(course);
        applyTeeValues(tee, request);

        if (tee.isActive() != Boolean.FALSE) {
            tee.setActive(true);
        }

        CourseTee saved = courseTeeRepository.save(tee);
        return toCourseTeeResponse(saved);
    }

    @Transactional
    public CourseTeeResponse updateTee(Long teeId, SaveCourseTeeRequest request) {
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        validateTeeRequest(tee.getCourse().getId(), request, teeId);
        applyTeeValues(tee, request);

        CourseTee saved = courseTeeRepository.save(tee);
        return toCourseTeeResponse(saved);
    }

    @Transactional
    public CourseTeeResponse setTeeActive(Long teeId, boolean active) {
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        tee.setActive(active);
        courseTeeRepository.save(tee);

        return toCourseTeeResponse(tee);
    }

    public List<CourseHoleResponse> getHolesForTeeResponse(Long courseTeeId) {
        if (!courseTeeRepository.existsById(courseTeeId)) {
            throw new IllegalArgumentException("Course tee not found");
        }

        List<CourseHole> holes = courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTeeId);
        List<CourseHoleResponse> responses = new ArrayList<>();

        for (CourseHole hole : holes) {
            responses.add(toCourseHoleResponse(hole));
        }

        return responses;
    }

    @Transactional
    public List<CourseHoleResponse> saveHolesForTee(Long courseTeeId, List<SaveCourseHoleRequest> requests) {
        CourseTee tee = courseTeeRepository.findById(courseTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        validateHoleRequests(tee, requests);

        courseHoleRepository.deleteByCourseTee_Id(courseTeeId);
        courseHoleRepository.flush();

        List<CourseHole> holesToSave = new ArrayList<>();

        for (SaveCourseHoleRequest request : requests) {
            CourseHole hole = new CourseHole();
            hole.setCourseTee(tee);
            hole.setHoleNumber(request.getHoleNumber());
            hole.setPar(request.getPar());
            hole.setHandicap(request.getHandicap());
            hole.setYardage(request.getYardage());
            holesToSave.add(hole);
        }

        courseHoleRepository.saveAll(holesToSave);
        courseHoleRepository.flush();

        return getHolesForTeeResponse(courseTeeId);
    }
    
    public List<CourseHole> getHolesForTee(Long courseTeeId) {
        return courseHoleRepository.findByCourseTee_IdOrderByHoleNumberAsc(courseTeeId);
    }

    public Integer getHolePar(Long courseTeeId, int holeNumber) {
        return courseHoleRepository.findByCourseTee_IdAndHoleNumber(courseTeeId, holeNumber)
                .map(CourseHole::getPar)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hole not found for tee " + courseTeeId + ", hole " + holeNumber));
    }

    public Integer getHoleHandicap(Long courseTeeId, int holeNumber) {
        return courseHoleRepository.findByCourseTee_IdAndHoleNumber(courseTeeId, holeNumber)
                .map(CourseHole::getHandicap)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hole not found for tee " + courseTeeId + ", hole " + holeNumber));
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

    private void validateTeeRequest(Long courseId, SaveCourseTeeRequest request, Long existingTeeId) {
        if (request == null) {
            throw new IllegalArgumentException("Course tee request is required");
        }

        String teeName = trimToNull(request.getTeeName());
        if (teeName == null) {
            throw new IllegalArgumentException("Tee name is required");
        }

        if (request.getCourseRating() == null) {
            throw new IllegalArgumentException("Course rating is required");
        }

        if (request.getSlope() == null || request.getSlope() <= 0) {
            throw new IllegalArgumentException("Slope is required");
        }

        if (request.getParTotal() == null || request.getParTotal() <= 0) {
            throw new IllegalArgumentException("Par total is required");
        }

        boolean duplicateName;
        if (existingTeeId == null) {
            duplicateName = courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCase(courseId, teeName);
        } else {
            duplicateName = courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCaseAndIdNot(courseId, teeName, existingTeeId);
        }

        if (duplicateName) {
            throw new IllegalArgumentException("Tee name already exists for this course");
        }
    }

    private void applyTeeValues(CourseTee tee, SaveCourseTeeRequest request) {
        tee.setTeeName(trimToNull(request.getTeeName()));
        tee.setCourseRating(request.getCourseRating());
        tee.setSlope(request.getSlope());
        tee.setParTotal(request.getParTotal());

        if (request.getActive() == null) {
            if (!tee.isActive()) {
                tee.setActive(true);
            }
        } else {
            tee.setActive(request.getActive());
        }
    }

    private void validateHoleRequests(CourseTee tee, List<SaveCourseHoleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one hole is required");
        }

        Set<Integer> holeNumbers = new HashSet<>();
        Set<Integer> handicaps = new HashSet<>();
        int parTotal = 0;

        for (SaveCourseHoleRequest request : requests) {
            if (request.getHoleNumber() == null || request.getHoleNumber() < 1 || request.getHoleNumber() > 18) {
                throw new IllegalArgumentException("Hole number must be between 1 and 18");
            }

            if (!holeNumbers.add(request.getHoleNumber())) {
                throw new IllegalArgumentException("Duplicate hole number: " + request.getHoleNumber());
            }

            if (request.getPar() == null || request.getPar() < 3 || request.getPar() > 6) {
                throw new IllegalArgumentException("Invalid par for hole " + request.getHoleNumber());
            }

            if (request.getHandicap() == null || request.getHandicap() < 1 || request.getHandicap() > 18) {
                throw new IllegalArgumentException("Handicap must be between 1 and 18 for hole " + request.getHoleNumber());
            }

            if (!handicaps.add(request.getHandicap())) {
                throw new IllegalArgumentException("Duplicate handicap: " + request.getHandicap());
            }

            if (request.getYardage() != null && request.getYardage() <= 0) {
                throw new IllegalArgumentException("Yardage must be positive for hole " + request.getHoleNumber());
            }

            parTotal += request.getPar();
        }

        if (requests.size() != 18) {
            throw new IllegalArgumentException("Exactly 18 holes are required");
        }

        if (tee.getParTotal() != null && parTotal != tee.getParTotal()) {
            throw new IllegalArgumentException(
                    "Hole par total " + parTotal + " does not match tee par total " + tee.getParTotal());
        }
    }

    private CourseListResponse toCourseListResponse(Course course) {
        CourseListResponse response = new CourseListResponse();
        response.setCourseId(course.getId());
        response.setCourseName(course.getName());
        response.setLocation(course.getLocation());
        return response;
    }

    private CourseTeeListResponse toCourseTeeListResponse(CourseTee tee) {
        CourseTeeListResponse response = new CourseTeeListResponse();
        response.setCourseTeeId(tee.getId());
        response.setCourseId(tee.getCourse().getId());
        response.setTeeName(tee.getTeeName());
        response.setCourseRating(tee.getCourseRating());
        response.setSlope(tee.getSlope());
        response.setParTotal(tee.getParTotal());
        return response;
    }

    private CourseTeeResponse toCourseTeeResponse(CourseTee tee) {
        return new CourseTeeResponse(
                tee.getId(),
                tee.getCourse().getId(),
                tee.getTeeName(),
                tee.getCourseRating(),
                tee.getSlope(),
                tee.getParTotal(),
                tee.isActive()
        );
    }

    private CourseHoleResponse toCourseHoleResponse(CourseHole hole) {
        return new CourseHoleResponse(
                hole.getId(),
                hole.getHoleNumber(),
                hole.getPar(),
                hole.getHandicap(),
                hole.getYardage()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
