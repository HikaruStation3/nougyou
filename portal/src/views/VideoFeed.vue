<template>
  <div ref="pageRef" class="feed-page">
    <!-- 顶部悬浮条：返回、排序、声音、发布入口 -->
    <header class="feed-top">
      <button class="icon-btn" title="返回首页" @click="goHome">
        <el-icon :size="20"><ArrowLeft /></el-icon>
      </button>
      <div class="feed-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="feed-tab"
          :class="{ active: sortBy === tab.value }"
          @click="switchSort(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="feed-top-right">
        <button class="icon-btn" :title="muted ? '开启声音' : '静音'" @click="toggleMute">
          <el-icon :size="19">
            <Mute v-if="muted" />
            <Microphone v-else />
          </el-icon>
        </button>
        <button v-if="isFarmer" class="publish-btn" @click="goPublish">
          <el-icon><Plus /></el-icon>
          <span>发布</span>
        </button>
      </div>
    </header>

    <!-- 加载 / 空态 -->
    <div v-if="firstLoading" class="feed-placeholder">
      <el-icon class="is-loading" :size="30"><Loading /></el-icon>
      <p>正在加载助农短视频…</p>
    </div>
    <div v-else-if="!list.length" class="feed-placeholder">
      <el-empty description="暂时还没有通过审核的短视频">
        <el-button type="primary" round @click="goHome">回首页逛逛</el-button>
      </el-empty>
    </div>

    <!-- 竖向滚动容器：每个视频占满一屏，scroll-snap 实现上下切换 -->
    <div
      v-show="!firstLoading && list.length"
      ref="scrollerRef"
      class="feed-scroller"
    >
      <section
        v-for="(item, index) in list"
        :key="item.id"
        class="slide"
        :class="{ 'is-active': index === activeIndex }"
      >
        <div class="slide-backdrop" :style="{ backgroundImage: `url(${item.cover || ''})` }"></div>
        <div class="slide-stage">
          <video
            :ref="(el) => setVideoEl(el, index)"
            class="slide-video"
            :src="item.videoUrl"
            :poster="item.cover || undefined"
            :muted="muted"
            loop
            playsinline
            webkit-playsinline
            preload="metadata"
            @click="togglePlay(index)"
            @play="onPlay(index)"
            @pause="onPause(index)"
            @timeupdate="onTimeUpdate($event, item)"
            @loadedmetadata="onLoadedMeta($event, item)"
          ></video>

          <!-- 暂停时的大播放按钮 -->
          <button
            v-if="playingIndex !== index"
            class="play-mask"
            type="button"
            aria-label="播放"
            @click="togglePlay(index)"
          >
            <el-icon :size="52"><VideoPlay /></el-icon>
          </button>

          <!-- 右侧互动区：关注、点赞、评论、分享、商家主页 -->
          <div class="action-rail">
            <div class="rail-item rail-avatar">
              <el-avatar
                :size="46"
                :src="item.farmerAvatar || undefined"
                class="rail-avatar-img"
                @click="openProfile(item)"
              >
                {{ initial(item.farmerName) }}
              </el-avatar>
              <button
                v-if="!item.followed && !item.mine"
                class="follow-badge"
                title="关注商家"
                @click.stop="onFollow(item)"
              >
                <el-icon :size="13"><Plus /></el-icon>
              </button>
            </div>

            <button class="rail-item rail-btn" :class="{ liked: item.liked }" @click="onLike(item)">
              <span class="rail-icon" :class="{ beat: heartBeatId === item.id }">
                <svg viewBox="0 0 24 24" width="30" height="30" aria-hidden="true">
                  <path
                    d="M12 20.7c-.3 0-.6-.1-.8-.3C6.4 16.7 2.6 13.1 2.6 9c0-2.9 2.2-5 5-5 1.6 0 3.1.8 4.4 2.3C13.3 4.8 14.8 4 16.4 4c2.8 0 5 2.1 5 5 0 4.1-3.8 7.7-8.6 11.4-.2.2-.5.3-.8.3z"
                  />
                </svg>
              </span>
              <span class="rail-count">{{ formatCount(item.likeCount) }}</span>
            </button>

            <button class="rail-item rail-btn" @click="openComments(item)">
              <span class="rail-icon">
                <svg viewBox="0 0 24 24" width="29" height="29" aria-hidden="true">
                  <path
                    d="M12 3.2c-4.9 0-8.8 3.5-8.8 7.9 0 2.4 1.2 4.6 3.1 6.1-.2 1.4-.9 2.8-1.9 3.8 1.9-.2 3.6-1 4.9-2.1 1 .3 2 .4 3 .4 4.9 0 8.8-3.5 8.8-7.9S16.9 3.2 12 3.2z"
                  />
                </svg>
              </span>
              <span class="rail-count">{{ formatCount(item.commentCount) }}</span>
            </button>

            <button class="rail-item rail-btn rail-share" @click="share(item)">
              <span class="rail-icon">
                <svg viewBox="0 0 24 24" width="27" height="27" aria-hidden="true">
                  <path d="M21.5 2.5 10.8 13.2M21.5 2.5l-6.3 19-4.4-8.3-8.3-4.4 19-6.3z" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <span class="rail-count">分享</span>
            </button>

            <button class="rail-item rail-btn" @click="openProfile(item)">
              <span class="rail-icon">
                <el-icon :size="26"><Shop /></el-icon>
              </span>
              <span class="rail-count">店铺</span>
            </button>
          </div>

          <!-- 底部信息区 -->
          <div class="slide-info">
            <button class="author" @click="openProfile(item)">
              <span class="at">@</span>{{ item.shopName || item.farmerName }}
              <span v-if="item.mine" class="mine-tag">我的作品</span>
            </button>
            <h2 class="slide-title">{{ item.title }}</h2>
            <p v-if="item.description" class="slide-desc">{{ item.description }}</p>
            <div class="slide-meta">
              <span>{{ formatCount(item.viewCount) }} 次播放</span>
              <span>·</span>
              <span>{{ timeAgo(item.publishTime) }}</span>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="progress" @click.stop="seek(item, $event)">
            <div class="progress-fill" :style="{ width: percentOf(item) + '%' }"></div>
          </div>
        </div>
      </section>

      <section v-if="!hasMore" class="slide slide-end">
        <div class="end-card">
          <h3>已经看完啦</h3>
          <p>去商城看看这些农户家的好东西吧</p>
          <div class="end-actions">
            <el-button type="primary" round @click="goProducts">逛农产品商城</el-button>
            <el-button round @click="goHome">回首页</el-button>
          </div>
        </div>
      </section>
    </div>

    <!-- 上下切换按钮（桌面端） -->
    <div v-if="list.length > 1" class="feed-nav">
      <button class="nav-btn" :disabled="activeIndex === 0" title="上一个" @click="goStep(-1)">
        <el-icon :size="20"><ArrowUpBold /></el-icon>
      </button>
      <button
        class="nav-btn"
        :disabled="activeIndex >= list.length - 1"
        title="下一个"
        @click="goStep(1)"
      >
        <el-icon :size="20"><ArrowDownBold /></el-icon>
      </button>
    </div>

    <!-- 评论面板 -->
    <transition name="fade">
      <div v-if="commentState.open" class="comment-mask" @click="closeComments"></div>
    </transition>
    <transition name="slide-panel">
      <aside v-if="commentState.open" class="comment-panel" @click.stop>
        <header class="comment-head">
          <div>
            <strong>{{ commentState.total }}</strong>
            <span>条评论</span>
          </div>
          <button class="icon-btn" @click="closeComments">
            <el-icon :size="18"><Close /></el-icon>
          </button>
        </header>

        <div class="comment-list">
          <div v-if="commentState.loading && !commentState.list.length" class="comment-tip">
            <el-icon class="is-loading"><Loading /></el-icon>
            加载中…
          </div>
          <div v-else-if="!commentState.list.length" class="comment-tip">
            还没有评论，来说两句吧
          </div>

          <div v-for="item in commentState.list" :key="item.id" class="comment-item">
            <el-avatar :size="34" :src="item.avatar || undefined" class="comment-avatar">
              {{ initial(item.nickname) }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <span class="comment-name">{{ item.nickname }}</span>
                <span class="comment-time">{{ timeAgo(item.createTime) }}</span>
              </div>
              <p class="comment-text">{{ item.content }}</p>
              <div class="comment-ops">
                <button @click="startReply(item)">回复</button>
                <button v-if="item.canDelete" class="danger" @click="removeComment(item, null)">
                  删除
                </button>
              </div>

              <div v-if="item.children?.length" class="comment-children">
                <div v-for="reply in item.children" :key="reply.id" class="comment-child">
                  <span class="comment-name">{{ reply.nickname }}</span>
                  <template v-if="reply.replyToName">
                    <span class="reply-arrow">回复</span>
                    <span class="comment-name">@{{ reply.replyToName }}</span>
                  </template>
                  <span class="child-text">：{{ reply.content }}</span>
                  <div class="comment-ops">
                    <span class="comment-time">{{ timeAgo(reply.createTime) }}</span>
                    <button @click="startReply(reply, item)">回复</button>
                    <button v-if="reply.canDelete" class="danger" @click="removeComment(reply, item)">
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <button
            v-if="commentState.hasMore"
            class="comment-more"
            :disabled="commentState.loading"
            @click="loadComments(false)"
          >
            {{ commentState.loading ? "加载中…" : "查看更多评论" }}
          </button>
        </div>

        <footer class="comment-editor">
          <div v-if="commentState.replyTo" class="reply-hint">
            回复 @{{ commentState.replyTo.nickname }}
            <button @click="cancelReply">
              <el-icon :size="12"><Close /></el-icon>
            </button>
          </div>
          <div class="editor-row">
            <el-input
              v-model="commentState.draft"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 4 }"
              maxlength="200"
              resize="none"
              :placeholder="commentState.replyTo ? '回复TA…' : '说点什么…'"
              @keydown.enter.exact.prevent="submitComment"
            />
            <el-button
              type="primary"
              round
              :loading="commentState.submitting"
              :disabled="!commentState.draft.trim()"
              @click="submitComment"
            >
              发送
            </el-button>
          </div>
        </footer>
      </aside>
    </transition>

    <!-- 商家主页 -->
    <el-dialog v-model="profile.open" width="720px" class="profile-dialog" append-to-body>
      <div v-if="profile.loading" class="profile-loading">
        <el-icon class="is-loading" :size="26"><Loading /></el-icon>
      </div>
      <template v-else-if="profile.data">
        <div class="profile-head">
          <el-avatar :size="64" :src="profile.data.avatar || undefined" class="profile-avatar">
            {{ initial(profile.data.realName) }}
          </el-avatar>
          <div class="profile-copy">
            <h3>{{ profile.data.shopName || profile.data.realName }}</h3>
            <p>{{ profile.data.realName }} · 钟山镇农户商家</p>
            <div class="profile-stats">
              <span><strong>{{ profile.data.followCount }}</strong> 粉丝</span>
              <span><strong>{{ profile.data.productCount }}</strong> 在售</span>
              <span><strong>{{ profile.data.videoCount }}</strong> 视频</span>
            </div>
          </div>
          <el-button
            v-if="!profile.data.isFollowed"
            type="primary"
            round
            @click="onFollowProfile"
          >
            <el-icon><Plus /></el-icon>关注
          </el-button>
          <el-button v-else round @click="onFollowProfile">已关注</el-button>
        </div>

        <div v-if="profile.data.products?.length" class="profile-block">
          <h4>在售农产品</h4>
          <div class="profile-products">
            <div
              v-for="product in profile.data.products"
              :key="product.id"
              class="profile-product"
              @click="goProduct(product.id)"
            >
              <el-image :src="product.cover" fit="cover" class="product-cover">
                <template #error>
                  <div class="cover-fallback">暂无图</div>
                </template>
              </el-image>
              <div class="product-name">{{ product.name }}</div>
              <div class="product-price">￥{{ money(product.price) }}</div>
            </div>
          </div>
        </div>

        <div class="profile-block">
          <h4>TA 的短视频</h4>
          <div v-if="profile.data.videos?.length" class="profile-videos">
            <div
              v-for="video in profile.data.videos"
              :key="video.id"
              class="profile-video"
              @click="playFromProfile(video)"
            >
              <el-image :src="video.cover" fit="cover" class="video-cover">
                <template #error>
                  <div class="cover-fallback">暂无封面</div>
                </template>
              </el-image>
              <span class="video-duration">{{ formatDuration(video.duration) }}</span>
              <span class="video-title">{{ video.title }}</span>
            </div>
          </div>
          <p v-else class="profile-empty">TA 还没有发布短视频</p>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ArrowDownBold,
  ArrowLeft,
  ArrowUpBold,
  Close,
  Loading,
  Microphone,
  Mute,
  Plus,
  Shop,
  VideoPlay,
} from "@element-plus/icons-vue";
import {
  addComment,
  deleteComment,
  getComments,
  getFarmerProfile,
  getVideos,
  reportView,
  toggleFollow,
  toggleLike,
} from "../api/video";
import { useUserStore } from "../store/user";
import { formatCount, formatDuration, money, timeAgo } from "../utils/format";

