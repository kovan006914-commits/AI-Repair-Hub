package com.airepair.config;
import com.airepair.user.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.ApplicationRunner; import org.springframework.context.annotation.*; import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration public class AdminBootstrap {
 @Bean ApplicationRunner bootstrap(UserMapper users,PasswordEncoder encoder,@Value("${app.admin-username}") String name,@Value("${app.admin-password}") String password){return args->{if(name.isBlank()&&password.isBlank())return;if(!name.matches("[A-Za-z0-9_]{3,32}")||password.length()<8||password.length()>72)throw new IllegalStateException("Invalid bootstrap admin credentials");if(users.byName(name)==null){User u=new User();u.username=name;u.passwordHash=encoder.encode(password);u.role="ADMIN";users.insert(u);}else if(!"ADMIN".equals(users.byName(name).role))throw new IllegalStateException("Bootstrap username exists but is not an administrator");};}
}
