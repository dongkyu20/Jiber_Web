<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { DirectiveBinding } from 'vue'

import brandLogoUrl from '@/assets/brand/jiper-estate-real-logo-cropped.png'
import heroCityDayUrl from '@/assets/landing/seoul-city-day.png'
import heroCityNightUrl from '@/assets/landing/seoul-city-night.png'
import ThemeToggle from '@/components/ThemeToggle.vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()
const heroCityUrl = computed(() => (uiStore.isDarkMode ? heroCityNightUrl : heroCityDayUrl))

const vReveal = {
  mounted(el: HTMLElement, binding: DirectiveBinding<{ delay?: number } | undefined>) {
    const delay = binding.value?.delay ?? 0
    el.style.transitionDelay = `${delay}ms`
    el.classList.add('reveal')
    const scrollRoot = el.closest('.lp-scroll') as Element | null
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          el.classList.add('revealed')
          observer.disconnect()
        }
      },
      { root: scrollRoot, threshold: 0.06 }
    )
    observer.observe(el)
  },
}

const activeFilter = ref('전체')
const filters = ['전체', '탐색', 'AI 분석', '회원 기능']

const allListings = [
  { location: '지도 검색', name: '키워드와 현재 화면 범위 검색', desc: '단지명 자동완성과 지도 이동 검색으로 보고 있는 지역의 매물을 확인합니다.', price: '지도 열기', type: '탐색', to: '/map' },
  { location: '매물 유형', name: '아파트·오피스텔·빌라 필터', desc: '거래유형 대신 매물 유형 기준으로 필요한 주거 형태만 좁혀 봅니다.', price: '필터 적용', type: '탐색', to: '/map' },
  { location: '뉴스', name: '최신 부동산 뉴스 피드', desc: 'Google 뉴스 RSS 기반으로 부동산 관련 최신 기사 흐름을 빠르게 확인합니다.', price: '뉴스 보기', type: '탐색', to: '/news' },
  { location: '상세보기', name: '거래내역별 추정가와 요인 설명', desc: '매매 거래 행을 선택하면 해당 거래 기준의 추정가와 가격 요인 차트를 확인합니다.', price: '요인 분석', type: 'AI 분석', to: '/map' },
  { location: '신규매물 분석', name: '입력 조건 기반 적정가 분석', desc: '신규 아파트 조건을 입력하면 가격예측 모델과 가격 요인 분석을 실행합니다.', price: '분석하기', type: 'AI 분석', to: '/new-analysis' },
  { location: 'AI 챗봇', name: '문서 근거 부동산 질문 답변', desc: '주택 동향, 전세 체크리스트, 법령 문서 기반 답변을 마크다운으로 확인합니다.', price: '질문하기', type: 'AI 분석', to: '/chat' },
  { location: '관심목록', name: '관심 단지와 관심 지역 저장', desc: '자주 보는 단지와 지도 영역을 저장해 다시 탐색할 때 바로 불러옵니다.', price: '저장하기', type: '회원 기능', to: '/favorites' },
]

const filteredListings = computed(() =>
  activeFilter.value === '전체'
    ? allListings
    : allListings.filter(l => l.type === activeFilter.value)
)
</script>