const router = useRouter();
const route = useRoute();
const store = useUserStore();

const MUTE_KEY = "portal_video_muted";
const PAGE_SIZE = 6;

const tabs = [
  { label: "推荐", value: "new" },
  { label: "最热", value: "hot" },
];

const pageRef = ref(null);
const scrollerRef = ref(null);
const videoEls = ref([]);
const list = ref([]);
const total = ref(0);
const pageNum = ref(1);
const hasMore = ref(true);
const loading = ref(false);
const firstLoading = ref(true);
const sortBy = ref("new");
const activeIndex = ref(0);
const playingIndex = ref(-1);
const muted = ref(localStorage.getItem(MUTE_KEY) !== "0");
const heartBeatId = ref(null);
const progressMap = reactive({});

const commentState = reactive({
  open: false,
  videoId: null,
  list: [],
  /** 作品总评论数（含回复），用于面板标题与右侧计数保持一致 */
  total: 0,
  /** 一级评论分页总数，只用于「查看更多」判断 */
  pageTotal: 0,
  pageNum: 1,
  pageSize: 10,
  hasMore: true,
  loading: false,
  submitting: false,
  draft: "",
  replyTo: null,
  replyRootId: null,
});

const profile = reactive({ open: false, loading: false, data: null, farmerId: null });

