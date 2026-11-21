import request from "@/utils/request";

export interface ChatRequest {
  /** 用户消息 */
  message: string;
}

export interface ChatResponse {
  /** AI 回复内容 */
  content: string;
}

const AI_BASE_URL = "/api/v1/ai";

const AIAPI = {
  /** 单轮对话 */
  chat(data: ChatRequest) {
    return request<any, ChatResponse>({
      url: `${AI_BASE_URL}/chat`,
      method: "post",
      data,
    });
  },
};

export default AIAPI;
