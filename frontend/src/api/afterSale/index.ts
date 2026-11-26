import request from "@/utils/request";

const ADMIN_AFTER_SALE_BASE_URL = "/api/v1/admin/after-sales";

export interface AfterSaleQueryParams {
  pageNum?: number;
  pageSize?: number;
  status?: number;
  keyword?: string;
  startTime?: string;
  endTime?: string;
}

export interface AfterSaleVO {
  id: number;
  afterSaleNo: string;
  orderId: number;
  orderNo?: string;
  orderItemId: number;
  productName?: string;
  productCover?: string;
  quantity?: number;
  itemSubtotal?: number;
  buyerId: number;
  buyerName?: string;
  farmerId: number;
  farmerName?: string;
  shopName?: string;
  saleType: number;
  reason?: string;
  description?: string;
  images?: string[];
  refundAmount?: number;
  status: number;
  handleUserId?: number;
  handleUserName?: string;
  handleTime?: string;
  handleRemark?: string;
  createTime?: string;
}

export interface AfterSaleHandleForm {
  handleResult: number;
  refundAmount?: number;
  handleRemark?: string;
}

const AfterSaleAPI = {
  /** 后台售后列表（平台介入前置查询） */
  getAdminPage(queryParams: AfterSaleQueryParams) {
    return request<any, PageResult<AfterSaleVO>>({
      url: ADMIN_AFTER_SALE_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  /** API-066 平台介入处理 */
  intervene(id: number, data: AfterSaleHandleForm) {
    return request<any, AfterSaleVO>({
      url: `${ADMIN_AFTER_SALE_BASE_URL}/${id}/handle`,
      method: "put",
      data,
    });
  },
};

export default AfterSaleAPI;
