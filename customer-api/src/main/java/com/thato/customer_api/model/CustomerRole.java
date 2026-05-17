package com.thato.customer_api.model;

public enum CustomerRole {
    ROLE_CUSTOMER, // this is regular bank customer, can only access their profile and be able to edit contact details

    ROLE_ADMIN //bank admin
    //can see all the registerd customers, update any customer, delete and search
}
