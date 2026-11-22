import { createRouter, createWebHashHistory } from "vue-router";
import PortalLayout from "../layouts/PortalLayout.vue";

const routes = [
  {
    path: "/",
    component: PortalLayout,
    children: [
      {
        path: "",
        name: "home",
        component: () => import("../views/Home.vue"),
        meta: { title: "首页" },
      },
      {
        path: "login",
        name: "login",
        component: () => import("../views/Login.vue"),
        meta: { title: "登录" },
      },
      {
        path: "register",
        name: "register",
        component: () => import("../views/Register.vue"),
        meta: { title: "注册" },
      },
      {
        path: "join",
        name: "join",
        component: () => import("../views/Join.vue"),
        meta: { title: "加入我们", requiresAuth: true },
      },
      {
        path: "projects",
        name: "projects",
        component: () => import("../views/ProjectsList.vue"),
        meta: { title: "农家乐项目" },
      },
      {
        path: "projects/:id",
        name: "project-detail",
        component: () => import("../views/ProjectDetail.vue"),
        meta: { title: "项目详情" },
      },
      {
        path: "consult",
        name: "consult",
        component: () => import("../views/Consult.vue"),
        meta: { title: "我的咨询" },
      },
      {
        path: "profile",
        name: "profile",
        component: () => import("../views/Profile.vue"),
        meta: { title: "个人中心", requiresAuth: true },
      },
      {
        path: "merchant/projects",
        name: "merchant-projects",
        component: () => import("../views/MerchantProjects.vue"),
        meta: { title: "我的农家乐", requiresAuth: true },
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/",
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · Nougyou` : "Nougyou 用户门户";
  if (to.meta.requiresAuth && !localStorage.getItem("portal_token")) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  return true;
});

export default router;
