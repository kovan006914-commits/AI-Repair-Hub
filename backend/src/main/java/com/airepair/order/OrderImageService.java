package com.airepair.order;

import com.airepair.common.BusinessException;
import com.airepair.user.User;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrderImageService {
 private static final long MAX_BYTES = 3L * 1024 * 1024;
 private final OrderMapper mapper;
 private final OrderService orders;
 private final Path root;
 public OrderImageService(OrderMapper mapper, OrderService orders, @Value("${app.upload-dir:./uploads}") String uploadDir) {
  this.mapper=mapper; this.orders=orders; this.root=Path.of(uploadDir).toAbsolutePath().normalize();
 }
 public List<OrderImage> list(User actor,long orderId){orders.get(actor,orderId);return mapper.images(orderId);}
 public record ImageData(byte[] bytes,String contentType){}
 public ImageData read(User actor,long orderId,long imageId) throws IOException {
  orders.get(actor,orderId);
  OrderImage image=mapper.image(orderId,imageId);
  if(image==null) throw BusinessException.missing();
  Path file=root.resolve(image.storageKey).normalize();
  if(!file.getParent().equals(root) || !Files.isRegularFile(file)) throw BusinessException.missing();
  return new ImageData(Files.readAllBytes(file),image.contentType);
 }
 @Transactional(rollbackFor=Exception.class) public OrderImage upload(User actor,long orderId,MultipartFile file) throws IOException {
  RepairOrder order=orders.get(actor,orderId);
  boolean student=actor.role.equals("STUDENT") && actor.id.equals(order.studentId) && (order.status.equals("PENDING_REVIEW") || order.status.equals("PENDING_ASSIGNMENT"));
  boolean technician=actor.role.equals("TECHNICIAN") && actor.id.equals(order.technicianId) && order.status.equals("IN_PROGRESS");
  if(!student && !technician) throw BusinessException.forbidden();
  if(file.isEmpty() || file.getSize()>MAX_BYTES) throw BusinessException.bad("图片大小必须在 3 MB 以内");
  String contentType;
  try(InputStream in=file.getInputStream(); ImageInputStream imageInput=ImageIO.createImageInputStream(in)){
   if(imageInput==null) throw BusinessException.bad("图片格式无效");
   Iterator<ImageReader> readers=ImageIO.getImageReaders(imageInput);
   if(!readers.hasNext()) throw BusinessException.bad("仅支持 JPEG 和 PNG 图片");
   ImageReader reader=readers.next();
   try {
    reader.setInput(imageInput,true,true);
    String format=reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
    if(!format.equals("png") && !format.equals("jpeg") && !format.equals("jpg")) throw BusinessException.bad("仅支持 JPEG 和 PNG 图片");
    long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
    if(pixels<=0 || pixels>20_000_000) throw BusinessException.bad("图片尺寸过大");
    if(reader.read(0)==null) throw BusinessException.bad("图片内容无效");
    contentType=format.equals("png")?"image/png":"image/jpeg";
   } finally {reader.dispose();}
  } catch(IOException ex){throw BusinessException.bad("图片内容无效");}
  // Reserve the current version before counting or writing so simultaneous uploads and state changes serialize.
  if(mapper.touch(order.id,order.version,order.status)!=1) throw BusinessException.conflict();
  if(mapper.imageCount(orderId)>=6) throw BusinessException.bad("每个工单最多上传 6 张图片");
  Files.createDirectories(root);
  String key=UUID.randomUUID().toString();Path dest=root.resolve(key);
  try(InputStream in=file.getInputStream()){Files.copy(in,dest);}catch(IOException ex){Files.deleteIfExists(dest);throw ex;}
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED){try{Files.deleteIfExists(dest);}catch(IOException ignored){}}}});
  OrderImage image=new OrderImage();image.orderId=orderId;image.uploaderId=actor.id;image.storageKey=key;image.contentType=contentType;
  mapper.insertImage(image);mapper.log(orderId,actor.id,order.status,order.status,"UPLOAD_IMAGE");
  return mapper.image(orderId,image.id);
 }
}
