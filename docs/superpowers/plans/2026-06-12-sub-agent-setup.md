# Sub-Agent Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create persistent project-local role definitions for six Codex sub-agents and a root guide that explains how to use them.

**Architecture:** Store durable role prompts under `.agents/` and expose them through root `AGENTS.md`. Each role file includes responsibilities, non-responsibilities, ownership paths, inputs, outputs, handoff rules, and initial tasks.

**Tech Stack:** Markdown documentation for Codex agent coordination.

---

## File Structure

- Create `.agents/README.md`: shared operating model for all sub-agents.
- Create `.agents/architecture-design-agent.md`: system design and cross-service contract role.
- Create `.agents/backend-api-agent.md`: Spring Boot API and MyBatis role.
- Create `.agents/auth-security-agent.md`: OAuth2, JWT, Spring Security, and access control role.
- Create `.agents/frontend-map-agent.md`: Vue, Kakao Maps, routing, state, and chart role.
- Create `.agents/ai-data-integration-agent.md`: FastAPI, hedonic valuation, SHAP, and data contract role.
- Create `.agents/qa-review-agent.md`: verification and review role.
- Create `AGENTS.md`: root entrypoint for contributors and Codex agents.

### Task 1: Create Shared Agent Guide

**Files:**
- Create: `.agents/README.md`
- Create: `AGENTS.md`

- [ ] **Step 1: Write `.agents/README.md`**

Create a shared guide with hybrid operation, ownership path rules, live sub-agent prompt template, final output contract, and handoff format.

- [ ] **Step 2: Write root `AGENTS.md`**

Create a root entrypoint that directs agents to `.agents/README.md` and summarizes the six role files.

### Task 2: Create Role Definition Files

**Files:**
- Create: `.agents/architecture-design-agent.md`
- Create: `.agents/backend-api-agent.md`
- Create: `.agents/auth-security-agent.md`
- Create: `.agents/frontend-map-agent.md`
- Create: `.agents/ai-data-integration-agent.md`
- Create: `.agents/qa-review-agent.md`

- [ ] **Step 1: Write six role files**

Each file must include purpose, responsibilities, non-responsibilities, primary ownership paths, shared / contract paths, restricted paths, expected inputs, expected outputs, handoff rules, first tasks, and live sub-agent prompt seed.

- [ ] **Step 2: Keep ownership paths aligned with the approved spec**

Use the ownership path model from `docs/superpowers/specs/2026-06-12-sub-agent-setup-design.md`.

### Task 3: Verify Setup

**Files:**
- Read: `.agents/*.md`
- Read: `AGENTS.md`
- Read: `docs/superpowers/specs/2026-06-12-sub-agent-setup-design.md`

- [ ] **Step 1: List created files**

Run: `find .agents -maxdepth 1 -type f`

Expected: all seven `.agents` Markdown files are present.

- [ ] **Step 2: Check required sections**

Run: `rg -n "Primary ownership paths|Shared / contract paths|Restricted paths|Handoff Rules|First Tasks" .agents AGENTS.md`

Expected: each role file includes ownership and handoff sections.

- [ ] **Step 3: Note git status limitation**

Run: `git status --short`

Expected: command may fail because this workspace is not currently a git repository.
