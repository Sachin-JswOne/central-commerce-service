package com.jswone.commerce.core.util;

import com.jswone.commerce.core.model.auth.JwtUserContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PermissionValidator {

    /**
     * Validates if the user mask provides the required permissions based on binary AND operation.
     * @param userMask The binary permission mask of the user (e.g. "11000000")
     * @param requiredMask The binary required mask for the operation (e.g. "10000000")
     * @return true if user has the required permission, false otherwise
     */
    public boolean hasPermission(String userMask, String requiredMask) {
        if (userMask == null || requiredMask == null || userMask.isEmpty() || requiredMask.isEmpty()) {
            return false;
        }

        try {
            int userVal = Integer.parseInt(userMask, 2);
            int requiredVal = Integer.parseInt(requiredMask, 2);
            return (userVal & requiredVal) == requiredVal;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates if a user has permission for a specific resource.
     * @param permissions The map of user permissions
     * @param resource The resource name (e.g., "ORDER", "CASHBACK")
     * @param requiredMask The binary mask required for the operation
     * @return true if permission is granted, false otherwise
     */
    public boolean hasPermissionForResource(Map<String, String> permissions, String resource, String requiredMask) {
        if (permissions == null || !permissions.containsKey(resource)) {
            return false;
        }
        
        String userMask = permissions.get(resource);
        return hasPermission(userMask, requiredMask);
    }

    /**
     * Checks if the given principal is an anonymous (guest) user.
     * @param principal The security principal (typically a JwtUserContext)
     * @return true if the principal represents a guest user, false otherwise
     */
    public boolean isAnonymousUser(Object principal) {
        if (principal instanceof JwtUserContext) {

            return ((JwtUserContext) principal).isAnonymous();
        }
        return false;
    }
}
