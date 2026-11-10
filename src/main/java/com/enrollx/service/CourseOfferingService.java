package com.enrollx.service;

import com.enrollx.dto.CourseOfferingDto;
import com.enrollx.dto.OfferingCapacityDto;
import com.enrollx.dto.SeatStatusDto;
import com.enrollx.entity.Course;
import com.enrollx.entity.CourseOffering;
import com.enrollx.entity.SeatReservation;
import com.enrollx.entity.User;
import com.enrollx.enums.EnrollmentStatus;
import com.enrollx.exception.InvalidCapacityException;
import com.enrollx.exception.ResourceNotFoundException;
import com.enrollx.repository.CourseOfferingRepository;
import com.enrollx.repository.EnrollmentRepository;
import com.enrollx.repository.SeatReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final SeatReservationRepository seatReservationRepository;

    public CourseOfferingService(CourseOfferingRepository courseOfferingRepository,
                                 CourseService courseService,
                                 EnrollmentRepository enrollmentRepository,
                                 SeatReservationRepository seatReservationRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.seatReservationRepository = seatReservationRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseOffering> getAllOfferings() {
        return courseOfferingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CourseOffering getOfferingById(Long id) {
        return courseOfferingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found with ID: " + id));
    }

    @Transactional
    public CourseOffering createOffering(CourseOfferingDto dto) {
        if (courseOfferingRepository.existsByCourseIdAndSemesterAndAcademicYear(
                dto.getCourseId(), dto.getSemester().trim(), dto.getAcademicYear().trim())) {
            throw new IllegalArgumentException("An offering for this course in semester '" + 
                    dto.getSemester() + "' and year '" + dto.getAcademicYear() + "' already exists.");
        }

        Course course = courseService.getCourseById(dto.getCourseId());

        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setSemester(dto.getSemester().trim());
        offering.setAcademicYear(dto.getAcademicYear().trim());
        offering.setCapacity(dto.getCapacity());
        offering.setInstructor(dto.getInstructor().trim());
        offering.setRoom(dto.getRoom() != null ? dto.getRoom().trim() : null);
        offering.setSchedule(dto.getSchedule() != null ? dto.getSchedule().trim() : null);
        offering.setActive(dto.isActive());

        return courseOfferingRepository.save(offering);
    }

    @Transactional
    public CourseOffering updateCapacity(Long offeringId, int newCapacity) {
        CourseOffering offering = getOfferingById(offeringId);
        long confirmedEnrollments = enrollmentRepository.countByCourseOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);

        if (newCapacity < confirmedEnrollments) {
            throw new InvalidCapacityException("Cannot reduce capacity to " + newCapacity + 
                    ". There are already " + confirmedEnrollments + " confirmed enrollments!");
        }

        offering.setCapacity(newCapacity);
        return courseOfferingRepository.save(offering);
    }

    @Transactional(readOnly = true)
    public List<OfferingCapacityDto> getAvailableOfferings(String semester, Long departmentId, String searchQuery, User currentUser) {
        List<CourseOffering> offerings = courseOfferingRepository.searchOfferings(
                (semester != null && !semester.isBlank()) ? semester : null,
                departmentId,
                (searchQuery != null && !searchQuery.isBlank()) ? searchQuery.trim() : null
        );

        Instant now = Instant.now();
        List<OfferingCapacityDto> dtoList = new ArrayList<>();

        for (CourseOffering off : offerings) {
            OfferingCapacityDto dto = convertToCapacityDto(off, now, currentUser);
            dtoList.add(dto);
        }
        return dtoList;
    }

    @Transactional(readOnly = true)
    public OfferingCapacityDto getOfferingCapacityDto(Long offeringId, User currentUser) {
        CourseOffering offering = getOfferingById(offeringId);
        return convertToCapacityDto(offering, Instant.now(), currentUser);
    }

    @Transactional(readOnly = true)
    public SeatStatusDto getSeatStatus(Long offeringId) {
        CourseOffering offering = getOfferingById(offeringId);
        Instant now = Instant.now();
        long enrolled = enrollmentRepository.countByCourseOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);
        long reserved = seatReservationRepository.countActiveReservations(offeringId, now);
        int available = Math.max(0, offering.getCapacity() - (int) (enrolled + reserved));

        return new SeatStatusDto(offeringId, offering.getCapacity(), enrolled, reserved, available);
    }

    private OfferingCapacityDto convertToCapacityDto(CourseOffering off, Instant now, User currentUser) {
        OfferingCapacityDto dto = new OfferingCapacityDto();
        dto.setOfferingId(off.getId());
        dto.setCourseId(off.getCourse().getId());
        dto.setCourseCode(off.getCourse().getCode());
        dto.setCourseTitle(off.getCourse().getTitle());
        dto.setDepartmentCode(off.getCourse().getDepartment().getCode());
        dto.setDepartmentName(off.getCourse().getDepartment().getName());
        dto.setCredits(off.getCourse().getCredits());
        dto.setSemester(off.getSemester());
        dto.setAcademicYear(off.getAcademicYear());
        dto.setInstructor(off.getInstructor());
        dto.setRoom(off.getRoom());
        dto.setSchedule(off.getSchedule());
        dto.setCapacity(off.getCapacity());

        long enrolled = enrollmentRepository.countByCourseOfferingIdAndStatus(off.getId(), EnrollmentStatus.ENROLLED);
        long reserved = seatReservationRepository.countActiveReservations(off.getId(), now);
        int available = Math.max(0, off.getCapacity() - (int) (enrolled + reserved));

        dto.setEnrolledCount(enrolled);
        dto.setReservedCount(reserved);
        dto.setAvailableSeats(available);

        if (currentUser != null) {
            boolean alreadyEnrolled = enrollmentRepository.existsByCourseOfferingIdAndUserIdAndStatus(
                    off.getId(), currentUser.getId(), EnrollmentStatus.ENROLLED);
            dto.setAlreadyEnrolled(alreadyEnrolled);

            Optional<SeatReservation> activeRes = seatReservationRepository
                    .findActiveReservationForUserAndOffering(currentUser.getId(), off.getId(), now);
            dto.setHasActiveReservation(activeRes.isPresent());
            activeRes.ifPresent(r -> dto.setActiveReservationId(r.getId()));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctSemesters() {
        return courseOfferingRepository.findDistinctSemesters();
    }

    @Transactional(readOnly = true)
    public long countActiveOfferings() {
        return courseOfferingRepository.findByActiveTrue().size();
    }
}
