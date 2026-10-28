package org.yearup.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.ProfileUpdateRequest;
import org.yearup.models.User;

import javax.validation.Valid;
import java.security.Principal;

@RestController
@RequestMapping("/profile")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class ProfileController
{
    private final ProfileDao profileDao;
    private final UserDao userDao;

    public ProfileController(ProfileDao profileDao, UserDao userDao)
    {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    @GetMapping
    public Profile getProfile(Principal principal)
    {
        int userId = getAuthenticatedUserId(principal);
        Profile profile = profileDao.getByUserId(userId);
        if (profile == null)
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found.");
        }
        return profile;
    }

    @PutMapping
    public Profile updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Principal principal)
    {
        int userId = getAuthenticatedUserId(principal);
        if (profileDao.getByUserId(userId) == null)
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found.");
        }

        // Never accept a user ID from JSON: only update the current authenticated user.
        Profile updated = request.toProfile(userId);
        profileDao.update(updated);
        return updated;
    }

    private int getAuthenticatedUserId(Principal principal)
    {
        if (principal == null || principal.getName() == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        User user = userDao.getByUserName(principal.getName());
        if (user == null)
        {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found.");
        }
        return user.getId();
    }
}
