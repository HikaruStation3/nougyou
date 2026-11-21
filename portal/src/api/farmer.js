import http from "./http";

export function submitFarmerApply(data) {
  return http.post("/farmer/applies", data);
}

export function getMyFarmerApplies() {
  return http.get("/farmer/applies/my");
}
