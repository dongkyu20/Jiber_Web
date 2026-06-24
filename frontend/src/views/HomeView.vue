<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { DirectiveBinding } from 'vue'

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
const filters = ['전체', '아파트', '펜트하우스', '빌라']

const allListings = [
  { location: '용산 · 한남동', name: '한남 더힐 펜트하우스', desc: '전용 84m² · 침실 3 · 한강 파노라마', price: '₩28억', type: '펜트하우스' },
  { location: '성동 · 성수동', name: '성수 트라이베카', desc: '전용 59m² · 침실 2 · 복층 구조', price: '₩19억', type: '아파트' },
  { location: '강남 · 청담동', name: '청담 빌라드제니스', desc: '전용 112m² · 침실 4 · 프라이빗 테라스', price: '₩42억', type: '빌라' },
  { location: '서초 · 반포동', name: '반포 아크로리버파크', desc: '전용 78m² · 침실 3 · 리버뷰', price: '₩35억', type: '아파트' },
  { location: '강남 · 압구정동', name: '압구정 갤러리아 포레', desc: '전용 95m² · 침실 4 · 코너 세대', price: '₩52억', type: '아파트' },
  { location: '용산 · 이태원동', name: '이태원 더 프라임', desc: '전용 68m² · 침실 2 · 남산 조망', price: '₩24억', type: '빌라' },
]

const filteredListings = computed(() =>
  activeFilter.value === '전체'
    ? allListings
    : allListings.filter(l => l.type === activeFilter.value)
)
</script>

