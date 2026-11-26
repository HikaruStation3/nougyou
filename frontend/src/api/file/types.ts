/**
 * File 文件上传类型定义
 */

/** 业务类型：决定存储目录与校验规则（API-113） */
export type FileBizType = "idcard" | "proof" | "product" | "video" | "avatar";

/** 文件信息 */
export interface FileInfo {
  /** 文件名称 */
  name: string;
  /** 文件URL */
  url: string;
  /** 文件大小（字节） */
  size?: number;
  /** MIME 类型 */
  contentType?: string;
  /** 对象存储中的路径（旧版接口返回） */
  objectName?: string;
}
