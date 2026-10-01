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
import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.entity.CourseTeeComboHole;
import com.myrtletrip.course.model.TeeType;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeComboHoleRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CourseService {

    private final CourseHoleRepository courseHoleRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseTeeComboHoleRepository courseTeeComboHoleRepository;
    private final CourseHoleService courseHoleService;

    public CourseService(CourseHoleRepository courseHoleRepository,
                         CourseRepository courseRepository,
                         CourseTeeRepository courseTeeRepository,
                         CourseTeeComboHoleRepository courseTeeComboHoleRepository,
                         CourseHoleService courseHoleService) {
        this.courseHoleRepository = courseHoleRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseTeeComboHoleRepository = courseTeeComboHoleRepository;
        this.courseHoleService = courseHoleService;
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

        List<CourseTee> tees = courseTeeRepository.findByCourse_IdAndActiveTrueOrderByTeeNameAscEffectiveDateDesc(courseId);
        tees.sort((a, b) -> {
            double left = a.getCourseRating() == null ? -999.0 : a.getCourseRating().doubleValue();
            double right = b.getCourseRating() == null ? -999.0 : b.getCourseRating().doubleValue();
            int ratingCompare = Double.compare(right, left);
            if (ratingCompare != 0) {
                return ratingCompare;
            }
            String leftName = a.getTeeName() == null ? "" : a.getTeeName().toLowerCase();
            String rightName = b.getTeeName() == null ? "" : b.getTeeName().toLowerCase();
            return leftName.compareTo(rightName);
        });
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

        List<CourseTee> tees = courseTeeRepository.findByCourse_IdOrderByTeeNameAscEffectiveDateDesc(courseId);
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

        CourseTee saved = courseTeeRepository.saveAndFlush(tee);
        return toCourseTeeResponse(saved);
    }

    @Transactional
    public CourseTeeResponse updateTee(Long teeId, SaveCourseTeeRequest request) {
        CourseTee tee = courseTeeRepository.findById(teeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        validateTeeRequest(tee.getCourse().getId(), request, teeId);
        applyTeeValues(tee, request);

        if (isComboTee(tee)) {
            courseHoleRepository.deleteByCourseTee_Id(teeId);
        } else {
            courseTeeComboHoleRepository.deleteByComboTee_Id(teeId);
        }

        CourseTee saved = courseTeeRepository.saveAndFlush(tee);
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
        CourseTee comboTee = courseTeeRepository.findById(comboTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(comboTee)) {
            throw new IllegalArgumentException("Tee is not a combo tee");
        }

        List<CourseTeeComboHole> mappings = courseTeeComboHoleRepository.findByComboTee_IdOrderByHoleNumberAsc(comboTeeId);
        List<CourseTeeComboHoleResponse> responses = new ArrayList<>();

        for (CourseTeeComboHole mapping : mappings) {
            responses.add(toComboHoleResponse(mapping));
        }

        return responses;
    }

    @Transactional
    public List<CourseTeeComboHoleResponse> saveComboHolesForTee(Long comboTeeId, List<SaveCourseTeeComboHoleRequest> requests) {
        CourseTee comboTee = courseTeeRepository.findById(comboTeeId)
                .orElseThrow(() -> new IllegalArgumentException("Course tee not found"));

        if (!isComboTee(comboTee)) {
            throw new IllegalArgumentException("Tee is not a combo tee");
        }

        validateComboHoleRequests(comboTee, requests);

        courseTeeComboHoleRepository.deleteByComboTee_Id(comboTeeId);
        courseTeeComboHoleRepository.flush();

        List<CourseTeeComboHole> mappingsToSave = new ArrayList<>();
        for (SaveCourseTeeComboHoleRequest request : requests) {
            CourseTee sourceTee = courseTeeRepository.findById(request.getSourceTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Source tee not found"));

            CourseTeeComboHole mapping = new CourseTeeComboHole();
            mapping.setComboTee(comboTee);
            mapping.setHoleNumber(request.getHoleNumber());
            mapping.setSourceTee(sourceTee);
            mappingsToSave.add(mapping);
        }

        courseTeeComboHoleRepository.saveAll(mappingsToSave);
        courseTeeComboHoleRepository.flush();
        updateComboTeeTotals(comboTee);

        return getComboHolesForTee(comboTeeId);
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

        LocalDate effectiveDate = normalizeEffectiveDate(request.getEffectiveDate());

        if (request.getRetiredDate() != null && request.getRetiredDate().isBefore(effectiveDate)) {
            throw new IllegalArgumentException("Retired date cannot be before effective date");
        }

        boolean hasMenRating = request.getCourseRating() != null && request.getSlope() != null && request.getSlope() > 0;
        boolean hasWomenRating = request.getWomenCourseRating() != null && request.getWomenSlope() != null && request.getWomenSlope() > 0;

        if (!hasMenRating && !hasWomenRating) {
            throw new IllegalArgumentException("At least one complete rating/slope set is required.");
        }

        if (request.getCourseRating() != null && (request.getSlope() == null || request.getSlope() <= 0)) {
            throw new IllegalArgumentException("Men's slope is required when men's course rating is entered.");
        }

        if (request.getSlope() != null && request.getSlope() > 0 && request.getCourseRating() == null) {
            throw new IllegalArgumentException("Men's course rating is required when men's slope is entered.");
        }

        if (request.getWomenCourseRating() != null && (request.getWomenSlope() == null || request.getWomenSlope() <= 0)) {
            throw new IllegalArgumentException("Women's slope is required when women's course rating is entered.");
        }

        if (request.getWomenSlope() != null && request.getWomenSlope() > 0 && request.getWomenCourseRating() == null) {
            throw new IllegalArgumentException("Women's course rating is required when women's slope is entered.");
        }

        if (request.getParTotal() == null || request.getParTotal() <= 0) {
            throw new IllegalArgumentException("Primary par total is required");
        }

        if (request.getYardageTotal() != null && request.getYardageTotal() <= 0) {
            throw new IllegalArgumentException("Yardage total must be positive when provided");
        }

        parseTeeType(request.getTeeType());

        boolean duplicateVersion;
        if (existingTeeId == null) {
            duplicateVersion = courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCaseAndEffectiveDate(courseId, teeName, effectiveDate);
        } else {
            duplicateVersion = courseTeeRepository.existsByCourse_IdAndTeeNameIgnoreCaseAndEffectiveDateAndIdNot(courseId, teeName, effectiveDate, existingTeeId);
        }

        if (duplicateVersion) {
            throw new IllegalArgumentException("A tee version with that name and effective date already exists for this course");
        }
    }

    private void applyTeeValues(CourseTee tee, SaveCourseTeeRequest request) {
        if (tee == null || request == null) {
            throw new IllegalArgumentException("Course tee request is required");
        }

        tee.setTeeName(trimToNull(request.getTeeName()));
        tee.setTeeType(parseTeeType(request.getTeeType()));
        tee.setEffectiveDate(normalizeEffectiveDate(request.getEffectiveDate()));
        tee.setRetiredDate(request.getRetiredDate());
        tee.setCourseRating(request.getCourseRating());
        tee.setSlope(request.getSlope());
        tee.setParTotal(request.getParTotal());
        tee.setYardageTotal(request.getYardageTotal());
        tee.setWomenCourseRating(request.getWomenCourseRating());
        tee.setWomenSlope(request.getWomenSlope());
        tee.setWomenParTotal(request.getWomenParTotal());

        if (request.getActive() == null) {
            tee.setActive(true);
        } else {
            tee.setActive(request.getActive());
        }
    }
    private void validateComboHoleRequests(CourseTee comboTee, List<SaveCourseTeeComboHoleRequest> requests) {
        if (requests == null || requests.size() != 18) {
            throw new IllegalArgumentException("Exactly 18 combo tee hole mappings are required");
        }

        Set<Integer> holeNumbers = new HashSet<>();
        for (SaveCourseTeeComboHoleRequest request : requests) {
            if (request.getHoleNumber() == null || request.getHoleNumber() < 1 || request.getHoleNumber() > 18) {
                throw new IllegalArgumentException("Hole number must be between 1 and 18");
            }

            if (!holeNumbers.add(request.getHoleNumber())) {
                throw new IllegalArgumentException("Duplicate hole number: " + request.getHoleNumber());
            }

            if (request.getSourceTeeId() == null) {
                throw new IllegalArgumentException("Source tee is required for hole " + request.getHoleNumber());
            }

            CourseTee sourceTee = courseTeeRepository.findById(request.getSourceTeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Source tee not found"));

            if (sourceTee.getId().equals(comboTee.getId())) {
                throw new IllegalArgumentException("Combo tee cannot source holes from itself");
            }

            if (!sourceTee.getCourse().getId().equals(comboTee.getCourse().getId())) {
                throw new IllegalArgumentException("Source tees must belong to the same course as the combo tee");
            }

            if (isComboTee(sourceTee)) {
                throw new IllegalArgumentException("Combo tees cannot use another combo tee as a source tee");
            }

            courseHoleRepository.findByCourseTee_IdAndHoleNumber(sourceTee.getId(), request.getHoleNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source tee " + sourceTee.getTeeName() + " does not have hole " + request.getHoleNumber()));
        }
    }
    private void updateComboTeeTotals(CourseTee comboTee) {
        List<CourseHole> resolvedHoles = courseHoleService.getHolesForTee(comboTee.getId());
        int parTotal = 0;
        int womenParTotal = 0;
        int yardageTotal = 0;
        boolean hasAnyMenPar = false;
        boolean hasAnyWomenPar = false;
        boolean hasAnyYardage = false;

        for (CourseHole hole : resolvedHoles) {
            if (hole.getPar() != null) {
                hasAnyMenPar = true;
                parTotal += hole.getPar();
            }

            if (hole.getWomenPar() != null) {
                hasAnyWomenPar = true;
                womenParTotal += hole.getWomenPar();
            }

            if (hole.getYardage() != null) {
                hasAnyYardage = true;
                yardageTotal += hole.getYardage();
            }
        }

        comboTee.setParTotal(hasAnyMenPar ? parTotal : womenParTotal);
        comboTee.setWomenParTotal(hasAnyWomenPar ? womenParTotal : null);
        comboTee.setYardageTotal(hasAnyYardage ? yardageTotal : null);
        courseTeeRepository.save(comboTee);
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
        response.setTeeType(getTeeTypeName(tee));
        response.setEffectiveDate(tee.getEffectiveDate());
        response.setRetiredDate(tee.getRetiredDate());
        response.setCourseRating(tee.getCourseRating());
        response.setSlope(tee.getSlope());
        response.setParTotal(tee.getParTotal());
        response.setYardageTotal(tee.getYardageTotal());
        response.setWomenCourseRating(tee.getWomenCourseRating());
        response.setWomenSlope(tee.getWomenSlope());
        response.setWomenParTotal(tee.getWomenParTotal());
        response.setHoleCount(resolvedHoleCountForList(tee));
        return response;
    }

    private Long resolvedHoleCountForList(CourseTee tee) {
        if (tee == null || tee.getId() == null) {
            return 0L;
        }

        if (!isComboTee(tee)) {
            return courseHoleRepository.countByCourseTee_Id(tee.getId());
        }

        return courseTeeComboHoleRepository.countByComboTee_Id(tee.getId());
    }

    private CourseTeeResponse toCourseTeeResponse(CourseTee tee) {
        return new CourseTeeResponse(
                tee.getId(),
                tee.getCourse().getId(),
                tee.getTeeName(),
                getTeeTypeName(tee),
                tee.getEffectiveDate(),
                tee.getRetiredDate(),
                tee.getCourseRating(),
                tee.getSlope(),
                tee.getParTotal(),
                tee.getYardageTotal(),
                tee.getWomenCourseRating(),
                tee.getWomenSlope(),
                tee.getWomenParTotal(),
                tee.isActive()
        );
    }

    private CourseTeeComboHoleResponse toComboHoleResponse(CourseTeeComboHole mapping) {
        Long sourceTeeId = null;
        String sourceTeeName = null;

        if (mapping.getSourceTee() != null) {
            sourceTeeId = mapping.getSourceTee().getId();
            sourceTeeName = mapping.getSourceTee().getTeeName();
        }

        return new CourseTeeComboHoleResponse(
                mapping.getId(),
                mapping.getHoleNumber(),
                sourceTeeId,
                sourceTeeName
        );
    }
    private TeeType parseTeeType(String teeType) {
        if (teeType == null || teeType.trim().isEmpty()) {
            return TeeType.REGULAR;
        }

        String normalized = teeType.trim().toUpperCase();
        if ("COMBO".equals(normalized)) {
            return TeeType.COMBO;
        }

        if ("REGULAR".equals(normalized)) {
            return TeeType.REGULAR;
        }

        throw new IllegalArgumentException("Invalid tee type: " + teeType);
    }

    private boolean isComboTee(CourseTee tee) {
        return tee.getTeeType() == TeeType.COMBO;
    }

    private String getTeeTypeName(CourseTee tee) {
        return tee.getTeeType() == null ? TeeType.REGULAR.name() : tee.getTeeType().name();
    }

    private LocalDate normalizeEffectiveDate(LocalDate effectiveDate) {
        return effectiveDate == null ? LocalDate.of(1900, 1, 1) : effectiveDate;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
