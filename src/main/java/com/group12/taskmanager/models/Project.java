package com.group12.taskmanager.models;

import jakarta.persistence.*;
import java.util.List;

@Entity // Marks this class as a JPA entity (mapped to a database table)
@Table(name = "`PROJECT`") // Specifies the table name "PROJECT" (backticks used to avoid conflicts with reserved keywords)
public class Project {

    @Id // Specifies the primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID using database identity strategy
    @Column(name = "ID") // Maps this field to the "ID" column in the table
    private Integer id;

    @Column(name = "NAME", nullable = false, length = 50)
    // Maps to "NAME" column with a max length of 50 and cannot be null
    private String name;

    @ManyToOne // Many projects can belong to one group
    @JoinColumn(name = "GROUP_ID", nullable = false) // Foreign key column referencing the Group entity
    private Group group;

    @Transient // This field is not stored in the database
    private List<Task> tasks; // A list of tasks associated with the project (not directly mapped by JPA)

    // Default constructor (required by JPA)
    public Project() {
    }

    // Constructor with name and group parameters
    public Project(String name, Group group) {
        this.name = name;
        this.group = group;
    }

    // Getter and setter for id
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

    // Getter and setter for group
    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    // Getter and setter for tasks (not persisted in DB)
    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;

    }
}