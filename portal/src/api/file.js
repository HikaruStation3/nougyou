import http from "./http";

/**
 * 上传文件到 MinIO 对象存储（API-113）。
 * 后端接口：POST /api/v1/files/upload（multipart/form-data，字段 file + bizType）
 * bizType：idcard / proof / product / video / avatar，决定存储目录与校验规则。
 * 返回 { url, name, size, contentType }，url 为可直接访问的地址。
 *
 * @param {File} file 文件
 * @param {string} bizType 业务类型，默认 product
 * @param {(percent:number)=>void} onProgress 上传进度回调
 */
export function uploadFile(file, bizType = "product", onProgress) {
  // 兼容旧调用：uploadFile(file, onProgress)
  if (typeof bizType === "function") {
    onProgress = bizType;
    bizType = "product";
  }
  const formData = new FormData();
  formData.append("file", file);
  formData.append("bizType", bizType);
  return http.post("/files/upload", formData, {
    // 短视频最大 100MB，上传不设 15s 的默认超时
    timeout: 0,
    headers: { "Content-Type": "multipart/form-data" },
    onUploadProgress: (event) => {
      if (onProgress && event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    }
  });
}

/** 删除 MinIO 中的文件 */
export function deleteFile(filePath) {
  return http.delete("/files", { params: { filePath } });
}
