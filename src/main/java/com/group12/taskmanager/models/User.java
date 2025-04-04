package com.group12.taskmanager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity // Marks this class as a JPA entity (linked to a database table)
@Table(name = "`USER`") // Maps this entity to the table "USER" (backticks used because "USER" is a reserved SQL keyword)
public class User {

    @Id // Primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates the ID using identity column strategy
    @Column(name = "ID") // Maps this field to the "ID" column in the table
    private Integer id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    // Maps to "NAME" column with a max length of 50, must be unique and not null
    private String name;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 100)
    // Maps to "EMAIL" column with a max length of 100, must be unique and not null
    private String email;

    @JsonIgnore // Prevents this field from being serialized in JSON responses (for security)
    @Column(name = "PASSWD", nullable = false, length = 100)
    // Maps to "PASSWD" column, required field with a max length of 100
    private String password;

    @JsonIgnore // Hides this list from JSON output to avoid circular references or unnecessary data
    @ManyToMany(mappedBy = "users") // Many-to-many relationship with Group entity, mapped by the "users" field in Group
    private List<Group> groups = new ArrayList<>(); // List of groups this user belongs to

    // Default constructor required by JPA
    public User() {
    }

    // Constructor to initialize essential user fields
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getter and setter for ID
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Getter and setter for name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter and setter for email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Getter and setter for password (ignored in JSON)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Getter for groups the user is a member of
    public List<Group> getGroups() {
        return groups;

    }
}
