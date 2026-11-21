import { reactive } from "vue";
import http from "../api/http";
import { TOKEN_KEY, REFRESH_TOKEN_KEY, USER_KEY } from "../api/http";

const state = reactive({
  user: JSON.parse(localStorage.getItem(USER_KEY) || "null"),
});

export function useUserStore() {
  async function loadUser() {
    const data = await http.get("/users/me");
    state.user = data;
    localStorage.setItem(USER_KEY, JSON.stringify(data));
    return data;
  }

  function saveLogin(result) {
    localStorage.setItem(TOKEN_KEY, result.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, result.refreshToken);
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    state.user = null;
  }

  function isLoggedIn() {
    return Boolean(localStorage.getItem(TOKEN_KEY));
  }

  return { state, loadUser, saveLogin, logout, isLoggedIn };
}
