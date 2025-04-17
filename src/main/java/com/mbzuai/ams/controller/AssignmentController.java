package com.mbzuai.ams.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.mbzuai.ams.model.Assignment;
import com.mbzuai.ams.model.User;
import com.mbzuai.ams.payload.request.AssignmentRequest;
import com.mbzuai.ams.payload.response.AssignmentResponse;
import com.mbzuai.ams.repository.AssignmentRepository;
import com.mbzuai.ams.repository.UserRepository;
import com.mbzuai.ams.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {
    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAllAssignments() {
        List<Assignment> assignments = assignmentRepository.findAllByOrderByDueDateDesc();
        List<AssignmentResponse> responses = assignments.stream()
                .map(assignment -> new AssignmentResponse(
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getDescription(),
                        assignment.getDueDate(),
                        assignment.getTeacher().getFullName(),
                        assignment.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        AssignmentResponse response = new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getTeacher().getFullName(),
                assignment.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<AssignmentResponse> createAssignment(@Valid @RequestBody AssignmentRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User teacher = userRepository.findById(userDetails.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setTeacher(teacher);
        
        Assignment savedAssignment = assignmentRepository.save(assignment);

        AssignmentResponse response = new AssignmentResponse(
                savedAssignment.getId(),
                savedAssignment.getTitle(),
                savedAssignment.getDescription(),
                savedAssignment.getDueDate(),
                savedAssignment.getTeacher().getFullName(),
                savedAssignment.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<AssignmentResponse> updateAssignment(@PathVariable Long id, @Valid @RequestBody AssignmentRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Check if the teacher is the owner of the assignment
        if (!assignment.getTeacher().getId().equals(userDetails.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        
        Assignment updatedAssignment = assignmentRepository.save(assignment);

        AssignmentResponse response = new AssignmentResponse(
                updatedAssignment.getId(),
                updatedAssignment.getTitle(),
                updatedAssignment.getDescription(),
                updatedAssignment.getDueDate(),
                updatedAssignment.getTeacher().getFullName(),
                updatedAssignment.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Check if the teacher is the owner of the assignment
        if (!assignment.getTeacher().getId().equals(userDetails.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        assignmentRepository.delete(assignment);
        return ResponseEntity.noContent().build();
    }
}
