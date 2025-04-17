package com.mbzuai.ams.repository;

import com.mbzuai.ams.model.Assignment;
import com.mbzuai.ams.model.User;
import com.mbzuai.ams.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByAssignment(Assignment assignment);
    List<Submission> findByStudent(User student);
    Optional<Submission> findByAssignmentAndStudent(Assignment assignment, User student);
}
