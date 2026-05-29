# 阶段6：网页客户端应用访问与SSO（实施计划与实现结果）

## 一、实施计划

### 1. 目标

实现网页客户端完整“应用访问 + 身份票据 + SSO + 业务系统验证”流程，覆盖票据生命周期操作与验证链路，满足阶段6联调与演示要求。

### 2. 实施范围

- 页面改造：完善 `/web/apps` 为可操作业务页面。
- 票据能力：
  - 票据签发、校验、刷新、保活、失效。
  - 接口：`/api/v1/tickets/issue|verify|{ticketId}/refresh|{ticketId}/keepalive|{ticketId}/revoke`。
- SSO能力：
  - SSO access、SSO validate。
  - 接口：`/api/v1/sso/access|validate`。
- 业务应用访问模拟：
  - 调用 mock 业务应用访问校验接口。
  - 接口：`/api/v1/mock-business-app/access`。
- 页面展示：
  - 展示票据关键字段与各步验证结果提示。

### 3. 验收标准

- 用户登录后可在网页端完成票据签发和SSO流程调用。
- 票据生命周期接口可执行并在页面回显结果。
- mock业务应用访问可调用并输出校验结果。
- 构建通过，路由守卫不回退。

## 二、实现结果

### 1. 代码实现清单

- 网页端访问模块类型与API：
  - `frontend/src/types/web-access.ts`
  - `frontend/src/api/web-access.ts`
- 页面实现：
  - `frontend/src/views/web/WebAppsView.vue`（阶段6完整业务页）

### 2. 功能完成度

- 票据生命周期操作：
  - 票据签发（`/tickets/issue`）
  - 票据校验（`/tickets/verify`）
  - 票据刷新（`/tickets/{ticketId}/refresh`）
  - 票据保活（`/tickets/{ticketId}/keepalive`）
  - 票据失效（`/tickets/{ticketId}/revoke`）
- SSO流程：
  - SSO access（`/sso/access`）
  - SSO validate（`/sso/validate`）
- 业务应用模拟访问：
  - mock业务应用访问校验（`/mock-business-app/access`）
- 页面回显：
  - 票据关键字段（ticketId、appCode、trustLevel、expiresAt等）
  - 校验结果消息（票据校验、SSO、mock访问）

### 3. 风格与交互

- 保持网页客户端既有风格：亮色渐变头部、左侧导航、卡片与表单分区。
- 关键动作具备加载状态、成功/失败提示，避免无反馈操作。
- 与阶段1登录态、路由守卫机制兼容。

### 4. 验证记录

- 构建验证：`npm run build` 成功。
- 运行验证：`npm run dev -- --host 0.0.0.0 --port 3002` 启动成功并可访问。
- 备注：构建存在 Rolldown pure annotation 与 chunk size 警告，不影响阶段6功能验收。

## 三、结论

阶段6已完整实现，可进入下一阶段开发。
