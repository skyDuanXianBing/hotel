const en = {
  saasSubscription: {
    menu: {
      group: 'Subscription',
      myPlan: 'My Plan',
    },
    periods: {
      MONTH: 'mo',
      YEAR: 'yr',
    },
    subscriptionStatus: {
      ACTIVE: 'Active',
      EXPIRED: 'Expired',
      CANCELLED: 'Cancelled',
    },
    featureNames: {
      independent_website: 'Independent Website',
      ai_website_gen: 'AI Website Generation',
      room_count: 'Room Capacity',
    },
    myPlan: {
      title: 'My Plan',
      description: 'View your current subscription, entitlement usage, and available plans.',
      currentTitle: 'Current Plan',
      currentPackage: 'Current Plan',
      refresh: 'Refresh',
      validity: 'Valid until {end}',
      included: 'Included',
      unlimited: 'Unlimited',
      quotaUsage: '{used}/{total} used',
      quotaResetHint: '{remaining} remaining · resets at {date}',
      capacityLimit: 'Up to {limit}',
      capacityUsage: 'Current {used} / limit {limit}',
      capacityExceeded: 'Over the limit. Upgrade to add more.',
      pricePaid: 'Paid ${price}',
      manualGrantTag: 'granted by admin',
      noSubscription: 'No active subscription. Pick a plan below to get started.',
      lowerTier: 'Lower than current plan',
      lowerTierHint: 'To switch to a lower-tier plan, please contact the administrator.',
      defaultPackageNotice:
        'You are on the default protection plan. Purchase a paid plan to unlock the full service.',
    },
    packages: {
      title: 'Available Plans',
      buyAction: 'Subscribe',
      upgradeAction: 'Upgrade',
      subscribeConfirmTitle: 'Confirm Subscription',
      subscribeConfirm: 'Subscribe to {name} at ${price}/{period}?',
      upgradeConfirm:
        'The upgrade takes effect immediately. Your current subscription will be replaced with "{name}". The remaining time will not be credited toward the price difference; used quota is preserved.',
      repurchaseConfirm:
        'One more period will be appended after the current expiry at {end}; used quota is preserved.',
      subscribeConfirmButton: 'Confirm & Pay',
      subscribeCancel: 'Cancel',
      subscribeSuccess: 'Subscribed to {name}',
      subscribeFailed: 'Subscription failed',
    },
    quota: {
      aiWebsiteGenRemaining: 'AI generation: {count} left',
      aiWebsiteGenUnlimited: 'AI generation: unlimited',
    },
    upgradeDialog: {
      title: 'Upgrade Required',
      noSubscription: 'You don’t have an active plan yet. Pick a plan below to get started.',
      featureLocked: '「{feature}」is not included in your current plan. Upgrade to unlock it.',
      quotaExhausted: '「{feature}」quota exhausted ({used}/{limit}). Upgrade for more.',
      compareTitle: 'Compare plans',
      includesFeature: 'Included',
      excludesFeature: 'Not included',
      viewPlans: 'View Plans',
      later: 'Maybe Later',
    },
  },
}