<template>
  <div :class="['lp', uiStore.isDarkMode ? 'lp--dark' : 'lp--light']">
    <!-- Fixed header above the scroll container -->
    <header class="lp-header">
      <div class="lp-container lp-header-inner">
        <RouterLink to="/" class="lp-brand">
          <img class="brand-logo-img brand-logo-img--header" :src="brandLogoUrl" alt="집er estate real" />
        </RouterLink>
        <nav class="lp-nav">
          <a href="#listings">주요 기능</a>
          <a href="#how-it-works">이용 흐름</a>
          <a href="#community">커뮤니티</a>
          <a href="#data-ai">데이터·AI</a>
          <a href="#start">시작하기</a>
        </nav>
        <div class="lp-header-actions">
          <ThemeToggle />
          <RouterLink to="/login" class="lp-cta-btn">로그인</RouterLink>
        </div>
      </div>
    </header>

    <!-- Full-page scroll container -->
    <div class="lp-scroll">

      <!-- PAGE 1: Hero -->
      <section class="page">
        <div class="lp-container hero-inner">
          <div class="hero-content">
            <p class="eyebrow-tag" v-reveal="{ delay: 0 }">REAL TRANSACTION DATA PLATFORM</p>
            <h1 class="hero-heading" v-reveal="{ delay: 100 }">부동산 거래 정보를<br>데이터로 확인하는 곳</h1>
            <p class="hero-desc" v-reveal="{ delay: 200 }">
              지도 기반 실거래 탐색부터 최신 부동산 뉴스, 아파트 가격예측,
              가격 요인 설명과 문서 근거 챗봇까지 집er에서 한 번에 확인하세요.
            </p>
            <div class="hero-search" v-reveal="{ delay: 300 }">
              <div class="search-field">
                <label>지역</label>
                <span>서울 전역</span>
              </div>
              <div class="search-sep" />
              <div class="search-field">
                <label>유형</label>
                <span>아파트, 빌라, 오피스텔</span>
              </div>
              <div class="search-sep" />
              <div class="search-field">
                <label>분석</label>
                <span>실거래 · AI</span>
              </div>
              <RouterLink to="/map" class="search-btn">검색</RouterLink>
            </div>
          </div>
          <div class="hero-visual" v-reveal="{ delay: 150 }">
            <div class="hero-img">
              <img class="hero-photo" :src="heroCityUrl" alt="서울 도심 야경" />
            </div>
            <div class="hero-badge">
              <p class="badge-label">상세보기에서 바로 확인</p>
              <h3 class="badge-name">AI 적정가 · 요인 설명</h3>
              <p class="badge-desc">최근 실거래 조건 기반 가격예측과 요인 분석</p>
              <div class="badge-row">
                <RouterLink to="/map" class="badge-link">지도에서 보기 →</RouterLink>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 2: Stats -->
      <section class="page page-dark-alt">
        <div class="lp-container stats-page-inner">
          <div class="stat-big" v-reveal="{ delay: 0 }">
            <p class="stat-big-num">25<em>구</em></p>
            <p class="stat-big-label">서울 자치구 탐색</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 100 }">
            <p class="stat-big-num">AI</p>
            <p class="stat-big-label">아파트 적정가 추정</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 200 }">
            <p class="stat-big-num">SHAP</p>
            <p class="stat-big-label">가격 요인 설명</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 300 }">
            <p class="stat-big-num">RAG</p>
            <p class="stat-big-label">문서 근거 챗봇</p>
          </div>
        </div>
      </section>

      <!-- PAGE 3: Featured Listings -->
      <section class="page" id="listings">
        <div class="lp-container">
          <div class="sec-header" v-reveal>
            <div>
              <p class="sec-eyebrow">CORE FEATURES</p>
              <h2 class="sec-title">필요한 기능을 바로 찾으세요</h2>
            </div>
            <div class="filter-group">
              <button
                v-for="f in filters"
                :key="f"
                :class="['filter-btn', { active: activeFilter === f }]"
                @click="activeFilter = f"
              >{{ f }}</button>
            </div>
          </div>
          <div class="listings-grid">
            <div
              v-for="(item, i) in filteredListings"
              :key="item.name"
              class="listing-card"
              v-reveal="{ delay: i * 70 }"
            >
              <p class="listing-loc">{{ item.location }}</p>
              <h3 class="listing-name">{{ item.name }}</h3>
              <p class="listing-desc">{{ item.desc }}</p>
              <div class="listing-foot">
                <span class="listing-price">{{ item.price }}</span>
                <RouterLink :to="item.to" class="listing-link">시작하기 →</RouterLink>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 4: How It Works -->
      <section class="page page-dark-alt" id="how-it-works">
        <div class="lp-container">
          <div class="sec-header center-header" v-reveal>
            <div style="text-align:center">
              <p class="sec-eyebrow">HOW IT WORKS</p>
              <h2 class="sec-title">4단계로 이어지는<br>탐색 흐름</h2>
            </div>
          </div>
          <div class="how-grid">
            <div class="how-item" v-reveal="{ delay: 0 }">
              <span class="how-num">01</span>
              <h3>지도에서 좁히기</h3>
              <p>검색어 자동완성과 현재 화면 범위 검색으로 관심 지역과 단지를 먼저 찾습니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 120 }">
              <span class="how-num">02</span>
              <h3>거래내역 비교</h3>
              <p>매매, 전세, 월세 거래를 구분해서 보고 월세는 보증금과 월세를 따로 확인합니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 240 }">
              <span class="how-num">03</span>
              <h3>매매 거래 AI 확인</h3>
              <p>매매 거래 행을 선택하거나 신규매물 조건을 입력해 적정가와 요인 분석을 확인합니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 360 }">
              <span class="how-num">04</span>
              <h3>저장하고 질문하기</h3>
              <p>관심목록에 저장하거나 커뮤니티와 문서 기반 챗봇에서 추가 정보를 확인합니다.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 5: Community & News -->
      <section class="page" id="community">
        <div class="lp-container">
          <div class="sec-header" v-reveal>
            <div>
              <p class="sec-eyebrow">COMMUNITY</p>
              <h2 class="sec-title">커뮤니티에서 경험을 나누고<br>뉴스로 흐름을 확인하세요</h2>
            </div>
            <RouterLink to="/community" class="text-arrow-link">커뮤니티 바로가기 →</RouterLink>
          </div>
          <div class="community-grid">
            <div class="hot-posts" v-reveal="{ delay: 0 }">
              <div class="hot-header">
                <span aria-hidden="true">•</span>
                <strong>커뮤니티에서 이런 주제를 다룹니다</strong>
              </div>
              <ol class="post-list">
                <li><span class="post-rank">1</span><span class="post-tag">후기</span><span class="post-title">실거주 후기와 매물 방문 경험을 공유합니다</span><span class="post-cmts">소통</span></li>
                <li><span class="post-rank">2</span><span class="post-tag">Q&amp;A</span><span class="post-title">가격예측과 요인 설명 결과를 어떻게 읽는지 질문합니다</span><span class="post-cmts">질문</span></li>
                <li><span class="post-rank">3</span><span class="post-tag">정보</span><span class="post-title">전세 계약 전 체크리스트와 실거래 확인 팁을 공유합니다</span><span class="post-cmts">공유</span></li>
                <li><span class="post-rank">4</span><span class="post-tag">지역</span><span class="post-title">관심 지역의 거래 흐름과 주변 단지 정보를 살펴봅니다</span><span class="post-cmts">토론</span></li>
                <li><span class="post-rank">5</span><span class="post-tag">문의</span><span class="post-title">서비스 사용 중 불편한 점이나 개선 의견을 남깁니다</span><span class="post-cmts">피드백</span></li>
              </ol>
            </div>
            <div class="comm-stats" v-reveal="{ delay: 150 }">
              <div class="comm-cat">
                <div><p class="cat-name">뉴스 페이지</p><p class="cat-desc">Google 뉴스 RSS 기반 최신 부동산 기사</p></div>
                <RouterLink to="/news" class="cat-count">보기</RouterLink>
              </div>
              <div class="comm-cat">
                <div><p class="cat-name">주요 키워드</p><p class="cat-desc">부동산, 아파트, 전세, 재건축, 청약</p></div>
                <span class="cat-count">RSS</span>
              </div>
              <div class="comm-cat">
                <div><p class="cat-name">활용 방식</p><p class="cat-desc">커뮤니티 의견과 별도로 시장 흐름을 빠르게 확인</p></div>
                <span class="cat-count">NEWS</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 6: Data Sources -->
      <section class="page page-dark-alt" id="data-ai">
        <div class="lp-container">
          <div v-reveal style="text-align:center; margin-bottom: 48px;">
            <p class="sec-eyebrow">DATA & AI</p>
            <h2 class="sec-title">실거래 데이터와 AI 결과를<br>구분해서 보여줍니다</h2>
          </div>
          <div class="review-grid">
            <div class="review-card" v-reveal="{ delay: 0 }">
              <p class="review-quote">실거래 정보는 지도와 상세보기에서 확인하고, AI 가격예측은 아파트 상세보기에서 매매 거래내역을 클릭하면 표시됩니다.</p>
              <div class="review-author">
                <div class="author-avatar" />
                <div>
                  <p class="author-name">실거래 기반 탐색</p>
                  <p class="author-loc">지도 · 상세보기 · 최근 거래</p>
                </div>
              </div>
            </div>
            <div class="review-card" v-reveal="{ delay: 150 }">
              <p class="review-quote">뉴스 화면에서는 Google 뉴스 RSS 기반 최신 부동산 기사를 확인하고, 챗봇에서는 문서 근거 답변을 참고용으로 확인합니다.</p>
              <div class="review-author">
                <div class="author-avatar" />
                <div>
                  <p class="author-name">뉴스와 문서 기반 답변</p>
                  <p class="author-loc">Google 뉴스 RSS · 전세 · 법령 문서</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 7: Contact + Footer -->
      <section class="page page-contact-footer" id="start">
        <div class="lp-container">
          <div class="contact-card" v-reveal>
            <div class="contact-left">
              <p class="sec-eyebrow" style="color:#8a7060">START EXPLORING</p>
              <h2 class="contact-heading">지도에서 시작해<br>AI 설명까지 확인하세요</h2>
              <p class="contact-desc">집er는 중개나 투자 판단 대신, 실거래와 모델 설명을 이해하기 쉽게 모아 보여주는 데이터 서비스입니다.</p>
            </div>
            <form class="contact-form" @submit.prevent>
              <input type="text" value="지도 기반 실거래 탐색" readonly />
              <input type="text" value="Google 뉴스 RSS 기반 부동산 뉴스" readonly />
              <input type="text" value="아파트 가격예측 모델" readonly />
              <input type="text" value="가격 요인 설명" readonly />
            </form>
          </div>
        </div>
        <!-- Footer inside last page -->
        <footer class="lp-footer">
          <div class="lp-container footer-inner">
            <div class="footer-brand">
              <RouterLink to="/" class="lp-brand">
                <img class="brand-logo-img brand-logo-img--footer" :src="brandLogoUrl" alt="집er estate real" />
              </RouterLink>
              <p>지도 기반 부동산 거래 정보 플랫폼</p>
              <p>실거래 분석 · 가격예측 · XAI · 문서 기반 챗봇</p>
            </div>
            <nav class="footer-nav">
              <p class="footer-nav-title">EXPLORE</p>
              <RouterLink to="/map">지도 검색</RouterLink>
              <RouterLink to="/news">뉴스</RouterLink>
              <RouterLink to="/chat">AI 챗봇</RouterLink>
              <RouterLink to="/favorites">관심목록</RouterLink>
            </nav>
            <nav class="footer-nav">
              <p class="footer-nav-title">COMPANY</p>
              <RouterLink to="/community">커뮤니티</RouterLink>
              <RouterLink to="/news">뉴스</RouterLink>
              <RouterLink to="/login">로그인</RouterLink>
            </nav>
          </div>
          <div class="lp-container footer-copy">
            <p>© 2026 집ER Estate Real. All rights reserved.</p>
          </div>
        </footer>
      </section>

    </div><!-- /lp-scroll -->
  </div>
