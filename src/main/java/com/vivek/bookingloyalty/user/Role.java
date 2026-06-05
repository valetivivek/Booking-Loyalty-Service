package com.vivek.bookingloyalty.user;

/**
 * Application roles. Spring Security expects the "ROLE_" prefix, which we add
 * when building authorities; here we keep the clean enum names.
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
