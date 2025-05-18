package com.group12.taskmanager.repositories;

import com.group12.taskmanager.models.Project;
import com.group12.taskmanager.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
    //secure
    List<Project> findByGroup_Id(int groupId);

    //secure
    @Query("SELECT p FROM Project p WHERE p.group IN (SELECT g FROM Group g JOIN g.users u WHERE u = :user)")
    Page<Project> findByUser(@Param("user") User user, Pageable pageable);
}