</template>

<style scoped>
/* ── Design tokens ── */
.lp {
  --bg: #160d04;
  --bg-alt: #120b03;
  --bg-card: #241608;
  --gold: #c9a56e;
  --gold-dim: #a07840;
  --cream: #f0e6d0;
  --cream-muted: #9a8060;
  --cream-card: #ede0c4;
  --border: rgba(200, 160, 100, 0.15);
  --border-card: rgba(200, 160, 100, 0.12);
  --lp-header-bg: rgba(22, 13, 4, 0.88);
  --header-h: 65px;

  /* Full-screen container */
  position: fixed;
  inset: 0;
  z-index: 50;
  background: var(--bg);
  font-family: var(--font-body);
  color: var(--cream);
}

.lp.lp--light {
  --bg: #f7f2e8;
  --bg-alt: #efe5d4;
  --bg-card: #fffaf1;
  --gold: #9d6b27;
  --gold-dim: #b98945;
  --cream: #25180c;
  --cream-muted: #725f47;
  --cream-card: #ffffff;
  --border: rgba(121, 84, 37, 0.18);
  --border-card: rgba(121, 84, 37, 0.14);
  --lp-header-bg: rgba(247, 242, 232, 0.88);
}

/* ── Scroll reveal ── */
.reveal {
  opacity: 0;
  transform: translateY(26px);
  transition:
    opacity 0.7s cubic-bezier(0.22, 0.61, 0.36, 1),
    transform 0.7s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

/* ── Layout container ── */
.lp-container {
  width: calc(100% - 80px);
  max-width: 1520px;
  margin: 0 auto;
}

/* ── HEADER (fixed above scroll container) ── */
.lp-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 200;
  background: var(--lp-header-bg);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border);
}