const ja = {
  saasSubscription: {
    menu: {
      group: 'サブスクリプション',
      myPlan: 'マイプラン',
    },
    periods: {
      MONTH: '月',
      YEAR: '年',
    },
    subscriptionStatus: {
      ACTIVE: '有効',
      EXPIRED: '期限切れ',
      CANCELLED: '解約済み',
    },
    featureNames: {
      independent_website: '独立サイト',
      ai_website_gen: 'AI サイト生成',
      room_count: '客室数上限',
    },
    myPlan: {
      title: 'マイプラン',
      description: '現在の契約プラン、特典の利用状況、購入可能なプランを確認できます。',
      currentTitle: '現在のプラン',
      currentPackage: '現在のプラン',
      refresh: '更新',
      validity: '有効期限：{end}',
      included: '利用可能',
      unlimited: '無制限',
      quotaUsage: '{used}/{total} 使用済み',
      quotaResetHint: '残り {remaining} 回・{date} にリセット',
      capacityLimit: '上限 {limit}',
      capacityUsage: '現在 {used} / 上限 {limit}',
      capacityExceeded: '上限を超えています。アップグレードまで新規追加できません。',
      pricePaid: '支払実額 ${price}',
      manualGrantTag: '管理者による付与',
      noSubscription: '有効な契約がありません。下のプランからお選びください。',
      lowerTier: '現在のプランより下位',
      lowerTierHint: '下位プランへの変更は管理者までお問い合わせください。',
      defaultPackageNotice:
        '現在はデフォルト保証プランをご利用中です。有料プランの購入で全機能をご利用いただけます。',
    },
    packages: {
      title: '購入可能なプラン',
      buyAction: '購入する',
      upgradeAction: 'アップグレード',
      subscribeConfirmTitle: '購入の確認',
      subscribeConfirm: '{name} を ${price}/{period} で購入しますか？',
      upgradeConfirm:
        'アップグレードは即時有効になります。現在の契約は「{name}」に置き換わります。残り期間は差額換算されず、使用済みクォータは引き継がれます。',
      repurchaseConfirm:
        '現在の有効期限 {end} の後に 1 周期分が延長されます。使用済みクォータは引き継がれます。',
      subscribeConfirmButton: '確認して支払う',
      subscribeCancel: 'キャンセル',
      subscribeSuccess: '{name} を購入しました',
      subscribeFailed: '購入に失敗しました',
    },
    quota: {
      aiWebsiteGenRemaining: 'AI 生成 残り {count} 回',
      aiWebsiteGenUnlimited: 'AI 生成 無制限',
    },
    upgradeDialog: {
      title: 'アップグレードが必要です',
      noSubscription: 'まだプランをご契約いただいていません。下のプランからお選びください。',
      featureLocked:
        '「{feature}」は現在のプランに含まれていません。アップグレードで解放されます。',
      quotaExhausted:
        '「{feature}」の利用回数が上限に達しました（{used}/{limit}）。上位プランをご検討ください。',
      compareTitle: 'プラン比較',
      includesFeature: '含まれる',
      excludesFeature: '含まれない',
      viewPlans: 'プランを見る',
      later: '後で',
    },
  },
}

const zhCN = {
  saasSubscription: {
    menu: {
      group: '订阅套餐',
      myPlan: '我的套餐',
    },
    periods: {
      MONTH: '月',
      YEAR: '年',
    },
    subscriptionStatus: {
      ACTIVE: '生效中',
      EXPIRED: '已过期',
      CANCELLED: '已取消',
    },
    featureNames: {
      independent_website: '独立站模块',
      ai_website_gen: 'AI 建站生成次数',
      room_count: '可存在房间数量',
    },
    myPlan: {
      title: '我的套餐',
      description: '查看当前订阅、权益用量与在售套餐。',
      currentTitle: '当前套餐',
      currentPackage: '当前套餐',
      refresh: '刷新',
      validity: '有效期至 {end}',
      included: '已包含',
      unlimited: '不限',
      quotaUsage: '已用 {used}/{total}',
      quotaResetHint: '剩余 {remaining} 次 · {date} 重置',
      capacityLimit: '上限 {limit}',
      capacityUsage: '当前 {used} / 上限 {limit}',
      capacityExceeded: '已超上限，升级前无法新增',
      pricePaid: '实付 ${price}',
      manualGrantTag: '人工开通',
      noSubscription: '当前没有生效中的订阅，可在下方选择套餐开通。',
      lowerTier: '低于当前档位',
      lowerTierHint: '如需调整至更低档位，请联系管理员。',
      defaultPackageNotice: '您当前处于默认保障套餐，购买付费套餐可解锁完整服务。',
    },
    packages: {
      title: '在售套餐',
      buyAction: '立即购买',
      upgradeAction: '升级',
      subscribeConfirmTitle: '确认购买',
      subscribeConfirm: '确定以 ${price}/{period} 购买「{name}」吗？',
      upgradeConfirm:
        '升级后立即生效，当前订阅将替换为「{name}」，剩余时长不折算差价，配额已用量保留。',
      repurchaseConfirm: '将在当前到期时间 {end} 后续延一个周期，配额已用量保留。',
      subscribeConfirmButton: '确认支付',
      subscribeCancel: '取消',
      subscribeSuccess: '已成功订阅「{name}」',
      subscribeFailed: '订阅失败',
    },
    quota: {
      aiWebsiteGenRemaining: 'AI 建站剩余 {count} 次',
      aiWebsiteGenUnlimited: 'AI 建站不限次数',
    },
    upgradeDialog: {
      title: '需要升级套餐',
      noSubscription: '您还未开通套餐，选择一档套餐开始使用。',
      featureLocked: '当前套餐未包含「{feature}」，升级后即可使用。',
      quotaExhausted: '「{feature}」额度已用完（{used}/{limit}），升级套餐可获得更多额度。',
      compareTitle: '套餐对比',
      includesFeature: '包含该权益',
      excludesFeature: '不含该权益',
      viewPlans: '查看套餐',
      later: '稍后再说',
    },
  },
}

