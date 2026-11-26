import request from "@/utils/request";
import type { FileBizType, FileInfo } from "./types";

const FileAPI = {
  /** 上传文件（API-113，需指定业务类型） */
  uploadWithBizType(file: File, bizType: FileBizType = "product", onProgress?: (percent: number) => void) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("bizType", bizType);
    return request<any, FileInfo>({
      url: "/api/v1/files/upload",
      method: "post",
      data: formData,
      // 短视频可达 100MB，上传不设请求超时
      timeout: 0,
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress?.(percent);
        }
      },
    });
  },

  /** 上传文件 （传入 FormData，上传进度回调） */
  upload(formData: FormData, onProgress?: (percent: number) => void) {
    return request<any, FileInfo>({
      url: "/api/v1/files",
      method: "post",
      data: formData,
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress?.(percent);
        }
      },
    });
  },

  /** 上传文件（传入 File，走旧版兼容接口） */
  uploadFile(file: File) {
    const formData = new FormData();
    formData.append("file", file);
    return request<any, FileInfo>({
      url: "/api/v1/files",
      method: "post",
      data: formData,
      headers: { "Content-Type": "multipart/form-data" },
    });
  },

  /** 删除文件 */
  delete(filePath?: string) {
    return request({
      url: "/api/v1/files",
      method: "delete",
      params: { filePath },
    });
  },

  /** 下载文件 */
  download(url: string, fileName?: string) {
    return request({
      url,
      method: "get",
      responseType: "blob",
    }).then((res) => {
      const blob = new Blob([res.data]);
      const a = document.createElement("a");
      const urlObject = window.URL.createObjectURL(blob);
      a.href = urlObject;
      a.download = fileName || "下载文件";
      a.click();
      window.URL.revokeObjectURL(urlObject);
    });
  },
};

export default FileAPI;

// 重导出类型
export * from "./types";
