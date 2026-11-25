package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.User;

import java.security.Principal;

@RestController
@RequestMapping("/profile")
@CrossOrigin
@PreAuthorize("isAuthenticated()") // a user can only see and edit their own profile
public class ProfileController
{
    private final ProfileDao profileDao;
    private final UserDao userDao;

    @Autowired
    public ProfileController(ProfileDao profileDao, UserDao userDao)
    {
        this.profileDao = profileDao;
        this.userDao = userDao;
    }

    // GET http://localhost:8080/profile
    @GetMapping
    public Profile getProfile(Principal principal)
    {
        int userId = currentUserId(principal);

        Profile profile;
        try
        {
            profile = profileDao.getByUserId(userId);
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load profile.");
        }

        if (profile == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found.");

        return profile;
    }

    // PUT http://localhost:8080/profile
    // the body is a Profile; the userId always comes from the logged-in user, never the body
    @PutMapping
    public Profile updateProfile(@RequestBody Profile profile, Principal principal)
    {
        int userId = currentUserId(principal);

        try
        {
            profile.setUserId(userId);
            profileDao.update(userId, profile);
            return profileDao.getByUserId(userId);
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile.");
        }
    }

    private int currentUserId(Principal principal)
    {
        User user = userDao.getByUserName(principal.getName());
        if (user == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found.");
        return user.getId();
    }
}
