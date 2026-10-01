# HMS 前端

Vue 3 + TypeScript + Vite，使用 Vue Router、Axios 与 ECharts。

完整环境配置、数据库准备及运行说明见[根目录 README](../README.md)。

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

浏览器默认请求 `/api`，Vite 将请求代理到 `http://localhost:8081`。`API_PROXY_TARGET` 可改变开发和预览代理目标；`VITE_API_BASE_URL` 可改变浏览器 API 前缀。

```powershell
npm test
npm run build
npm run preview
```

`build` 同时执行类型检查和生产打包。`preview` 仅用于本地预览；生产服务器需配置 `/api` 反向代理及 SPA 路由回退。

主要目录：`src/views` 页面、`src/components` 组件、`src/api` 请求与类型、`src/utils` 登录及提醒日历逻辑、`tests` 回归测试。
