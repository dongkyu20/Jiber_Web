class SearchTrieNode {
  readonly children = new Map<string, SearchTrieNode>()
  readonly suggestions: string[] = []
}

function normalizeSearchText(value: string): string {
  return value.trim().toLocaleLowerCase('ko-KR')
}

export class SearchTrie {
  private readonly root = new SearchTrieNode()
  private readonly seen = new Set<string>()

  constructor(private readonly perPrefixLimit = 12) {}

  insert(value: string) {
    const label = value.trim()
    const key = normalizeSearchText(label)

    if (!label || this.seen.has(key)) {
      return
    }

    this.seen.add(key)
    let node = this.root

    for (const char of Array.from(key)) {
      let next = node.children.get(char)
      if (!next) {
        next = new SearchTrieNode()
        node.children.set(char, next)
      }
      node = next

      if (node.suggestions.length < this.perPrefixLimit) {
        node.suggestions.push(label)
      }
    }
  }

  suggest(prefix: string, limit = 6): string[] {
    const key = normalizeSearchText(prefix)
    if (!key) {
      return []
    }

    let node = this.root
    for (const char of Array.from(key)) {
      const next = node.children.get(char)
      if (!next) {
        return []
      }
      node = next
    }

    return node.suggestions.slice(0, limit)
  }
}
