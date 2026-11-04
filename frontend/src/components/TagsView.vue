<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import { useTagsViewStore, type TagView } from '@/stores/tagsView'

const route = useRoute()
const router = useRouter()
const store = useTagsViewStore()

/** 首页固定标签：不可关闭，关闭全部时回首页 */
const AFFIX_PATH = '/dashboard'

const scrollbarRef = ref()
const innerRef = ref<HTMLElement>()

const isActive = (v: TagView) => v.path === route.path
const isCurrent = (v: TagView) => v.path === route.path

// ===== 右键菜单状态（声明在前，immediate watch 里会调 closeMenu） =====
const menu = reactive({ visible: false, left: 0, top: 0, view: null as TagView | null })

function closeMenu() {
  menu.visible = false
  menu.view = null
}

function toView(v: TagView) {
  if (v.fullPath !== route.fullPath) router.push(v.fullPath)
}

// 路由变化 -> 打开/更新标签页，并把激活标签滚到可视区
watch(
  route,
  (r) => {
    store.addView({
      name: r.name as string,
      path: r.path,
      fullPath: r.fullPath,
      title: (r.meta?.title as string) || '未命名',
      affix: r.path === AFFIX_PATH,
    })
    closeMenu()
    nextTick(() => scrollIntoActive())
  },
  { immediate: true }
)

onMounted(() => {
  // 刷新页面时保证首页固定标签始终存在
  store.addView({ name: 'Dashboard', path: AFFIX_PATH, fullPath: AFFIX_PATH, title: '首页', affix: true })
})

/** 关闭当前 tab 后跳哪：优先右侧邻居，其次左侧，最后回首页 */
function goAfterClose(idx: number) {
  const views = store.visitedViews
  const next = views[Math.min(idx, views.length - 1)]
  router.push(next ? next.fullPath : AFFIX_PATH)
}

function closeTag(v: TagView) {
  if (v.affix) return
  const idx = store.delView(v)
  if (isActive(v)) goAfterClose(idx)
}

function closeOthers(v: TagView) {
  store.delOthersViews(v)
  if (!isCurrent(v)) router.push(v.fullPath)
}

function closeLeft(v: TagView) {
  store.delLeftViews(v)
  if (!isCurrent(v)) router.push(v.fullPath)
}

function closeRight(v: TagView) {
  store.delRightViews(v)
  if (!isCurrent(v)) router.push(v.fullPath)
}

function closeAll() {
  store.delAllViews()
  if (route.path !== AFFIX_PATH) router.push(AFFIX_PATH)
}

/** 鼠标中键关闭 */
function closeByMiddle(v: TagView, e: MouseEvent) {
  if (e.button === 1) closeTag(v)
}

// ===== 右键菜单可见性逻辑 =====
type MenuAction = 'current' | 'others' | 'left' | 'right' | 'all'

const menuIndex = computed(() =>
  menu.view ? store.visitedViews.findIndex((v) => v.path === menu.view!.path) : -1
)
const hasOtherClosable = computed(() =>
  store.visitedViews.some((v) => !v.affix && v.path !== menu.view?.path)
)
const hasLeftClosable = computed(
  () => menuIndex.value > 0 && store.visitedViews.slice(0, menuIndex.value).some((v) => !v.affix)
)
const hasRightClosable = computed(
  () => menuIndex.value !== -1 && store.visitedViews.slice(menuIndex.value + 1).some((v) => !v.affix)
)

function openMenu(v: TagView, e: MouseEvent) {
  e.preventDefault()
  e.stopPropagation()
  menu.view = v
  // 防止菜单贴边溢出
  menu.left = Math.min(e.clientX, window.innerWidth - 140)
  menu.top = Math.min(e.clientY, window.innerHeight - 200)
  menu.visible = true
  document.addEventListener('click', closeMenu, { once: true })
  document.addEventListener('contextmenu', closeMenu, { once: true })
}

