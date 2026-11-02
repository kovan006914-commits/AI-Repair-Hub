package com.airepair.auth;
import com.airepair.common.*; import com.airepair.user.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") public class AuthController {
 private final UserMapper users; private final PasswordEncoder encoder; private final JwtService jwt;
 public AuthController(UserMapper u,PasswordEncoder e,JwtService j){users=u;encoder=e;jwt=j;}
 public record Credentials(@NotBlank @Pattern(regexp="[A-Za-z0-9_]{3,32}",message="用户名须为3到32位字母、数字或下划线") String username,@NotBlank @Size(min=8,max=72,message="密码长度须为8到72位") String password){}
 public record Session(String token,long id,String username,String role){}
 @PostMapping("/register") public Api<Session> register(@Valid @RequestBody Credentials req){User u=new User();u.username=req.username();u.passwordHash=encoder.encode(req.password());u.role="STUDENT";users.insert(u);return Api.ok(new Session(jwt.issue(u.id),u.id,u.username,u.role));}
 @PostMapping("/login") public Api<Session> login(@Valid @RequestBody Credentials req){User u=users.byName(req.username());if(u==null||!u.enabled||!encoder.matches(req.password(),u.passwordHash))throw new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED,"用户名或密码错误");return Api.ok(new Session(jwt.issue(u.id),u.id,u.username,u.role));}
}