const isFarmer = computed(() => (store.state.user?.roles || []).includes("FARMER"));

let observer = null;
let lastWheelAt = 0;

/* ------------------------------ 数据加载 ------------------------------ */

async function loadVideos(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  loading.value = true;
  if (reset) {
    pageNum.value = 1;
    hasMore.value = true;
    list.value = [];
    activeIndex.value = 0;
    videoEls.value = [];
  }
  try {
    const page = await getVideos({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      sortBy: sortBy.value,
    });
    const rows = page.list || [];
    list.value = reset ? rows : [...list.value, ...rows];
    total.value = page.total || 0;
    hasMore.value = list.value.length < total.value;
    if (hasMore.value) {
      pageNum.value += 1;
    }
    // 先让滚动容器可见再观察：v-show 隐藏时容器高度为 0，IntersectionObserver 不会触发
    firstLoading.value = false;
    await nextTick();
    observeSlides();
    if (reset) {
      jumpToQueryVideo();
    }
  } catch {
    hasMore.value = false;
  } finally {
    loading.value = false;
    firstLoading.value = false;
  }
}

/** 深链 #/videos?id=3：首屏加载后定位到指定视频 */
function jumpToQueryVideo() {
  const targetId = Number(route.query.id);
  if (!targetId || !list.value.length) {
    playActive();
    return;
  }
  const index = list.value.findIndex((item) => item.id === targetId);
  if (index > 0) {
    scrollToIndex(index, false);
    activeIndex.value = index;
    nextTick(() => playActive());
  } else {
    playActive();
  }
}

