package com.group12.taskmanager.models;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity // Marks this class as a JPA entity (i.e., mapped to a database table)
@Table(name = "`GROUP`") // Specifies the table name in the database (backticks used to avoid SQL keyword conflict)
public class Group {

    @Id // Specifies the primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates the ID using the database identity column
    @Column(name = "ID") // Maps this field to the "ID" column in the table
    private Integer id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50) // Maps to "NAME" column with constraints
    private String name;

    @ManyToOne // Many groups can be owned by one user
    @JoinColumn(name = "OWNER", nullable = false) // Defines foreign key column "OWNER" in the table
    private User owner;

    @ManyToMany // Defines a many-to-many relationship with the User entity
    @JoinTable(
            name = "group_user", // Name of the join table
            joinColumns = @JoinColumn(name = "group_id"), // Foreign key to this Group in join table
            inverseJoinColumns = @JoinColumn(name = "user_id") // Foreign key to User in join table
    )
    private List<User> users = new ArrayList<>(); // List of users in the group

    @Transient // Not persisted in the database
    private boolean isOwner; // Helper field to indicate if the current user is the owner

    @Transient // Not persisted in the database
    private boolean isPersonal; // Helper field to indicate if the group is personal

    // Default constructor (required by JPA)
    public Group() {
    }

    // Constructor with name and owner
    public Group(String name, User owner) {
        this.name = name;
        this.owner = owner;
    }

    // Getter and setter methods

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setIsOwner(boolean isOwner) {
        this.isOwner = isOwner;
    }

    public boolean isPersonal() {
        return isPersonal;
    }

    public void setIsPersonal(boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    public List<User> getUsers() {
        return users;
    }
}
