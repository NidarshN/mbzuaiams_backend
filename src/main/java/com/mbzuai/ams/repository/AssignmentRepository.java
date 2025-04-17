package com.mbzuai.ams.repository;

import com.mbzuai.ams.model.User;
import com.mbzuai.ams.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByOrderByDueDateDesc();
    List<Assignment> findByTeacherOrderByDueDateDesc(User teacher);
}
