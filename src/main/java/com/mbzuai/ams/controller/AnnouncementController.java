package com.mbzuai.ams.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.mbzuai.ams.model.Announcement;
import com.mbzuai.ams.model.User;
import com.mbzuai.ams.payload.request.AnnouncementRequest;
import com.mbzuai.ams.payload.response.AnnouncementResponse;
import com.mbzuai.ams.repository.AnnouncementRepository;
import com.mbzuai.ams.repository.UserRepository;
import com.mbzuai.ams.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        List<Announcement> announcements = announcementRepository.findAllByOrderByCreatedAtDesc();
        List<AnnouncementResponse> responses = announcements.stream()
                .map(announcement -> new AnnouncementResponse(
                        announcement.getId(),
                        announcement.getTitle(),
                        announcement.getContent(),
                        announcement.getTeacher().getFullName(),
                        announcement.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(@PathVariable Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        AnnouncementResponse response = new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getTeacher().getFullName(),
                announcement.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User teacher = userRepository.findById(userDetails.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setTeacher(teacher);
        
        Announcement savedAnnouncement = announcementRepository.save(announcement);

        AnnouncementResponse response = new AnnouncementResponse(
                savedAnnouncement.getId(),
                savedAnnouncement.getTitle(),
                savedAnnouncement.getContent(),
                savedAnnouncement.getTeacher().getFullName(),
                savedAnnouncement.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementRequest request) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        // Check if the teacher is the owner of the announcement
        if (!announcement.getTeacher().getId().equals(userDetails.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        
        Announcement updatedAnnouncement = announcementRepository.save(announcement);

        AnnouncementResponse response = new AnnouncementResponse(
                updatedAnnouncement.getId(),
                updatedAnnouncement.getTitle(),
                updatedAnnouncement.getContent(),
                updatedAnnouncement.getTeacher().getFullName(),
                updatedAnnouncement.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TEACHER')")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        // Check if the teacher is the owner of the announcement
        if (!announcement.getTeacher().getId().equals(userDetails.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        announcementRepository.delete(announcement);
        return ResponseEntity.noContent().build();
    }
}