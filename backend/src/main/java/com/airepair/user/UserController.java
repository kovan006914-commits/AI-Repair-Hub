package com.airepair.user;
import com.airepair.common.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.List; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users") public class UserController {
 private final UserMapper users; public UserController(UserMapper u){users=u;}
 public record Profile(long id,String username,String role){} public record RoleRequest(@NotBlank @Pattern(regexp="STUDENT|TECHNICIAN|ADMIN") String role){}
 @GetMapping("/me") public Api<Profile> me(@AuthenticationPrincipal User u){return Api.ok(new Profile(u.id,u.username,u.role));}
 @GetMapping @PreAuthorize("hasRole('ADMIN')") public Api<java.util.Map<String,Object>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){if(page<1||page>100000||size<1||size>100)throw BusinessException.bad("分页参数无效");return Api.ok(java.util.Map.of("items",users.list(size,(page-1)*size).stream().map(u->new Profile(u.id,u.username,u.role)).toList(),"total",users.count()));}
 @GetMapping("/technicians") @PreAuthorize("hasRole('ADMIN')") public Api<List<Profile>> technicians(){return Api.ok(users.technicians().stream().map(u->new Profile(u.id,u.username,u.role)).toList());}
 @PutMapping("/{id}/role") @PreAuthorize("hasRole('ADMIN')") public Api<Void> role(@PathVariable long id,@Valid @RequestBody RoleRequest req,@AuthenticationPrincipal User actor){if(id==actor.id)throw BusinessException.bad("不能修改自己的角色");if(users.changeRole(id,req.role())!=1)throw BusinessException.missing();return Api.ok(null);}
}