/* ------------------------------ 滚动与切换 ------------------------------ */

function observeSlides() {
  if (!scrollerRef.value) return;
  if (observer) observer.disconnect();
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting || entry.intersectionRatio < 0.6) return;
        const index = Number(entry.target.dataset.index);
        if (Number.isNaN(index) || index === activeIndex.value) return;
        activeIndex.value = index;
        playActive();
        maybeLoadMore(index);
      });
    },
    { root: scrollerRef.value, threshold: [0.6] }
  );
  Array.from(scrollerRef.value.querySelectorAll(".slide:not(.slide-end)")).forEach((slide, index) => {
    slide.dataset.index = String(index);
    observer.observe(slide);
  });
}

function maybeLoadMore(index) {
  if (hasMore.value && !loading.value && index >= list.value.length - 2) {
    loadVideos(false);
  }
}

function slideHeight() {
  return scrollerRef.value?.clientHeight || window.innerHeight;
}

function scrollToIndex(index, smooth = true) {
  if (!list.value.length) return;
  const target = Math.min(Math.max(index, 0), list.value.length - 1);
  scrollerRef.value?.scrollTo({
    top: target * slideHeight(),
    behavior: smooth ? "smooth" : "auto",
  });
}

function goStep(step) {
  const next = activeIndex.value + step;
  if (next < 0 || next >= list.value.length) return;
  scrollToIndex(next);
}

function onWheel(event) {
  if (!list.value.length) return;
  if (Math.abs(event.deltaY) < 4) return;
  event.preventDefault();
  const now = Date.now();
  if (now - lastWheelAt < 520) return;
  lastWheelAt = now;
  const step = event.deltaY > 0 ? 1 : -1;
  const next = activeIndex.value + step;
  if (next < 0) return;
  if (next >= list.value.length) {
    maybeLoadMore(activeIndex.value);
    return;
  }
  scrollToIndex(next);
}

function onKeydown(event) {
  if (commentState.open) return;
  const tag = document.activeElement?.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA") return;
  if (event.key === "ArrowDown" || event.key === "PageDown") {
    event.preventDefault();
    goStep(1);
  } else if (event.key === "ArrowUp" || event.key === "PageUp") {
    event.preventDefault();
    goStep(-1);
  } else if (event.key === " ") {
    event.preventDefault();
    togglePlay(activeIndex.value);
  } else if (event.key === "m" || event.key === "M") {
    toggleMute();
  }
}

function switchSort(value) {
  if (sortBy.value === value) return;
  sortBy.value = value;
  firstLoading.value = true;
  loadVideos(true);
}

/* ------------------------------ 播放控制 ------------------------------ */

function setVideoEl(el, index) {
  if (el) {
    videoEls.value[index] = el;
  }
}

function playActive() {
  const index = activeIndex.value;
  const el = videoEls.value[index];
  videoEls.value.forEach((video, i) => {
    if (video && i !== index) {
      video.pause();
    }
  });
  if (!el) return;
  el.muted = muted.value;
  el.play()
    .then(() => {
      playingIndex.value = index;
      markViewed(list.value[index]);
    })
    .catch(() => {
      // 浏览器拦截带声音的自动播放：退化为静音播放
      el.muted = true;
      muted.value = true;
      el.play()
        .then(() => {
          playingIndex.value = index;
          markViewed(list.value[index]);
        })
        .catch(() => {
          playingIndex.value = -1;
        });
    });
}

/** 播放量按会话去重，避免来回滑动重复上报 */
const viewedIds = new Set();
function markViewed(item) {
  if (!item || viewedIds.has(item.id)) return;
  viewedIds.add(item.id);
  item.viewCount = (item.viewCount || 0) + 1;
  reportView(item.id).catch(() => {});
}

