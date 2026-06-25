<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { communityApi } from '@/api/community'
import { getApiErrorMessage } from '@/api/client'
import type { CommunityCategory } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const allCategoryOptions: Array<{ value: CommunityCategory; label: string }> = [
  { value: 'NOTICE', label: '공지' },
  { value: 'FREE', label: '자유게시판' },
  { value: 'DEAL_REVIEW', label: '매물 후기' },
  { value: 'QNA', label: 'Q&A' }
]

const initialCategory = route.query.category === 'NOTICE' && authStore.isAdmin ? 'NOTICE' : 'FREE'
const category = ref<CommunityCategory>(initialCategory)
const title = ref('')
const content = ref('')
const relatedPropertyId = ref('')
const submitting = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() => title.value.trim().length > 0 && content.value.trim().length > 0 && !submitting.value)
const categoryOptions = computed(() =>
  authStore.isAdmin ? allCategoryOptions : allCategoryOptions.filter((option) => option.value !== 'NOTICE')
)
const isNoticePost = computed(() => category.value === 'NOTICE')

async function submitPost() {
  if (!canSubmit.value) return

  submitting.value = true
  errorMessage.value = ''
  try {
    const response = await communityApi.createPost({
      category: category.value,
      title: title.value.trim(),
      content: content.value.trim(),
      relatedPropertyId: relatedPropertyId.value.trim() ? Number(relatedPropertyId.value) : undefined
    })
    await router.push(`/community/${response.id}`)
  } catch (error) {
    errorMessage.value = getApiErrorMessage(error, '게시글을 등록하지 못했습니다.')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="community-write">
    <div class="page-heading">
      <p class="eyebrow">COMMUNITY</p>
      <h1>{{ isNoticePost ? '공지사항 작성' : '커뮤니티 글쓰기' }}</h1>
      <p>{{ isNoticePost ? '커뮤니티 상단에 노출할 공지사항을 작성합니다.' : '실거주 후기, 매물 경험, 질문을 자유롭게 남겨주세요.' }}</p>
    </div>

    <form class="write-panel" @submit.prevent="submitPost">
      <label>
        <span>분류</span>
        <select v-model="category">
          <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </label>

      <label>
        <span>제목</span>
        <input v-model="title" maxlength="200" type="text" placeholder="제목을 입력하세요" />
      </label>

      <label>
        <span>내용</span>
        <textarea v-model="content" maxlength="20000" rows="14" placeholder="내용을 입력하세요"></textarea>
      </label>

      <label>
        <span>관련 매물 ID</span>
        <input v-model="relatedPropertyId" inputmode="numeric" type="text" placeholder="선택 입력" />
      </label>

      <p v-if="errorMessage" class="inline-error">{{ errorMessage }}</p>

      <div class="write-actions">
        <button class="secondary-button" type="button" @click="router.push('/community')">취소</button>
        <button class="primary-button" type="submit" :disabled="!canSubmit">
          {{ submitting ? '등록 중' : '등록' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.community-write {
  max-width: 820px;
  margin: 0 auto;
}

.write-panel {
  display: grid;
  gap: 18px;
  padding: 28px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}

.write-panel label {
  display: grid;
  gap: 8px;
}

.write-panel span {
  color: var(--cream-muted);
  font-size: 0.84rem;
  font-weight: 700;
}

.write-panel textarea {
  resize: vertical;
  min-height: 260px;
}

.write-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
