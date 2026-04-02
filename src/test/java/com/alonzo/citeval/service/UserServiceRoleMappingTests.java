package com.alonzo.citeval.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceRoleMappingTests {

    @Test
    void mapsStudentEmailsUsingTheStudentSuffixRule() {
        assertEquals("STUDENT", UserService.determineRoleForEmail("  jane.doe.student@ua.edu.ph  "));
    }

    @Test
    void mapsFacultyEmailsUsingTheUaDomainRule() {
        assertEquals("FACULTY", UserService.determineRoleForEmail("madalipay@ua.edu.ph"));
    }

    @Test
    void rejectsNonUaEmailsAsGuests() {
        assertEquals("GUEST", UserService.determineRoleForEmail("someone@example.com"));
    }
}