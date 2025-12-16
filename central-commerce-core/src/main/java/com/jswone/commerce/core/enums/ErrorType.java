package com.jswone.commerce.core.enums;

public enum ErrorType {

    ENTITY_DOESNT_EXIST("Entity Doesn't exist"),
    INCORRECT_INPUT("Invalid Input"),
    ENTITY_STATUS_MISMATCH("Entity Status mismatch"),
    ENTITY_ALREADY_EXIST("Entity already exist"),
    TYPE_NOT_APPLICABLE("Given type is not applicable"),
    INVALID_USER("Not valid approver"),
    DATABASE_EXCEPTION("Database Exception"),
    MISSING_REQUIRED_FIELDS("Missing required fields"),
    INVALID_FILE("Invalid File"),
    ATTRIBUTE_TYPE_AND_CONSTRAINT_INVALID("Attribute type or Constraint is not valid"),
    DUPLICATE_ENTRY("Duplicate entry for Attribution Distribution key"),
    IMPORT_IN_PROGRESS("Import in progress"),
    IMPORT_NOT_IN_PROGRESS("Import not in progress"),
    IMPORT_FAILURE("Import Failure"),
    EXPORT_FAILURE("Import Failure"),
    CATALOGUE_SYSTEM_ERROR("Catalogue System Error");

    ErrorType(String value) {

    }
}
