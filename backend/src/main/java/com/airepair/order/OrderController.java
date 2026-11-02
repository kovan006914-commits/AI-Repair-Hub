package com.airepair.order;
import com.airepair.common.Api; import com.airepair.user.User; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.util.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/orders") public class OrderController {
 private final OrderService service; public OrderController(OrderService s){service=s;}
 public record CreateRequest(@NotBlank @Size(max=100) String building,@NotBlank @Size(max=30) String room,@NotBlank @Size(max=50) String category,@NotBlank @Size(max=2000) String description,@NotBlank @Pattern(regexp="LOW|NORMAL|HIGH") String urgency){}
 public record FeedbackRequest(@NotNull @Min(1) @Max(5) Integer rating,@Size(max=500) String comment){}
 public record ActionRequest(Long technicianId,@Size(max=1000) String result){}
 @PostMapping public Api<RepairOrder> create(@AuthenticationPrincipal User u,@Valid @RequestBody CreateRequest req){return Api.ok(service.create(u,req));}
 @GetMapping public Api<Map<String,Object>> list(@AuthenticationPrincipal User u,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size){return Api.ok(service.page(u,page,size));}
 @GetMapping("/{id}") public Api<RepairOrder> get(@AuthenticationPrincipal User u,@PathVariable long id){return Api.ok(service.get(u,id));}
 @GetMapping("/{id}/history") public Api<List<Map<String,Object>>> history(@AuthenticationPrincipal User u,@PathVariable long id){return Api.ok(service.history(u,id));}
 @PostMapping("/{id}/actions/{action}") public Api<RepairOrder> action(@AuthenticationPrincipal User u,@PathVariable long id,@PathVariable String action,@Valid @RequestBody(required=false) ActionRequest req){return Api.ok(service.transition(u,id,action,req==null?null:req.technicianId(),req==null?null:req.result()));}
 @PostMapping("/{id}/feedback") public Api<Void> feedback(@AuthenticationPrincipal User u,@PathVariable long id,@Valid @RequestBody FeedbackRequest req){service.feedback(u,id,req.rating(),req.comment());return Api.ok(null);}
 @GetMapping("/{id}/feedback") public Api<Map<String,Object>> feedback(@AuthenticationPrincipal User u,@PathVariable long id){return Api.ok(service.feedbackFor(u,id));}
}
