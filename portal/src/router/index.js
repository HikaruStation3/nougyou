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
        path: "products",
        name: "products",
        component: () => import("../views/ProductsList.vue"),
        meta: { title: "农产品商城" },
      },
      {
        path: "products/:id",
        name: "product-detail",
        component: () => import("../views/ProductDetail.vue"),
        meta: { title: "商品详情" },
      },
      {
        path: "cart",
        name: "cart",
        component: () => import("../views/Cart.vue"),
        meta: { title: "我的购物车", requiresAuth: true },
      },
      {
        path: "checkout",
        name: "checkout",
        component: () => import("../views/Checkout.vue"),
        meta: { title: "确认订单", requiresAuth: true },
      },
      {
        path: "orders",
        name: "orders",
        component: () => import("../views/Orders.vue"),
        meta: { title: "我的订单", requiresAuth: true },
      },
      {
        path: "orders/:id",
        name: "order-detail",
        component: () => import("../views/OrderDetail.vue"),
        meta: { title: "订单详情", requiresAuth: true },
      },
      {
        path: "merchant/orders",
        name: "merchant-orders",
        component: () => import("../views/MerchantOrders.vue"),
        meta: { title: "商家订单", requiresAuth: true, requiresFarmer: true },
      },
      {
        path: "merchant/entrusts",
        name: "merchant-entrusts",
        component: () => import("../views/MerchantEntrusts.vue"),
        meta: { title: "委托销售", requiresAuth: true, requiresFarmer: true },
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
      {
        path: "merchant/products",
        name: "merchant-products",
        component: () => import("../views/MerchantProducts.vue"),
        meta: { title: "我的农产品", requiresAuth: true, requiresFarmer: true },
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

/** 读取本地缓存的用户信息，用于路由级角色校验 */
function cachedUser() {
  try {
    return JSON.parse(localStorage.getItem("portal_user") || "null");
  } catch {
    return null;
  }
}

router.beforeEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · Nougyou`
    : "Nougyou 用户门户";
  if (to.meta.requiresAuth && !localStorage.getItem("portal_token")) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  // 商家专属页面（如我的农产品）仅对农户商家开放，买家误入时回首页
  if (
    to.meta.requiresFarmer &&
    !(cachedUser()?.roles || []).includes("FARMER")
  ) {
    return { name: "home" };
  }
  return true;
});

export default router;
