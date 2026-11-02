package com.airepair;
import com.fasterxml.jackson.databind.*; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import org.springframework.test.web.servlet.MvcResult; import java.util.concurrent.*; import static org.assertj.core.api.Assertions.assertThat; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc class WorkflowIntegrationTest {
 @org.springframework.test.context.DynamicPropertySource static void imageStorage(org.springframework.test.context.DynamicPropertyRegistry r){String dir=System.getProperty("java.io.tmpdir")+"/repair-test-"+java.util.UUID.randomUUID();r.add("app.upload-dir",()->dir);}
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 JsonNode post(String path,String body,String token,int status)throws Exception{var b=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(body);if(token!=null)b.header("Authorization","Bearer "+token);MvcResult r=mvc.perform(b).andExpect(status().is(status)).andReturn();return json.readTree(r.getResponse().getContentAsString());}
 JsonNode register(String name)throws Exception{return post("/api/auth/register","{\"username\":\""+name+"\",\"password\":\"password-123\",\"role\":\"ADMIN\"}",null,200).path("data");}
 @Test void fullWorkflowEnforcesOwnershipRolesAndAtomicAccept()throws Exception{
  JsonNode a=register("alice"),b=register("bob"),t=register("repairer"); assertThat(a.path("role").asText()).isEqualTo("STUDENT");
  String student=a.path("token").asText(),other=b.path("token").asText(),tech=t.path("token").asText();long tid=t.path("id").asLong();
  String admin=post("/api/auth/login","{\"username\":\"admin\",\"password\":\"admin-password-123\"}",null,200).path("data").path("token").asText();
  mvc.perform(put("/api/users/"+tid+"/role").header("Authorization","Bearer "+student).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/users/"+tid+"/role").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}")).andExpect(status().isOk());
  long id=post("/api/orders","{\"building\":\"3号楼\",\"room\":\"302\",\"category\":\"下水道\",\"description\":\"下水慢\",\"urgency\":\"NORMAL\"}",student,200).path("data").path("id").asLong();
  mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
  post("/api/orders/"+id+"/actions/confirm","{}",student,409);
  java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",buffer);
  var image=new org.springframework.mock.web.MockMultipartFile("file","photo.png","image/png",buffer.toByteArray());
  String upload=mvc.perform(multipart("/api/orders/"+id+"/images").file(image).header("Authorization","Bearer "+student)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  long imageId=json.readTree(upload).path("data").path("id").asLong();
  assertThat(upload).doesNotContain("storageKey");
  mvc.perform(get("/api/orders/"+id+"/images/"+imageId).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
  mvc.perform(get("/api/orders/"+id+"/images/"+imageId).header("Authorization","Bearer "+student)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  var fake=new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png","not an image".getBytes());
  mvc.perform(multipart("/api/orders/"+id+"/images").file(fake).header("Authorization","Bearer "+student)).andExpect(status().isBadRequest());
  post("/api/orders/"+id+"/actions/approve","{}",admin,200);
  post("/api/orders/"+id+"/actions/assign","{\"technicianId\":"+tid+"}",admin,200);
  ExecutorService pool=Executors.newFixedThreadPool(2);try{Callable<Integer> accept=()->mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/orders/"+id+"/actions/accept").header("Authorization","Bearer "+tech).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus();Future<Integer> x=pool.submit(accept),y=pool.submit(accept);assertThat(new int[]{x.get(),y.get()}).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}
  post("/api/orders/"+id+"/actions/finish","{\"result\":\"清理完成\"}",tech,200);
  assertThat(post("/api/orders/"+id+"/actions/confirm","{}",student,200).path("data").path("status").asText()).isEqualTo("COMPLETED");
  post("/api/orders/"+id+"/feedback","{\"rating\":5,\"comment\":\"很好\"}",other,403);
  post("/api/orders/"+id+"/feedback","{\"rating\":5,\"comment\":\"很好\"}",student,200);
  post("/api/orders/"+id+"/feedback","{\"rating\":5}",student,409);
  String response=mvc.perform(get("/api/orders/"+id+"/history").header("Authorization","Bearer "+student)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(json.readTree(response).path("data").size()).isEqualTo(8);
 }
 @Test void dormitoryIsPrivateAndAdminCanReassignInProgressOrder() throws Exception {
  JsonNode a=register("dormstudent"),b=register("dormother"),t=register("techone"),t2=register("techtwo");
  String student=a.path("token").asText(),other=b.path("token").asText(),tech=t.path("token").asText(),newTech=t2.path("token").asText();
  String admin=post("/api/auth/login","{\"username\":\"admin\",\"password\":\"admin-password-123\"}",null,200).path("data").path("token").asText();
  mvc.perform(put("/api/users/me/dormitory").header("Authorization","Bearer "+student).contentType(MediaType.APPLICATION_JSON).content("{\"building\":\"3号楼\",\"room\":\"302\"}")).andExpect(status().isOk());
  mvc.perform(put("/api/users/me/dormitory").header("Authorization","Bearer "+student).contentType(MediaType.APPLICATION_JSON).content("{\"building\":\"4号楼\",\"room\":\"401\"}")).andExpect(status().isOk());
  String d=mvc.perform(get("/api/users/me/dormitory").header("Authorization","Bearer "+student)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  assertThat(json.readTree(d).path("data").path("room").asText()).isEqualTo("401");
  String missing=mvc.perform(get("/api/users/me/dormitory").header("Authorization","Bearer "+other)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(json.readTree(missing).path("data").isNull()).isTrue();
  for(long id:new long[]{t.path("id").asLong(),t2.path("id").asLong()}) mvc.perform(put("/api/users/"+id+"/role").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/users/me/dormitory").header("Authorization","Bearer "+tech)).andExpect(status().isForbidden());
  long id=post("/api/orders","{\"building\":\"4号楼\",\"room\":\"401\",\"category\":\"灯具\",\"description\":\"灯不亮\",\"urgency\":\"NORMAL\"}",student,200).path("data").path("id").asLong();
  post("/api/orders/"+id+"/actions/approve","{}",admin,200);
  post("/api/orders/"+id+"/actions/assign","{\"technicianId\":"+t.path("id").asLong()+"}",admin,200);
  post("/api/orders/"+id+"/actions/accept","{}",tech,200);
  post("/api/orders/"+id+"/actions/reassign","{\"technicianId\":"+t2.path("id").asLong()+"}",tech,403);
  var reassigned=post("/api/orders/"+id+"/actions/reassign","{\"technicianId\":"+t2.path("id").asLong()+"}",admin,200).path("data");
  assertThat(reassigned.path("status").asText()).isEqualTo("PENDING_ACCEPTANCE");assertThat(reassigned.path("acceptedAt").isNull()).isTrue();
  mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+tech)).andExpect(status().isForbidden());
  post("/api/orders/"+id+"/actions/accept","{}",newTech,200);
  post("/api/orders/"+id+"/actions/finish","{\"result\":\"维修完成\"}",newTech,200);
  post("/api/orders/"+id+"/actions/confirm","{}",student,200);
 }
}