function togglePlay(index) {
  const el = videoEls.value[index];
  if (!el) return;
  if (el.paused) {
    el.play().catch(() => {});
  } else {
    el.pause();
  }
}

function onPlay(index) {
  playingIndex.value = index;
}

function onPause(index) {
  if (playingIndex.value === index) {
    playingIndex.value = -1;
  }
}

function toggleMute() {
  muted.value = !muted.value;
  localStorage.setItem(MUTE_KEY, muted.value ? "1" : "0");
  videoEls.value.forEach((video) => {
    if (video) video.muted = muted.value;
  });
}

function onLoadedMeta(event, item) {
  const duration = event.target?.duration;
  if (Number.isFinite(duration) && duration > 0) {
    progressMap[item.id] = { current: 0, duration };
  }
}

function onTimeUpdate(event, item) {
  const el = event.target;
  if (!el || !Number.isFinite(el.duration)) return;
  progressMap[item.id] = { current: el.currentTime, duration: el.duration };
}

function percentOf(item) {
  const state = progressMap[item.id];
  if (!state || !state.duration) return 0;
  return Math.min(100, (state.current / state.duration) * 100);
}

function seek(item, event) {
  const el = videoEls.value[activeIndex.value];
  if (!el || !Number.isFinite(el.duration)) return;
  const rect = event.currentTarget.getBoundingClientRect();
  const ratio = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
  el.currentTime = ratio * el.duration;
}

/* ------------------------------ 互动 ------------------------------ */

function requireLogin() {
  if (store.isLoggedIn()) return true;
  ElMessage.warning("登录后才能点赞、评论和关注");
  router.push({ name: "login", query: { redirect: "/videos" } });
  return false;
}

async function onLike(item) {
  if (!requireLogin()) return;
  const before = { liked: item.liked, likeCount: item.likeCount };
  item.liked = !item.liked;
  item.likeCount = Math.max(0, (item.likeCount || 0) + (item.liked ? 1 : -1));
  if (item.liked) {
    heartBeatId.value = item.id;
    setTimeout(() => {
      if (heartBeatId.value === item.id) heartBeatId.value = null;
    }, 520);
  }
  try {
    const vo = await toggleLike(item.id);
    item.liked = vo.liked;
    item.likeCount = vo.likeCount;
  } catch {
    item.liked = before.liked;
    item.likeCount = before.likeCount;
  }
}

async function onFollow(item) {
  if (!requireLogin() || item.mine) return;
  try {
    const data = await toggleFollow(item.farmerId);
    const followed = Boolean(data.isFollowed);
    list.value.forEach((video) => {
      if (video.farmerId === item.farmerId) video.followed = followed;
    });
    ElMessage.success(followed ? "已关注，刷到新视频会优先推荐" : "已取消关注");
  } catch {
    /* 失败提示已由拦截器统一处理 */
  }
}

async function share(item) {
  const url = `${window.location.origin}${window.location.pathname}#/videos?id=${item.id}`;
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url);
    } else {
      const input = document.createElement("input");
      input.value = url;
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      document.body.removeChild(input);
    }
    ElMessage.success("链接已复制，去分享给朋友吧");
  } catch {
    ElMessage.info(url);
  }
}

/* ------------------------------ 评论 ------------------------------ */

async function openComments(item) {
  commentState.open = true;
  commentState.videoId = item.id;
  commentState.list = [];
  commentState.total = item.commentCount || 0;
  commentState.pageTotal = 0;
  commentState.pageNum = 1;
  commentState.hasMore = true;
  commentState.replyTo = null;
  commentState.draft = "";
  await loadComments(true);
}

function closeComments() {
  commentState.open = false;
  commentState.replyTo = null;
  commentState.draft = "";
}

async function loadComments(reset) {
  if (!commentState.videoId || commentState.loading) return;
  commentState.loading = true;
  try {
    const page = await getComments(commentState.videoId, {
      pageNum: commentState.pageNum,
      pageSize: commentState.pageSize,
    });
    const rows = page.list || [];
    commentState.list = reset ? rows : [...commentState.list, ...rows];
    commentState.pageTotal = page.total || 0;
    commentState.hasMore = commentState.list.length < commentState.pageTotal;
    if (commentState.hasMore) commentState.pageNum += 1;
  } catch {
    commentState.hasMore = false;
  } finally {
    commentState.loading = false;
  }
}

/** 右侧操作栏与面板标题共用同一个评论数，避免出现 2 与 3 两个数字 */
function adjustVideoCommentCount(videoId, delta) {
  const target = list.value.find((item) => item.id === videoId);
  if (target) {
    target.commentCount = Math.max(0, (target.commentCount || 0) + delta);
  }
}

function startReply(comment, root) {
  if (!requireLogin()) return;
  commentState.replyTo = comment;
  commentState.replyRootId = root ? root.id : comment.id;
}

function cancelReply() {
  commentState.replyTo = null;
  commentState.replyRootId = null;
}

