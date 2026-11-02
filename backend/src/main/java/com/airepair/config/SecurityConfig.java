package com.airepair.config;
import com.airepair.auth.JwtService; import com.airepair.user.*; import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*; import jakarta.servlet.http.*; import java.io.IOException; import java.util.List;
import org.springframework.context.annotation.*; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; import org.springframework.web.filter.OncePerRequestFilter;
@Configuration @EnableMethodSecurity public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain security(HttpSecurity http, JwtService jwt,UserMapper users,ObjectMapper json) throws Exception {
  http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**").permitAll().anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),com.airepair.common.Api.error("请先登录"));}).accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),com.airepair.common.Api.error("无权执行此操作"));}))
   .addFilterBefore(new OncePerRequestFilter(){protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String h=req.getHeader("Authorization");if(h!=null&&h.startsWith("Bearer ")){try{User u=users.byId(jwt.parse(h.substring(7)));if(u!=null&&u.enabled){var auth=new UsernamePasswordAuthenticationToken(u,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role)));SecurityContextHolder.getContext().setAuthentication(auth);}}catch(io.jsonwebtoken.JwtException|IllegalArgumentException ex){SecurityContextHolder.clearContext();}}chain.doFilter(req,res);}},UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
}
