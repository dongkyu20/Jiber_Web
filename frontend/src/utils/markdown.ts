function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function safeUrl(value: string): string {
  return /^https?:\/\//i.test(value) ? value : '#'
}

function renderInline(value: string): string {
  return escapeHtml(value)
    .replace(/`([^`\n]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*\n]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*\n]+)\*/g, '<em>$1</em>')
    .replace(/\[([^\]\n]+)]\((https?:\/\/[^\s)]+)\)/g, (_, label: string, url: string) => {
      return `<a href="${safeUrl(url)}" target="_blank" rel="noreferrer noopener">${label}</a>`
    })
}

function renderTextSegment(segment: string): string {
  const blocks: string[] = []
  let paragraph: string[] = []
  let listItems: string[] = []
  let listType: 'ul' | 'ol' | null = null

  function flushParagraph() {
    if (!paragraph.length) return
    blocks.push(`<p>${paragraph.map(renderInline).join('<br>')}</p>`)
    paragraph = []
  }

  function flushList() {
    if (!listType || !listItems.length) return
    blocks.push(`<${listType}>${listItems.map(item => `<li>${renderInline(item)}</li>`).join('')}</${listType}>`)
    listItems = []
    listType = null
  }

  for (const line of segment.split('\n')) {
    const trimmed = line.trim()

    if (!trimmed) {
      flushParagraph()
      flushList()
      continue
    }

    const heading = trimmed.match(/^(#{1,3})\s+(.+)$/)
    if (heading) {
      flushParagraph()
      flushList()
      const level = heading[1].length + 2
      blocks.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      continue
    }

    const unordered = trimmed.match(/^[-*]\s+(.+)$/)
    if (unordered) {
      flushParagraph()
      if (listType !== 'ul') flushList()
      listType = 'ul'
      listItems.push(unordered[1])
      continue
    }

    const ordered = trimmed.match(/^\d+\.\s+(.+)$/)
    if (ordered) {
      flushParagraph()
      if (listType !== 'ol') flushList()
      listType = 'ol'
      listItems.push(ordered[1])
      continue
    }

    flushList()
    paragraph.push(trimmed)
  }

  flushParagraph()
  flushList()

  return blocks.join('')
}

export function renderMarkdown(value: string): string {
  const normalized = value.replace(/\r\n/g, '\n')
  const html: string[] = []
  const fencePattern = /```([A-Za-z0-9_-]+)?\n([\s\S]*?)```/g
  let cursor = 0
  let match: RegExpExecArray | null

  while ((match = fencePattern.exec(normalized))) {
    html.push(renderTextSegment(normalized.slice(cursor, match.index)))
    html.push(`<pre><code>${escapeHtml(match[2].trim())}</code></pre>`)
    cursor = match.index + match[0].length
  }

  html.push(renderTextSegment(normalized.slice(cursor)))

  return `<div class="markdown-body">${html.join('')}</div>`
}
