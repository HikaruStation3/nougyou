import request from "@/utils/request";
import { AuthStorage } from "@/utils/auth";

/** 业务场景：经营咨询 / 销售数据统计 / 销售可行性分析 */
export type AiSceneCode = "BIZ_CONSULT" | "SALE_STAT" | "SALE_ANALYSIS";

export interface ChatSession {
  id: number;
  userId?: number;
  /** 管理员监控视角返回 */
  userName?: string;
  sceneCode: AiSceneCode;
  sceneName: string;
  title: string;
  model: string;
  msgCount: number;
  createTime: string;
  updateTime: string;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: "user" | "assistant";
  content: string;
  flagged: boolean;
  createTime: string;
}

export interface ChatRequest {
  /** 为空时按 sceneCode 新建会话 */
  sessionId?: number;
  sceneCode?: AiSceneCode;
  question: string;
}

export interface ChatReply {
  sessionId: number;
  messageId: number;
  content: string;
  flagged: boolean;
}

/** 流式事件：meta 建立会话 → delta 增量 → done 结束 */
export interface ChatStreamMeta {
  sessionId: number;
  sceneCode: AiSceneCode;
  sceneName: string;
}

export interface ChatStreamDone {
  sessionId: number;
  messageId: number;
  content: string;
  blocked: boolean;
  flagged: boolean;
}

export interface ChatStreamHandlers {
  onMeta?: (meta: ChatStreamMeta) => void;
  onDelta?: (content: string) => void;
  onDone?: (done: ChatStreamDone) => void;
  onError?: (message: string) => void;
}

const AI_BASE_URL = "/api/v1/ai";
const ADMIN_AI_BASE_URL = "/api/v1/admin/ai";

/* ------------------------------- FR-027 销售统计 ------------------------------- */

export interface SalesPoint {
  /** yyyy-MM */
  month: string;
  qty: number;
  amount: number;
}

export interface SalesStatItem {
  productId: number;
  productName: string;
  cover?: string;
  stock: number;
  unit?: string;
  saleQty: number;
  saleAmount: number;
  refundQty: number;
  refundAmount: number;
  trend: SalesPoint[];
}

export interface SalesStatQuery {
  productId?: number;
  startDate?: string;
  endDate?: string;
  groupBy?: "product" | "month";
}

export interface SalesStatResult {
  startDate: string;
  endDate: string;
  groupBy: "product" | "month";
  totalQty: number;
  totalAmount: number;
  refundQty: number;
  refundAmount: number;
  productCount: number;
  stockTotal: number;
  items: SalesStatItem[];
  trend: SalesPoint[];
}

/* ------------------------------- FR-028 可行性分析 ------------------------------- */

export interface SalesAnalysisRequest {
  productId: number;
  months?: number;
  sessionId?: number;
}

export interface ProductAnalysis {
  productId: number;
  productName: string;
  saleQty: number;
  trendDirection: "up" | "flat" | "down";
  suggestion: string;
}

export interface SalesAnalysisResult {
  sessionId: number;
  messageId: number;
  answer: string;
  products: ProductAnalysis[];
  tokenCount: number;
}

/* ------------------------------- FR-036 对话监控 ------------------------------- */

export interface AdminSessionQuery {
  pageNum?: number;
  pageSize?: number;
  userId?: number;
  sceneCode?: AiSceneCode;
  startTime?: string;
  endTime?: string;
  keyword?: string;
}

export interface AiOverview {
  sessionCount: number;
  messageCount: number;
  flaggedCount: number;
  todaySessions: number;
  activeUsers: number;
}

/* ------------------------------- FR-037 提示词管理 ------------------------------- */

export interface PromptConfig {
  id: number;
  sceneCode: string;
  sceneName: string;
  promptContent: string;
  version: number;
  isEnable: number;
  updateUserId?: number;
  updateUserName?: string;
  updateTime: string;
}

export interface PromptSaveRequest {
  sceneCode: string;
  sceneName: string;
  promptContent: string;
}


interface SseFrame {
  event: string;
  data?: any;
}

function parseSseFrame(raw: string): SseFrame | null {
  const frame: SseFrame = { event: "message" };
  const dataLines: string[] = [];

  for (const line of raw.split("\n")) {
    if (!line || line.startsWith(":")) continue;
    if (line.startsWith("event:")) {
      frame.event = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).replace(/^ /, ""));
    }
  }

  if (!dataLines.length) return null;

  const data = dataLines.join("\n");
  try {
    frame.data = JSON.parse(data);
  } catch {
    frame.data = data;
  }
  return frame;
}

function dispatchEvent(frame: SseFrame | null, handlers: ChatStreamHandlers) {
  if (!frame) return;

  switch (frame.event) {
    case "meta":
      handlers.onMeta?.(frame.data as ChatStreamMeta);
      break;
    case "delta": {
      const content = typeof frame.data === "string" ? frame.data : frame.data?.content;
      if (content) handlers.onDelta?.(content);
      break;
    }
    case "done":
      handlers.onDone?.(frame.data as ChatStreamDone);
      break;
    case "error":
      handlers.onError?.(frame.data?.message ?? "AI 服务暂时不可用");
      break;
    default:
      break;
  }
}

/**
 * 收到业务终态（done/error）后，最多再等多久让服务端正常关闭 SSE 连接。
 * 超时则主动断开，保证前端一定能从「生成中」状态恢复。
 */
const STREAM_END_TIMEOUT_MS = 5000;