async function submitComment() {
  if (!requireLogin()) return;
  const content = commentState.draft.trim();
  if (!content) return;
  if (content.length > 200) {
    ElMessage.warning("评论最多 200 字");
    return;
  }
  commentState.submitting = true;
  try {
    const payload = { content, parentId: commentState.replyTo ? commentState.replyTo.id : 0 };
    const created = await addComment(commentState.videoId, payload);
    if (commentState.replyTo) {
      const rootId = commentState.replyRootId || commentState.replyTo.id;
      const root = commentState.list.find((item) => item.id === rootId);
      if (root) {
        root.children = [...(root.children || []), created];
      } else {
        commentState.list = [created, ...commentState.list];
      }
    } else {
      commentState.list = [created, ...commentState.list];
    }
    commentState.total += 1;
    commentState.pageTotal += 1;
    commentState.draft = "";
    cancelReply();
    adjustVideoCommentCount(commentState.videoId, 1);
    ElMessage.success("评论已发布");
  } catch {
    /* 失败提示已由拦截器统一处理 */
  } finally {
    commentState.submitting = false;
  }
}

async function removeComment(comment) {
  try {
    await ElMessageBox.confirm("删除后不可恢复，确认删除这条评论？", "删除评论", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消",
    });
  } catch {
    return;
  }
  try {
    await deleteComment(comment.id);
    // 顶级评论会被后端级联删除回复，本地同步移除并回退对应条数
    const removed = 1 + (comment.children?.length || 0);
    commentState.list = commentState.list
      .filter((item) => item.id !== comment.id)
      .map((item) => ({
        ...item,
        children: (item.children || []).filter((child) => child.id !== comment.id),
      }));
    commentState.total = Math.max(0, commentState.total - removed);
    commentState.pageTotal = Math.max(0, commentState.pageTotal - (comment.parentId ? 0 : 1));
    adjustVideoCommentCount(commentState.videoId, -removed);
    ElMessage.success("已删除");
  } catch {
    /* 失败提示已由拦截器统一处理 */
  }
}

/* ------------------------------ 商家主页 ------------------------------ */

async function openProfile(item) {
  profile.open = true;
  profile.loading = true;
  profile.data = null;
  profile.farmerId = item.farmerId;
  try {
    profile.data = await getFarmerProfile(item.farmerId);
  } catch {
    profile.open = false;
  } finally {
    profile.loading = false;
  }
}

async function onFollowProfile() {
  if (!requireLogin()) return;
  try {
    const data = await toggleFollow(profile.farmerId);
    profile.data = data;
    list.value.forEach((video) => {
      if (video.farmerId === profile.farmerId) video.followed = data.isFollowed;
    });
    ElMessage.success(data.isFollowed ? "已关注" : "已取消关注");
  } catch {
    /* 失败提示已由拦截器统一处理 */
  }
}

function playFromProfile(video) {
  profile.open = false;
  const index = list.value.findIndex((item) => item.id === video.id);
  if (index >= 0) {
    scrollToIndex(index);
    return;
  }
  list.value = [video, ...list.value];
  nextTick(() => scrollToIndex(0));
}

/* ------------------------------ 导航 ------------------------------ */

function goHome() {
  router.push("/");
}

function goProducts() {
  router.push("/products");
}

function goPublish() {
  router.push("/merchant/videos");
}

function goProduct(id) {
  profile.open = false;
  router.push(`/products/${id}`);
}

function initial(name) {
  return (name || "农").slice(0, 1);
}

/* ------------------------------ 生命周期 ------------------------------ */

function handleVisibility() {
  if (document.hidden) {
    videoEls.value.forEach((video) => video?.pause());
  } else {
    playActive();
  }
}

onMounted(async () => {
  if (store.isLoggedIn()) {
    store.loadUser().catch(() => {});
  }
  await loadVideos(true);
  window.addEventListener("keydown", onKeydown);
  document.addEventListener("visibilitychange", handleVisibility);
  scrollerRef.value?.addEventListener("wheel", onWheel, { passive: false });
  // 首次点击页面后尝试恢复声音（浏览器要求用户手势）
  const unlockAudio = () => {
    if (localStorage.getItem(MUTE_KEY) === "0") {
      muted.value = false;
      playActive();
    }
    window.removeEventListener("pointerdown", unlockAudio);
  };
  window.addEventListener("pointerdown", unlockAudio);
});

onBeforeUnmount(() => {
  observer?.disconnect();
  window.removeEventListener("keydown", onKeydown);
  document.removeEventListener("visibilitychange", handleVisibility);
  scrollerRef.value?.removeEventListener("wheel", onWheel);
  videoEls.value.forEach((video) => video?.pause());
});
</script>

<style scoped>
.feed-page {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #0a0d0b;
  color: #f4f7f5;
  overflow: hidden;
}

.feed-top {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 16px 20px;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.55), transparent);
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: #f4f7f5;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.12);
  border: none;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.22);
}

.feed-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 999px;
}

.feed-tab {
  padding: 6px 16px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 999px;
  transition: all 0.2s ease;
}

