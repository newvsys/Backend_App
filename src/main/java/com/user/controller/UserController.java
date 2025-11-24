package com.user.controller;


import com.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.user.dto.UserDto;

@RestController
@RequestMapping("/users")
public class UserController {

   @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto user) {

        UserDto registeredUser = userService.registerUser(user);
        return ResponseEntity.ok(registeredUser);
    }

  /*  @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        return ResponseEntity.ok(userService.login(email, password));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refresh_token");
        return ResponseEntity.ok(userService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getUserProfile(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getUserProfile(token));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody User user) {
        userService.updateProfile(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<Map<String, String>> addAddress(@RequestBody Address address) {
        Address addedAddress = userService.addAddress(address);
        return ResponseEntity.ok(Map.of("id", addedAddress.getId(), "message", "Address added successfully"));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Map<String, String>> deleteAddress(@PathVariable String addressId) {
        userService.deleteAddress(addressId);
        return ResponseEntity.ok(Map.of("message", "Address deleted successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> request) {
        String oldPassword = request.get("old_password");
        String newPassword = request.get("new_password");
        userService.changePassword(oldPassword, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "Password reset instructions sent to email"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@RequestHeader("Authorization") String token) {
        userService.deleteAccount(token);
        return ResponseEntity.ok(Map.of("message", "User account deleted"));
    }*/
}