<template>
  <div class="lp">
    <!-- Fixed header above the scroll container -->
    <header class="lp-header">
      <div class="lp-container lp-header-inner">
        <RouterLink to="/" class="lp-brand">
          <span class="brand-ko">집</span><span class="brand-en">ER</span>
          <span class="brand-sub">ESTATE REAL</span>
        </RouterLink>
        <nav class="lp-nav">
          <RouterLink to="/map">매물 검색</RouterLink>
          <a href="#services">서비스</a>
          <a href="#community">커뮤니티</a>
          <RouterLink to="/login">로그인</RouterLink>
        </nav>
        <RouterLink to="/map" class="lp-cta-btn">지도에서 검색</RouterLink>
      </div>
    </header>

    <!-- Full-page scroll container -->
    <div class="lp-scroll">

      <!-- PAGE 1: Hero -->
      <section class="page">
        <div class="lp-container hero-inner">
          <div class="hero-content">
            <p class="eyebrow-tag" v-reveal="{ delay: 0 }">♦ PREMIUM RESIDENTIAL COLLECTION</p>
            <h1 class="hero-heading" v-reveal="{ delay: 100 }">당신의 일상이<br>작품이 되는 공간</h1>
            <p class="hero-desc" v-reveal="{ delay: 200 }">
              실거래가 분석부터 AI 적정가 추정, 설명가능한 XAI까지 — 데이터로
              검증된 부동산 의사결정을 집er에서 시작하세요.
            </p>
            <div class="hero-search" v-reveal="{ delay: 300 }">
              <div class="search-field">
                <label>지역</label>
                <span>강남 · 성수</span>
              </div>
              <div class="search-sep" />
              <div class="search-field">
                <label>유형</label>
                <span>아파트</span>
              </div>
              <div class="search-sep" />
              <div class="search-field">
                <label>가격대</label>
                <span>20억 ~</span>
              </div>
              <RouterLink to="/map" class="search-btn">검색</RouterLink>
            </div>
          </div>
          <div class="hero-visual" v-reveal="{ delay: 150 }">
            <div class="hero-img">
              <div class="arch-v" />
              <div class="arch-h arch-h-1" />
              <div class="arch-h arch-h-2" />
              <div class="arch-d arch-d-1" />
              <div class="arch-d arch-d-2" />
            </div>
            <div class="hero-badge">
              <p class="badge-label">이번 주 추천</p>
              <h3 class="badge-name">성수 트라이베카</h3>
              <p class="badge-desc">전용 59m² · 복층 · 한강 조망</p>
              <div class="badge-row">
                <span class="badge-price">₩19억</span>
                <a href="#" class="badge-link">상세 →</a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 2: Stats -->
      <section class="page page-dark-alt">
        <div class="lp-container stats-page-inner">
          <div class="stat-big" v-reveal="{ delay: 0 }">
            <p class="stat-big-num">2.3<em>만</em></p>
            <p class="stat-big-label">학습 실거래 데이터</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 100 }">
            <p class="stat-big-num">0.91</p>
            <p class="stat-big-label">적정가 모델 R²</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 200 }">
            <p class="stat-big-num">25<em>구</em></p>
            <p class="stat-big-label">서울 전 자치구</p>
          </div>
          <div class="stat-sep-v" />
          <div class="stat-big" v-reveal="{ delay: 300 }">
            <p class="stat-big-num">24<em>h</em></p>
            <p class="stat-big-label">실거래가 갱신</p>
          </div>
        </div>
      </section>

      <!-- PAGE 3: Featured Listings -->
      <section class="page" id="listings">
        <div class="lp-container">
          <div class="sec-header" v-reveal>
            <div>
              <p class="sec-eyebrow">FEATURED LISTINGS</p>
              <h2 class="sec-title">이번 달 추천 매물</h2>
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
                <a href="#" class="listing-link">상세보기 →</a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 4: Services -->
      <section class="page" id="services">
        <div class="lp-container">
          <div class="sec-header" v-reveal>
            <div>
              <p class="sec-eyebrow">WHAT 집ER DOES</p>
              <h2 class="sec-title">데이터가 답하는<br>부동산 의사결정 플랫폼</h2>
            </div>
            <a href="#" class="text-arrow-link">서비스 시작하기 →</a>
          </div>
          <div class="services-grid">
            <div class="svc-card" v-reveal="{ delay: 0 }">
              <span class="svc-num">01</span>
              <h3>지도 기반 실시간 검색</h3>
              <p>지도를 움직이면 화면 영역 내 단지와 실거래가가 자동 갱신됩니다.</p>
            </div>
            <div class="svc-card" v-reveal="{ delay: 70 }">
              <span class="svc-num">02</span>
              <h3>실거래가 &amp; 시세 분석</h3>
              <p>기간별 실거래 추이, 면적별 평균가, 유사 단지 비교까지 한 화면에서.</p>
            </div>
            <div class="svc-card" v-reveal="{ delay: 140 }">
              <span class="svc-num">03</span>
              <h3>AI 적정가 추정</h3>
              <p>Hedonic Price Model로 적정가를 추정하고 호가와의 괴리를 보여줍니다.</p>
            </div>
            <div class="svc-card" v-reveal="{ delay: 70 }">
              <span class="svc-num">04</span>
              <h3>설명가능한 AI · SHAP</h3>
              <p>면적·조망·입지 등 가격 요인을 SHAP 기반 XAI로 직관적으로 설명합니다.</p>
            </div>
            <div class="svc-card" v-reveal="{ delay: 140 }">
              <span class="svc-num">05</span>
              <h3>AI 챗봇 상담</h3>
              <p>매물 분석 결과를 컨텍스트로, 적정가·요인을 대화로 물어볼 수 있습니다.</p>
            </div>
            <div class="svc-card" v-reveal="{ delay: 210 }">
              <span class="svc-num">06</span>
              <h3>커뮤니티 &amp; 관심목록</h3>
              <p>실거주 후기를 나누고 관심 단지·지역 시세 변동 알림을 받아보세요.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 5: How It Works -->
      <section class="page page-dark-alt">
        <div class="lp-container">
          <div class="sec-header center-header" v-reveal>
            <div style="text-align:center">
              <p class="sec-eyebrow">HOW IT WORKS</p>
              <h2 class="sec-title">4단계로 시작하는<br>스마트 부동산</h2>
            </div>
          </div>
          <div class="how-grid">
            <div class="how-item" v-reveal="{ delay: 0 }">
              <span class="how-num">01</span>
              <h3>지도에서 검색</h3>
              <p>관심 지역을 지도에서 탐색하고 유형·거래방식으로 필터링합니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 120 }">
              <span class="how-num">02</span>
              <h3>실거래·시세 분석</h3>
              <p>기간별 실거래 추이와 면적별·주변 단지 시세를 비교합니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 240 }">
              <span class="how-num">03</span>
              <h3>AI 적정가 · XAI</h3>
              <p>Hedonic 모델 적정가와 SHAP 기반 가격 형성 요인을 확인합니다.</p>
            </div>
            <div class="how-item" v-reveal="{ delay: 360 }">
              <span class="how-num">04</span>
              <h3>AI 챗봇 상담</h3>
              <p>분석 결과를 컨텍스트로 챗봇에 묻고 합리적으로 결정합니다.</p>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 6: Community -->
      <section class="page" id="community">
        <div class="lp-container">
          <div class="sec-header" v-reveal>
            <div>
              <p class="sec-eyebrow">COMMUNITY</p>
              <h2 class="sec-title">실거주자가 만드는<br>부동산 이야기</h2>
            </div>
            <a href="#" class="text-arrow-link">커뮤니티 바로가기 →</a>
          </div>
          <div class="community-grid">
            <div class="hot-posts" v-reveal="{ delay: 0 }">
              <div class="hot-header">
                <span aria-hidden="true">🔥</span>
                <strong>실시간 인기글</strong>
              </div>
              <ol class="post-list">
                <li><span class="post-rank">1</span><span class="post-tag">후기</span><span class="post-title">반포 아크로리버파크 84m² 실거주 1년 후기 (장단점 정리)</span><span class="post-cmts">댓글 42</span></li>
                <li><span class="post-rank">2</span><span class="post-tag">자유</span><span class="post-title">요즘 마포 vs 성수 어디가 더 오를까요?</span><span class="post-cmts">댓글 88</span></li>
                <li><span class="post-rank">3</span><span class="post-tag">Q&amp;A</span><span class="post-title">AI 적정가가 호가보다 낮으면 협상 가능한가요?</span><span class="post-cmts">댓글 31</span></li>
                <li><span class="post-rank">4</span><span class="post-tag">자유</span><span class="post-title">경희궁자이 학군 관련 정보 공유합니다</span><span class="post-cmts">댓글 24</span></li>
                <li><span class="post-rank">5</span><span class="post-tag">후기</span><span class="post-title">헬리오시티 전세 살아본 솔직 후기</span><span class="post-cmts">댓글 19</span></li>
              </ol>
            </div>
            <div class="comm-stats" v-reveal="{ delay: 150 }">
              <div class="comm-cat">
                <div><p class="cat-name">매매후기</p><p class="cat-desc">실거주 경험을 나눠요</p></div>
                <span class="cat-count">1,240</span>
              </div>
              <div class="comm-cat">
                <div><p class="cat-name">자유게시판</p><p class="cat-desc">시세 전망 · 지역 분석</p></div>
                <span class="cat-count">8,530</span>
              </div>
              <div class="comm-cat">
                <div><p class="cat-name">질문 / 답변</p><p class="cat-desc">전문가와 이웃이 답해요</p></div>
                <span class="cat-count">3,140</span>
              </div>
              <div class="comm-meta">
                <p class="meta-label">커뮤니티 현황</p>
                <div class="meta-row"><span>오늘 작성글</span><span>128</span></div>
                <div class="meta-row"><span>전체 게시글</span><span>24,910</span></div>
                <div class="meta-row"><span>활동 회원</span><span>8,402</span></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 7: Client Stories -->
      <section class="page page-dark-alt">
        <div class="lp-container">
          <div v-reveal style="text-align:center; margin-bottom: 48px;">
            <p class="sec-eyebrow">CLIENT STORIES</p>
            <h2 class="sec-title">고객이 전하는 집er</h2>
          </div>
          <div class="review-grid">
            <div class="review-card" v-reveal="{ delay: 0 }">
              <p class="review-quote">"원하던 한남동 펜트하우스를 집er를 통해 만났습니다. 권리관계까지 투명하게 짚어줘서 마음 편히 계약했어요."</p>
              <div class="review-author">
                <div class="author-avatar" />
                <div>
                  <p class="author-name">김도윤 고객님</p>
                  <p class="author-loc">용산 한남동 · 2025</p>
                </div>
              </div>
            </div>
            <div class="review-card" v-reveal="{ delay: 150 }">
              <p class="review-quote">"바쁜 일정에도 전담 컨설턴트가 모든 과정을 챙겨줬습니다. 분양 단지 관람부터 계약까지 군더더기가 없었어요."</p>
              <div class="review-author">
                <div class="author-avatar" />
                <div>
                  <p class="author-name">이서연 고객님</p>
                  <p class="author-loc">성동 성수동 · 2025</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- PAGE 8: Contact + Footer -->
      <section class="page page-contact-footer">
        <div class="lp-container">
          <div class="contact-card" v-reveal>
            <div class="contact-left">
              <p class="sec-eyebrow" style="color:#8a7060">GET IN TOUCH</p>
              <h2 class="contact-heading">원하는 집,<br>집er가 찾아드립니다</h2>
              <p class="contact-desc">상담을 신청하시면 조건에 맞는 매물을 24시간 안에 제안해 드립니다.</p>
              <p class="contact-phone">02 · 1234 · 5678</p>
            </div>
            <form class="contact-form" @submit.prevent>
              <input type="text" placeholder="이름" />
              <input type="text" placeholder="연락처" />
              <input type="text" placeholder="관심 지역 (예: 강남 · 청담)" />
              <button type="submit" class="contact-submit">무료 상담 신청</button>
            </form>
          </div>
        </div>
        <!-- Footer inside last page -->
        <footer class="lp-footer">
          <div class="lp-container footer-inner">
            <div class="footer-brand">
              <RouterLink to="/" class="lp-brand"><span class="brand-ko">집</span><span class="brand-en">ER</span></RouterLink>
              <p>프리미엄 부동산 매매 · 분양</p>
              <p>서울특별시 강남구 청담동 · 02-1234-5678</p>
            </div>
            <nav class="footer-nav">
              <p class="footer-nav-title">EXPLORE</p>
              <a href="#">전체 매물</a>
              <a href="#">분양 정보</a>
              <a href="#">지역별 검색</a>
            </nav>
            <nav class="footer-nav">
              <p class="footer-nav-title">COMPANY</p>
              <a href="#">회사 소개</a>
              <a href="#">고객 후기</a>
              <a href="#">상담 신청</a>
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
  --header-h: 65px;

  /* Full-screen container */
  position: fixed;
  inset: 0;
  z-index: 50;
  background: var(--bg);
  font-family: 'Noto Serif KR', 'Nanum Myeongjo', 'Malgun Gothic', serif;
  color: var(--cream);
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
  background: rgba(22, 13, 4, 0.88);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--border);
}

