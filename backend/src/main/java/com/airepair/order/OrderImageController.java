package com.airepair.order;
import com.airepair.common.Api;
import com.airepair.user.User;
import java.io.IOException;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/orders/{orderId}/images")
public class OrderImageController {
 private final OrderImageService images;
 public OrderImageController(OrderImageService images){this.images=images;}
 @GetMapping public Api<List<OrderImage>> list(@AuthenticationPrincipal User actor,@PathVariable long orderId){return Api.ok(images.list(actor,orderId));}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Api<OrderImage> upload(@AuthenticationPrincipal User actor,@PathVariable long orderId,@RequestParam("file") MultipartFile file)throws IOException{return Api.ok(images.upload(actor,orderId,file));}
 @GetMapping("/{id}") public ResponseEntity<byte[]> read(@AuthenticationPrincipal User actor,@PathVariable long orderId,@PathVariable long id)throws IOException{
  var image=images.read(actor,orderId,id);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.contentType())).header("X-Content-Type-Options","nosniff").body(image.bytes());
 }
}
