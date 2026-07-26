// Canvas 渲染专用 Tailwind 运行时（@unocss/runtime + preset-wind3）。
// 契约要求：单例惰性初始化、禁 preflight/reset（避免与 global.css 的 * reset 及 Element Plus 样式互相污染）。
// 不从 main.ts 引入，由 CanvasRenderer 挂载时动态加载，公开页/BLOCKS 路径不受影响。

let initPromise: Promise<void> | null = null

export const ensureCanvasTailwind = (): Promise<void> => {
  if (initPromise) {
    return initPromise
  }
  if (typeof window === 'undefined') {
    return Promise.resolve()
  }
  // 已在别处初始化过（如编辑器与公开页同会话）时直接复用，避免重复挂 MutationObserver
  if (window.__unocss_runtime) {
    initPromise = Promise.resolve()
    return initPromise
  }

  initPromise = (async () => {
    const [{ default: initUnocssRuntime }, { default: presetWind3 }] = await Promise.all([
      import('@unocss/runtime'),
      import('@unocss/preset-wind3'),
    ])
    window.__unocss = {
      // wind3 默认 preflight=true 会注入全局 reset，必须关闭
      presets: [presetWind3({ preflight: false })],
    }
    await initUnocssRuntime()
  })().catch((error: unknown) => {
    // 初始化失败不阻断渲染（退化为无 utility 样式的语义化 DOM），下次挂载允许重试一次
    initPromise = null
    console.warn('[canvasTailwind] UnoCSS 运行时初始化失败，画布将以无 Tailwind 样式渲染', error)
  })

  return initPromise
}
