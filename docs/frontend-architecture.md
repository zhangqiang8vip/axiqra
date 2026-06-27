# Axiqra 前端架构文档

> 状态：技术选型已完成，待后端 API 稳定后启动开发
> 版本：v0.1.0
> 日期：2026-06-23
> 技术栈确认依据：项目约束（Vue 3 + TypeScript）+ 当前后端 API 盘点

---

## 一、技术选型

| 层次 | 选项 | 选择 | 理由 |
|------|------|------|------|
| **包管理器** | npm / yarn / pnpm | **pnpm** | 速度快、节省磁盘、支持 monorepo、Cockpit/node_modules 隔离更安全 |
| **构建工具** | Webpack / Vite / esbuild | **Vite 6.x** | HMR 极快、开发体验最佳、Vue 官方推荐、与项目 CORS 配置（localhost:5173）一致 |
| **框架** | Vue 3 Composition API | **Vue 3.5.x + `<script setup>`** | 组合式 API 是当前标准，`<script setup>` 语法更简洁 |
| **类型** | TypeScript 5.x | **TypeScript 5.x strict mode** | 约束（必须用 TypeScript） |
| **路由** | Vue Router 4.x | **Vue Router 4.x** | Vue 官方路由库 |
| **状态管理** | Pinia 2.x | **Pinia** | Vue 官方推荐，比 Vuex 更轻量、类型推导更好 |
| **HTTP 客户端** | Axios | **Axios + instance** | 成熟稳定、拦截器完善、与 Sa-Token token 方案契合 |
| **UI 组件库** | Element Plus / Naive UI / PrimeVue | **Naive UI** | Vue 3 原生支持、API 设计优雅、组件按需引入、打包体积小 |
| **CSS 方案** | UnoCSS / TailwindCSS / 原生 CSS | **UnoCSS** | 原子化 CSS 中最轻量之一、按需生成、无运行时开销 |
| **图表库** | ECharts / Chart.js / Apache ECharts | **Apache ECharts** | 贡献榜单、Solution 统计等数据可视化场景 |
| **测试** | Vitest + Vue Test Utils | **Vitest + Vue Test Utils** | Vite 原生集成、速度极快 |
| **Lint** | ESLint + Prettier | **ESLint + Prettier + husky** | 代码质量保障 |

### 不选其他方案的原因

| 未选方案 | 原因 |
|---------|------|
| Element Plus | 体量大（>1MB gzip），适合后台管理系统，不适合 Axiqra 这类开发者工具 |
| TailwindCSS | 需 JIT 运行时配置，UnoCSS 性能更好且配置更简洁 |
| Vuex | Pinia 是 Vue 官方推荐的后继，API 更现代 |
| fetch | Axios 有成熟的请求/响应拦截器、取消请求、自动 JSON 转换 |

---

## 二、项目结构

