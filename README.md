# 🗂 Task Manager

*Task Manager* is a web application designed to help users organize their personal and collaborative work. Users can create projects, add tasks within those projects, and group them for better team coordination. Whether you're managing your own to-dos or working with others, Task Manager makes it easy to plan and track tasks across multiple projects.

---
    

## 📘 Entities

The application manages the following main entities, which can be created, edited, viewed, and deleted:

- **User**
- **Group**
- **Task**
- **Project**

---

### 🔄 Key Relationships

- **User** ⇄ **Group** through 'likes' → N:M relationship
- **User** → creates multiple **Project** → 1:N relationship
- **Task** → belongs to only one **Project** → 1:N relationship

---

### 🔐 User Permissions

|             | Permissions                                                                                         |
|-------------|-----------------------------------------------------------------------------------------------------|
| Regular User | Can edit, delete their profile and data, create new projects, and leave the groups they are part of. The user will only be able to see the groups they are part of. |
| Owner User | As a regular user, but can manage the group it is owner of, this means add and remove people, and the possibility of change the ownership. |
| Admin User | It is like the owner of every group (and project). |

> *Note:*  In this phase, the user is simulated as an admin user to create projects and join groups.

---

## 🖼 Task Images

When creating tasks, users have the option to attach images as complementary material.  
This feature allows for better visualization, the inclusion of references, or sharing of additional context for each task.

For example, you can:

- Attach screenshots related to the task.
- Add visual references or mockups.
- Include any supporting images that help describe the task clearly.

---


### 🗂️ Database Schema

![Database diagram](src/main/resources/static/img/diagrama.png)

This diagram illustrates the main entities of the application and their relationships, including a many-to-many association between users and companies through likes.

---

## 👥 Development Team

| Name               | University Email                    | GitHub Username   |
|--------------------|-------------------------------------|-------------------|
| Roi Martínez Roque | r.martinezr.2023@alumnos.urjc.es      | @RoiMartinezRoque |

---

## 🧪 Postman Collection

We include a Postman collection to test the REST API of this project.

- File: src/main/resources/postman/taskmanager_collections.zip

> The collection includes examples of entities such as tasks, users, etc.
