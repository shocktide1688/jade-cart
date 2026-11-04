import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 一个标签页对应一条路由记录，path 作为唯一标识（同一路由不同 query 不新开 tab） */
export interface TagView {
  /** 路由 name，同时用于 keep-alive 的 include 匹配 */
  name?: string
  path: string
  /** 完整路径（含 query），用于跳转 */
  fullPath: string
  title: string
  /** 固定标签（如首页），不可关闭，关闭全部时保留 */
  affix?: boolean
}

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([])

  function addView(view: TagView) {
    const idx = visitedViews.value.findIndex((v) => v.path === view.path)
    if (idx === -1) {
      visitedViews.value.push({ ...view })
    } else {
      // 已存在时只同步 fullPath / title，不重复开 tab
      visitedViews.value[idx] = {
        ...visitedViews.value[idx],
        name: view.name,
        fullPath: view.fullPath,
        title: view.title,
      }
    }
  }

  /** 删除标签，返回其原下标（关闭当前 tab 后跳邻居用） */
  function delView(view: TagView): number {
    const idx = visitedViews.value.findIndex((v) => v.path === view.path)
    if (idx !== -1) visitedViews.value.splice(idx, 1)
    return idx
  }

  function delOthersViews(view: TagView) {
    visitedViews.value = visitedViews.value.filter((v) => v.path === view.path || v.affix)
  }

  function delLeftViews(view: TagView) {
    const idx = visitedViews.value.findIndex((v) => v.path === view.path)
    if (idx <= 0) return
    visitedViews.value = visitedViews.value.filter((v, i) => i >= idx || v.affix)
  }

  function delRightViews(view: TagView) {
    const idx = visitedViews.value.findIndex((v) => v.path === view.path)
    if (idx === -1) return
    visitedViews.value = visitedViews.value.filter((v, i) => i <= idx || v.affix)
  }

  function delAllViews() {
    visitedViews.value = visitedViews.value.filter((v) => v.affix)
  }

  /** 登出时清空（含固定标签，重新登录后重建） */
  function clearViews() {
    visitedViews.value = []
  }

  return {
    visitedViews,
    addView,
    delView,
    delOthersViews,
    delLeftViews,
    delRightViews,
    delAllViews,
    clearViews,
  }
})