.lp-header-inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 40px;
  height: var(--header-h);
}

.lp-header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.lp-brand {
  display: flex;
  align-items: center;
  text-decoration: none;
  font-family: var(--font-logo);
}

.brand-ko { color: var(--cream); font-size: 1.75rem; font-weight: 700; letter-spacing: -0.02em; }
.brand-en { color: var(--cream); font-size: 1.45rem; font-weight: 700; font-style: italic; }
.brand-sub { color: var(--cream-muted); font-size: 0.7rem; letter-spacing: 0.18em; font-weight: 400; margin-left: 4px; }

.lp-nav {
  display: flex;
  justify-content: center;
  gap: clamp(20px, 3vw, 48px);
  flex-wrap: wrap;
}

.lp-nav a {
  color: var(--cream-muted);
  font-size: 0.9rem;
  letter-spacing: 0.02em;
  transition: color 0.2s;
  text-decoration: none;
}

.lp-nav a:hover,
.lp-nav a.router-link-active { color: var(--cream); }

.lp-cta-btn {
  border: 1px solid var(--cream);
  border-radius: 999px;
  padding: 9px 22px;
  color: var(--cream);
  font-size: 0.86rem;
  letter-spacing: 0.02em;
  transition: background 0.2s, color 0.2s;
  text-decoration: none;
  white-space: nowrap;
}

