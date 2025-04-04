package com.group12.taskmanager.models;

import jakarta.persistence.*;

@Entity // Marks this class as a JPA entity (mapped to a database table)
@Table(name = "`TASK`") // Maps this entity to the "TASK" table (backticks avoid reserved keyword issues)
public class Task {

    @Id // Primary key of the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID using identity column in the DB
    @Column(name = "ID") // Maps this field to the "ID" column
    private Integer id;

    @Column(name = "TITLE", length = 50) // Maps to "TITLE" column with a max length of 50 characters
    private String title;

    @Column(name = "DESCRIPTION", length = 700) // Maps to "DESCRIPTION" column with a max length of 700 characters
    private String description;

    @Lob // Specifies that this is a large object (LOB), typically for storing binary data
    @Column(name = "image") // Maps to "image" column, stores image as byte array (BLOB)
    private byte[] image;

    @ManyToOne // Many tasks can belong to one project
    @JoinColumn(name = "PROJECT", nullable = false) // Foreign key to the Project entity, cannot be null
    private Project project;

    @Transient // This field is not persisted to the database
    private String imageBase64; // Helper field to store image as a Base64 string (for frontend or APIs)

    // Default constructor (required by JPA)
    public Task() {
    }

    // Getter and setter for id
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Getter and setter for title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Getter and setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getter and setter for project
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    // Getter and setter for image (binary format)
    public byte[] getImage() {
        return image;
    }


    public void setImage(byte[] image) {
        this.image = image;
    }

    // Getter and setter for Base64 image string (not saved to DB)
    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
