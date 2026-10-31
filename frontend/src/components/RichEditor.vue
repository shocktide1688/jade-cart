<template>
  <div class="rich-editor">
    <QuillEditor ref="quillRef" theme="snow" v-model:content="content" contentType="html"
                 :toolbar="toolbar" :style="{ height }" @update:content="onUpdate" />
  </div>
</template>

<script setup lang="ts">
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { ref, watch } from 'vue'

const props = defineProps<{ modelValue: string; height?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

// 图片按钮隐藏: quill 默认 base64 内嵌会灌库, 图片统一走封面/媒体库
const toolbar = [
  [{ header: [1, 2, 3, false] }],
  ['bold', 'italic', 'underline', 'strike'],
  [{ list: 'ordered' }, { list: 'bullet' }],
  ['blockquote', 'link'],
  ['clean'],
]

const content = ref(props.modelValue || '')

const height = props.height || '260px'
let suppress = false

// 外部重置 (打开抽屉回显) 时同步
watch(() => props.modelValue, (v) => {
  if (v !== content.value) {
    suppress = true
    content.value = v || '<p><br></p>'
    setTimeout(() => { suppress = false }, 0)
  }
})

function onUpdate(v: string) {
  if (suppress) return
  // 空编辑器归一为空串, 避免存 '<p><br></p>' 垃圾
  const cleaned = v === '<p><br></p>' ? '' : v
  emit('update:modelValue', cleaned)
}
</script>

<style scoped>
.rich-editor { border: 1px solid #dcdfe6; border-radius: 4px; }
.rich-editor :deep(.ql-toolbar) { border: none; border-bottom: 1px solid #dcdfe6; }
.rich-editor :deep(.ql-container) { border: none; font: inherit; }
.rich-editor :deep(.ql-editor) { min-height: v-bind(height); }
</style>