```
axiqra-website/
├── public/
│   └── assets/              # 静态资源（图片、字体等）
├── src/
│   ├── api/                 # API 请求层（所有 HTTP 调用集中在此）
│   │   ├── index.ts         # Axios instance 创建 + 全局拦截器
│   │   ├── auth.ts          # 认证相关：登录、注册、token 管理
│   │   ├── connect.ts       # Connect 模块 API
│   │   ├── search.ts        # Search 模块 API
│   │   ├── solution.ts      # Solution 模块 API
│   │   ├── trace.ts         # Trace 模块 API
│   │   ├── projectCase.ts   # Project Case 模块 API
│   │   ├── publicCase.ts    # Public Case 模块 API
│   │   ├── invocation.ts    # Invocation 模块 API
│   │   ├── feedback.ts      # Feedback 模块 API
│   │   ├── contribution.ts  # Contribution 模块 API
│   │   ├── review.ts        # Review 模块 API
│   │   ├── workspace.ts     # Workspace 模块 API
│   │   ├── toolModel.ts     # ToolModel 模块 API
│   │   └── types/           # API 请求/响应类型（对应后端 DTO/VO）
│   │       ├── request/
│   │       └── response/
│   ├── assets/              # 前端资源（CSS、图标等）
│   │   ├── styles/          # 全局样式
│   │   └── icons/           # SVG 图标
│   ├── components/          # 公共组件（按功能分组）
│   │   ├── common/         # 通用：Button、Card、Modal、Table、Pagination
│   │   ├── layout/         # 布局：Header、Sidebar、Breadcrumb
│   │   ├── auth/           # 认证相关：LoginForm、RegisterForm
│   │   ├── solution/        # Solution 相关组件
│   │   ├── trace/          # Trace 相关组件
│   │   └── review/         # Review 相关组件
│   ├── composables/        # 组合式函数（可复用的 Vue 逻辑）
│   │   ├── useAuth.ts      # 认证状态（token、user info）
│   │   ├── useQuota.ts     # 配额状态
│   │   ├── useWorkspace.ts  # 工作空间上下文
│   │   ├── useFetch.ts     # 通用数据请求封装
│   │   └── useRouter.ts    # 路由跳转封装
│   ├── router/
│   │   └── index.ts        # Vue Router 配置 + 路由守卫
│   ├── stores/             # Pinia 状态管理
│   │   ├── auth.ts         # 认证状态 store
│   │   ├── workspace.ts     # 工作空间 store
│   │   ├── nav.ts          # 导航菜单 store
│   │   └── ui.ts           # UI 状态（侧边栏折叠、主题等）
│   ├── views/              # 页面组件（按路由分组）
│   │   ├── auth/
│   │   │   ├── Login.vue
│   │   │   └── Register.vue
│   │   ├── dashboard/
│   │   │   └── Dashboard.vue
│   │   ├── workspace/
│   │   │   ├── WorkspaceList.vue
│   │   │   ├── WorkspaceDetail.vue
│   │   │   └── WorkspaceSettings.vue
│   │   ├── connect/
│   │   │   ├── SessionList.vue
│   │   │   └── DoctorPanel.vue
│   │   ├── search/
│   │   │   └── SearchPanel.vue
│   │   ├── solution/
│   │   │   ├── SolutionDetail.vue
│   │   │   └── SolutionVersion.vue
│   │   ├── trace/
│   │   │   ├── TraceList.vue
│   │   │   └── TraceEditor.vue
│   │   ├── case/
│   │   │   ├── ProjectCaseDetail.vue
│   │   │   └── PublicCaseList.vue
│   │   ├── review/
│   │   │   ├── ReviewQueue.vue
│   │   │   └── ReviewDetail.vue
│   │   └── profile/
│   │       └── UserProfile.vue
│   ├── App.vue
│   └── main.ts
├── .env                     # 开发环境变量（本地默认值）
├── .env.production          # 生产环境变量
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── uno.config.ts           # UnoCSS 配置
└── eslint.config.js        # ESLint 配置
```

---

## 三、核心设计决策

### 3.1 API 请求层设计

所有 API 调用必须通过 `src/api/` 层的封装，禁止在组件或 composable 中直接调用 Axios。

```typescript
// src/api/index.ts（Axios instance）
import axios from 'axios'
import type { AxiosInstance, AxiosError } from 'axios'
import router from '@/router'

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：注入 Sa-Token（Token 存储在 localStorage 的 axiqra_token 键）
api.interceptors.request.use(config => {
  const token = localStorage.getItem('axiqra_token')
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

// 响应拦截器：统一错误处理 + token 过期跳转登录
api.interceptors.response.use(
  response => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('axiqra_token')
      router.push('/auth/login')
    }
    return Promise.reject(error)
  }
)
```

### 3.2 认证设计

基于 Sa-Token 的 UUID token：

| 行为 | 实现 |
|------|------|
| 登录 | 调用 `POST /api/auth/login`，后端返回 token，存入 `localStorage('axiqra_token')` |
| 登出 | 调用 `POST /api/auth/logout`，清除 `localStorage` 和 Pinia auth store |
| 路由守卫 | `router.beforeEach` 检查 `localStorage('axiqra_token')`，无 token 跳转 `/auth/login` |
| 导航菜单 | 调用 `GET /api/auth/nav` 获取累加式菜单（Sa-Token 登录后自动可用） |
| API 签名 | `ApiSignatureFilter` 在后端处理，前端无需关心 HMAC 签名 |

> **重要**：Token 直接存储在 Authorization header 中，不加 Bearer 前缀（Sa-Token 默认行为）。

### 3.3 Vite 开发代理配置

前端 dev server（5173）通过代理转发到后端（8080），避免 CORS 问题：

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 不需要 rewrite，后端 context-path 已是 /api
      }
    }
  }
})
```

### 3.4 环境变量

```env
# .env（开发）
VITE_API_BASE_URL=/api
VITE_APP_NAME=Axiqra
VITE_APP_ENV=development

