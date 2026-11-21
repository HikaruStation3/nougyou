import request from "@/utils/request";

const FARMER_APPLY_BASE_URL = "/api/v1/admin/farmer/applies";

export interface FarmerApplyQueryParams {
  pageNum?: number;
  pageSize?: number;
  auditStatus?: number;
  keywords?: string;
}

export interface FarmerApplyItem {
  id: string;
  applyNo: string;
  userId: string;
  username?: string;
  realName: string;
  idCard: string;
  phone: string;
  village: string;
  povertyCardNo?: string;
  povertyLevel?: string;
  householdNum?: number;
  mainProduct?: string;
  idCardFront?: string;
  idCardBack?: string;
  povertyProof?: string;
  applyDesc?: string;
  auditStatus: number;
  auditStatusText: string;
  auditUserId?: string;
  auditUsername?: string;
  auditTime?: string;
  auditRemark?: string;
  createTime?: string;
}

export interface FarmerApplyAuditForm {
  approved: boolean;
  remark?: string;
}

const FarmerApplyAPI = {
  getPage(queryParams: FarmerApplyQueryParams) {
    return request<any, PageResult<FarmerApplyItem>>({
      url: FARMER_APPLY_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  getDetail(id: string) {
    return request<any, FarmerApplyItem>({
      url: `${FARMER_APPLY_BASE_URL}/${id}`,
      method: "get",
    });
  },

  audit(id: string, data: FarmerApplyAuditForm) {
    return request<any, FarmerApplyItem>({
      url: `${FARMER_APPLY_BASE_URL}/${id}/audit`,
      method: "put",
      data,
    });
  },
};

export default FarmerApplyAPI;
