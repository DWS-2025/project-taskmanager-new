# 🗂 Task Manager

*Task Manager* is a web application designed to help users organize their personal and collaborative work.

It allows its users to be grouped into workgroups, of which there is an user owner. This is the one that can add or remove members of the group, or assign the ownership to another user belonging to the group. Users as members can leave the groups at will.

Users can create projects and assign them to a group, and the assignment can be changed to any group that the owner user owns. Each user has their own personal group for private projects, and participants in a group can work on all the projects assigned to that group.

Within projects, tasks can be created, consisting of a title, a rich text description, and the ability to upload an associated image. Tasks also have the possibility to generate a PDF report of them, there is the possibility to consult the last report generated, or generate a new one. Each user is the owner of his created tasks, which means that only he can modify them, or perform operations with the reports.

Users with ROLE_ADMIN will be able to access and modify all groups and projects, and the tasks of each of these. They will also be able to manage the members and property of any group, as well as being able to reassign any project to any group. These users can also list and delete all users, except the admins.

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
| Admin User (ROLE_ADMIN)  | It is like the owner of every group (and project). They can also list and delete all users                                                                                                                 |

---

## 🖼 Task Images

In the cration of a task, users have the option to attach images as complementary material.  
This feature allows for better visualization, the inclusion of references, or sharing of additional context for each task.

---

## 📄 Reports

You can download a report of a Task in PDF, or get the last generated report 
Be careful because only the creator of the task can modify or generate reports

---

### 🗂️ Database Schema

![Database diagram](src/main/resources/static/img/diagrama.png)

This diagram illustrates the main entities of the application and their relationships, including a many-to-many association between users and groups.

---

## 👥 Development Team

| Name               | University Email                    | GitHub Username   |
|--------------------|-------------------------------------|-------------------|
| Roi Martínez Roque | r.martinezr.2023@alumnos.urjc.es      | @RoiMartinezRoque |

---

## 🧪 Postman Collection

It's included a Postman collection to test the REST API of this project.

- File: src/main/resources/postman/RestControllers.postman_collection.zip

> You have to set cookies, like the "jwt" or the "XSRF-TOKEN", when sending POST, PUT or DELETE requests, and some fields like "challenge" in login or registration requests
