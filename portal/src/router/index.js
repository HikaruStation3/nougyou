import { createRouter, createWebHashHistory } from "vue-router";
import { ElMessage } from "element-plus";
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
        path: "policies",
        name: "policies",
        component: () => import("../views/PolicyList.vue"),
        meta: { title: "扶贫政策" },
      },
      {
        path: "policies/:id",
        name: "policy-detail",
        component: () => import("../views/PolicyDetail.vue"),
        meta: { title: "政策详情" },
      },
      {
        path: "news",
        name: "news",
        component: () => import("../views/NewsList.vue"),
        meta: { title: "乡镇资讯" },
      },
      {
        path: "news/:id",
        name: "news-detail",
        component: () => import("../views/NewsDetail.vue"),
        meta: { title: "资讯详情" },
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
        path: "after-sales",
        name: "after-sales",
        component: () => import("../views/AfterSales.vue"),
        meta: { title: "我的售后", requiresAuth: true },
      },
      {
        // 支付宝沙箱同步跳转落地页，return_url 配的就是这个地址
        path: "pay/result",
        name: "pay-result",
        component: () => import("../views/PayResult.vue"),
        meta: { title: "支付结果", requiresAuth: true },
      },
      {
        path: "merchant",
        redirect: "/merchant/workbench",
      },
      {
        path: "merchant/workbench",
        name: "merchant-workbench",
        component: () => import("../views/MerchantDashboard.vue"),
        meta: { title: "商家工作台", requiresAuth: true, requiresFarmer: true },
      },
      {
        path: "merchant/orders",
        name: "merchant-orders",
        component: () => import("../views/MerchantOrders.vue"),
        meta: { title: "商家订单", requiresAuth: true, requiresFarmer: true },
      },
      {
        path: "merchant/after-sales",
        name: "merchant-after-sales",
        component: () => import("../views/MerchantAfterSales.vue"),
        meta: { title: "商家售后", requiresAuth: true, requiresFarmer: true },
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
      {
        path: "merchant/videos",
        name: "merchant-videos",
        component: () => import("../views/MerchantVideos.vue"),
        meta: { title: "我的短视频", requiresAuth: true, requiresFarmer: true },
      },
    ],
  },
  {
    // 沉浸式播放页：不套 PortalLayout，整个视口交给短视频信息流
    path: "/videos",
    name: "videos",
    component: () => import("../views/VideoFeed.vue"),
    meta: { title: "助农短视频" },
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
    return JSON.parse(sessionStorage.getItem("portal_user") || "null");
  } catch {
    return null;
  }
}

router.beforeEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} · Nougyou`
    : "Nougyou 用户门户";
  if (to.meta.requiresAuth && !sessionStorage.getItem("portal_token")) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  // 商家专属页面（如我的农产品）仅对农户商家开放，买家误入时提示并回首页
  if (
    to.meta.requiresFarmer &&
    !(cachedUser()?.roles || []).includes("FARMER")
  ) {
    ElMessage.warning("该页面仅对农户商家开放");
    return { name: "home" };
  }
  return true;
});

export default router;