.feed-tab.active {
  color: #123320;
  background: #eef8f1;
}

.feed-top-right {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-left: auto;
}

.publish-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 7px 14px;
  font-size: 13px;
  color: #fff;
  cursor: pointer;
  background: #2e9e5b;
  border: none;
  border-radius: 999px;
}

.publish-btn:hover {
  background: #257e48;
}

.feed-placeholder {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.7);
}

.feed-scroller {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  scroll-behavior: smooth;
  scroll-snap-type: y mandatory;
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.feed-scroller::-webkit-scrollbar {
  display: none;
}

.slide {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  scroll-snap-align: start;
  scroll-snap-stop: always;
  overflow: hidden;
}

.slide-backdrop {
  position: absolute;
  inset: -60px;
  background-position: center;
  background-size: cover;
  filter: blur(36px) brightness(0.32) saturate(1.1);
  transform: scale(1.15);
}

.slide-stage {
  position: relative;
  width: min(100vw, 460px);
  height: 100%;
  overflow: hidden;
  background: #000;
  box-shadow: 0 0 60px rgba(0, 0, 0, 0.55);
}

.slide-video {
  display: block;
  width: 100%;
  height: 100%;
  cursor: pointer;
  object-fit: cover;
}

.play-mask {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  color: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  background: rgba(0, 0, 0, 0.34);
  border: none;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: transform 0.2s ease;
}

.play-mask:hover {
  transform: translate(-50%, -50%) scale(1.06);
}

.action-rail {
  position: absolute;
  right: 12px;
  bottom: 96px;
  z-index: 6;
  display: flex;
  flex-direction: column;
  gap: 18px;
  align-items: center;
}

.rail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.rail-btn {
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: none;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.45));
}

.rail-icon svg {
  fill: #fff;
  stroke: none;
  transition: transform 0.18s ease;
}

.rail-btn.liked .rail-icon svg {
  fill: #ff4d4f;
}

/* 分享图标的纸飞机是描边风格，需要单独覆盖 */
.rail-share .rail-icon svg {
  fill: none;
  stroke: #fff;
}

.rail-count {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.6);
}

.rail-btn.liked .rail-count {
  color: #ff9ea0;
}

.rail-icon.beat {
  animation: beat 0.5s ease;
}

@keyframes beat {
  0% {
    transform: scale(1);
  }

  35% {
    transform: scale(1.32);
  }

  70% {
    transform: scale(0.94);
  }

  100% {
    transform: scale(1);
  }
}

.rail-avatar {
  position: relative;
  margin-bottom: 6px;
  cursor: pointer;
}

.rail-avatar-img {
  border: 2px solid rgba(255, 255, 255, 0.9);
  background: #2e9e5b;
}

.follow-badge {
  position: absolute;
  bottom: -8px;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #fff;
  cursor: pointer;
  background: #2e9e5b;
  border: none;
  border-radius: 50%;
  transform: translateX(-50%);
}

.slide-info {
  position: absolute;
  right: 84px;
  bottom: 26px;
  left: 16px;
  z-index: 6;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.55);
}

.author {
  display: inline-flex;
  gap: 2px;
  align-items: center;
  padding: 0;
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: none;
}

.author .at {
  color: #8fe0ad;
}

.mine-tag {
  padding: 1px 7px;
  margin-left: 6px;
  font-size: 11px;
  font-weight: 500;
  color: #123320;
  background: #b7f0c9;
  border-radius: 999px;
}

.slide-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
}

.slide-desc {
  display: -webkit-box;
  margin: 0 0 8px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.86);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.slide-meta {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.66);
}

.progress {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 7;
  height: 18px;
  cursor: pointer;
}

.progress::before {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  content: "";
  background: rgba(255, 255, 255, 0.25);
}

.progress-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #45c97c;
  transition: width 0.1s linear;
}

.slide-end {
  display: flex;
  align-items: center;
  justify-content: center;
}

.end-card {
  padding: 40px 36px;
  text-align: center;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
}

.end-card h3 {
  margin: 0 0 8px;
  font-size: 20px;
}

.end-card p {
  margin: 0 0 20px;
  color: rgba(255, 255, 255, 0.66);
}

.end-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.feed-nav {
  position: absolute;
  right: 22px;
  bottom: 26px;
  z-index: 15;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  color: #fff;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.14);
  border: none;
  border-radius: 50%;
  transition: background 0.2s ease;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.26);
}

.nav-btn:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

/* ------------------------------ 评论面板 ------------------------------ */

.comment-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgba(0, 0, 0, 0.42);
}

.comment-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 31;
  display: flex;
  flex-direction: column;
  width: min(400px, 92vw);
  color: #1f2d24;
  background: #fff;
  box-shadow: -12px 0 40px rgba(0, 0, 0, 0.28);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #edf1ee;
}

.comment-head strong {
  font-size: 17px;
}

.comment-head span {
  margin-left: 4px;
  font-size: 13px;
  color: #6b7c72;
}