.lp-header-inner {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
  height: var(--header-h);
}

.lp-brand {
  display: flex;
  align-items: baseline;
  gap: 5px;
  text-decoration: none;
}

.brand-ko { color: var(--cream); font-size: 1.75rem; font-weight: 700; letter-spacing: -0.02em; }
.brand-en { color: var(--cream); font-size: 1.45rem; font-weight: 700; font-style: italic; }
.brand-sub { color: var(--cream-muted); font-size: 0.7rem; letter-spacing: 0.18em; font-weight: 400; margin-left: 4px; }

.lp-nav {
  display: flex;
  justify-content: center;
  gap: 32px;
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
  text-decoration: none;
  letter-spacing: 0.04em;
  transition: background 0.2s;
  white-space: nowrap;
}

.search-btn:hover { background: #0d0804; }

.hero-visual { position: relative; }

.hero-img {
  border-radius: 18px;
  overflow: hidden;
  height: clamp(280px, 44vh, 420px);
  background: radial-gradient(ellipse at 30% 20%, #3a2a14 0%, #241a0a 40%, #120c04 100%);
  position: relative;
}

.arch-v {
  position: absolute; left: 42%; top: 0; bottom: 0; width: 1px;
  background: linear-gradient(to bottom, transparent, rgba(200,160,100,.18) 30%, rgba(200,160,100,.1) 70%, transparent);
}
.arch-h { position: absolute; left: 0; right: 0; height: 1px; }
.arch-h-1 { top: 28%; background: linear-gradient(to right, transparent, rgba(200,160,100,.14), transparent); transform: rotate(-3deg); }
.arch-h-2 { top: 55%; background: linear-gradient(to right, rgba(200,160,100,.1), rgba(200,160,100,.06), transparent); }
.arch-d { position: absolute; height: 1px; width: 140%; background: rgba(200,160,100,.08); transform-origin: left center; }
.arch-d-1 { top: 40%; transform: rotate(-28deg); }
.arch-d-2 { top: 65%; transform: rotate(-18deg); opacity: .6; }

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

/* ── PAGE 4: SERVICES ── */
.services-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.svc-card {
  background: var(--bg-card);
  border: 1px solid var(--border-card);
  border-radius: 14px;
  padding: 24px 22px;
  transition: border-color 0.25s;
}
.svc-card:hover { border-color: rgba(200,160,100,.3); }
.svc-num { display: block; font-size: 0.78rem; color: var(--gold-dim); letter-spacing: 0.12em; margin-bottom: 12px; }
.svc-card h3 { font-size: 1rem; font-weight: 700; margin: 0 0 10px; }
.svc-card p { font-size: 0.82rem; color: var(--cream-muted); line-height: 1.7; margin: 0; }

/* ── PAGE 5: HOW IT WORKS ── */
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

.comm-meta { background: var(--bg-card); border: 1px solid var(--border-card); border-radius: 14px; padding: 16px 20px; }
.meta-label { font-size: 0.7rem; color: var(--cream-muted); letter-spacing: 0.1em; margin: 0 0 10px; }
.meta-row { display: flex; justify-content: space-between; font-size: 0.82rem; margin-bottom: 7px; color: var(--cream-muted); }
.meta-row:last-child { margin-bottom: 0; }
.meta-row span:last-child { color: var(--cream); font-weight: 700; }

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
.contact-submit {
  background: #1a1208;
  color: var(--cream);
  border: none;
  border-radius: 9px;
  padding: 14px;
  font-size: 0.92rem;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  margin-top: 2px;
  transition: background 0.2s;
}
.contact-submit:hover { background: #0d0804; }

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
  .listings-grid, .services-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 900px) {
  .lp-cta-btn { display: none; }
  .lp-header-inner { grid-template-columns: auto 1fr; }
  .lp-nav { justify-content: flex-end; gap: 16px; }

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
  .lp-nav { gap: 10px; font-size: 0.8rem; }
  .listings-grid, .services-grid, .how-grid, .review-grid { grid-template-columns: 1fr; }
  .how-item { border-right: none; }
  .sec-header { flex-direction: column; align-items: flex-start; }
  .stats-page-inner { display: grid; grid-template-columns: 1fr 1fr; gap: 0; }
  .stat-sep-v { display: none; }
  .stat-big { padding: 20px; border-bottom: 1px solid var(--border); }
  .footer-inner { grid-template-columns: 1fr; gap: 20px; }
}
</style>
