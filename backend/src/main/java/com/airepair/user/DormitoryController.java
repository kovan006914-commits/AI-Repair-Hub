package com.airepair.user;

import com.airepair.common.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/dormitory")
@PreAuthorize("hasRole('STUDENT')")
public class DormitoryController {
 private final UserMapper users;
 public DormitoryController(UserMapper users){this.users=users;}
 public record Request(@NotBlank @Size(max=100) String building,@NotBlank @Size(max=30) String room){}
 @GetMapping public Api<Dormitory> get(@AuthenticationPrincipal User user){return Api.ok(users.getDormitory(user.id));}
 @PutMapping public Api<Dormitory> put(@AuthenticationPrincipal User user,@Valid @RequestBody Request req){
  users.saveDormitory(user.id,req.building().trim(),req.room().trim());
  return Api.ok(users.getDormitory(user.id));
 }
}