.comment-head .icon-btn {
  color: #4a5b51;
  background: #f1f5f2;
}

.comment-list {
  flex: 1;
  padding: 8px 18px 16px;
  overflow-y: auto;
}

.comment-tip {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  font-size: 13px;
  color: #8a9d92;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f5f3;
}

.comment-avatar {
  flex-shrink: 0;
  color: #fff;
  background: linear-gradient(135deg, #63b985, #2e9e5b);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.comment-name {
  font-size: 13px;
  font-weight: 600;
  color: #2b3b31;
}

.comment-time {
  font-size: 11px;
  color: #9aa8a0;
}

.comment-text {
  margin: 4px 0 6px;
  font-size: 14px;
  line-height: 1.6;
  color: #31423a;
  word-break: break-word;
}

.comment-ops {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12px;
  color: #8a9d92;
}

.comment-ops button {
  padding: 0;
  font-size: 12px;
  color: #6b7c72;
  cursor: pointer;
  background: transparent;
  border: none;
}

.comment-ops button:hover {
  color: #2e9e5b;
}

.comment-ops button.danger:hover {
  color: #e05b4c;
}

.comment-children {
  padding: 8px 10px;
  margin-top: 8px;
  background: #f7faf8;
  border-radius: 10px;
}

.comment-child {
  padding: 6px 0;
  font-size: 13px;
  line-height: 1.6;
  color: #31423a;
}

.comment-child + .comment-child {
  border-top: 1px dashed #e6ece8;
}

.reply-arrow {
  margin: 0 4px;
  font-size: 12px;
  color: #9aa8a0;
}

.child-text {
  word-break: break-word;
}

.comment-more {
  width: 100%;
  padding: 14px 0;
  font-size: 13px;
  color: #2e9e5b;
  cursor: pointer;
  background: transparent;
  border: none;
}

.comment-editor {
  padding: 12px 16px 16px;
  border-top: 1px solid #edf1ee;
  background: #fbfdfc;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #4a5b51;
  background: #eef8f1;
  border-radius: 8px;
}

.reply-hint button {
  display: flex;
  padding: 0;
  color: #6b7c72;
  cursor: pointer;
  background: transparent;
  border: none;
}

.editor-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.editor-row :deep(.el-textarea__inner) {
  border-radius: 14px;
}

/* ------------------------------ 商家主页 ------------------------------ */

.profile-loading {
  padding: 60px 0;
  text-align: center;
  color: #6b7c72;
}

.profile-head {
  display: flex;
  gap: 16px;
  align-items: center;
  padding-bottom: 18px;
  border-bottom: 1px solid #edf1ee;
}

.profile-avatar {
  color: #fff;
  background: linear-gradient(135deg, #63b985, #2e9e5b);
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-copy h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: #1f3d2b;
}

.profile-copy p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #6b7c72;
}

.profile-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #6b7c72;
}

.profile-stats strong {
  margin-right: 2px;
  font-size: 14px;
  color: #1f3d2b;
}

.profile-block {
  margin-top: 18px;
}

.profile-block h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: #1f3d2b;
}

.profile-products {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.profile-product {
  cursor: pointer;
}

.product-cover,
.video-cover {
  width: 100%;
  height: 92px;
  overflow: hidden;
  background: #eef3f0;
  border-radius: 10px;
}

.product-name {
  margin-top: 6px;
  overflow: hidden;
  font-size: 12px;
  color: #31423a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  font-size: 12px;
  font-weight: 700;
  color: #e05b4c;
}

.profile-videos {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.profile-video {
  position: relative;
  padding-bottom: 16px;
  cursor: pointer;
}

.video-cover {
  height: 120px;
}

.video-duration {
  position: absolute;
  top: 96px;
  right: 6px;
  padding: 0 5px;
  font-size: 11px;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 4px;
}

.video-title {
  display: block;
  margin-top: 6px;
  overflow: hidden;
  font-size: 12px;
  color: #31423a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-empty {
  padding: 16px 0;
  font-size: 13px;
  color: #8a9d92;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-size: 12px;
  color: #8a9d92;
}

/* ------------------------------ 过渡 ------------------------------ */

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.22s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-panel-enter-active,
.slide-panel-leave-active {
  transition: transform 0.28s ease;
}

.slide-panel-enter-from,
.slide-panel-leave-to {
  transform: translateX(100%);
}

/* ------------------------------ 移动端 ------------------------------ */

@media (max-width: 640px) {
  .feed-nav {
    display: none;
  }

  .slide-stage {
    width: 100vw;
    box-shadow: none;
  }

  .slide-info {
    right: 76px;
    bottom: 22px;
  }

  .action-rail {
    right: 8px;
    bottom: 88px;
    gap: 16px;
  }

  .comment-panel {
    top: auto;
    left: 0;
    width: 100%;
    height: 68vh;
    border-radius: 18px 18px 0 0;
  }

  .slide-panel-enter-from,
  .slide-panel-leave-to {
    transform: translateY(100%);
  }

  .profile-products,
  .profile-videos {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
