import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    children: [
      { path: "", name: "Home", component: () => import("@/views/Home.vue") },
      {
        path: "recipes",
        name: "Recipes",
        component: () => import("@/views/Recipes.vue"),
      },
      {
        path: "recipe/:id",
        name: "RecipeDetail",
        component: () => import("@/views/RecipeDetail.vue"),
      },
      {
        path: "recipe/:id/cook",
        name: "CookingMode",
        component: () => import("@/views/CookingMode.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "search",
        name: "Search",
        component: () => import("@/views/Search.vue"),
      },
      {
        path: "user",
        name: "UserCenter",
        component: () => import("@/views/user/UserCenter.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "user/favorites",
        name: "Favorites",
        component: () => import("@/views/user/Favorites.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "user/report",
        name: "UserReport",
        component: () => import("@/views/user/UserReport.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "create-recipe",
        name: "CreateRecipe",
        component: () => import("@/views/CreateRecipe.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "recommend",
        name: "Recommend",
        component: () => import("@/views/Recommend.vue"),
      },
    ],
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/Login.vue"),
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/Register.vue"),
  },
  {
    path: "/admin/login",
    name: "AdminLogin",
    component: () => import("@/views/admin/Login.vue"),
  },
  {
    path: "/admin",
    component: () => import("@/components/admin/AdminLayout.vue"),
    meta: { requiresAdminAuth: true },
    children: [
      {
        path: "",
        redirect: "/admin/dashboard",
      },
      {
        path: "dashboard",
        name: "AdminDashboard",
        component: () => import("@/views/admin/Dashboard.vue"),
      },
      {
        path: "users",
        name: "AdminUsers",
        component: () => import("@/views/admin/UserManagement.vue"),
      },
      {
        path: "recipes",
        name: "AdminRecipes",
        component: () => import("@/views/admin/RecipeManagement.vue"),
      },
      {
        path: "categories",
        name: "AdminCategories",
        component: () => import("@/views/admin/CategoryManagement.vue"),
      },
      {
        path: "attributes",
        name: "AdminAttributes",
        component: () => import("@/views/admin/AttributeManagement.vue"),
      },
      {
        path: "logs",
        name: "AdminLogs",
        component: () => import("@/views/admin/SystemLogs.vue"),
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");
  const adminToken = localStorage.getItem("admin_token");

  // 检查是否需要用户认证
  if (to.meta.requiresAuth && !token) {
    next("/login");
    return;
  }

  // 检查是否需要管理员认证
  if (to.meta.requiresAdminAuth && !adminToken) {
    next("/admin/login");
    return;
  }

  // 已登录管理员访问登录页，重定向到首页
  if (to.path === "/admin/login" && adminToken) {
    next("/admin/dashboard");
    return;
  }

  next();
});

export default router;
