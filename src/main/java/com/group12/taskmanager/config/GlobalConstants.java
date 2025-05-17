package com.group12.taskmanager.config;

import org.springframework.stereotype.Component;

@Component
public class GlobalConstants {

    // admin1234 <-- SHA256
    public String getAdminPassword() {return "ac9689e2272427085e35b9d3e3e8bed88cb3434828b43b86fc0596cad4c6e270";}

    public String getAdminRole() { return "ROLE_ADMIN";}
}
