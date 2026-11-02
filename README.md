# AI-Repair-Hub 校园智能报修系统

这是第一阶段的开发版：Java / Spring Boot 后端与 Vue Web 前端，提供学生提交报修、管理员受理派单、维修人员接单处理、学生确认的最小业务闭环。AI、RAG 和统计尚未实现，不能将其作为已完成的功能展示。

## 技术选型

Java 21、Spring Boot 3.5.7、Spring Security、JWT、MyBatis-Plus 3.5.18、MySQL 8、Springdoc 2.8.14；Vue 3、TypeScript、Vite、Element Plus、Axios、Pinia、Vue Router。未来拟采用与 Boot 3.5 兼容的 Spring AI 1.1.x。数据库初始化脚本不会删除现有表或数据；请使用新的独立数据库，不要对已有生产数据库盲目执行。

## Windows 环境启动

1. 安装 JDK 21、Maven 3.9+、MySQL 8、Node.js（建议 22 LTS）、Git。使用 `java -version`、`mvn -version`、`mysql --version`、`node -v` 验证。IntelliJ IDEA 打开项目时将 Project SDK 和 Maven 运行 JDK 设置为 21。
2. 用 MySQL 管理工具连接本机 MySQL，创建空数据库：

   ```sql
   CREATE DATABASE IF NOT EXISTS ai_repair_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   选择 `ai_repair_hub` 数据库，运行 `sql/schema.sql` 的全部内容。准备仅具有此库所需权限的数据库账号（本地初次尝试也可用你现有的 MySQL 账号）。
3. 在 **PowerShell** 中进入 `backend`，设置当前终端环境变量；下方密码与密钥均须替换成自己的值。`.env.example` 仅供参考，Spring Boot 不会自动读取该文件。建议生成随机 JWT 密钥，至少 32 个 UTF-8 字节；下面仅作占位：

   ```powershell
   cd AI-Repair-Hub\backend
   $env:DB_URL = 'jdbc:mysql://localhost:3306/ai_repair_hub?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
   $env:DB_USER = '你的数据库用户名'
   $env:DB_PASSWORD = '你的数据库密码'
   $env:JWT_SECRET = '替换成至少32字节的随机且保密的密钥'
   $env:ADMIN_USERNAME = 'admin'
   $env:ADMIN_PASSWORD = '替换成至少8位的管理员密码'
   mvn spring-boot:run
   ```

   首次启动时若指定的用户名不存在，系统创建管理员；如果该用户名已存在且不是管理员，会拒绝启动。已有管理员密码不会被此环境变量重置。若在 IDEA 运行，请把同样的变量填入 Run Configuration 的 Environment variables。不要把真实密码、密钥提交到 Git。
4. 新开一个 PowerShell 窗口，进入前端目录：

   ```powershell
   cd AI-Repair-Hub\frontend
   npm install
   npm run dev
   ```

   按终端输出打开本地地址（通常为 `http://localhost:5173`）。开发服务器将 `/api` 代理到 `http://localhost:8080`。Swagger 地址：`http://localhost:8080/swagger-ui/index.html`。生产部署需要另行设置同源反向代理和 HTTPS。

## 体验流程

1. 在登录页注册学生 A，再注册另一个账号作为维修人员候选。公开注册始终是学生，不接受自选角色。学生可保存自己的宿舍信息，后续报修时会自动填入；修改宿舍信息不会改变历史工单的地址。
2. 使用启动时设置的管理员账号登录，在“用户与角色”中将候选账号设为维修人员。
3. 学生 A 登录，填写宿舍楼、宿舍号、类别、描述、紧急程度并提交。
4. 管理员登录，在工单列表中先“受理”，再选择维修人员并“派单”。
5. 维修人员登录，接单、填写维修结果并完成维修。
6. 管理员可对待接单或处理中工单重新派单给其他维修人员，旧维修人员随即失去工单访问权限。
7. 学生确认完成后可提交一次 1–5 星评价；点击工单可查看操作记录。学生在待受理或待派单阶段可以取消，派单后无法取消。
8. 点击工单进入详情：学生在待受理或待派单时可以上传故障图片；被指派的维修人员在处理中可以上传维修图片。每单最多 6 张，仅支持 JPEG/PNG，单张不超过 3 MB。只有工单相关用户和管理员能通过认证接口查看。

