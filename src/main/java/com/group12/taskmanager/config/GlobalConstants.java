package com.group12.taskmanager.config;

import org.springframework.stereotype.Component;

@Component
public class GlobalConstants {

    public String getAdminPassword() {return "admin1234";}

    public String getAdminRole() { return "ROLE_ADMIN";}
}
