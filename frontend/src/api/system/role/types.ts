/**
 * Role 角色与权限类型定义
 */

/** 角色 */
export interface RoleItem {
  /** 角色ID */
  id: number;
  /** 角色名称 */
  name: string;
  /** 角色编码 */
  code: string;
  /** 排序 */
  sort?: number;
}

/** 权限点 */
export interface PermissionItem {
  /** 权限ID */
  id: number;
  /** 权限名称 */
  name: string;
  /** 权限编码 */
  code: string;
  /** 权限类型：MENU / BUTTON */
  type: string;
  /** 排序 */
  sort?: number;
}
