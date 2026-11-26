import { reactive } from "vue";
import http from "../api/http";
import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY } from "../api/http";

const state = reactive({
  user: JSON.parse(sessionStorage.getItem(USER_KEY) || "null"),
});

export function useUserStore() {
  async function loadUser() {
    const data = await http.get("/users/me");
    state.user = data;
    sessionStorage.setItem(USER_KEY, JSON.stringify(data));
    return data;
  }

  function saveLogin(result) {
    sessionStorage.setItem(TOKEN_KEY, result.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken);
  }

  function logout() {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    state.user = null;
  }

  function isLoggedIn() {
    return Boolean(sessionStorage.getItem(TOKEN_KEY));
  }

  return { state, loadUser, saveLogin, logout, isLoggedIn };
}
