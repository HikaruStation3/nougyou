import request from "@/utils/request";

const PROJECT_BASE_URL = "/api/v1/projects";
const FARMER_PROJECT_BASE_URL = "/api/v1/farmer/projects";
const ADMIN_PROJECT_BASE_URL = "/api/v1/admin/projects";

export interface ProjectQueryParams {
  pageNum?: number;
  pageSize?: number;
  projectType?: string;
  address?: string;
  keyword?: string;
  auditStatus?: number;
}

export interface ProjectItem {
  id: string;
  farmerId?: string;
  farmerName?: string;
  shopName?: string;
  title: string;
  projectType: string;
  cover?: string;
  images?: string[];
  address?: string;
  intro?: string;
  priceDesc?: string;
  capacity?: number;
  contactPhone?: string;
  businessHours?: string;
  auditStatus: number;
  auditRemark?: string;
  shelfStatus: number;
  viewCount?: number;
  createTime?: string;
}

export interface ProjectForm {
  title?: string;
  projectType?: string;
  cover?: string;
  images?: string[];
  address?: string;
  intro?: string;
  priceDesc?: string;
  capacity?: number;
  contactPhone?: string;
  businessHours?: string;
  resubmit?: boolean;
}

export interface ProjectAuditForm {
  auditStatus: number;
  auditRemark?: string;
}

const ProjectAPI = {
  /* ---------------- 公开（API-033 / API-034） ---------------- */
  getPage(queryParams: ProjectQueryParams) {
    return request<any, PageResult<ProjectItem>>({
      url: PROJECT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  getDetail(id: string) {
    return request<any, ProjectItem>({
      url: `${PROJECT_BASE_URL}/${id}`,
      method: "get",
    });
  },

  /* ---------------- 农户商家（API-035 ~ API-038） ---------------- */
  create(data: ProjectForm) {
    return request<any, ProjectItem>({
      url: FARMER_PROJECT_BASE_URL,
      method: "post",
      data,
    });
  },

  getMyPage(queryParams: ProjectQueryParams) {
    return request<any, PageResult<ProjectItem>>({
      url: FARMER_PROJECT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  update(id: string, data: ProjectForm) {
    return request<any, ProjectItem>({
      url: `${FARMER_PROJECT_BASE_URL}/${id}`,
      method: "put",
      data,
    });
  },

  shelf(id: string, shelfStatus: number) {
    return request<any, ProjectItem>({
      url: `${FARMER_PROJECT_BASE_URL}/${id}/shelf`,
      method: "put",
      data: { shelfStatus },
    });
  },

  /* ---------------- 管理员（API-039 及审核列表） ---------------- */
  getAdminPage(queryParams: ProjectQueryParams) {
    return request<any, PageResult<ProjectItem>>({
      url: ADMIN_PROJECT_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  audit(id: string, data: ProjectAuditForm) {
    return request<any, ProjectItem>({
      url: `${ADMIN_PROJECT_BASE_URL}/${id}/audit`,
      method: "put",
      data,
    });
  },
};

export default ProjectAPI;
