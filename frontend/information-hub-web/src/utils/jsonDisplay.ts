import type { JsonValue } from '@/types/json'

/**
 * 将标准化业务 JSON 格式化为稳定的只读文本，不执行其中的任何内容。
 */
export function formatJsonForDisplay(value: JsonValue): string {
  return JSON.stringify(value, null, 2)
}
