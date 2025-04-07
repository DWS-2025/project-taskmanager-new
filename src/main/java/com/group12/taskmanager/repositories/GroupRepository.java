package com.group12.taskmanager.repositories;

import com.group12.taskmanager.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import com.group12.taskmanager.models.User;
import org.springframework.data.domain.Page;


import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Integer> {

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.users WHERE g.id = :groupId")
    Group findByIdWithUsers(@Param("groupId") int groupId);

    @Modifying
    @Query(value = "DELETE FROM group_user WHERE group_id = :groupId AND user_id = :userId", nativeQuery = true)
    void deleteUserFromGroup(@Param("groupId") int groupId, @Param("userId") int userId);



    @Query("SELECT g FROM Group g JOIN g.users u WHERE u = :user")
    Page<Group> findByUsersContains(@Param("user") User user, Pageable pageable);



    List<Group> findAllByOrderByIdDesc();
}
