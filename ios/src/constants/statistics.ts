import { ROUTE_PATHS } from '@/router/guards'

export type StatisticsReportCategory = 'operation' | 'finance'

export interface StatisticsMetric {
  labelKey: string
  valueKey?: string
  currencyValue?: number
  compactCurrency?: boolean
  dynamicValue?: 'pendingReviews'
  noteKey: string
  tone: 'primary' | 'success' | 'warning' | 'secondary'
}

export interface StatisticsSection {
  titleKey: string
  descriptionKey: string
  bulletKeys: string[]
}

export interface StatisticsReportDefinition {
  key: string
  titleKey: string
  shortTitleKey: string
  path: string
  category: StatisticsReportCategory
  eyebrowKey: string
  subtitleKey: string
  chipKeys: string[]
  metrics: StatisticsMetric[]
  sections: StatisticsSection[]
  boundaryNoteKeys: string[]
  metricsDescriptionKey?: string | null
  showHero?: boolean
  showSections?: boolean
  showBoundaryNotes?: boolean
}

export const STATISTICS_HOME_METRICS: StatisticsMetric[] = [
  {
    labelKey: 'statistics.home.metrics.business.label',
    currencyValue: 128000,
    compactCurrency: true,
    noteKey: 'statistics.home.metrics.business.note',
    tone: 'primary',
  },
  {
    labelKey: 'statistics.home.metrics.channel.label',
    valueKey: 'statistics.values.percent68',
    noteKey: 'statistics.home.metrics.channel.note',
    tone: 'success',
  },
  {
    labelKey: 'statistics.home.metrics.pending.label',
    dynamicValue: 'pendingReviews',
    noteKey: 'statistics.home.metrics.pending.note',
    tone: 'warning',
  },
  {
    labelKey: 'statistics.home.metrics.finance.label',
    valueKey: 'statistics.values.twoItems',
    noteKey: 'statistics.home.metrics.finance.note',
    tone: 'secondary',
  },
]

const reportKey = (report: string, field: string) => `statistics.reports.${report}.${field}`
const reportListKey = (report: string, field: string, index: number) =>
  `statistics.reports.${report}.${field}.${index}`
const reportMetric = (
  report: string,
  index: number,
  tone: StatisticsMetric['tone'],
  options: Pick<StatisticsMetric, 'valueKey' | 'currencyValue' | 'compactCurrency'>,
): StatisticsMetric => ({
  labelKey: reportListKey(report, 'metrics.label', index),
  noteKey: reportListKey(report, 'metrics.note', index),
  tone,
  ...options,
})
const reportSection = (report: string, index: number, bulletCount: number): StatisticsSection => ({
  titleKey: reportListKey(report, 'sections.title', index),
  descriptionKey: reportListKey(report, 'sections.description', index),
  bulletKeys: Array.from({ length: bulletCount }, (_, bulletIndex) =>
    reportListKey(report, `sections.${index}.bullets`, bulletIndex),
  ),
})

