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

|                          | Permissions                                                                                                                                                         |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Anonymous user           | Only can access to Login and Logup, and in first instance, to /projects                                                                                             |
| Regular User (ROLE_USER) | Can edit, delete their profile and data, create new projects, and leave the groups they are part of. The user will only be able to see the groups they are part of. |
| Owner User (ROLE_USER)   | As a regular user, but can manage the group it is owner of, this means add and remove people, and the possibility of change the ownership.                          |
| Admin User (ROLE_ADMIN)  | It is like the owner of every group (and project).                                                                                                                  |

> *Note:*  In this phase, the entities are stored in the database

---

## 🖼 Task Images

When creating tasks, users have the option to attach images as complementary material.  
This feature allows for better visualization, the inclusion of references, or sharing of additional context for each task.

For example, you can:

- Attach screenshots related to the task.
- Add visual references or mockups.
- Include any supporting images that help describe the task clearly.

---

## 📄 Reports

You can download a report of a Task in PDF, or obtain the last report generated
Be careful because only the creator of the Task can modify or generate report

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

It's included a Postman collection to test the REST API of this project.

- File: src/main/resources/postman/RestControllers.postman_collection.zip

> You have to set the cookies like JWT, and specify fields like "challenge"
when submitting forms
