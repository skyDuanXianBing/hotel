import { ROUTE_PATHS } from '@/router/guards'

export type StatisticsReportCategory = 'operation' | 'finance'

export interface StatisticsMetric {
  labelKey: string
  dynamicValue?: 'pendingReviews'
  valueFormat?: 'currency' | 'percent' | 'number' | 'text'
  compactCurrency?: boolean
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
  downloadType?: 'room-fees' | 'transaction-summary' | 'daily'
}

export const STATISTICS_HOME_METRICS: StatisticsMetric[] = [
  {
    labelKey: 'statistics.home.metrics.business.label',
    valueFormat: 'currency',
    compactCurrency: true,
    noteKey: 'statistics.home.metrics.business.note',
    tone: 'primary',
  },
  {
    labelKey: 'statistics.home.metrics.channel.label',
    valueFormat: 'percent',
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
    valueFormat: 'currency',
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
  options: Pick<StatisticsMetric, 'valueFormat' | 'compactCurrency'>,
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
      reportMetric('businessSummary', 0, 'primary', { valueFormat: 'currency' }),
      reportMetric('businessSummary', 1, 'success', { valueFormat: 'percent' }),
      reportMetric('businessSummary', 2, 'secondary', { valueFormat: 'currency' }),
      reportMetric('businessSummary', 3, 'warning', { valueFormat: 'number' }),
    ],
    sections: [reportSection('businessSummary', 0, 3), reportSection('businessSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('businessSummary', 'boundaryNotes', index)),
    showSections: false,
    showBoundaryNotes: false,
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
      reportMetric('channelSummary', 0, 'primary', { valueFormat: 'text' }),
      reportMetric('channelSummary', 1, 'success', { valueFormat: 'percent' }),
      reportMetric('channelSummary', 2, 'warning', { valueFormat: 'number' }),
      reportMetric('channelSummary', 3, 'secondary', { valueFormat: 'currency' }),
    ],
    sections: [reportSection('channelSummary', 0, 3), reportSection('channelSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('channelSummary', 'boundaryNotes', index)),
    showSections: false,
    showBoundaryNotes: false,
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
      reportMetric('notesSummary', 0, 'primary', { valueFormat: 'number' }),
      reportMetric('notesSummary', 1, 'warning', { valueFormat: 'number' }),
      reportMetric('notesSummary', 2, 'success', { valueFormat: 'currency' }),
      reportMetric('notesSummary', 3, 'secondary', { valueFormat: 'currency' }),
    ],
    sections: [reportSection('notesSummary', 0, 3), reportSection('notesSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('notesSummary', 'boundaryNotes', index)),
    showSections: false,
    showBoundaryNotes: false,
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
      reportMetric('revenueSummary', 0, 'primary', { valueFormat: 'currency' }),
      reportMetric('revenueSummary', 1, 'warning', { valueFormat: 'currency' }),
      reportMetric('revenueSummary', 2, 'secondary', { valueFormat: 'currency' }),
      reportMetric('revenueSummary', 3, 'success', { valueFormat: 'currency' }),
    ],
    sections: [reportSection('revenueSummary', 0, 3), reportSection('revenueSummary', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('revenueSummary', 'boundaryNotes', index)),
    showSections: false,
    showBoundaryNotes: false,
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
      reportMetric('operationReport', 0, 'success', { valueFormat: 'percent' }),
      reportMetric('operationReport', 1, 'secondary', { valueFormat: 'currency' }),
      reportMetric('operationReport', 2, 'primary', { valueFormat: 'currency' }),
      reportMetric('operationReport', 3, 'warning', { valueFormat: 'number' }),
    ],
    sections: [reportSection('operationReport', 0, 3), reportSection('operationReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('operationReport', 'boundaryNotes', index)),
    downloadType: 'daily',
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
      reportMetric('accommodationReport', 0, 'primary', { valueFormat: 'number' }),
      reportMetric('accommodationReport', 1, 'success', { valueFormat: 'percent' }),
      reportMetric('accommodationReport', 2, 'secondary', { valueFormat: 'number' }),
      reportMetric('accommodationReport', 3, 'warning', { valueFormat: 'number' }),
    ],
    sections: [reportSection('accommodationReport', 0, 3), reportSection('accommodationReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('accommodationReport', 'boundaryNotes', index)),
    downloadType: 'room-fees',
    showSections: false,
    showBoundaryNotes: false,
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
      reportMetric('financeReport', 0, 'primary', { valueFormat: 'currency' }),
      reportMetric('financeReport', 1, 'secondary', { valueFormat: 'currency' }),
      reportMetric('financeReport', 2, 'warning', { valueFormat: 'currency' }),
      reportMetric('financeReport', 3, 'success', { valueFormat: 'currency' }),
    ],
    sections: [reportSection('financeReport', 0, 3), reportSection('financeReport', 1, 3)],
    boundaryNoteKeys: [0, 1].map((index) => reportListKey('financeReport', 'boundaryNotes', index)),
    downloadType: 'transaction-summary',
    showSections: false,
    showBoundaryNotes: false,
  },
}
