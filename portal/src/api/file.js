import http from "./http";

/**
 * 上传文件到 MinIO 对象存储。
 * 后端接口：POST /api/v1/files（multipart/form-data，字段名 file）
 * 返回 { name, url, objectName }，url 为可直接访问的地址。
 */
export function uploadFile(file, onProgress) {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/files", formData, {
    onUploadProgress: (event) => {
      if (event.total) {
        onProgress?.(Math.round((event.loaded * 100) / event.total));
      }
    }
  });
}

/** 删除 MinIO 中的文件 */
export function deleteFile(filePath) {
  return http.delete("/files", { params: { filePath } });
}
