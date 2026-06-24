import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('vite config', () => {
  it('loads VITE environment variables from the repository root', () => {
    const configPath = join(process.cwd(), 'vite.config.ts')
    const source = readFileSync(configPath, 'utf-8')

    expect(source).toContain("envDir: fileURLToPath(new URL('..', import.meta.url))")
  })
})
