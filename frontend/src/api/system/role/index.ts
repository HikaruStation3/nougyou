import request from "@/utils/request";
import type { RoleItem, PermissionItem } from "./types";
import type { OptionItem } from "@/api/common";

const ROLE_BASE_URL = "/api/v1/roles";

const RoleAPI = {
  /** 获取角色列表 */
  getList() {
    return request<any, RoleItem[]>({ url: `${ROLE_BASE_URL}`, method: "get" });
  },
  /** 获取角色下拉数据源 */
  getOptions() {
    return request<any, OptionItem[]>({ url: `${ROLE_BASE_URL}/options`, method: "get" });
  },
  /** 获取全部权限点 */
  getPermissions() {
    return request<any, PermissionItem[]>({ url: `${ROLE_BASE_URL}/permissions`, method: "get" });
  },
  /** 获取角色已配置的权限点ID */
  getRolePermissionIds(roleId: number) {
    return request<any, number[]>({ url: `${ROLE_BASE_URL}/${roleId}/permissions`, method: "get" });
  },
  /** 保存角色权限配置 */
  updateRolePermissions(roleId: number, permissionIds: number[]) {
    return request({
      url: `${ROLE_BASE_URL}/${roleId}/permissions`,
      method: "put",
      data: { permissionIds },
    });
  },
};

export default RoleAPI;

// 重导出类型
export * from "./types";
