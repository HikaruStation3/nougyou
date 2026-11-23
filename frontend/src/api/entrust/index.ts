import request from "@/utils/request";

const ADMIN_ENTRUST_BASE_URL = "/api/v1/admin/entrusts";
const ENTRUST_BASE_URL = "/api/v1/entrusts";

export interface EntrustQueryParams {
  pageNum?: number;
  pageSize?: number;
  status?: number;
  keyword?: string;
}

export interface EntrustVO {
  id: string;
  entrustNo: string;
  entrustUserId: string;
  entrustUserName?: string;
  agentUserId: string;
  agentUserName?: string;
  productId: string;
  productName?: string;
  productCover?: string;
  splitRatio?: number;
  settleCycle: number;
  agreementDesc?: string;
  startDate?: string;
  endDate?: string;
  status: number;
  confirmTime?: string;
  createTime?: string;
}

export interface SettlementVO {
  id: string;
  entrustId: string;
  entrustNo: string;
  productName?: string;
  settleMonth: string;
  saleQty?: number;
  saleAmount?: number;
  refundQty?: number;
  refundAmount?: number;
  entrustIncome?: number;
  agentIncome?: number;
  status: number;
  settleTime?: string;
}

const EntrustAPI = {
  /** 后台委托关系列表（结算页选择关系用） */
  getAdminPage(queryParams: EntrustQueryParams) {
    return request<any, PageResult<EntrustVO>>({
      url: ADMIN_ENTRUST_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  /** API-095 生成月度结算单 */
  settle(id: string, settleMonth: string) {
    return request<any, SettlementVO>({
      url: `${ADMIN_ENTRUST_BASE_URL}/${id}/settle`,
      method: "post",
      data: { settleMonth },
    });
  },

  /** API-094 结算单列表 */
  getSettlements(id: string, queryParams?: { settleMonth?: string; pageNum?: number; pageSize?: number }) {
    return request<any, PageResult<SettlementVO>>({
      url: `${ENTRUST_BASE_URL}/${id}/settlements`,
      method: "get",
      params: queryParams,
    });
  },
};

export default EntrustAPI;