.lp-cta-btn:hover { background: var(--cream); color: var(--bg); }

/* ── SCROLL CONTAINER (full-page snap) ── */
.lp-scroll {
  position: absolute;
  inset: 0;
  overflow-y: scroll;
  scroll-snap-type: y mandatory;
  scrollbar-width: none;
}

.lp-scroll::-webkit-scrollbar { display: none; }

/* ── BASE PAGE ── */
.page {
  height: 100vh;
  scroll-snap-align: start;
  scroll-snap-stop: always;
  display: flex;
  flex-direction: column;
  justify-content: center;
  overflow: hidden;
  background: var(--bg);
  padding-top: var(--header-h);
  box-sizing: border-box;
}

.page-dark-alt { background: var(--bg-alt); }

/* ── PAGE 1: HERO ── */
.hero-inner {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 56px;
  align-items: center;
}

.eyebrow-tag {
  color: var(--gold);
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  margin: 0 0 18px;
  font-weight: 400;
}

.hero-heading {
  margin: 0 0 20px;
  font-size: clamp(2.2rem, 3.5vw, 3.6rem);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.02em;
}

.hero-desc {
  margin: 0 0 32px;
  color: var(--cream-muted);
  line-height: 1.72;
  font-size: 0.94rem;
  max-width: 460px;
}

.hero-search {
  display: flex;
  align-items: center;
  background: var(--cream-card);
  border-radius: 12px;
  overflow: hidden;
  max-width: 560px;
  padding: 4px;
  border: 1px solid var(--border-card);
}

