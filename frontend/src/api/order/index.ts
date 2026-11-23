import request from "@/utils/request";

const ADMIN_ORDER_BASE_URL = "/api/v1/admin/orders";

export interface OrderQueryParams {
  pageNum?: number;
  pageSize?: number;
  orderStatus?: number;
  keyword?: string;
  orderNo?: string;
  buyerId?: number;
  farmerId?: number;
  startTime?: string;
  endTime?: string;
}

export interface OrderItemVO {
  id: string;
  productId: string;
  productName: string;
  productCover?: string;
  price?: number;
  quantity?: number;
  subtotal?: number;
  entrustId?: string;
  afterSaleStatus?: number;
}

export interface OrderVO {
  id: string;
  orderNo: string;
  buyerId: string;
  buyerName?: string;
  farmerId: string;
  farmerName?: string;
  shopName?: string;
  totalAmount?: number;
  freight?: number;
  payAmount?: number;
  payStatus: number;
  payType?: number;
  payTime?: string;
  orderStatus: number;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  remark?: string;
  logisticsCompany?: string;
  logisticsNo?: string;
  shipTime?: string;
  finishTime?: string;
  createTime?: string;
  items?: OrderItemVO[];
  afterSaleAllowed?: boolean;
}

const OrderAPI = {
  /** API-060 后台订单查询 */
  getAdminPage(queryParams: OrderQueryParams) {
    return request<any, PageResult<OrderVO>>({
      url: ADMIN_ORDER_BASE_URL,
      method: "get",
      params: queryParams,
    });
  },

  /** API-054 订单详情（管理员可查看全部） */
  getDetail(id: string) {
    return request<any, OrderVO>({
      url: `/api/v1/orders/${id}`,
      method: "get",
    });
  },
};

export default OrderAPI;
