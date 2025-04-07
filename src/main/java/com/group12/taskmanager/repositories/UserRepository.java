package com.group12.taskmanager.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.group12.taskmanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface  UserRepository extends JpaRepository<User, Integer> {
    User findByName(String name);
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.groups WHERE u.email = :email")
    User findByEmailWithGroups(@Param("email") String email);

    @Query("""
    SELECT u FROM User u
    WHERE LOWER(u.name) LIKE LOWER(CONCAT(:prefix, '%'))
    AND u NOT IN :excluded
""")
    List<User> findByNameStartingWithExcludingGroup(
            @Param("prefix") String prefix,
            @Param("excluded") List<User> excluded
    );
}