权限在服务端执行：学生只读自己的工单，维修人员只读指派给自己的工单，管理员可查看全部；每次请求会从数据库读取最新用户角色。状态更新使用 `version` 和旧状态作为条件，配合数据库事务把工单变更和操作日志一并提交。此版本登录限流、令牌吊销、密码重置、账号停用管理和生产运维安全加固尚未实现，不能直接部署到公网。

## 验证

```powershell
cd backend
mvn test
cd ..\frontend
npm run build
```

后端测试使用 H2 MySQL 模式覆盖身份权限、完整状态流转和重复接单；它不能代替 MySQL 8 的真实集成测试。真实 MySQL 的脚本执行、约束、并发和端到端体验仍须在部署环境验证。`GET /api/orders?page=1&size=20` 和 `GET /api/users?page=1&size=20` 支持分页；`POST /api/orders/{id}/actions/{action}` 的 action 为 approve、assign、reassign、accept、finish、confirm 或 cancel。接口需要 `Authorization: Bearer <token>`。

## 目录

- `backend/`：API、权限、工单状态机及自动化测试。
- `frontend/`：学生和管理工作台。
- `sql/schema.sql`：第一阶段所需的七张表及约束。
- `ROADMAP.md`：已完成范围与未完成的验收项。

## 版本与安全边界

这是学习开发版本，不是学校正式系统；演示数据不会伪造学校规定。高风险故障应由专业人员处理。当前无 AI 功能和知识库，也没有自动创建工单。将来接入模型时必须服务端先执行安全规则，限制危险建议、隔离不可信知识并要求用户确认。更多待办见 `ROADMAP.md`。

### 当前执行记录

2026-11-02：使用 Java 17 和 H2 执行 `mvn -Djava.version=17 test`，1 项集成测试通过，覆盖完整工单流程、越权、非法状态跳转和并发重复接单；执行 `npm run build` 前端构建通过。此结果不代表 Java 21、真实 MySQL 或浏览器端到端已验证。默认构建目标仍为 Java 21。

### v0.1 项目升级

如果之前已经初始化过数据库，备份后在原 `ai_repair_hub` 数据库重新执行 `sql/schema.sql`，其中新表使用 `CREATE TABLE IF NOT EXISTS`，不会删除原有数据或原有表。本脚本不会自动修改既有表结构；若使用过其他版本或自行修改过表，请先检查差异。

图片文件默认保存在后端工作目录下的 `uploads`，可用环境变量 `UPLOAD_DIR` 设置独立目录，例如 `$env:UPLOAD_DIR = 'D:\AI-Repair-Hub-uploads'`。数据库只存随机文件标识；备份和迁移时需同时备份数据库与上传目录。当前采用本地文件存储，多实例部署需要共享存储，文件系统与数据库无法在断电等极端情况下保证完全一致，生产环境需补充孤儿文件清理和备份恢复方案。

2026-11-02 后续更新：在同一 Java 17 / H2 环境重新运行集成测试，新增图片类型检查、图片越权访问和评价提交/重复提交验证，通过；前端 `npm run build` 通过。真实 MySQL 和 Java 21 环境仍需验证。

### 本次新增：宿舍信息与重新派单

执行 `sql/schema.sql` 可为既有数据库新增 `dormitory` 表，脚本不会删除已有表或数据。学生通过页面上的「宿舍信息」保存自己的楼栋和房间；管理员可在工单待接单或处理中更换维修人员，工单回到待接单状态，重新派单会保留操作和指派记录。重新派单不适用于已完结或取消的工单。