const zhTW = {
  saasSubscription: {
    menu: {
      group: '訂閱套餐',
      myPlan: '我的套餐',
    },
    periods: {
      MONTH: '月',
      YEAR: '年',
    },
    subscriptionStatus: {
      ACTIVE: '生效中',
      EXPIRED: '已過期',
      CANCELLED: '已取消',
    },
    featureNames: {
      independent_website: '獨立站模組',
      ai_website_gen: 'AI 建站生成次數',
      room_count: '可存在房間數量',
    },
    myPlan: {
      title: '我的套餐',
      description: '查看目前訂閱、權益用量與在售套餐。',
      currentTitle: '目前套餐',
      currentPackage: '目前套餐',
      refresh: '重新整理',
      validity: '有效期至 {end}',
      included: '已包含',
      unlimited: '不限',
      quotaUsage: '已用 {used}/{total}',
      quotaResetHint: '剩餘 {remaining} 次 · {date} 重置',
      capacityLimit: '上限 {limit}',
      capacityUsage: '目前 {used} / 上限 {limit}',
      capacityExceeded: '已超上限，升級前無法新增',
      pricePaid: '實付 ${price}',
      manualGrantTag: '人工開通',
      noSubscription: '目前沒有生效中的訂閱，可在下方選擇套餐開通。',
      lowerTier: '低於目前檔位',
      lowerTierHint: '如需調整至更低檔位，請聯絡管理員。',
      defaultPackageNotice: '您目前處於預設保障套餐，購買付費套餐可解鎖完整服務。',
    },
    packages: {
      title: '在售套餐',
      buyAction: '立即購買',
      upgradeAction: '升級',
      subscribeConfirmTitle: '確認購買',
      subscribeConfirm: '確定以 ${price}/{period} 購買「{name}」嗎？',
      upgradeConfirm:
        '升級後立即生效，目前訂閱將替換為「{name}」，剩餘時長不折算差價，配額已用量保留。',
      repurchaseConfirm: '將在目前到期時間 {end} 後續延一個週期，配額已用量保留。',
      subscribeConfirmButton: '確認支付',
      subscribeCancel: '取消',
      subscribeSuccess: '已成功訂閱「{name}」',
      subscribeFailed: '訂閱失敗',
    },
    quota: {
      aiWebsiteGenRemaining: 'AI 建站剩餘 {count} 次',
      aiWebsiteGenUnlimited: 'AI 建站不限次數',
    },
    upgradeDialog: {
      title: '需要升級套餐',
      noSubscription: '您還未開通套餐，選擇一檔套餐開始使用。',
      featureLocked: '目前套餐未包含「{feature}」，升級後即可使用。',
      quotaExhausted: '「{feature}」額度已用完（{used}/{limit}），升級套餐可獲得更多額度。',
      compareTitle: '套餐對比',
      includesFeature: '包含該權益',
      excludesFeature: '不含該權益',
      viewPlans: '查看套餐',
      later: '稍後再說',
    },
  },
}

export const saasSubscriptionMessages = { en, ja, 'zh-CN': zhCN, 'zh-TW': zhTW }