# .env.production（生产）
VITE_API_BASE_URL=https://api.axiqra.com
VITE_APP_NAME=Axiqra
VITE_APP_ENV=production
```

### 3.5 状态管理设计

Pinia stores 按领域划分：

| Store | 用途 | 持久化 |
|-------|------|--------|
| `auth` | token、user info、login/logout 行为 | token 持久化到 localStorage (`axiqra_token`) |
| `workspace` | 当前 workspace、成员列表 | 内存 |
| `nav` | 导航菜单（从 `/api/auth/nav` 获取） | 内存 |
| `ui` | 侧边栏状态、主题、loading 遮罩 | 内存 |

### 3.6 组件开发规范

- 所有页面组件使用 `<script setup lang="ts">`
- 业务逻辑提取到 `composables/`，组件只负责模板和样式
- API 调用必须通过 `src/api/` 层，不直接用 Axios
- 错误处理：使用 `useFetch` composable 封装，统一展示 `ApiResponse.fail` 错误信息

---

## 四、路由设计

```
/                           → 重定向到 /dashboard
/auth/login                 → 登录页（公开）
/auth/register              → 注册页（公开）

/dashboard                 → 工作台（需登录）
/workspaces                → 工作空间列表（需登录）
/workspaces/:id            → 工作空间详情（需登录）
/workspaces/:id/settings   → 工作空间设置（需 OWNER）

/connect/sessions          → Connect 会话列表（需登录）
/connect/doctor            → Doctor 检测面板（需登录）

/search                    → 搜索页（需登录）
/solutions/:id             → Solution 详情（需登录）

/traces                    → Trace 列表（需登录）
/traces/:id                → Trace 详情（需登录）
/traces/new                → 新建 Trace（需登录）

/cases                     → Case 列表（Project + Public）（需登录）
/cases/project/:id         → Project Case 详情（需登录）
/cases/public/:id          → Public Case 详情（需登录）

/reviews                   → 审核队列（需 review:read scope）
/reviews/:id               → 审核详情（需 review:read scope）

/profile                   → 个人中心（需登录）
/leaderboard               → 工具模型排行榜（需登录）
```

---

## 五、API 层与后端对应关系

| 前端模块 | 后端 Controller | API 路径前缀 | 认证方式 |
|---------|----------------|-------------|---------|
| auth | NavController | `/api/auth/nav` | Sa-Token |
| workspace | WorkspaceController | `/api/workspaces` | Sa-Token + WorkspaceRole |
| connect | ConnectController | `/api/connect` | Sa-Token |
| search | SearchController | `/api/search` | Sa-Token |
| solution | SolutionController | `/api/solutions` | Sa-Token |
| trace | TraceController | `/api/traces` | Sa-Token |
| case (project) | ProjectCaseController | `/api/project-cases` | Sa-Token |
| case (public) | PublicCaseController | `/api/public-cases` | Sa-Token |
| invocation | InvocationController | `/api/v1/invocations` | Sa-Token + scope |
| feedback | FeedbackController | `/api/v1/feedbacks` | Sa-Token + scope |
| contribution | ContributionController | `/api/v1/contributions` | Sa-Token + scope |
| review | ReviewController | `/api/v1/reviews` | Sa-Token + scope |
| toolModel | ToolModelController | `/api/v1/tool-models` | Sa-Token |
| user | UserController | `/api/users` | Sa-Token |

> **注意**：后端当前有路径叠加问题（`/api/v1/...` 与 context-path `/api` 叠加为 `/api/api/v1/...`），前端 API 层需在后端修复后再对接。

---

## 六、关键 TODO（启动前端开发前）

| 编号 | TODO | 前置条件 |
|------|------|---------|
| FE-TODO-001 | 修复后端路径叠加异常（`/api/api/v1/...`） | TASK-DEV-004 完成 |
| FE-TODO-002 | 配置 CORS Bean | TASK-DEV-005 完成 |
| FE-TODO-003 | 配置 OpenAPI SecurityScheme | TASK-DEV-006 完成 |
| FE-TODO-004 | 补充 CandidateSeedController | TASK-DEV-009 完成 |
| FE-TODO-005 | 启动后端验证 `/api/v3/api-docs` 可访问 | 依赖服务已启动 |
| FE-TODO-006 | 用 Swagger codegen 或 manual 生成 TypeScript 类型 | `/api/v3/api-docs` 可访问 |
| FE-TODO-007 | 确认登录 API（`/api/auth/login`）路径和响应格式 | API 盘点完成 |
| FE-TODO-008 | 确认 Sa-Token token 存储方式（Bearer JWT 还是 cookie） | Sa-Token 配置确认 |