const AIAPI = {
  /** 新建会话 */
  createSession(data: { sceneCode?: AiSceneCode; title?: string } = {}) {
    return request<any, ChatSession>({
      url: `${AI_BASE_URL}/sessions`,
      method: "post",
      data,
    });
  },

  /** 我的会话列表 */
  listSessions(params: { pageNum?: number; pageSize?: number; sceneCode?: AiSceneCode } = {}) {
    return request<any, PageResult<ChatSession>>({
      url: `${AI_BASE_URL}/sessions`,
      method: "get",
      params,
    });
  },

  /** 会话历史消息 */
  listMessages(sessionId: number, params: { pageNum?: number; pageSize?: number } = {}) {
    return request<any, PageResult<ChatMessage>>({
      url: `${AI_BASE_URL}/sessions/${sessionId}/messages`,
      method: "get",
      params,
    });
  },

  /** 非流式问答（降级/脚本用） */
  chat(data: ChatRequest) {
    return request<any, ChatReply>({
      url: `${AI_BASE_URL}/chat`,
      method: "post",
      data,
    });
  },

  /** 销量与库存统计（FR-027），仅本店数据 */
  salesStat(params: SalesStatQuery = {}) {
    return request<any, SalesStatResult>({
      url: `${AI_BASE_URL}/sales/stat`,
      method: "get",
      params,
    });
  },

  /** 销售可行性分析（FR-028），传入 sessionId 可继续追问 */
  salesAnalysis(data: SalesAnalysisRequest) {
    return request<any, SalesAnalysisResult>({
      url: `${AI_BASE_URL}/sales/analysis`,
      method: "post",
      data,
      timeout: 120000,
    });
  },

  /** 管理员：AI 监控概览（FR-036） */
  adminOverview() {
    return request<any, AiOverview>({
      url: `${ADMIN_AI_BASE_URL}/overview`,
      method: "get",
    });
  },

  /** 管理员：全平台 AI 会话分页（FR-036） */
  adminSessions(params: AdminSessionQuery = {}) {
    return request<any, PageResult<ChatSession>>({
      url: `${ADMIN_AI_BASE_URL}/sessions`,
      method: "get",
      params,
    });
  },

  /** 管理员：会话全文，命中管控规则的消息 flagged=true（FR-036） */
  adminMessages(sessionId: number, params: { pageNum?: number; pageSize?: number } = {}) {
    return request<any, PageResult<ChatMessage>>({
      url: `${ADMIN_AI_BASE_URL}/sessions/${sessionId}/messages`,
      method: "get",
      params,
    });
  },

  /** 管理员：提示词全部版本（FR-037） */
  listPrompts(sceneCode?: string) {
    return request<any, PromptConfig[]>({
      url: `${ADMIN_AI_BASE_URL}/prompts`,
      method: "get",
      params: { sceneCode },
    });
  },

  /** 管理员：新增提示词版本并立即启用（FR-037） */
  createPrompt(data: PromptSaveRequest) {
    return request<any, PromptConfig>({
      url: `${ADMIN_AI_BASE_URL}/prompts`,
      method: "post",
      data,
    });
  },

  /** 管理员：回滚启用指定版本（FR-037） */
  enablePrompt(id: number) {
    return request<any, PromptConfig>({
      url: `${ADMIN_AI_BASE_URL}/prompts/${id}/enable`,
      method: "put",
    });
  },

  /**
   * 流式问答。EventSource 无法携带 Authorization 与请求体，故用 fetch 读取 SSE 流。
   */
  async chatStream(
    data: ChatRequest,
    handlers: ChatStreamHandlers,
    signal?: AbortSignal
  ): Promise<void> {
    const token = AuthStorage.getAccessToken();
    const response = await fetch(`${import.meta.env.VITE_APP_BASE_API}${AI_BASE_URL}/chat/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json;charset=utf-8",
        Accept: "text/event-stream",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
      signal,
    });

    if (!response.ok || !response.body) {
      let message = `请求失败（${response.status}）`;
      try {
        const payload = await response.json();
        message = payload?.msg || payload?.message || message;
      } catch {
        // 非 JSON 响应沿用默认提示
      }
      if (response.status === 401) {
        message = "登录状态已过期，请重新登录";
      }
      throw new Error(message);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    // done / error 即业务终态。正常情况下服务端随后会关闭连接，循环自然结束；
    // 若中间代理或服务端迟迟不关连接，看门狗会在超时后主动断开，
    // 避免 UI 永远停留在「生成中」（停止生成按钮不消失）。
    let finished = false;
    let watchdog: ReturnType<typeof setTimeout> | null = null;
    const armWatchdog = () => {
      if (watchdog) return;
      watchdog = setTimeout(() => {
        reader.cancel().catch(() => undefined);
      }, STREAM_END_TIMEOUT_MS);
    };
    const wrapped: ChatStreamHandlers = {
      ...handlers,
      onDone(done) {
        finished = true;
        handlers.onDone?.(done);
        armWatchdog();
      },
      onError(message) {
        finished = true;
        handlers.onError?.(message);
        armWatchdog();
      },
    };

    try {
      for (;;) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");

        let boundary = buffer.indexOf("\n\n");
        while (boundary >= 0) {
          const raw = buffer.slice(0, boundary);
          buffer = buffer.slice(boundary + 2);
          dispatchEvent(parseSseFrame(raw), wrapped);
          boundary = buffer.indexOf("\n\n");
        }
      }

      // 流结束时补处理最后一个未以空行收尾的事件
      if (!finished && buffer.trim()) {
        dispatchEvent(parseSseFrame(buffer), wrapped);
      }
    } finally {
      if (watchdog) clearTimeout(watchdog);
    }
  },
};

export default AIAPI;
