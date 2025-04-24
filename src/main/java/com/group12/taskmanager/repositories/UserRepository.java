package com.group12.taskmanager.repositories;

import com.group12.taskmanager.models.Group;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.group12.taskmanager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface  UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);

    @Query("""
    SELECT u FROM User u
    WHERE LOWER(u.name) LIKE LOWER(CONCAT(:prefix, '%'))
    AND u NOT IN :excluded
""")
    List<User> findByNameStartingWithExcludingGroup(
            @Param("prefix") String prefix,
            @Param("excluded") List<User> excluded
    );

    @Query("SELECT g FROM User u JOIN u.groups g WHERE u.id = :id")
    List<Group> findGroupsByUserId(@Param("id") int id);

}
