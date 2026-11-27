import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('index.html', () => {
  it('uses "Aqua Grow" as the page title', () => {
    const html = readFileSync(resolve(process.cwd(), 'index.html'), 'utf-8')
    expect(html).toMatch(/<title>Aqua Grow<\/title>/)
  })
})
