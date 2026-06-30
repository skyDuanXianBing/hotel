import path from 'path'
import express, { ErrorRequestHandler, NextFunction, Request, Response } from 'express'
import cors from 'cors'
import chalk from 'chalk'

import config from './config'
import { requestLogger, getLogs, clearLogs } from './logger'
import mockSuApiRouter from './mock-su-api'
import {
  clearMappingPriceFailures,
  getMappingPriceStateSnapshot,
  resetMappingPriceState,
  setMappingPriceFailures,
} from './mock-su-api/mappingPriceState'
import webhookSenderRouter from './webhook-sender'
import e2eRouter from './e2e'
import { getPmsConfigDiagnostic } from './pms-client'

const app = express()

// 基础中间件
app.use(cors())
app.use(express.json({ limit: '10mb' }))
app.use(express.urlencoded({ extended: true, limit: '10mb' }))

// 自动记录所有进入 Mock API 的请求
app.use(requestLogger)

app.use(mockSuApiRouter)
app.use(webhookSenderRouter)
app.use(e2eRouter)

// 控制面板内部 API
app.get('/api/logs', (_req: Request, res: Response) => {
  res.json({ success: true, data: getLogs() })
})

app.delete('/api/logs', (_req: Request, res: Response) => {
  clearLogs()
  res.json({ success: true, message: 'logs cleared' })
})

app.post('/api/test/channel-mapping-price-settings/reset', (req: Request, res: Response) => {
  res.json(
    resetMappingPriceState({
      hotelId: req.body?.hotelId || req.body?.hotelid,
      channelId: req.body?.channelId || req.body?.channelid,
      scenario: req.body?.scenario,
    }),
  )
})

app.get('/api/test/channel-mapping-price-settings/state', (req: Request, res: Response) => {
  res.json(
    getMappingPriceStateSnapshot({
      hotelId: req.query.hotelId || req.query.hotelid,
      channelId: req.query.channelId || req.query.channelid,
    }),
  )
})

app.post('/api/test/channel-mapping-price-settings/failures', (req: Request, res: Response) => {
  res.json(setMappingPriceFailures(req.body || {}))
})

app.delete('/api/test/channel-mapping-price-settings/failures', (_req: Request, res: Response) => {
  res.json(clearMappingPriceFailures())
})

app.get('/api/config', (_req: Request, res: Response) => {
  const pmsDiagnostic = getPmsConfigDiagnostic()
  // 隐藏敏感字段
  res.json({
    success: true,
    data: {
      port: config.port,
      pmsBaseUrl: config.pmsBaseUrl,
      pmsAuth: {
        hasToken: Boolean(config.pmsAuth.token),
        storeId: config.pmsAuth.storeId || null,
      },
      testSupportAuth: {
        hasKey: pmsDiagnostic.hasTestSupportKey === true,
        source: pmsDiagnostic.testSupportKeySource || 'missing',
      },
      defaultHotelId: config.defaultHotelId,
      webhookAuth: {
        username: config.webhookAuth.username,
        password: config.webhookAuth.password ? '***' : '',
      },
      suAuth: {
        clientId: config.suAuth.clientId,
        clientSecret: config.suAuth.clientSecret ? '***' : '',
      },
    },
  })
})

const dashboardPath = path.join(__dirname, 'dashboard')

type ViteModule = typeof import('vite')

function isApiRequestPath(reqPath: string): boolean {
  return (
    reqPath === '/api' ||
    reqPath.startsWith('/api/') ||
    reqPath === '/SUAPI' ||
    reqPath.startsWith('/SUAPI/') ||
    reqPath === '/scenarios' ||
    reqPath.startsWith('/scenarios/') ||
    reqPath === '/webhook' ||
    reqPath.startsWith('/webhook/')
  )
}

function shouldUseViteDashboard(): boolean {
  return process.env.NODE_ENV !== 'production' && path.basename(__dirname) !== 'dist'
}

async function loadViteModule(): Promise<ViteModule> {
  // Preserve native dynamic import so compiled production startup does not require Vite.
  const dynamicImport = new Function('specifier', 'return import(specifier)') as (
    specifier: string,
  ) => Promise<ViteModule>
  return dynamicImport('vite')
}

async function mountDashboard(): Promise<void> {
  if (shouldUseViteDashboard()) {
    const { createServer: createViteServer } = await loadViteModule()
    const vite = await createViteServer({
      root: path.join(__dirname, '..'),
      server: {
        middlewareMode: true,
      },
      appType: 'spa',
    })

    app.use((req: Request, res: Response, next: NextFunction) => {
      if (isApiRequestPath(req.path) || (req.method !== 'GET' && req.method !== 'HEAD')) {
        next()
        return
      }

      vite.middlewares(req, res, next)
    })
    return
  }

  // Vue 控制面板生产构建产物
  app.use(express.static(dashboardPath))

  app.get('*', (req: Request, res: Response, next) => {
    if (isApiRequestPath(req.path) || !req.accepts('html')) {
      next()
      return
    }

    res.sendFile(path.join(dashboardPath, 'index.html'), (err) => {
      if (err) {
        next()
      }
    })
  })
}

async function start(): Promise<void> {
  await mountDashboard()

  // 兜底 404
  app.use((req: Request, res: Response) => {
    res.status(404).json({ success: false, message: `Not Found: ${req.method} ${req.originalUrl}` })
  })

  app.use(errorHandler)

  app.listen(config.port, () => {
    const banner = chalk.bold.cyan('Channel Simulator')
    // eslint-disable-next-line no-console
    console.log(chalk.gray('────────────────────────────────────────────────'))
    // eslint-disable-next-line no-console
    console.log(`${banner} ${chalk.green('started')}`)
    // eslint-disable-next-line no-console
    console.log(`  ${chalk.bold('Port:')}        ${chalk.yellow(config.port)}`)
    // eslint-disable-next-line no-console
    console.log(`  ${chalk.bold('Dashboard:')}   ${chalk.underline(`http://localhost:${config.port}/`)}`)
    // eslint-disable-next-line no-console
    console.log(`  ${chalk.bold('PMS target:')}  ${chalk.underline(config.pmsBaseUrl)}`)
    // eslint-disable-next-line no-console
    console.log(chalk.gray('────────────────────────────────────────────────'))
  })
}

// 全局错误处理
const errorHandler: ErrorRequestHandler = (err, _req, res, _next) => {
  // eslint-disable-next-line no-console
  console.error(chalk.red('[server] unhandled error:'), err)
  const message = err && err.message ? err.message : 'Internal Server Error'
  res.status(500).json({ success: false, message })
}

start().catch((err) => {
  // eslint-disable-next-line no-console
  console.error(chalk.red('[server] failed to start:'), err)
  process.exitCode = 1
})

export default app
