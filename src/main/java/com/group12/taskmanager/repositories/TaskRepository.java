package com.group12.taskmanager.repositories;

import com.group12.taskmanager.models.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    //secure
    List<Task> findByProjectId(Integer projectId);
}
