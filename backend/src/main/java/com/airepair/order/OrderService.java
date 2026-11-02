package com.airepair.order;
import com.airepair.common.BusinessException; import com.airepair.user.*; import java.util.List; import java.util.Map; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class OrderService {
 private final OrderMapper orders;private final UserMapper users; public OrderService(OrderMapper o,UserMapper u){orders=o;users=u;}
 @Transactional public RepairOrder create(User actor,OrderController.CreateRequest req){if(!actor.role.equals("STUDENT"))throw BusinessException.forbidden();RepairOrder o=new RepairOrder();o.studentId=actor.id;o.building=req.building();o.room=req.room();o.category=req.category();o.description=req.description();o.urgency=req.urgency();orders.insert(o);orders.log(o.id,actor.id,null,"PENDING_REVIEW","CREATE");return orders.get(o.id);}
 public RepairOrder get(User actor,long id){RepairOrder o=orders.get(id);if(o==null)throw BusinessException.missing();if(!actor.role.equals("ADMIN")&&!(actor.role.equals("STUDENT")&&actor.id.equals(o.studentId))&&!(actor.role.equals("TECHNICIAN")&&actor.id.equals(o.technicianId)))throw BusinessException.forbidden();return o;}
 public Map<String,Object> page(User actor,int page,int size){if(page<1||page>100000||size<1||size>100)throw BusinessException.bad("分页参数无效");return Map.of("items",orders.list(actor.role,actor.id,size,(page-1)*size),"total",orders.count(actor.role,actor.id),"page",page,"size",size);}
 public List<Map<String,Object>> history(User actor,long id){get(actor,id);return orders.history(id);}
 @Transactional public RepairOrder transition(User actor,long id,String action,Long technicianId,String result){RepairOrder o=get(actor,id);String next;
  switch(action){
   case "approve" -> {require(actor,"ADMIN",o,"PENDING_REVIEW");next="PENDING_ASSIGNMENT";}
   case "assign" -> {require(actor,"ADMIN",o,"PENDING_ASSIGNMENT");if(technicianId==null)throw BusinessException.bad("请选择维修人员");User t=users.byId(technicianId);if(t==null||!t.enabled||!t.role.equals("TECHNICIAN"))throw BusinessException.bad("维修人员不存在或不可用");o.technicianId=technicianId;next="PENDING_ACCEPTANCE";}
   case "accept" -> {require(actor,"TECHNICIAN",o,"PENDING_ACCEPTANCE");next="IN_PROGRESS";}
   case "finish" -> {require(actor,"TECHNICIAN",o,"IN_PROGRESS");if(result==null||result.isBlank()||result.length()>1000)throw BusinessException.bad("维修结果须为1到1000字");o.result=result;next="PENDING_CONFIRMATION";}
   case "confirm" -> {require(actor,"STUDENT",o,"PENDING_CONFIRMATION");next="COMPLETED";}
   case "cancel" -> {if(!actor.role.equals("STUDENT")||!actor.id.equals(o.studentId))throw BusinessException.forbidden();if(!o.status.equals("PENDING_REVIEW")&&!o.status.equals("PENDING_ASSIGNMENT"))throw BusinessException.conflict();next="CANCELLED";}
   default -> throw BusinessException.bad("未知操作");
  }
  if(orders.transition(id,o.version,o.status,next,o.technicianId,o.result)!=1)throw BusinessException.conflict();
  if(action.equals("assign"))orders.assignment(id,o.technicianId,actor.id);
  orders.log(id,actor.id,o.status,next,action.toUpperCase());return orders.get(id);
 }
 @Transactional public void feedback(User actor,long id,int rating,String comment){RepairOrder o=get(actor,id);require(actor,"STUDENT",o,"COMPLETED");if(orders.touch(o.id,o.version,o.status)!=1)throw BusinessException.conflict();orders.feedback(id,actor.id,rating,comment==null?"":comment);orders.log(id,actor.id,o.status,o.status,"FEEDBACK");}
 public Map<String,Object> feedbackFor(User actor,long id){get(actor,id);return orders.getFeedback(id);}
 private void require(User u,String role,RepairOrder o,String status){if(!u.role.equals(role))throw BusinessException.forbidden();if(!o.status.equals(status))throw BusinessException.conflict();}
}
