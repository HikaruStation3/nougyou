import { Storage } from "./storage";
import { STORAGE_KEYS, ROLE_ROOT } from "@/constants";
import { useUserStoreHook } from "@/stores/user";
import router from "@/router";

// 负责本地凭证与偏好的读写
//
// 登录态统一存放在 sessionStorage，保证「一个标签页 = 一个账号」，
// 便于同时登录多个账号做联调与演示。
// localStorage 只保留非敏感的偏好项（主题、记住的账号名等）。
export const AuthStorage = {
  getAccessToken(): string {
    return Storage.sessionGet(STORAGE_KEYS.ACCESS_TOKEN, "");
  },

  getRefreshToken(): string {
    return Storage.sessionGet(STORAGE_KEYS.REFRESH_TOKEN, "");
  },

  setTokens(accessToken: string, refreshToken: string, rememberMe: boolean): void {
    // token 只写 sessionStorage：每个标签页独立，可并行登录多个账号
    Storage.sessionSet(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    Storage.sessionSet(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    // 清理旧版本可能残留在 localStorage 的全局 token，避免新标签页串号
    Storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
    Storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
    Storage.set(STORAGE_KEYS.REMEMBER_ME, rememberMe);
  },

  clearAuth(): void {
    Storage.remove(STORAGE_KEYS.ACCESS_TOKEN);
    Storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
    Storage.sessionRemove(STORAGE_KEYS.ACCESS_TOKEN);
    Storage.sessionRemove(STORAGE_KEYS.REFRESH_TOKEN);
  },

  getRememberMe(): boolean {
    return Storage.get<boolean>(STORAGE_KEYS.REMEMBER_ME, false);
  },

  /** 记住的登录账号（只有用户名，不含凭证） */
  getRememberedUsername(): string {
    return Storage.get<string>(STORAGE_KEYS.REMEMBERED_USERNAME, "");
  },

  setRememberedUsername(username: string, remember: boolean): void {
    if (remember && username) {
      Storage.set(STORAGE_KEYS.REMEMBERED_USERNAME, username);
    } else {
      Storage.remove(STORAGE_KEYS.REMEMBERED_USERNAME);
    }
  },
};

/**
 * 权限判断
 */
export function hasPerm(value: string | string[], type: "button" | "role" = "button"): boolean {
  const { roles, perms } = useUserStoreHook().userInfo;

  if (!roles || !perms) {
    return false;
  }

  // 超级管理员拥有所有权限
  if (type === "button" && roles.includes(ROLE_ROOT)) {
    return true;
  }

  const auths = type === "button" ? perms : roles;
  return typeof value === "string"
    ? auths.includes(value)
    : value.some((perm) => auths.includes(perm));
}

/**
 * 重定向到登录页面
 */
export async function redirectToLogin(message: string = "请重新登录"): Promise<void> {
  ElNotification({
    title: "提示",
    message,
    type: "warning",
    duration: 3000,
  });

  await useUserStoreHook().resetAllState();

  try {
    // 跳转到登录页，保留当前路由用于登录后跳转
    const currentPath = router.currentRoute.value.fullPath;
    await router.push(`/login?redirect=${encodeURIComponent(currentPath)}`);
  } catch (error) {
    console.error("Redirect to login error:", error);
    // 强制跳转，即使路由重定向失败
    window.location.href = "/login";
  }
}
