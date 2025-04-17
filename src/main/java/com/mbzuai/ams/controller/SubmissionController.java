package com.mbzuai.ams.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mbzuai.ams.model.Assignment;
import com.mbzuai.ams.model.FileDocument;
import com.mbzuai.ams.model.Submission;
import com.mbzuai.ams.model.User;
import com.mbzuai.ams.payload.request.GradeRequest;
import com.mbzuai.ams.payload.response.SubmissionResponse;
import com.mbzuai.ams.repository.AssignmentRepository;
import com.mbzuai.ams.repository.FileRepository;
import com.mbzuai.ams.repository.SubmissionRepository;
import com.mbzuai.ams.repository.UserRepository;
import com.mbzuai.ams.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        List<Submission> submissions = submissionRepository.findByAssignment(assignment);
        List<SubmissionResponse> responses = submissions.stream()
                .map(submission -> new SubmissionResponse(
                        submission.getId(),
                        submission.getAssignment().getId(),
                        submission.getStudent().getId(),
                        submission.getStudent().getFullName(),
                        submission.getFileName(),
                        submission.getSubmittedAt(),
                        submission.getGrade(),
                        submission.getFeedback(),
                        submission.getGradedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<List<SubmissionResponse>> getStudentSubmissions() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User student = userRepository.findById(userDetails.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<Submission> submissions = submissionRepository.findByStudent(student);
        List<SubmissionResponse> responses = submissions.stream()
                .map(submission -> new SubmissionResponse(
                        submission.getId(),
                        submission.getAssignment().getId(),
                        submission.getStudent().getId(),
                        submission.getStudent().getFullName(),
                        submission.getFileName(),
                        submission.getSubmittedAt(),
                        submission.getGrade(),
                        submission.getFeedback(),
                        submission.getGradedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        // Check if user is authorized (teacher or the student who submitted)
        boolean isTeacher = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEACHER"));
        boolean isOwner = submission.getStudent().getId().equals(userDetails.id());

        if (!isTeacher && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SubmissionResponse response = new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getFullName(),
                submission.getFileName(),
                submission.getSubmittedAt(),
                submission.getGrade(),
                submission.getFeedback(),
                submission.getGradedAt());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasRole('ROLE_STUDENT')")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User student = userRepository.findById(userDetails.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Check if student already submitted
        submissionRepository.findByAssignmentAndStudent(assignment, student)
                .ifPresent(s -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment already submitted");
                });

        // Check if past due date
        if (LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission deadline has passed");
        }

        // Store file in MongoDB
        FileDocument fileDocument = new FileDocument();
        fileDocument.setFileName(file.getOriginalFilename());
        fileDocument.setContentType(file.getContentType());
        fileDocument.setContent(file.getBytes());
        FileDocument savedFile = fileRepository.save(fileDocument);

        // Create submission
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setFileId(savedFile.getId());
        submission.setFileName(file.getOriginalFilename());
        
        Submission savedSubmission = submissionRepository.save(submission);

        SubmissionResponse response = new SubmissionResponse(
                savedSubmission.getId(),
                savedSubmission.getAssignment().getId(),
                savedSubmission.getStudent().getId(),
                savedSubmission.getStudent().getFullName(),
                savedSubmission.getFileName(),
                savedSubmission.getSubmittedAt(),
                null,
                null,
                null);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        // Check if user is authorized (teacher or the student who submitted)
        boolean isTeacher = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEACHER"));
        boolean isOwner = submission.getStudent().getId().equals(userDetails.id());

        if (!isTeacher && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        FileDocument file = fileRepository.findById(submission.getFileId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(file.getContent());
    }

    @PutMapping("/{id}/grade")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long id,
            @Valid @RequestBody GradeRequest request) {
        
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        submission.setGrade(request.getGrade());
        submission.setFeedback(request.getFeedback());
        submission.setGradedAt(LocalDateTime.now());
        
        Submission gradedSubmission = submissionRepository.save(submission);

        SubmissionResponse response = new SubmissionResponse(
                gradedSubmission.getId(),
                gradedSubmission.getAssignment().getId(),
                gradedSubmission.getStudent().getId(),
                gradedSubmission.getStudent().getFullName(),
                gradedSubmission.getFileName(),
                gradedSubmission.getSubmittedAt(),
                gradedSubmission.getGrade(),
                gradedSubmission.getFeedback(),
                gradedSubmission.getGradedAt());

        return ResponseEntity.ok(response);
    }
}