function onMenuItem(action: MenuAction) {
  const v = menu.view
  closeMenu()
  if (!v) return
  if (action === 'current') closeTag(v)
  else if (action === 'others') closeOthers(v)
  else if (action === 'left') closeLeft(v)
  else if (action === 'right') closeRight(v)
  else closeAll()
}

// ===== 横向滚动：多开不撑爆，滚轮/切换自动滚动 =====
function handleWheel(e: WheelEvent) {
  const wrap = scrollbarRef.value?.wrapRef as HTMLElement | undefined
  if (!wrap) return
  // 竖向滚轮转为横向滚动
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
    wrap.scrollLeft += e.deltaY
    e.preventDefault()
  }
}

function scrollIntoActive() {
  const wrap = scrollbarRef.value?.wrapRef as HTMLElement | undefined
  const active = innerRef.value?.querySelector('.tag-item.active') as HTMLElement | null
  if (!wrap || !active) return
  const left = active.offsetLeft
  const width = active.offsetWidth
  if (left < wrap.scrollLeft) {
    wrap.scrollTo({ left: left - 8, behavior: 'smooth' })
  } else if (left + width > wrap.scrollLeft + wrap.clientWidth) {
    wrap.scrollTo({ left: left + width - wrap.clientWidth + 8, behavior: 'smooth' })
  }
}
</script>

<template>
  <div class="tags-view">
    <el-scrollbar ref="scrollbarRef" @wheel="handleWheel">
      <div ref="innerRef" class="tags-inner">
        <span
          v-for="v in store.visitedViews"
          :key="v.path"
          class="tag-item"
          :class="{ active: isActive(v), affix: v.affix }"
          @click="toView(v)"
          @contextmenu="openMenu(v, $event)"
          @mouseup="closeByMiddle(v, $event)"
        >
          {{ v.title }}
          <el-icon v-if="!v.affix" class="tag-close" @click.stop="closeTag(v)"><Close /></el-icon>
        </span>
      </div>
    </el-scrollbar>

    <teleport to="body">
      <transition name="el-fade-in-linear">
        <ul
          v-if="menu.visible"
          class="tag-context-menu"
          :style="{ left: `${menu.left}px`, top: `${menu.top}px` }"
        >
          <li :class="{ disabled: !menu.view || menu.view.affix }" @click="onMenuItem('current')">
            关闭当前
          </li>
          <li :class="{ disabled: !hasOtherClosable }" @click="onMenuItem('others')">关闭其他</li>
          <li :class="{ disabled: !hasLeftClosable }" @click="onMenuItem('left')">关闭左侧</li>
          <li :class="{ disabled: !hasRightClosable }" @click="onMenuItem('right')">关闭右侧</li>
          <li class="divided" :class="{ disabled: !hasOtherClosable }" @click="onMenuItem('all')">
            关闭全部
          </li>
        </ul>
      </transition>
    </teleport>
  </div>
</template>

<style scoped>
.tags-view {
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.tags-inner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  white-space: nowrap;
}
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  cursor: pointer;
  user-select: none;
  transition: all 0.15s;
}
.tag-item:hover {
  color: #00a86b;
  border-color: #00a86b;
}
.tag-item.active {
  color: #fff;
  background: #00a86b;
  border-color: #00a86b;
}
.tag-item.affix {
  cursor: default;
}
.tag-close {
  font-size: 12px;
  border-radius: 50%;
}
.tag-close:hover {
  background: rgba(0, 0, 0, 0.18);
  color: #fff;
}
.tag-item.affix:hover {
  color: #00a86b;
}
</style>

<style>
/* teleport 到 body 的右键菜单，不能用 scoped */
.tag-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 110px;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  font-size: 13px;
  color: #303133;
}
.tag-context-menu li {
  padding: 7px 16px;
  cursor: pointer;
}
.tag-context-menu li:hover {
  background: #f5f7fa;
  color: #00a86b;
}
.tag-context-menu li.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}
.tag-context-menu li.disabled:hover {
  background: transparent;
  color: #c0c4cc;
}
.tag-context-menu li.divided {
  margin-top: 4px;
  border-top: 1px solid #ebeef5;
}
</style>
