import { describe, it, expect } from 'vitest'

// Import the helper directly — it is a module-level export
import { formatMonth } from './FeedLogPivotTable.vue'

describe('formatMonth', () => {
  it('formats a mid-month date as "Month YYYY"', () => {
    expect(formatMonth('2026-08-15')).toBe('August 2026')
  })

  it('formats first-of-month without UTC shift', () => {
    expect(formatMonth('2026-01-01')).toBe('January 2026')
  })

  it('formats last-of-month without UTC shift', () => {
    expect(formatMonth('2026-12-31')).toBe('December 2026')
  })

  it('returns "Unknown Month" for undefined', () => {
    expect(formatMonth(undefined)).toBe('Unknown Month')
  })

  it('returns "Unknown Month" for empty string', () => {
    expect(formatMonth('')).toBe('Unknown Month')
  })
})

describe('month group key', () => {
  it('produces YYYY-MM key from YYYY-MM-DD date', () => {
    const date = '2026-08-15'
    expect(date.slice(0, 7)).toBe('2026-08')
  })

  it('same month key for dates in the same month', () => {
    expect('2026-08-01'.slice(0, 7)).toBe('2026-08-31'.slice(0, 7))
  })

  it('different month keys for dates in different months', () => {
    expect('2026-08-31'.slice(0, 7)).not.toBe('2026-09-01'.slice(0, 7))
  })

  it('YYYY-MM keys sort most-recent-first with localeCompare', () => {
    const keys = ['2026-06', '2026-08', '2026-07']
    const sorted = [...keys].sort((a, b) => b.localeCompare(a))
    expect(sorted).toEqual(['2026-08', '2026-07', '2026-06'])
  })
})