.search-field {
  display: flex;
  flex-direction: column;
  padding: 12px 18px;
  flex: 1;
  min-width: 0;
}

.search-field label {
  font-size: 0.7rem;
  color: #8a7255;
  letter-spacing: 0.04em;
  margin-bottom: 3px;
  font-weight: 600;
}

.search-field span {
  color: #2a1a0a;
  font-size: 0.92rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-sep {
  width: 1px;
  height: 36px;
  background: rgba(100, 80, 50, 0.22);
  flex-shrink: 0;
}

.search-btn {
  background: #1a1208;
  color: var(--cream);
  padding: 0 26px;
  font-size: 0.92rem;
  font-weight: 700;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  border: 0;
  border-radius: 8px;
  text-decoration: none;
  letter-spacing: 0.04em;
  transition: background 0.2s;
  white-space: nowrap;
}

.search-btn:hover { background: #0d0804; }

.lp.lp--light .search-btn {
  background: #8f6126;
  color: #fffaf1;
}

.lp.lp--light .search-btn:hover { background: #704a1d; }

.hero-visual { position: relative; }

.hero-img {
  border-radius: 18px;
  overflow: hidden;
  height: clamp(280px, 44vh, 420px);
  background: #120c04;
  position: relative;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.34);
}

.hero-img::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(18, 12, 4, 0.04), rgba(18, 12, 4, 0.32));
  pointer-events: none;
}

.lp.lp--light .hero-img::after {
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.02), rgba(37, 24, 12, 0.12));
}

