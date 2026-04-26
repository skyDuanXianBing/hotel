import { ROUTE_PATHS } from '@/router/guards'

export type StatisticsReportCategory = 'operation' | 'finance'

export interface StatisticsMetric {
  label: string
  value: string
  note: string
  tone: 'primary' | 'success' | 'warning' | 'secondary'
}

export interface StatisticsSection {
  title: string
  description: string
  bullets: string[]
}

export interface StatisticsReportDefinition {
  key: string
  title: string
  shortTitle: string
  path: string
  category: StatisticsReportCategory
  eyebrow: string
  subtitle: string
  chips: string[]
  metrics: StatisticsMetric[]
  sections: StatisticsSection[]
  boundaryNotes: string[]
  metricsDescription?: string | null
  showHero?: boolean
  showSections?: boolean
  showBoundaryNotes?: boolean
}

export const STATISTICS_HOME_METRICS: StatisticsMetric[] = [
  {
    label: '营业概览',
    value: '¥128k',
    note: '近 7 天核心营收摘要',
    tone: 'primary',
  },
  {
    label: '渠道占比',
    value: '68%',
    note: 'OTA 贡献保持稳定',
    tone: 'success',
  },
  {
    label: '待审查',
    value: '6 条',
    note: '优先处理今日入住资料',
    tone: 'warning',
  },
  {
    label: '财务提醒',
    value: '2 项',
    note: '待核对流水与挂账',
    tone: 'secondary',
  },
]