export const STATISTICS_REPORTS: Record<string, StatisticsReportDefinition> = {
  businessSummary: {
    key: 'business-summary',
    titleKey: reportKey('businessSummary', 'title'),
    shortTitleKey: reportKey('businessSummary', 'shortTitle'),
    path: ROUTE_PATHS.statisticsBusinessSummary,
    category: 'operation',
    eyebrowKey: reportKey('businessSummary', 'eyebrow'),
    subtitleKey: reportKey('businessSummary', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('businessSummary', 'chips', index)),
    metrics: [
      reportMetric('businessSummary', 0, 'primary', { currencyValue: 56820 }),
      reportMetric('businessSummary', 1, 'success', { valueKey: 'statistics.values.percent84' }),
      reportMetric('businessSummary', 2, 'secondary', { currencyValue: 468 }),
      reportMetric('businessSummary', 3, 'warning', { valueKey: 'statistics.values.threeItems' }),
    ],
    sections: [reportSection('businessSummary', 0, 3), reportSection('businessSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('businessSummary', 'boundaryNotes', index)),
  },
  channelSummary: {
    key: 'channel-summary',
    titleKey: reportKey('channelSummary', 'title'),
    shortTitleKey: reportKey('channelSummary', 'shortTitle'),
    path: ROUTE_PATHS.statisticsChannelSummary,
    category: 'operation',
    eyebrowKey: reportKey('channelSummary', 'eyebrow'),
    subtitleKey: reportKey('channelSummary', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('channelSummary', 'chips', index)),
    metrics: [
      reportMetric('channelSummary', 0, 'primary', { valueKey: 'statistics.values.booking' }),
      reportMetric('channelSummary', 1, 'success', { valueKey: 'statistics.values.percent22' }),
      reportMetric('channelSummary', 2, 'warning', { valueKey: 'statistics.values.twoItems' }),
      reportMetric('channelSummary', 3, 'secondary', { currencyValue: 42600 }),
    ],
    sections: [reportSection('channelSummary', 0, 3), reportSection('channelSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('channelSummary', 'boundaryNotes', index)),
  },
  notesSummary: {
    key: 'notes-summary',
    titleKey: reportKey('notesSummary', 'title'),
    shortTitleKey: reportKey('notesSummary', 'shortTitle'),
    path: ROUTE_PATHS.statisticsNotesSummary,
    category: 'finance',
    eyebrowKey: reportKey('notesSummary', 'eyebrow'),
    subtitleKey: reportKey('notesSummary', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('notesSummary', 'chips', index)),
    metrics: [
      reportMetric('notesSummary', 0, 'primary', { valueKey: 'statistics.values.fourteenItems' }),
      reportMetric('notesSummary', 1, 'warning', { valueKey: 'statistics.values.threeItems' }),
      reportMetric('notesSummary', 2, 'success', { currencyValue: 2360 }),
      reportMetric('notesSummary', 3, 'secondary', { valueKey: 'statistics.values.oneItem' }),
    ],
    sections: [reportSection('notesSummary', 0, 3), reportSection('notesSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('notesSummary', 'boundaryNotes', index)),
  },
  revenueSummary: {
    key: 'revenue-summary',
    titleKey: reportKey('revenueSummary', 'title'),
    shortTitleKey: reportKey('revenueSummary', 'shortTitle'),
    path: ROUTE_PATHS.statisticsRevenueSummary,
    category: 'finance',
    eyebrowKey: reportKey('revenueSummary', 'eyebrow'),
    subtitleKey: reportKey('revenueSummary', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('revenueSummary', 'chips', index)),
    metrics: [
      reportMetric('revenueSummary', 0, 'primary', { currencyValue: 61280 }),
      reportMetric('revenueSummary', 1, 'warning', { currencyValue: 1280 }),
      reportMetric('revenueSummary', 2, 'secondary', { currencyValue: 6400 }),
      reportMetric('revenueSummary', 3, 'success', { valueKey: 'statistics.values.percent96' }),
    ],
    sections: [reportSection('revenueSummary', 0, 3), reportSection('revenueSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('revenueSummary', 'boundaryNotes', index)),
  },
  operationReport: {
    key: 'operation-report',
    metricsDescriptionKey: null,
    showHero: false,
    showSections: false,
    showBoundaryNotes: false,
    titleKey: reportKey('operationReport', 'title'),
    shortTitleKey: reportKey('operationReport', 'shortTitle'),
    path: ROUTE_PATHS.statisticsOperationReport,
    category: 'operation',
    eyebrowKey: reportKey('operationReport', 'eyebrow'),
    subtitleKey: reportKey('operationReport', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('operationReport', 'chips', index)),
    metrics: [
      reportMetric('operationReport', 0, 'success', { valueKey: 'statistics.values.percent81' }),
      reportMetric('operationReport', 1, 'secondary', { valueKey: 'statistics.values.nights18' }),
      reportMetric('operationReport', 2, 'primary', { valueKey: 'statistics.values.percent27' }),
      reportMetric('operationReport', 3, 'warning', { valueKey: 'statistics.values.fourItems' }),
    ],
    sections: [reportSection('operationReport', 0, 3), reportSection('operationReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('operationReport', 'boundaryNotes', index)),
  },
  accommodationReport: {
    key: 'accommodation-report',
    titleKey: reportKey('accommodationReport', 'title'),
    shortTitleKey: reportKey('accommodationReport', 'shortTitle'),
    path: ROUTE_PATHS.statisticsAccommodationReport,
    category: 'operation',
    eyebrowKey: reportKey('accommodationReport', 'eyebrow'),
    subtitleKey: reportKey('accommodationReport', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('accommodationReport', 'chips', index)),
    metrics: [
      reportMetric('accommodationReport', 0, 'primary', { valueKey: 'statistics.values.oneEightSix' }),
      reportMetric('accommodationReport', 1, 'success', { valueKey: 'statistics.values.percent24' }),
      reportMetric('accommodationReport', 2, 'secondary', { valueKey: 'statistics.values.nineOrders' }),
      reportMetric('accommodationReport', 3, 'warning', { valueKey: 'statistics.values.twoOrders' }),
    ],
    sections: [reportSection('accommodationReport', 0, 3), reportSection('accommodationReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('accommodationReport', 'boundaryNotes', index)),
  },
  financeReport: {
    key: 'finance-report',
    titleKey: reportKey('financeReport', 'title'),
    shortTitleKey: reportKey('financeReport', 'shortTitle'),
    path: ROUTE_PATHS.statisticsFinanceReport,
    category: 'finance',
    eyebrowKey: reportKey('financeReport', 'eyebrow'),
    subtitleKey: reportKey('financeReport', 'subtitle'),
    chipKeys: [0, 1, 2].map((index) => reportListKey('financeReport', 'chips', index)),
    metrics: [
      reportMetric('financeReport', 0, 'primary', { currencyValue: 48960 }),
      reportMetric('financeReport', 1, 'secondary', { currencyValue: 9880 }),
      reportMetric('financeReport', 2, 'warning', { currencyValue: 3200 }),
      reportMetric('financeReport', 3, 'success', { valueKey: 'statistics.values.healthy' }),
    ],
    sections: [reportSection('financeReport', 0, 3), reportSection('financeReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('financeReport', 'boundaryNotes', index)),
  },
}
