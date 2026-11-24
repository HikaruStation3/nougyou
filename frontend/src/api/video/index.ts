import request from "@/utils/request";

const ADMIN_VIDEO_BASE_URL = "/api/v1/admin/videos";

export interface VideoQueryParams {
  pageNum?: number;
  pageSize?: number;
  /** 0 待审核 / 1 通过 / 2 驳回 */
  auditStatus?: number;
  /** 标题或商家名称 */
  keyword?: string;
}

export interface VideoItem {
  id: number;
  farmerId: number;
  farmerName?: string;
  shopName?: string;
  farmerAvatar?: string;
  title: string;
  cover?: string;
  videoUrl: string;
  duration?: number;
  description?: string;
  auditStatus: number;
  auditRemark?: string;
  likeCount?: number;
  commentCount?: number;
  viewCount?: number;
  status?: number;
  publishTime?: string;
  createTime?: string;
}

export interface VideoAuditForm {
  auditStatus: number;
  auditRemark?: string;
}

const VideoAPI = {
  /** 管理端短视频列表（待审核优先） */
  getPage(queryParams: VideoQueryParams) {
    return request<any, PageResult<VideoItem>>({
      url: ADMIN_VIDEO_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  /** 审核短视频 */
  audit(id: number, data: VideoAuditForm) {
    return request<any, VideoItem>({
      url: `${ADMIN_VIDEO_BASE_URL}/${id}/audit`,
      method: "put",
      data,
    });
  },
};

export default VideoAPI;