export const STATISTICS_REPORTS: Record<string, StatisticsReportDefinition> = {
  businessSummary: {
    key: 'business-summary',
    title: '营业汇总',
    shortTitle: '营业',
    path: ROUTE_PATHS.statisticsBusinessSummary,
    category: 'operation',
    eyebrow: '经营概览',
    subtitle: '用清晰的指标卡、经营提醒与跟进清单，帮助负责人快速掌握当日经营状态。',
    chips: ['核心汇总', '移动端卡片', '适合晨会查看'],
    metrics: [
      { label: '营业额', value: '¥56,820', note: '较昨日 +8.4%', tone: 'primary' },
      { label: '入住率', value: '84%', note: '房态承压，请关注晚到客人', tone: 'success' },
      { label: '平均房价', value: '¥468', note: '会员单占比提升', tone: 'secondary' },
      { label: '异常提醒', value: '3 项', note: '未排房 / 待收款 / 待审查', tone: 'warning' },
    ],
    sections: [
      {
        title: '今日观察',
        description: '将重点信息整理为一屏可读的经营摘要。',
        bullets: ['预抵较昨日增加，晚间房态需要提前锁房', '高房价订单集中在双床与家庭房', '待离店房间建议联动保洁排班'],
      },
      {
        title: '跟进建议',
        description: '优先展示可执行动作，方便快速安排处理。',
        bullets: ['先处理未排房订单，再核对待收款记录', '审查入口保留在统计域，方便负责人连续处理', '重点事项可直接转为当班跟进安排'],
      },
    ],
    boundaryNotes: ['当前页聚焦关键信息，方便手机快速查看', '默认保留核心经营提醒，减少阅读负担'],
  },
  channelSummary: {
    key: 'channel-summary',
    title: '渠道汇总',
    shortTitle: '渠道',
    path: ROUTE_PATHS.statisticsChannelSummary,
    category: 'operation',
    eyebrow: '渠道表现追踪',
    subtitle: '按移动端节奏展示渠道贡献、转化提醒与高频核对项。',
    chips: ['OTA 概览', '转化提醒', '移动端摘要'],
    metrics: [
      { label: '主力渠道', value: 'Booking', note: '订单占比 31%', tone: 'primary' },
      { label: '直营占比', value: '22%', note: '需继续提升复购转化', tone: 'success' },
      { label: '待核对映射', value: '2 个', note: '优先检查价计划同步', tone: 'warning' },
      { label: '净收益', value: '¥42,600', note: '扣佣后较上周稳定', tone: 'secondary' },
    ],
    sections: [
      {
        title: '渠道结构',
        description: '突出负责人最关心的渠道占比与异常渠道。',
        bullets: ['直营单量稳定，但客单价仍低于 OTA', 'Agoda 夜间订单增长明显', '携程取消率偏高，建议跟进自动消息'],
      },
      {
        title: '移动端展示策略',
        description: '以结论清晰的卡片呈现，方便快速浏览。',
        bullets: ['先看高贡献渠道，再看异常提醒', '需要深入时可继续进入渠道页查看详情', '卡片间距与文字层级按单手浏览优化'],
      },
    ],
    boundaryNotes: ['本页强调经营结论，避免信息过度堆叠', '后续可继续补充更多指标与列表内容'],
  },
  notesSummary: {
    key: 'notes-summary',
    title: '记一笔汇总',
    shortTitle: '记一笔',
    path: ROUTE_PATHS.statisticsNotesSummary,
    category: 'finance',
    eyebrow: '零散经营记录',
    subtitle: '聚合移动端常见的临时记账、手工备注与待补录事项。',
    chips: ['手工记账', '待补录', '现金流补位'],
    metrics: [
      { label: '今日记账', value: '14 条', note: '前台与值班经理均有录入', tone: 'primary' },
      { label: '待补票据', value: '3 条', note: '集中在夜班现金收入', tone: 'warning' },
      { label: '手工收入', value: '¥2,360', note: '已计入当日流水', tone: 'success' },
      { label: '异常备注', value: '1 条', note: '需要负责人复核', tone: 'secondary' },
    ],
    sections: [
      {
        title: '适配重点',
        description: '将桌面端分散区域整合为移动端连续阅读。',
        bullets: ['先看金额与待补录数量，再看责任人和时间', '重点事项可继续安排跟进', '当前先保留查看与提醒能力'],
      },
      {
        title: '负责人提醒',
        description: '将需要二次跟进的条目收敛为简洁清单。',
        bullets: ['夜班现金收入应在交班前补齐照片', '异常备注建议与订单、审查动作关联查看', '低频历史明细留待后续增强'],
      },
    ],
    boundaryNotes: ['当前聚焦查看与提醒，方便移动场景快速核对', '先保证“看得见、找得到、可继续跟进”'],
  },
  revenueSummary: {
    key: 'revenue-summary',
    title: '流水汇总',
    shortTitle: '流水',
    path: ROUTE_PATHS.statisticsRevenueSummary,
    category: 'finance',
    eyebrow: '流水核对',
    subtitle: '围绕日常收款、退款与挂账，提供适合手机查看的流水摘要。',
    chips: ['收款视图', '退款核对', '挂账提醒'],
    metrics: [
      { label: '总收款', value: '¥61,280', note: '含线上与线下支付', tone: 'primary' },
      { label: '退款', value: '¥1,280', note: '2 笔需复核原因', tone: 'warning' },
      { label: '挂账', value: '¥6,400', note: '协议客为主', tone: 'secondary' },
      { label: '到账率', value: '96%', note: '较昨日提升 1.5%', tone: 'success' },
    ],
    sections: [
      {
        title: '重点核对项',
        description: '移动端优先保留负责人口径最常用的数字。',
        bullets: ['先看未到账金额，再看退款与挂账', '协议单位订单建议联动订单详情复核', '夜审前需再次确认线下收款凭证'],
      },
      {
        title: '展示边界',
        description: '复杂流水明细不在首版内全部展开。',
        bullets: ['默认卡片摘要，避免一屏堆叠过多表头', '需要完整明细时保留后续扩展空间', '当前页面以负责人巡检为主要场景'],
      },
    ],
    boundaryNotes: ['当前聚焦收款、退款与挂账的核心信息', '已预留后续查看更多明细的空间'],
  },
  operationReport: {
    key: 'operation-report',
    metricsDescription: null,
    showHero: false,
    showSections: false,
    showBoundaryNotes: false,
    title: '经营报表',
    shortTitle: '经营',
    path: ROUTE_PATHS.statisticsOperationReport,
    category: 'operation',
    eyebrow: '经营复盘',
    subtitle: '通过移动端摘要展示出租率、客源结构与经营提醒，适合店长日内巡检。',
    chips: ['经营复盘', '店长巡检', '结构化阅读'],
    metrics: [
      { label: '出租率', value: '81%', note: '周末档期较强', tone: 'success' },
      { label: '平均入住天数', value: '1.8 晚', note: '短住单为主', tone: 'secondary' },
      { label: '会员占比', value: '27%', note: '仍有提升空间', tone: 'primary' },
      { label: '经营提醒', value: '4 项', note: '关注异常取消与晚到', tone: 'warning' },
    ],
    sections: [
      {
        title: '阅读方式',
        description: '用结论卡替代桌面端长图表。',
        bullets: ['先看出租率，再看渠道与会员结构', '异常取消单独列为提醒，不淹没在长表中', '重要指标维持双列卡片，便于快速扫读'],
      },
      {
        title: '跟进重点',
        description: '围绕经营复盘保留清晰的查看顺序。',
        bullets: ['可先处理关键提醒，再继续查看趋势变化', '结论说明便于晨会或巡店时快速复盘', '页面结构清晰，方便后续补充更多内容'],
      },
    ],
    boundaryNotes: ['当前强调手机查看时的可读性与可达性', '保留清晰统一的报表命名，方便日常使用'],
  },
  accommodationReport: {
    key: 'accommodation-report',
    title: '住宿报表',
    shortTitle: '住宿',
    path: ROUTE_PATHS.statisticsAccommodationReport,
    category: 'operation',
    eyebrow: '住宿结构洞察',
    subtitle: '聚焦入住客群、房型分布与住宿结构，让移动端查看更集中。',
    chips: ['客群结构', '房型分布', '入住分析'],
    metrics: [
      { label: '客房夜数', value: '186', note: '较上周 +11%', tone: 'primary' },
      { label: '家庭出行', value: '24%', note: '亲子房需求提升', tone: 'success' },
      { label: '长住订单', value: '9 单', note: '建议重点维护', tone: 'secondary' },
      { label: '补证提醒', value: '2 单', note: '优先进入审查处理', tone: 'warning' },
    ],
    sections: [
      {
        title: '住宿画像',
        description: '重点描述房型与客群的变化。',
        bullets: ['双床房入住最满，晚间需留意超售风险', '家庭房预定上升，可提前准备儿童用品', '长住客建议同步前台与保洁排班'],
      },
      {
        title: '审查联动',
        description: '住宿结构页保留与审查链路的语义连接。',
        bullets: ['证件补齐需求从本页可快速感知', '待审查客单建议在统计首页统一进入', '移动端不重复堆叠桌面端明细表'],
      },
    ],
    boundaryNotes: ['当前先展示关键洞察，不展开全部住宿维度', '页面保持集中阅读，方便在手机上快速查看'],
  },
  financeReport: {
    key: 'finance-report',
    title: '财务报表',
    shortTitle: '财务',
    path: ROUTE_PATHS.statisticsFinanceReport,
    category: 'finance',
    eyebrow: '财务对账入口',
    subtitle: '保留收入、成本、挂账与提醒的核心脉络，适合移动端先看结论。',
    chips: ['收入成本', '挂账提醒', '负责人巡检'],
    metrics: [
      { label: '净收入', value: '¥48,960', note: '已扣退款与手续费', tone: 'primary' },
      { label: '成本支出', value: '¥9,880', note: '布草与渠道佣金为主', tone: 'secondary' },
      { label: '待核销', value: '¥3,200', note: '两笔协议客挂账', tone: 'warning' },
      { label: '现金健康', value: '良好', note: '本周未出现异常波动', tone: 'success' },
    ],
    sections: [
      {
        title: '财务重点',
        description: '在手机上优先看需要决策的事项。',
        bullets: ['先看待核销，再看成本异动', '协议客挂账建议结合订单详情复核', '退款原因单独保留说明，避免信息混杂'],
      },
      {
        title: '查看重点',
        description: '控制信息密度，方便在手机上快速掌握重点。',
        bullets: ['保留结论卡与提醒清单', '导出、明细穿透和复杂筛选后续补齐', '现阶段主要解决入口与可读性'],
      },
    ],
    boundaryNotes: ['财务页聚焦收入、成本与挂账等重点信息', '当前优先满足负责人移动巡检场景'],
  },
}