.hero-photo {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-badge {
  position: absolute;
  bottom: 22px; left: 22px; right: 22px;
  background: var(--cream-card);
  border-radius: 12px;
  padding: 18px 22px;
  color: #1a1208;
}

.badge-label { font-size: 0.7rem; color: #7a6040; letter-spacing: 0.06em; margin: 0 0 5px; }
.badge-name { font-size: 1.25rem; font-weight: 700; margin: 0 0 5px; color: #1a1208; }
.badge-desc { font-size: 0.8rem; color: #6a5035; margin: 0 0 12px; }
.badge-row { display: flex; align-items: center; justify-content: space-between; }
.badge-price { font-size: 1.4rem; font-weight: 700; color: #1a1208; }
.badge-link { font-size: 0.82rem; color: #7a6040; text-decoration: none; }

/* ── PAGE 2: STATS ── */
.stats-page-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}

.stat-big {
  flex: 1;
  text-align: center;
  padding: 0 32px;
}

.stat-big-num {
  margin: 0 0 12px;
  font-size: clamp(3.5rem, 6vw, 6rem);
  font-weight: 700;
  color: var(--cream);
  line-height: 1;
}

.stat-big-num em { font-style: normal; color: var(--gold); }

.stat-big-label {
  margin: 0;
  font-size: 0.88rem;
  color: var(--cream-muted);
  letter-spacing: 0.04em;
}

.stat-sep-v {
  width: 1px;
  height: 80px;
  background: var(--border);
  flex-shrink: 0;
}

/* ── SHARED SECTION ── */
.sec-eyebrow {
  font-size: 0.7rem;
  letter-spacing: 0.2em;
  color: var(--gold);
  margin: 0 0 12px;
  font-weight: 400;
}

.sec-title {
  font-size: clamp(1.7rem, 2.6vw, 2.6rem);
  font-weight: 700;
  margin: 0;
  line-height: 1.25;
}

.sec-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 36px;
  flex-wrap: wrap;
}

.center-header { justify-content: center; }

.text-arrow-link {
  color: var(--cream-muted);
  font-size: 0.86rem;
  text-decoration: none;
  white-space: nowrap;
  padding-bottom: 4px;
  transition: color 0.2s;
}
.text-arrow-link:hover { color: var(--cream); }

/* ── PAGE 3: LISTINGS ── */
.listings-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.filter-group { display: flex; gap: 8px; flex-wrap: wrap; }

.filter-btn {
  border: 1px solid var(--border);
  border-radius: 999px;
  background: transparent;
  color: var(--cream-muted);
  padding: 7px 18px;
  font-size: 0.82rem;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-btn.active { border-color: var(--gold); color: var(--cream); }
.filter-btn:hover:not(.active) { color: var(--cream); }

.listing-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 24px 22px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  transition: border-color 0.25s, transform 0.25s;
}
.listing-card:hover { border-color: rgba(200,160,100,.35); transform: translateY(-3px); }

.listing-loc { font-size: 0.75rem; color: var(--gold); letter-spacing: 0.04em; margin: 0; }
.listing-name { font-size: 1.15rem; font-weight: 700; margin: 0; }
.listing-desc { font-size: 0.8rem; color: var(--cream-muted); margin: 0; }

.listing-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--border-card);
}

.listing-price { font-size: 1.35rem; font-weight: 700; }
.listing-link { font-size: 0.8rem; color: var(--cream-muted); text-decoration: none; transition: color 0.2s; }
.listing-link:hover { color: var(--cream); }

/* ── PAGE 4: HOW IT WORKS ── */
.how-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.how-item {
  padding: 36px 28px;
  border-top: 1px solid var(--border);
}
.how-item:not(:last-child) { border-right: 1px solid var(--border); }
.how-num { display: block; font-size: 1.6rem; font-weight: 700; color: var(--gold); margin-bottom: 16px; }
.how-item h3 { font-size: 1rem; font-weight: 700; margin: 0 0 10px; }
.how-item p { font-size: 0.82rem; color: var(--cream-muted); line-height: 1.7; margin: 0; }

/* ── PAGE 6: COMMUNITY ── */
.community-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 20px;
  align-items: start;
}

.hot-posts { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: 14px; overflow: hidden; }
.hot-header { display: flex; align-items: center; gap: 8px; padding: 18px 22px; border-bottom: 1px solid var(--border-card); font-size: 0.92rem; font-weight: 700; }

.post-list { list-style: none; margin: 0; padding: 0; }
.post-list li { display: flex; align-items: center; gap: 10px; padding: 13px 22px; border-bottom: 1px solid var(--border-card); transition: background 0.2s; }
.post-list li:last-child { border-bottom: none; }
.post-list li:hover { background: rgba(200,160,100,.04); }
.post-rank { font-size: 0.95rem; font-weight: 700; color: var(--gold); min-width: 18px; }
.post-tag { font-size: 0.7rem; padding: 2px 7px; background: rgba(200,160,100,.12); border-radius: 4px; color: var(--gold-dim); flex-shrink: 0; letter-spacing: 0.04em; }
.post-title { font-size: 0.85rem; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.post-cmts { font-size: 0.75rem; color: var(--cream-muted); flex-shrink: 0; }

.comm-stats { display: flex; flex-direction: column; gap: 12px; }
.comm-cat { display: flex; align-items: center; justify-content: space-between; gap: 12px; background: var(--bg-card); border: 1px solid var(--border-card); border-radius: 14px; padding: 16px 20px; }
.cat-name { font-size: 0.92rem; font-weight: 700; margin: 0 0 3px; }
.cat-desc { font-size: 0.75rem; color: var(--cream-muted); margin: 0; }
.cat-count { font-size: 1.3rem; font-weight: 700; color: var(--gold); white-space: nowrap; }

/* ── PAGE 7: REVIEWS ── */
.review-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
}

.review-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 32px 28px;
}
.review-quote { font-size: 0.98rem; line-height: 1.78; margin: 0 0 26px; }
.review-author { display: flex; align-items: center; gap: 12px; }
.author-avatar { width: 42px; height: 42px; border-radius: 50%; background: linear-gradient(135deg, var(--gold-dim), #6a5030); flex-shrink: 0; }
.author-name { font-size: 0.88rem; font-weight: 700; margin: 0 0 3px; }
.author-loc { font-size: 0.76rem; color: var(--cream-muted); margin: 0; }

/* ── PAGE 8: CONTACT + FOOTER ── */
.page-contact-footer {
  justify-content: flex-start;
  padding-bottom: 0;
}

.page-contact-footer .lp-container {
  padding-top: 40px;
}

.contact-card {
  background: var(--cream-card);
  border-radius: 18px;
  padding: 44px 52px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 56px;
  align-items: center;
  color: #1a1208;
}

.contact-heading { font-size: clamp(1.5rem, 2.4vw, 2.2rem); font-weight: 700; margin: 0 0 14px; color: #1a1208; line-height: 1.3; }
.contact-desc { font-size: 0.88rem; color: #6a5535; line-height: 1.7; margin: 0 0 20px; }
.contact-phone { font-size: 1.5rem; font-weight: 700; color: #1a1208; margin: 0; }

.contact-form { display: flex; flex-direction: column; gap: 10px; }
.contact-form input {
  border: 1px solid rgba(100,80,50,.3);
  border-radius: 9px;
  background: rgba(255,255,255,.6);
  padding: 13px 16px;
  font-size: 0.9rem;
  color: #2a1a0a;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}
.contact-form input::placeholder { color: #9a8060; }
.contact-form input:focus { border-color: rgba(100,80,50,.6); }
/* ── FOOTER (inside last page) ── */
.lp-footer {
  background: #0d0804;
  border-top: 1px solid var(--border);
  margin-top: auto;
}

.footer-inner {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 56px;
  padding: 32px 0;
}

.footer-brand .lp-brand { display: inline-flex; margin-bottom: 10px; }
.footer-brand p { font-size: 0.8rem; color: var(--cream-muted); margin: 0 0 3px; }

.footer-nav { display: flex; flex-direction: column; gap: 8px; }
.footer-nav-title { font-size: 0.7rem; letter-spacing: 0.15em; color: var(--cream-muted); font-weight: 400; margin: 0 0 4px; }
.footer-nav a { font-size: 0.85rem; color: var(--cream-muted); text-decoration: none; transition: color 0.2s; }
.footer-nav a:hover { color: var(--cream); }

.footer-copy { padding: 16px 0; border-top: 1px solid var(--border); }
.footer-copy p { font-size: 0.76rem; color: var(--cream-muted); margin: 0; }

/* ── RESPONSIVE ── */
@media (max-width: 1100px) {
  .lp-container { width: calc(100% - 48px); }
  .listings-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 900px) {
  .lp-cta-btn { display: none; }
  .lp-header-inner { grid-template-columns: auto 1fr auto; gap: 20px; }
  .lp-nav { justify-content: flex-end; gap: 32px; }

  .hero-inner { grid-template-columns: 1fr; gap: 32px; }
  .hero-img { height: 220px; }

  .how-grid { grid-template-columns: repeat(2, 1fr); }
  .how-item:nth-child(2) { border-right: none; }

  .community-grid { grid-template-columns: 1fr; }
  .contact-card { grid-template-columns: 1fr; gap: 28px; padding: 32px 28px; }

  .footer-inner { grid-template-columns: 1fr 1fr; gap: 28px; }
  .footer-brand { grid-column: 1 / -1; }
}

@media (max-width: 640px) {
  .lp-container { width: calc(100% - 32px); }
  .lp-nav { gap: 20px; font-size: 0.8rem; }
  .hero-search { flex-wrap: wrap; }
  .search-field { min-width: 42%; }
  .search-btn { min-height: 46px; justify-content: center; flex: 1 0 100%; margin-left: 0; }
  .listings-grid, .how-grid, .review-grid { grid-template-columns: 1fr; }
  .how-item { border-right: none; }
  .sec-header { flex-direction: column; align-items: flex-start; }
  .stats-page-inner { display: grid; grid-template-columns: 1fr 1fr; gap: 0; }
  .stat-sep-v { display: none; }
  .stat-big { padding: 20px; border-bottom: 1px solid var(--border); }
  .footer-inner { grid-template-columns: 1fr; gap: 20px; }
}
</style>
