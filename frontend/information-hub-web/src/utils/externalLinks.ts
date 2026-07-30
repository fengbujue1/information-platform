/**
 * 只允许浏览器跳转到明确的 HTTP(S) 地址，拒绝脚本等危险协议。
 */
export function resolveSafeExternalUrl(
  value: string | null | undefined,
): string | null {
  const candidate = value?.trim()
  if (!candidate) {
    return null
  }

  try {
    const url = new URL(candidate)
    return url.protocol === 'http:' || url.protocol === 'https:'
      ? url.toString()
      : null
  } catch {
    return null
  }
}
