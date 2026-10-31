<template>
  <div class="rich-editor">
    <Toolbar class="rich-toolbar" :editor="editorRef" :defaultConfig="toolbarConfig" :mode="mode" />
    <Editor class="rich-content" :style="{ height }" v-model="valueHtml" :defaultConfig="editorConfig"
            :mode="mode" @onCreated="handleCreated" />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'

const props = defineProps<{ modelValue: string; height?: string; mode?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const editorRef = shallowRef()
const mode = props.mode || 'default'
const height = props.height || '260px'
const valueHtml = ref(props.modelValue || '')

const toolbarConfig = { excludeKeys: ['fullScreen', 'group-video'] }
const editorConfig = { placeholder: '输入商品描述… 支持标题/加粗/图片/列表' }

watch(() => props.modelValue, (v) => {
  // 外部重置 (如打开抽屉回显) 时同步, 避免编辑中被覆盖
  if (v !== valueHtml.value) valueHtml.value = v || ''
})
watch(valueHtml, (v) => emit('update:modelValue', v))

const handleCreated = (editor: any) => { editorRef.value = editor }
onBeforeUnmount(() => { editorRef.value?.destroy?.() })
</script>

<style scoped>
.rich-editor { border: 1px solid #dcdfe6; border-radius: 4px; z-index: 10; }
.rich-toolbar { border-bottom: 1px solid #dcdfe6; }
.rich-content { overflow-y: hidden; }
</style>
