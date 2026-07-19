<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ pageTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button
            class="app-page-header__icon-btn"
            fill="clear"
            aria-label="刷新数据"
            @click="handleRefreshTap"
          >
            <img src="/statistics-refresh.png" alt="" aria-hidden="true" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page data-center-page">
      <ion-refresher slot="fixed" @ionRefresh="handlePullRefresh">
        <ion-refresher-content pulling-text="下拉刷新数据中心" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="mobile-stack">
        <section class="mobile-card data-center-page__toolbar-card">
          <div class="data-center-page__toolbar-copy">
            <p class="data-center-page__toolbar-store">{{ storeName }}</p>
            <div class="data-center-page__toolbar-meta">
              <span>{{ activePrimaryLabel }}</span>
              <span>{{ dateRangeLabel }}</span>
            </div>
          </div>

          <ion-segment
            :value="activePrimarySection"
            class="data-center-page__primary-segment"
            @ionChange="handlePrimarySectionChange"
          >
            <ion-segment-button value="overview">
              <ion-label>总览</ion-label>
            </ion-segment-button>
            <ion-segment-button value="accommodation">
              <ion-label>住宿</ion-label>
            </ion-segment-button>
            <ion-segment-button value="notes">
              <ion-label>记一笔</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div class="data-center-page__filter-shell">
            <div class="data-center-page__preset-list">
              <button
                v-for="preset in DATE_PRESETS"
                :key="preset.value"
                type="button"
                class="data-center-page__preset-button"
                :class="{ 'is-active': activeDatePreset === preset.value }"
                @click="handlePresetClick(preset.value)"
              >
                {{ preset.label }}
              </button>
            </div>

            <div class="data-center-page__date-grid" role="group" aria-label="日期范围">
              <input
                v-model="startDate"
                aria-label="开始日期"
                type="date"
                class="data-center-page__date-input"
                @change="handleManualDateChange"
              />
              <span class="data-center-page__date-separator">至</span>
              <input
                v-model="endDate"
                aria-label="结束日期"
                type="date"
                class="data-center-page__date-input"
                @change="handleManualDateChange"
              />
            </div>
          </div>
        </section>

        <section v-if="loadNotice" class="mobile-card data-center-page__notice-card">
          <h2 class="mobile-section-title">同步提示</h2>
          <p class="mobile-note">{{ loadNotice }}</p>
        </section>

        <section v-if="activePrimarySection === 'overview'" class="data-center-page__overview-tabs">
          <div class="data-center-page__tab-grid" role="tablist" aria-label="总览分类">
            <button
              v-for="tab in OVERVIEW_TABS"
              :key="tab.value"
              type="button"
              role="tab"
              class="data-center-page__tab-button"
              :aria-selected="activeOverviewTab === tab.value ? 'true' : 'false'"
              :class="{ 'is-active': activeOverviewTab === tab.value }"
              @click="handleOverviewTabSelect(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
        </section>

        <section v-if="pageError" class="mobile-card data-center-page__error-card">
          <h2 class="mobile-section-title">加载失败</h2>
          <p class="mobile-note">{{ pageError }}</p>
          <ion-button expand="block" fill="outline" @click="reloadCurrentSection">重新加载</ion-button>
        </section>

        <section v-else-if="loading" class="mobile-card data-center-page__loading-card">
          <ion-spinner name="crescent" />
          <p class="mobile-note">正在加载{{ activePrimaryLabel }}数据…</p>
        </section>

        <template v-else>
          <template v-if="activePrimarySection === 'overview'">
            <section v-if="activeOverviewTab === 'business'" class="mobile-card data-center-page__overview-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">营业概况</h2>
                  <p class="mobile-note">营业额与住宿消费结构，按日期范围实时刷新。</p>
                </div>
              </div>

              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">住宿总营业额</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(businessOverview.totalRevenue) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">房费</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(businessOverview.roomFee) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">押金</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(businessOverview.deposit) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">退房金</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(businessOverview.checkoutFee) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">餐食/客房消费</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(businessOverview.roomServiceFee) }}</strong>
                </article>
              </div>
            </section>

            <section v-if="activeOverviewTab === 'business'" class="mobile-card data-center-page__overview-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">消费分类分布</h2>
                </div>
              </div>

              <div
                v-if="businessOverview.categoryDistribution.length > 0"
                class="data-center-page__summary-list data-center-page__summary-list--distribution"
              >
                <article
                  v-for="item in businessOverview.categoryDistribution"
                  :key="item.category"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.category }}</strong>
                    <p class="mobile-note">占比 {{ formatPercent(item.percentage) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.value) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无消费分类分布数据。</p>
            </section>

            <section v-if="activeOverviewTab === 'business'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title data-center-page__chart-title">
                住宿消费趋势
                <span>{{ chartDateRangeLabel }}</span>
              </h2>
              <div
                v-if="businessTrendChartItems.length > 0"
                class="data-center-page__trend-chart data-center-page__trend-chart--daily"
              >
                <article
                  v-for="item in businessTrendChartItems"
                  :key="item.date"
                  class="data-center-page__trend-column data-center-page__trend-column--daily"
                >
                  <div class="data-center-page__trend-value data-center-page__trend-value--compact">
                    {{ formatCompactCurrency(item.value) }}
                  </div>
                  <div class="data-center-page__trend-track">
                    <span
                      v-if="item.value > 0"
                      class="data-center-page__trend-bar"
                      :style="{ height: `${item.height}%` }"
                    />
                  </div>
                  <span class="data-center-page__trend-label">{{ item.label }}</span>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无趋势数据。</p>
            </section>

            <section v-if="activeOverviewTab === 'business'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">住宿消费明细</h2>
              <div
                v-if="businessOverview.consumptionDetails.length > 0"
                class="data-center-page__summary-list data-center-page__summary-list--table"
              >
                <article
                  v-for="item in businessOverview.consumptionDetails"
                  :key="item.category"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.category }}</strong>
                  </div>
                  <strong>{{ formatCurrency(item.total) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无消费明细。</p>
            </section>

            <section v-if="activeOverviewTab === 'revenue'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">流水汇总</h2>
              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">总流水</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(revenueSummary.totalRevenue) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">分账款</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(revenueSummary.splitAccount) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">实收款</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(revenueSummary.actualReceived) }}</strong>
                </article>
              </div>
            </section>

            <section v-if="activeOverviewTab === 'revenue'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">支付方式分布</h2>
              <div
                v-if="revenueSummary.paymentMethodStats.length > 0"
                class="data-center-page__summary-list data-center-page__summary-list--revenue-table"
              >
                <article
                  v-for="item in revenueSummary.paymentMethodStats"
                  :key="item.paymentMethod"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.paymentMethod }}</strong>
                    <p class="mobile-note">占比 {{ formatPercent(item.percentage) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.amount) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无支付方式统计。</p>
            </section>

            <section v-if="activeOverviewTab === 'revenue'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">款项分类统计</h2>
              <div
                v-if="revenueSummary.categoryStats.length > 0"
                class="data-center-page__summary-list data-center-page__summary-list--revenue-table"
              >
                <article
                  v-for="item in revenueSummary.categoryStats"
                  :key="item.category"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.category }}</strong>
                    <p class="mobile-note">占比 {{ formatPercent(item.percentage) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.amount) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无款项分类统计。</p>
            </section>

            <section v-if="activeOverviewTab === 'revenue'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">按日流水摘要</h2>
              <div
                v-if="revenueTrendItems.length > 0"
                class="data-center-page__summary-list data-center-page__summary-list--revenue-table"
              >
                <article
                  v-for="item in revenueTrendItems"
                  :key="item.date"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ formatShortDate(item.date) }}</strong>
                    <p class="mobile-note">分账 {{ formatCurrency(item.splitAccount) }} · 实收 {{ formatCurrency(item.actualReceived) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.totalRevenue) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无按日流水数据。</p>
            </section>

            <section v-if="activeOverviewTab === 'channel'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">渠道汇总</h2>
              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">渠道总收入</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(channelDisplaySummary.totalRevenue) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">总间夜</span>
                  <strong class="data-center-page__metric-value">{{ formatCount(channelDisplaySummary.totalRoomNights) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">平均间夜产值</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(channelAverageRevenuePerNight) }}</strong>
                </article>
              </div>
            </section>

            <section v-if="activeOverviewTab === 'channel'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">渠道消费分布</h2>
              <div v-if="channelDisplaySummary.revenueDistribution.length > 0" class="data-center-page__summary-list">
                <article
                  v-for="item in channelDisplaySummary.revenueDistribution"
                  :key="item.channelName"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.channelName }}</strong>
                    <p class="mobile-note">占比 {{ formatPercent(item.percentage) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.value) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无渠道消费分布。</p>
            </section>

            <section v-if="activeOverviewTab === 'channel'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">渠道间夜分布</h2>
              <div v-if="channelDisplaySummary.nightsDistribution.length > 0" class="data-center-page__summary-list">
                <article
                  v-for="item in channelDisplaySummary.nightsDistribution"
                  :key="item.channelName"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.channelName }}</strong>
                    <p class="mobile-note">占比 {{ formatPercent(item.percentage) }}</p>
                  </div>
                  <strong>{{ formatCount(item.value) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无渠道间夜分布。</p>
            </section>

            <section v-if="activeOverviewTab === 'channel'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title">渠道明细</h2>
              <div v-if="channelDisplaySummary.channelDetails.length > 0" class="data-center-page__summary-list">
                <article
                  v-for="item in channelDisplaySummary.channelDetails"
                  :key="item.channelName"
                  class="data-center-page__summary-item data-center-page__summary-item--stack"
                >
                  <div>
                    <strong>{{ item.channelName }}</strong>
                    <p class="mobile-note">{{ resolveChannelDetailMeta(item) }}</p>
                  </div>
                  <strong>{{ formatCurrency(item.revenue) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无渠道明细。</p>
            </section>

            <section v-if="activeOverviewTab === 'sales'" class="mobile-card data-center-page__overview-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">销售汇总</h2>
                </div>
              </div>

              <ion-searchbar
                ref="salesSearchbarRef"
                v-model="salesKeywordInput"
                :debounce="0"
                class="data-center-page__sales-searchbar"
                placeholder="搜索订单号、渠道号、客户名、手机号"
              />

              <div class="data-center-page__inline-actions">
                <ion-button size="small" @click="handleSalesSearch">搜索</ion-button>
                <ion-button size="small" fill="outline" @click="handleSalesSearchReset">清空</ion-button>
              </div>

              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">总销售额</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(salesSummary.totalSales) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">订单数</span>
                  <strong class="data-center-page__metric-value">{{ formatCount(salesSummary.totalOrders) }}</strong>
                </article>
              </div>
            </section>

            <section v-if="activeOverviewTab === 'sales'" class="mobile-card data-center-page__overview-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">销售订单明细</h2>
                </div>
              </div>
              <div v-if="salesSummary.orderDetails.length > 0" class="data-center-page__card-list">
                <article v-for="item in salesSummary.orderDetails" :key="item.id" class="data-center-page__detail-card">
                  <div class="data-center-page__detail-header">
                    <strong>{{ item.guestName || item.customerName || '未命名住客' }}</strong>
                    <span class="data-center-page__amount">{{ formatCurrency(item.amount) }}</span>
                  </div>
                  <p class="mobile-note">订单号 {{ item.orderNumber }}</p>
                  <p class="mobile-note">渠道号 {{ item.channelNumber || '-' }}</p>
                  <p class="mobile-note">{{ item.channelName || '未知渠道' }} · {{ formatDateTime(item.createdAt) }}</p>
                  <p class="mobile-note">手机号 {{ item.phone || '-' }}</p>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无销售订单。</p>
            </section>

            <section v-if="activeOverviewTab === 'sales'" class="mobile-card data-center-page__overview-card">
              <h2 class="mobile-section-title data-center-page__chart-title">
                每日销售额
                <span>{{ chartDateRangeLabel }}</span>
              </h2>
              <div
                v-if="salesTrendChartItems.length > 0"
                class="data-center-page__trend-chart data-center-page__trend-chart--daily"
              >
                <article
                  v-for="item in salesTrendChartItems"
                  :key="item.date"
                  class="data-center-page__trend-column data-center-page__trend-column--daily"
                >
                  <div class="data-center-page__trend-value data-center-page__trend-value--compact">
                    {{ formatCompactCurrency(item.value) }}
                  </div>
                  <div class="data-center-page__trend-track">
                    <span
                      v-if="item.value > 0"
                      class="data-center-page__trend-bar"
                      :style="{ height: `${item.height}%` }"
                    />
                  </div>
                  <span class="data-center-page__trend-label">{{ item.label }}</span>
                  <span class="data-center-page__trend-meta">{{ formatCount(item.orderCount) }}单</span>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无销售趋势数据。</p>
            </section>
          </template>

          <template v-if="activePrimarySection === 'accommodation'">
            <section class="mobile-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">经营指标</h2>
                </div>
              </div>

              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">总房费</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(operationalMetrics.totalRoomFee) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">平均房价 ADR</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(operationalMetrics.averageDailyRate) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">入住率</span>
                  <strong class="data-center-page__metric-value">{{ formatPercent(operationalMetrics.occupancyRate) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">RevPAR</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(operationalMetrics.revPAR) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">累计售出间夜</span>
                  <strong class="data-center-page__metric-value">{{ formatCount(operationalMetrics.totalSoldRoomNights) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">累计可售间夜</span>
                  <strong class="data-center-page__metric-value">{{ formatCount(operationalMetrics.totalAvailableRoomNights) }}</strong>
                </article>
              </div>
            </section>

            <section class="mobile-card data-center-page__statistics-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">经营指标趋势</h2>
                </div>
              </div>

              <ion-segment
                :value="activeAccommodationTrendTab"
                class="data-center-page__dark-segment data-center-page__accommodation-segment"
                @ionChange="handleAccommodationTrendTabChange"
              >
                <ion-segment-button value="roomFee">
                  <ion-label>总房费</ion-label>
                </ion-segment-button>
                <ion-segment-button value="adr">
                  <ion-label>ADR</ion-label>
                </ion-segment-button>
                <ion-segment-button value="roomNights">
                  <ion-label>间夜数</ion-label>
                </ion-segment-button>
                <ion-segment-button value="revpar">
                  <ion-label>RevPAR</ion-label>
                </ion-segment-button>
              </ion-segment>

              <div
                v-if="accommodationTrendItems.length > 0"
                class="data-center-page__accommodation-trend-list"
              >
                <article
                  v-for="item in accommodationTrendItems"
                  :key="item.date"
                  class="data-center-page__accommodation-trend-card"
                >
                  <strong class="data-center-page__accommodation-trend-date">
                    {{ formatMonthDaySlash(item.date) }}
                  </strong>
                  <p class="mobile-note data-center-page__accommodation-trend-meta">
                    {{ resolveAccommodationTrendMeta(item) }}
                  </p>
                  <strong class="data-center-page__accommodation-trend-value">
                    {{ resolveAccommodationTrendValue(item) }}
                  </strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无经营指标趋势。</p>
            </section>

            <section class="mobile-card data-center-page__statistics-card">
              <div class="mobile-inline-row data-center-page__detail-toolbar">
                <div class="data-center-page__detail-toolbar-copy">
                  <h2 class="mobile-section-title">经营指标明细</h2>
                </div>
                <ion-button
                  size="small"
                  fill="solid"
                  class="data-center-page__export-button"
                  @click="handleExportAccommodationDetails"
                >
                  导出明细
                </ion-button>
              </div>

              <ion-segment
                :value="activeAccommodationDetailTab"
                class="data-center-page__dark-segment data-center-page__accommodation-segment"
                @ionChange="handleAccommodationDetailTabChange"
              >
                <ion-segment-button value="roomFee">
                  <ion-label>房费明细</ion-label>
                </ion-segment-button>
                <ion-segment-button value="roomNights">
                  <ion-label>间夜明细</ion-label>
                </ion-segment-button>
                <ion-segment-button value="occupancy">
                  <ion-label>入住率明细</ion-label>
                </ion-segment-button>
                <ion-segment-button value="revpar">
                  <ion-label>RevPAR明细</ion-label>
                </ion-segment-button>
              </ion-segment>

              <div
                v-if="accommodationDetailItems.length > 0"
                class="data-center-page__accommodation-detail-list"
              >
                <article
                  v-for="item in accommodationDetailItems"
                  :key="`${activeAccommodationDetailTab}-${item.roomType}-${item.roomNumber}`"
                  class="data-center-page__accommodation-detail-card"
                >
                  <strong class="data-center-page__accommodation-detail-title">{{ item.roomType }}</strong>
                  <p class="mobile-note data-center-page__accommodation-detail-meta">
                    {{ resolveAccommodationDetailMeta(item) }}
                  </p>
                  <strong class="data-center-page__accommodation-detail-value">
                    {{ resolveAccommodationDetailValue(item) }}
                  </strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无经营指标明细。</p>
            </section>
          </template>

          <template v-if="activePrimarySection === 'notes'">
            <section class="mobile-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">记一笔统计</h2>
                  <p class="mobile-note">这里集中查看收支统计与明细，方便快速核对每日记录。</p>
                </div>
              </div>

              <div class="data-center-page__metric-grid">
                <article class="data-center-page__metric-card is-primary">
                  <span class="data-center-page__metric-label">净收入</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(notesStatistics.netIncome) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">总收入</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(notesStatistics.totalIncome) }}</strong>
                </article>
                <article class="data-center-page__metric-card">
                  <span class="data-center-page__metric-label">总支出</span>
                  <strong class="data-center-page__metric-value">{{ formatCurrency(notesStatistics.totalExpense) }}</strong>
                </article>
              </div>
            </section>

            <section class="data-center-page__notes-mode-shell">
              <ion-segment
                :value="notesStatsMode"
                class="data-center-page__notes-mode-segment"
                @ionChange="handleNotesStatsModeChange"
              >
                <ion-segment-button value="project">
                  <ion-label>按项目</ion-label>
                </ion-segment-button>
                <ion-segment-button value="payment">
                  <ion-label>按支付方式</ion-label>
                </ion-segment-button>
              </ion-segment>
            </section>

            <section class="mobile-card">
              <h2 class="mobile-section-title">收入统计</h2>
              <div v-if="activeNotesIncomeStats.length > 0" class="data-center-page__summary-list">
                <article
                  v-for="item in activeNotesIncomeStats"
                  :key="`income-${item.name}`"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.name }}</strong>
                    <p class="mobile-note">收入构成</p>
                  </div>
                  <strong>{{ formatCurrency(item.value) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无收入统计。</p>
            </section>

            <section class="mobile-card">
              <h2 class="mobile-section-title">支出统计</h2>
              <div v-if="activeNotesExpenseStats.length > 0" class="data-center-page__summary-list">
                <article
                  v-for="item in activeNotesExpenseStats"
                  :key="`expense-${item.name}`"
                  class="data-center-page__summary-item"
                >
                  <div>
                    <strong>{{ item.name }}</strong>
                    <p class="mobile-note">支出构成</p>
                  </div>
                  <strong>{{ formatCurrency(item.value) }}</strong>
                </article>
              </div>
              <p v-else class="mobile-note">当前日期范围暂无支出统计。</p>
            </section>

            <section class="mobile-card data-center-page__notes-filter-card">
              <div class="mobile-inline-row">
                <div>
                  <h2 class="mobile-section-title">记一笔明细</h2>
                  <p class="mobile-note">支持按类型筛选，可先查看明细与凭证数量。</p>
                </div>
              </div>

              <ion-segment
                :value="notesTypeFilter"
                class="data-center-page__dark-segment data-center-page__notes-type-segment"
                @ionChange="handleNotesTypeFilterChange"
              >
                <ion-segment-button value="all">
                  <ion-label>全部</ion-label>
                </ion-segment-button>
                <ion-segment-button value="income">
                  <ion-label>收入</ion-label>
                </ion-segment-button>
                <ion-segment-button value="expense">
                  <ion-label>支出</ion-label>
                </ion-segment-button>
              </ion-segment>
            </section>

            <section class="mobile-card data-center-page__notes-detail-card">
              <div v-if="notesList.length > 0" class="data-center-page__notes-detail-list">
                <article v-for="item in notesList" :key="item.id" class="data-center-page__notes-entry">
                  <div class="data-center-page__detail-header">
                    <div class="data-center-page__tag-group">
                      <span class="data-center-page__type-tag" :class="`is-${item.type}`">
                        {{ resolveNoteTypeLabel(item.type) }}
                      </span>
                      <strong>{{ resolveNoteCategoryLabel(item.category) }}</strong>
                    </div>
                    <span class="data-center-page__amount" :class="`is-${item.type}`">
                      {{ formatSignedCurrency(item.amount, item.type) }}
                    </span>
                  </div>
                  <p class="mobile-note">{{ formatDateTime(item.datetime) }}</p>
                  <p class="mobile-note">支付方式：{{ resolveNotePaymentMethodLabel(item.paymentMethod) }}</p>
                  <p class="mobile-note">关联房间：{{ item.roomNumber || '-' }}</p>
                  <p class="mobile-note">备注：{{ item.notes || '无' }}</p>
                  <div class="data-center-page__inline-actions">
                    <span class="mobile-note">凭证 {{ formatCount(item.voucherCount) }}</span>
                    <ion-button v-if="item.voucherCount > 0" size="small" fill="clear" @click="handleViewVoucher(item)">
                      查看凭证
                    </ion-button>
                  </div>
                </article>
              </div>
              <p v-else class="mobile-note data-center-page__notes-empty">当前日期范围暂无记一笔明细。</p>
              <div class="data-center-page__subtle-actions">
                <ion-button size="small" fill="outline" @click="handleExportNotes">导出报表</ion-button>
                <ion-button size="small" fill="outline" @click="handleVoucherComingSoon">凭证说明</ion-button>
              </div>
            </section>
          </template>
        </template>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  getBusinessOverview,
  getChannelSummary,
  getOperationalMetrics,
  getRevenueSummary,
  getSalesSummary,
  type BusinessOverviewDTO,
  type ChannelDetail,
  type ChannelSummaryDTO,
  type DailyConsumption,
  type DailyMetricsDTO,
  type OperationalMetricsDTO,
  type RevenueSummaryDTO,
  type RoomDetailDTO,
  type SalesSummaryDTO,
} from '@/api/statistics'
import {
  getNotesList,
  getNotesStatistics,
  type NoteDTO,
  type NotesStatisticsDTO,
  type NoteType,
} from '@/api/notes'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import {
  compareBusinessDates,
  formatBusinessDateLabel,
  formatStoreDateTime,
  getStoreDatePresetRange,
} from '@/utils/storeBusinessDate'

type DataCenterSection = 'overview' | 'accommodation' | 'notes'
type OverviewTab = 'business' | 'revenue' | 'channel' | 'sales'
type NotesStatsMode = 'project' | 'payment'
type DatePreset = 'today' | 'yesterday' | 'week' | 'month'
type AccommodationTrendTab = 'roomFee' | 'adr' | 'roomNights' | 'revpar'
type AccommodationDetailTab = 'roomFee' | 'roomNights' | 'occupancy' | 'revpar'

interface ChannelDistributionItem {
  channelName: string
  value: number
  percentage: number
}

interface ChannelDisplaySummary {
  totalRevenue: number
  totalRoomNights: number
  revenueDistribution: ChannelDistributionItem[]
  nightsDistribution: ChannelDistributionItem[]
  channelDetails: ChannelDetail[]
}

interface DatePresetOption {
  label: string
  value: DatePreset
}

interface OverviewTabOption {
  label: string
  value: OverviewTab
}

type SearchbarElement = HTMLElement & {
  getInputElement?: () => Promise<HTMLInputElement>
}

const DATE_PRESETS: DatePresetOption[] = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
]

const OVERVIEW_TABS: OverviewTabOption[] = [
  { label: '营业概况', value: 'business' },
  { label: '流水汇总', value: 'revenue' },
  { label: '渠道汇总', value: 'channel' },
  { label: '销售汇总', value: 'sales' },
]

const NOTE_CATEGORY_LABEL_MAP: Record<string, string> = {
  catering: '餐饮美食',
  tobacco_alcohol: '烟酒饮料',
  compensation: '物品赔付',
  ticket: '景点门票',
  souvenir: '特色纪念品',
  other: '其他杂项',
  utilities: '水电燃气',
  rent_property: '房租物业费',
  salary: '支付工资',
  maintenance: '房间维修',
  communication_transport: '通讯交通',
  daily_misc: '日常杂项',
}

const NOTE_PAYMENT_METHOD_LABEL_MAP: Record<string, string> = {
  wechat: '微信',
  alipay: '支付宝',
  cash: '现金',
}

const route = useRoute()
const storeStore = useStoreStore()

const activeDatePreset = ref<DatePreset | ''>('today')
const startDate = ref('')
const endDate = ref('')
const activePrimarySection = ref<DataCenterSection>('overview')
const activeOverviewTab = ref<OverviewTab>('business')
const salesSearchbarRef = ref<SearchbarElement | null>(null)
const salesKeywordInput = ref('')
const salesKeyword = ref('')
const notesStatsMode = ref<NotesStatsMode>('project')
const notesTypeFilter = ref<'all' | NoteType>('all')
const activeAccommodationTrendTab = ref<AccommodationTrendTab>('roomFee')
const activeAccommodationDetailTab = ref<AccommodationDetailTab>('roomFee')
const loading = ref(false)
const pageError = ref('')
const loadNotice = ref('')

const businessOverview = ref<BusinessOverviewDTO>(createEmptyBusinessOverview())
const revenueSummary = ref<RevenueSummaryDTO>(createEmptyRevenueSummary())
const channelSummary = ref<ChannelSummaryDTO>(createEmptyChannelSummary())
const salesSummary = ref<SalesSummaryDTO>(createEmptySalesSummary())
const operationalMetrics = ref<OperationalMetricsDTO>(createEmptyOperationalMetrics())
const notesStatistics = ref<NotesStatisticsDTO>(createEmptyNotesStatistics())
const notesList = ref<NoteDTO[]>([])

const storeName = computed(() => {
  return storeStore.currentStore?.name || '未选择门店'
})

const activePrimaryLabel = computed(() => {
  if (activePrimarySection.value === 'accommodation') {
    return '住宿'
  }

  if (activePrimarySection.value === 'notes') {
    return '记一笔'
  }

  return '总览'
})

const pageTitle = computed(() => {
  if (activePrimarySection.value === 'accommodation') {
    return '住宿'
  }

  if (activePrimarySection.value === 'notes') {
    return '记一笔'
  }

  return '数据中心'
})

const dateRangeLabel = computed(() => {
  if (!startDate.value || !endDate.value) {
    return '未选择日期'
  }

  if (startDate.value === endDate.value) {
    return formatDisplayDate(startDate.value)
  }

  return `${formatDisplayDate(startDate.value)}-${formatDisplayDate(endDate.value)}`
})

const chartDateRangeLabel = computed(() => {
  if (!startDate.value || !endDate.value) {
    return ''
  }

  const startLabel = formatMonthDaySlash(startDate.value)
  if (startDate.value === endDate.value) {
    return `（${startLabel}）`
  }

  return `（${startLabel}-${formatMonthDaySlash(endDate.value)}）`
})

const businessTrendChartItems = computed(() => {
  const items = getSortedItems(businessOverview.value.consumptionTrend)
    .slice(-7)
    .map((item) => ({
      ...item,
      label: formatMonthDaySlash(item.date),
      value: resolveBusinessTrendTotal(item),
    }))
  const maxValue = Math.max(...items.map((item) => item.value), 0)

  return items.map((item) => ({
    ...item,
    height:
      maxValue > 0
        ? Math.max((item.value / maxValue) * 100, item.value > 0 ? 8 : 0)
        : 0,
  }))
})

const revenueTrendItems = computed(() => {
  return getRecentItems(revenueSummary.value.dailyRevenues)
})

const salesTrendChartItems = computed(() => {
  const items = getSortedItems(salesSummary.value.dailySalesTrend).slice(-7)
  const maxValue = Math.max(...items.map((item) => item.sales), 0)

  return items.map((item) => ({
    date: item.date,
    label: formatMonthDaySlash(item.date),
    value: item.sales,
    orderCount: item.orderCount,
    height: maxValue > 0 ? Math.max((item.sales / maxValue) * 100, item.sales > 0 ? 8 : 0) : 0,
  }))
})

const accommodationTrendItems = computed(() => {
  return getSortedItems(operationalMetrics.value.dailyTrends).reverse()
})

const accommodationDetailItems = computed(() => {
  if (activeAccommodationDetailTab.value === 'roomNights') {
    return operationalMetrics.value.roomNightsDetails
  }

  if (activeAccommodationDetailTab.value === 'occupancy') {
    return operationalMetrics.value.occupancyDetails
  }

  if (activeAccommodationDetailTab.value === 'revpar') {
    return operationalMetrics.value.revparDetails
  }

  return operationalMetrics.value.roomFeeDetails
})

const channelDisplaySummary = computed<ChannelDisplaySummary>(() => {
  return buildChannelDisplaySummary(channelSummary.value.channelDetails)
})

const channelAverageRevenuePerNight = computed(() => {
  if (!channelDisplaySummary.value.totalRoomNights) {
    return 0
  }

  return channelDisplaySummary.value.totalRevenue / channelDisplaySummary.value.totalRoomNights
})

const activeNotesIncomeStats = computed(() => {
  if (notesStatsMode.value === 'payment') {
    return notesStatistics.value.incomeByPayment
  }

  return notesStatistics.value.incomeByProject
})

const activeNotesExpenseStats = computed(() => {
  if (notesStatsMode.value === 'payment') {
    return notesStatistics.value.expenseByPayment
  }

  return notesStatistics.value.expenseByProject
})

applyDatePreset('today', false)

watch(
  () => route.name,
  async (nextRouteName) => {
    activePrimarySection.value = resolvePrimarySection(nextRouteName)
    await loadCurrentSection()
  },
  { immediate: true },
)

watch(
  () => storeStore.currentStore?.id,
  async (nextStoreId, previousStoreId) => {
    if (!nextStoreId || nextStoreId === previousStoreId) {
      return
    }

    await loadCurrentSection()
  },
)

watch(
  [activePrimarySection, activeOverviewTab, salesSearchbarRef],
  async ([nextPrimarySection, nextOverviewTab, searchbarEl]) => {
    if (nextPrimarySection !== 'overview' || nextOverviewTab !== 'sales' || !searchbarEl) {
      return
    }

    await nextTick()
    await tuneSalesSearchbarInput(searchbarEl)
  },
  { flush: 'post' },
)

function createEmptyBusinessOverview(): BusinessOverviewDTO {
  return {
    totalRevenue: 0,
    roomFee: 0,
    deposit: 0,
    checkoutFee: 0,
    roomServiceFee: 0,
    categoryDistribution: [],
    consumptionTrend: [],
    consumptionDetails: [],
  }
}

function createEmptyRevenueSummary(): RevenueSummaryDTO {
  return {
    totalRevenue: 0,
    splitAccount: 0,
    actualReceived: 0,
    paymentMethodStats: [],
    categoryStats: [],
    incomeDistribution: [],
    dailyRevenues: [],
  }
}

function createEmptyChannelSummary(): ChannelSummaryDTO {
  return {
    totalRevenue: 0,
    totalRoomNights: 0,
    revenueDistribution: [],
    nightsDistribution: [],
    revenueTrend: [],
    nightsTrend: [],
    channelDetails: [],
  }
}

function createEmptySalesSummary(): SalesSummaryDTO {
  return {
    totalSales: 0,
    totalOrders: 0,
    dailySalesTrend: [],
    orderDetails: [],
  }
}

function createEmptyOperationalMetrics(): OperationalMetricsDTO {
  return {
    totalRoomFee: 0,
    averageDailyRate: 0,
    occupancyRate: 0,
    revPAR: 0,
    totalSoldRoomNights: 0,
    totalAvailableRoomNights: 0,
    totalRooms: 0,
    days: 0,
    dailyTrends: [],
    roomFeeDetails: [],
    roomNightsDetails: [],
    occupancyDetails: [],
    revparDetails: [],
  }
}

function createEmptyNotesStatistics(): NotesStatisticsDTO {
  return {
    netIncome: 0,
    totalIncome: 0,
    totalExpense: 0,
    incomeByProject: [],
    expenseByProject: [],
    incomeByPayment: [],
    expenseByPayment: [],
  }
}

function formatShortDate(value: string) {
  return formatBusinessDateLabel(value, 'month-day')
}

function formatMonthDaySlash(value: string) {
  return formatBusinessDateLabel(value, 'month-day').replace('-', '/')
}

function formatDisplayDate(value: string) {
  if (!value) {
    return '-'
  }

  return value.replace(/-/g, '/')
}

async function tuneSalesSearchbarInput(target: SearchbarElement | null) {
  if (!target?.getInputElement) {
    return
  }

  const input = await target.getInputElement()
  input.style.fontSize = '12px'
  input.style.paddingInlineStart = '1.5rem'
  input.style.paddingInlineEnd = '0.5rem'
  input.style.letterSpacing = '0'
}

function formatDateTime(value: string) {
  return formatStoreDateTime(value, 'month-day-time')
}

function formatCurrency(value: number) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatCompactCurrency(value: number) {
  const amount = Number(value || 0)
  const absoluteAmount = Math.abs(amount)

  if (absoluteAmount >= 100000000) {
    return `¥${(amount / 100000000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })}亿`
  }

  if (absoluteAmount >= 10000) {
    return `¥${(amount / 10000).toLocaleString('zh-CN', { maximumFractionDigits: 1 })}万`
  }

  return `¥${amount.toLocaleString('zh-CN', { maximumFractionDigits: 0 })}`
}

function formatSignedCurrency(value: number, type: NoteType) {
  if (type === 'expense') {
    return `-${formatCurrency(value)}`
  }

  return `+${formatCurrency(value)}`
}

function formatCount(value: number) {
  return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatPercent(value: number) {
  return `${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`
}

function getSortedItems<T extends { date: string }>(items: T[]) {
  const nextItems = [...items]
  nextItems.sort((firstItem, secondItem) => {
    return compareBusinessDates(firstItem.date, secondItem.date)
  })
  return nextItems
}

function getRecentItems<T extends { date: string }>(items: T[]) {
  const nextItems = getSortedItems(items)
  return nextItems.slice(-7).reverse()
}

function buildChannelDistributionItems(channelDetails: ChannelDetail[], valueType: 'revenue' | 'roomNights') {
  const totalValue = channelDetails.reduce((sum, item) => {
    return sum + item[valueType]
  }, 0)

  return channelDetails.map((item) => {
    let percentage = 0
    if (totalValue > 0) {
      percentage = (item[valueType] / totalValue) * 100
    }

    return {
      channelName: item.channelName,
      value: item[valueType],
      percentage,
    }
  })
}

function buildChannelDisplaySummary(channelDetails: ChannelDetail[]): ChannelDisplaySummary {
  const totalRevenue = channelDetails.reduce((sum, item) => {
    return sum + item.revenue
  }, 0)
  const totalRoomNights = channelDetails.reduce((sum, item) => {
    return sum + item.roomNights
  }, 0)

  return {
    totalRevenue,
    totalRoomNights,
    revenueDistribution: buildChannelDistributionItems(channelDetails, 'revenue'),
    nightsDistribution: buildChannelDistributionItems(channelDetails, 'roomNights'),
    channelDetails,
  }
}

function resolveBusinessTrendTotal(item: DailyConsumption) {
  return item.roomFee + item.deposit + item.checkoutFee + item.roomServiceFee
}

function resolveChannelDetailMeta(item: ChannelDetail) {
  const metaParts = [
    `间夜 ${formatCount(item.roomNights)}`,
    `均价 ${formatCurrency(item.averagePrice)}`,
  ]

  if (item.orderCount !== null) {
    metaParts.push(`订单 ${formatCount(item.orderCount)}`)
  }

  return metaParts.join(' · ')
}

function resolveAccommodationTrendValue(item: DailyMetricsDTO) {
  if (activeAccommodationTrendTab.value === 'adr') {
    return formatCurrency(item.averageDailyRate)
  }

  if (activeAccommodationTrendTab.value === 'roomNights') {
    return formatCount(item.roomNights)
  }

  if (activeAccommodationTrendTab.value === 'revpar') {
    return formatCurrency(item.revPAR)
  }

  return formatCurrency(item.totalRoomFee)
}

function resolveAccommodationTrendMeta(item: DailyMetricsDTO) {
  if (activeAccommodationTrendTab.value === 'adr') {
    return `房费 ${formatCurrency(item.totalRoomFee)} · 间夜 ${formatCount(item.roomNights)}`
  }

  if (activeAccommodationTrendTab.value === 'roomNights') {
    return `入住率 ${formatPercent(item.occupancyRate)} · RevPAR ${formatCurrency(item.revPAR)}`
  }

  if (activeAccommodationTrendTab.value === 'revpar') {
    return `ADR ${formatCurrency(item.averageDailyRate)} · 入住率 ${formatPercent(item.occupancyRate)}`
  }

  return `ADR ${formatCurrency(item.averageDailyRate)} · 间夜 ${formatCount(item.roomNights)}`
}

function resolveAccommodationDetailMeta(item: RoomDetailDTO) {
  if (activeAccommodationDetailTab.value === 'roomFee') {
    return `房间 ${item.roomNumber} · 当日 ${formatCurrency(item.currentDate)}`
  }

  if (activeAccommodationDetailTab.value === 'roomNights') {
    return `房间 ${item.roomNumber} · 当日 ${formatCount(item.currentDate)}`
  }

  if (activeAccommodationDetailTab.value === 'occupancy') {
    return `当前日期 ${formatPercent(item.currentDate)}`
  }

  return `当前日期 ${formatCurrency(item.currentDate)}`
}

function resolveAccommodationDetailValue(item: RoomDetailDTO) {
  if (activeAccommodationDetailTab.value === 'roomFee') {
    return formatCurrency(item.total)
  }

  if (activeAccommodationDetailTab.value === 'roomNights') {
    return formatCount(item.total)
  }

  if (activeAccommodationDetailTab.value === 'occupancy') {
    return formatPercent(item.total)
  }

  return formatCurrency(item.total)
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function resolveNoteTypeLabel(type: NoteType) {
  if (type === 'expense') {
    return '支出'
  }

  return '收入'
}

function resolveNoteCategoryLabel(category?: string | null) {
  if (!category) {
    return '-'
  }

  return NOTE_CATEGORY_LABEL_MAP[category] || category
}

function resolveNotePaymentMethodLabel(paymentMethod?: string | null) {
  if (!paymentMethod) {
    return '-'
  }

  return NOTE_PAYMENT_METHOD_LABEL_MAP[paymentMethod] || paymentMethod
}

function validateDateRange() {
  if (!startDate.value || !endDate.value) {
    showWarningToast('请选择完整的开始日期和结束日期')
    return false
  }

  if (startDate.value > endDate.value) {
    showWarningToast('开始日期不能晚于结束日期')
    return false
  }

  return true
}

function applyDatePreset(preset: DatePreset, shouldLoad = true) {
  const presetRange = getStoreDatePresetRange(preset)
  startDate.value = presetRange.startDate
  endDate.value = presetRange.endDate
  activeDatePreset.value = preset

  if (shouldLoad) {
    void loadCurrentSection()
  }
}

function resolvePrimarySection(routeName: unknown): DataCenterSection {
  if (routeName === 'StatisticsAccommodation') {
    return 'accommodation'
  }

  if (routeName === 'StatisticsNotes') {
    return 'notes'
  }

  return 'overview'
}

async function loadCurrentSection() {
  if (!validateDateRange()) {
    return
  }

  pageError.value = ''
  loadNotice.value = ''
  loading.value = true

  try {
    if (activePrimarySection.value === 'accommodation') {
      await loadAccommodationPage()
    } else if (activePrimarySection.value === 'notes') {
      await loadNotesPage()
    } else {
      await loadOverviewPage()
    }
  } catch (error) {
    const message = resolveWarningMessage(error, '数据中心加载失败')
    pageError.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
  }
}

async function loadOverviewPage() {
  if (activeOverviewTab.value === 'business') {
    const response = await getBusinessOverview({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (!response.success || !response.data) {
      throw new Error(response.message || '加载营业概况失败')
    }

    businessOverview.value = response.data
    return
  }

  if (activeOverviewTab.value === 'revenue') {
    const response = await getRevenueSummary({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (!response.success || !response.data) {
      throw new Error(response.message || '加载流水汇总失败')
    }

    revenueSummary.value = response.data
    return
  }

  if (activeOverviewTab.value === 'channel') {
    const response = await getChannelSummary({
      startDate: startDate.value,
      endDate: endDate.value,
    })

    if (!response.success || !response.data) {
      throw new Error(response.message || '加载渠道汇总失败')
    }

    channelSummary.value = response.data
    return
  }

  const response = await getSalesSummary({
    startDate: startDate.value,
    endDate: endDate.value,
    keyword: salesKeyword.value || undefined,
  })

  if (!response.success || !response.data) {
    throw new Error(response.message || '加载销售汇总失败')
  }

  salesSummary.value = response.data
}

async function loadAccommodationPage() {
  const response = await getOperationalMetrics({
    startDate: startDate.value,
    endDate: endDate.value,
  })

  if (!response.success || !response.data) {
    throw new Error(response.message || '加载住宿经营指标失败')
  }

  operationalMetrics.value = response.data
}

function buildNotesListParams() {
  const listParams: { startDate: string; endDate: string; type?: NoteType } = {
    startDate: startDate.value,
    endDate: endDate.value,
  }

  if (notesTypeFilter.value !== 'all') {
    listParams.type = notesTypeFilter.value
  }

  return listParams
}

async function loadNotesListForCurrentFilter() {
  const response = await getNotesList(buildNotesListParams())

  if (!response.success || !response.data) {
    throw new Error(response.message || '记一笔明细加载失败')
  }

  notesList.value = response.data
}

async function loadNotesPage() {
  const [statisticsResult, listResult] = await Promise.allSettled([
    getNotesStatistics({
      startDate: startDate.value,
      endDate: endDate.value,
    }),
    getNotesList(buildNotesListParams()),
  ])

  const warnings: string[] = []
  let statisticsLoaded = false
  let listLoaded = false

  if (statisticsResult.status === 'fulfilled') {
    const response = statisticsResult.value
    if (response.success && response.data) {
      notesStatistics.value = response.data
      statisticsLoaded = true
    } else {
      warnings.push(response.message || '记一笔统计加载失败')
    }
  } else {
    warnings.push(resolveWarningMessage(statisticsResult.reason, '记一笔统计加载失败'))
  }

  if (listResult.status === 'fulfilled') {
    const response = listResult.value
    if (response.success && response.data) {
      notesList.value = response.data
      listLoaded = true
    } else {
      warnings.push(response.message || '记一笔明细加载失败')
    }
  } else {
    warnings.push(resolveWarningMessage(listResult.reason, '记一笔明细加载失败'))
  }

  if (!statisticsLoaded && !listLoaded) {
    throw new Error(warnings.join('；') || '记一笔数据加载失败')
  }

  if (warnings.length > 0) {
    loadNotice.value = warnings.join('；')
  }
}

async function reloadCurrentSection() {
  await loadCurrentSection()
}

async function handlePrimarySectionChange(event: CustomEvent) {
  const nextValue = event.detail.value as DataCenterSection
  if (!nextValue || nextValue === activePrimarySection.value) {
    return
  }

  activePrimarySection.value = nextValue
  await loadCurrentSection()
}

async function handleOverviewTabSelect(nextValue: OverviewTab) {
  if (!nextValue || nextValue === activeOverviewTab.value) {
    return
  }

  activeOverviewTab.value = nextValue
  await loadCurrentSection()
}

async function handleNotesStatsModeChange(event: CustomEvent) {
  const nextValue = event.detail.value as NotesStatsMode
  if (!nextValue) {
    return
  }

  notesStatsMode.value = nextValue
}

async function handleNotesTypeFilterChange(event: CustomEvent) {
  const nextValue = event.detail.value as 'all' | NoteType
  if (!nextValue || nextValue === notesTypeFilter.value) {
    return
  }

  notesTypeFilter.value = nextValue
  loadNotice.value = ''

  try {
    await loadNotesListForCurrentFilter()
  } catch (error) {
    const message = resolveWarningMessage(error, '记一笔明细加载失败')
    loadNotice.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  }
}

function handleAccommodationTrendTabChange(event: CustomEvent) {
  const nextValue = event.detail.value as AccommodationTrendTab
  if (!nextValue) {
    return
  }

  activeAccommodationTrendTab.value = nextValue
}

function handleAccommodationDetailTabChange(event: CustomEvent) {
  const nextValue = event.detail.value as AccommodationDetailTab
  if (!nextValue) {
    return
  }

  activeAccommodationDetailTab.value = nextValue
}

function handlePresetClick(preset: DatePreset) {
  applyDatePreset(preset)
}

async function handleManualDateChange() {
  activeDatePreset.value = ''
  await loadCurrentSection()
}

async function handleSalesSearch() {
  salesKeyword.value = salesKeywordInput.value.trim()
  await loadCurrentSection()
}

async function handleSalesSearchReset() {
  salesKeywordInput.value = ''
  salesKeyword.value = ''
  await loadCurrentSection()
}

function handleExportNotes() {
  showWarningToast('导出功能即将上线，请先查看当前统计与明细。')
}

function handleExportAccommodationDetails() {
  showWarningToast('导出请在电脑端完成，手机端可先查看当前明细。')
}

function handleVoucherComingSoon() {
  showWarningToast('凭证查看功能即将上线，当前可先查看凭证数量与明细信息。')
}

function handleViewVoucher(_item: NoteDTO) {
  showWarningToast('凭证查看功能即将上线，当前可先查看凭证数量与明细信息。')
}

async function handleRefreshTap() {
  await loadCurrentSection()
}

async function handlePullRefresh(event: CustomEvent) {
  try {
    await loadCurrentSection()
  } finally {
    event.detail.complete()
  }
}
</script>

<style scoped>
.data-center-page {
  display: block;
  --background: #eef6ff;
  --padding-top: 12px;
  --padding-bottom: calc(36px + var(--app-safe-bottom));
  --padding-start: 14px;
  --padding-end: 14px;
}

.app-page-header__toolbar {
  --background: rgba(255, 255, 255, 0.97);
  --border-width: 0;
  --min-height: 56px;
}

.app-page-header__icon-btn {
  width: 38px;
  height: 38px;
  margin-inline-end: 8px;
  --padding-start: 0;
  --padding-end: 0;
  --color: #333;
}

.app-page-header__icon-btn::part(native) {
  border: 1px solid #e5eaf1;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 4px 12px rgba(51, 65, 85, 0.08);
}

.app-page-header__icon-btn img {
  display: block;
  width: 19px;
  height: 19px;
  object-fit: contain;
}

.data-center-page .mobile-stack {
  gap: 14px;
}

.data-center-page__toolbar-card,
.data-center-page__loading-card,
.data-center-page__error-card,
.data-center-page__notice-card,
.data-center-page__coming-soon-card {
  display: grid;
  gap: 10px;
}

.data-center-page__toolbar-card {
  gap: 16px;
  padding: 20px;
  border: 0;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(73, 103, 139, 0.1);
}

.data-center-page__toolbar-copy {
  display: grid;
  gap: 10px;
}

.data-center-page__toolbar-store {
  margin: 0;
  color: #303033;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
}

.data-center-page__toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.data-center-page__toolbar-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid #e7ebf0;
  background: #fff;
  color: #768395;
  font-size: 12px;
  font-weight: 500;
}

.data-center-page__primary-segment {
  height: 38px;
  min-height: 38px;
  padding: 0;
  border: 1px solid #e7ebf0;
  border-radius: 50px;
  overflow: hidden;
  background: #fff;
}

.data-center-page__primary-segment ion-segment-button {
  --border-radius: 50px;
  --color: #6c727c;
  --color-checked: #fff;
  --indicator-color: #343436;
  --indicator-box-shadow: none;
  --padding-start: 6px;
  --padding-end: 6px;
  height: 100%;
  min-height: 100%;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.data-center-page__primary-segment ion-segment-button::part(native) {
  height: 100%;
  border-radius: 50px;
}

.data-center-page__primary-segment ion-segment-button::part(indicator) {
  padding: 0;
}

.data-center-page__primary-segment ion-segment-button::part(indicator-background) {
  border-radius: 50px;
  box-shadow: none;
}

.data-center-page__filter-shell {
  display: grid;
  gap: 14px;
  padding-top: 16px;
  border-top: 2px solid #edf0f4;
}

.data-center-page__loading-card {
  justify-items: center;
  padding-block: 28px;
}

.data-center-page__notice-card {
  border-style: dashed;
  background: #fffaf0;
}

.data-center-page__error-card {
  border-style: dashed;
  background: #fff5f5;
}

.data-center-page__preset-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.data-center-page__preset-button {
  min-width: 48px;
  min-height: 28px;
  padding: 4px 11px;
  border: 1px solid #e7ebf0;
  border-radius: 999px;
  background: #fff;
  color: #616b78;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
}

.data-center-page__preset-button.is-active {
  border-color: #168cf3;
  background: #168cf3;
  color: #fff;
  box-shadow: none;
}

.data-center-page__date-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 5px;
  min-height: 40px;
  padding: 2px 12px;
  border: 1px solid #e3e8ef;
  border-radius: 999px;
  background: #fff;
}

.data-center-page__date-input {
  appearance: none;
  -webkit-appearance: none;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  min-height: 34px;
  padding: 6px 2px;
  border: 0;
  border-radius: 999px;
  outline: 0;
  background: transparent;
  color: #34363a;
  font: inherit;
  font-size: 13px;
  font-weight: 400;
  text-align: center;
}

.data-center-page__date-input::-webkit-calendar-picker-indicator,
.data-center-page__date-input::-webkit-inner-spin-button {
  display: none;
  -webkit-appearance: none;
}

.data-center-page__date-input:focus {
  background: #fff;
  box-shadow: 0 0 0 2px rgba(22, 140, 243, 0.14);
}

.data-center-page__date-separator {
  color: #8b96a5;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.data-center-page__overview-tabs {
  height: 34px;
  min-height: 34px;
  padding: 0;
  border: 1px solid rgba(218, 226, 236, 0.86);
  border-radius: 50px;
  overflow: hidden;
  background: #fff;
  box-shadow: none;
}

.data-center-page__tab-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  height: 100%;
}

.data-center-page__tab-button {
  min-width: 0;
  min-height: 100%;
  padding: 5px 3px;
  border: 0;
  border-radius: 50px;
  background: transparent;
  color: #41464e;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  text-align: center;
}

.data-center-page__tab-button.is-active {
  background: #168cf3;
  color: #fff;
  box-shadow: none;
}

.data-center-page__overview-card {
  padding: 20px;
  border: 0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(73, 103, 139, 0.09);
}

.data-center-page__overview-card .mobile-section-title {
  margin: 0;
  color: #333438;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.35;
}

.data-center-page__overview-card .mobile-note {
  margin: 5px 0 0;
  color: #8b9098;
  font-size: 13px;
  line-height: 1.5;
}

.data-center-page__statistics-card,
.data-center-page__notes-filter-card,
.data-center-page__notes-detail-card {
  padding: var(--ios-pms-space-5);
  border: 0;
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-surface-strong);
  box-shadow: var(--ios-pms-shadow-card);
}

.data-center-page__statistics-card .mobile-section-title,
.data-center-page__notes-filter-card .mobile-section-title {
  margin: 0;
  color: #333438;
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.35;
}

.data-center-page__dark-segment,
.data-center-page__notes-mode-segment {
  height: 36px;
  min-height: 36px;
  padding: 0;
  border: 1px solid #e1e4e8;
  border-radius: var(--ios-pms-radius-pill);
  overflow: hidden;
  background: #fff;
}

.data-center-page__dark-segment ion-segment-button,
.data-center-page__notes-mode-segment ion-segment-button {
  --border-radius: var(--ios-pms-radius-pill);
  --indicator-box-shadow: none;
  --padding-start: var(--ios-pms-space-1);
  --padding-end: var(--ios-pms-space-1);
  height: 100%;
  min-width: 0;
  min-height: 100%;
  margin: 0;
  font-size: 13px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.data-center-page__dark-segment ion-segment-button {
  --color: #242529;
  --color-checked: #fff;
  --indicator-color: #343436;
}

.data-center-page__notes-mode-segment ion-segment-button {
  --color: #242529;
  --color-checked: #fff;
  --indicator-color: #168cf3;
}

.data-center-page__dark-segment ion-segment-button::part(native),
.data-center-page__notes-mode-segment ion-segment-button::part(native) {
  height: 100%;
  border-radius: var(--ios-pms-radius-pill);
}

.data-center-page__dark-segment ion-segment-button::part(indicator),
.data-center-page__notes-mode-segment ion-segment-button::part(indicator) {
  padding: 0;
}

.data-center-page__dark-segment ion-segment-button::part(indicator-background),
.data-center-page__notes-mode-segment ion-segment-button::part(indicator-background) {
  border-radius: var(--ios-pms-radius-pill);
  box-shadow: none;
}

.data-center-page__accommodation-segment,
.data-center-page__notes-type-segment {
  margin-top: var(--ios-pms-space-2);
}

.data-center-page__accommodation-segment ion-segment-button {
  font-size: 12px;
}

.data-center-page__accommodation-trend-list,
.data-center-page__accommodation-detail-list {
  display: grid;
  gap: var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-3);
}

.data-center-page__accommodation-trend-card,
.data-center-page__accommodation-detail-card {
  display: grid;
  min-width: 0;
  border: 1px solid #dedede;
  border-radius: var(--ios-pms-radius-input);
  background: #fff;
}

.data-center-page__accommodation-trend-card {
  gap: 3px;
  min-height: 94px;
  padding: 9px 12px;
}

.data-center-page__accommodation-trend-date {
  color: #333438;
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.3;
}

.data-center-page__accommodation-trend-meta {
  color: #777b82;
  line-height: 1.45;
}

.data-center-page__accommodation-trend-value {
  color: #333438;
  font-size: var(--ios-pms-font-metric-lg-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.25;
}

.data-center-page__accommodation-detail-card {
  gap: 4px;
  min-height: 102px;
  padding: 10px 12px;
}

.data-center-page__accommodation-detail-title {
  min-width: 0;
  overflow: hidden;
  color: #333438;
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-bold);
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-center-page__accommodation-detail-meta {
  color: #777b82;
  line-height: 1.45;
}

.data-center-page__accommodation-detail-value {
  color: #333438;
  font-size: var(--ios-pms-font-metric-lg-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.25;
}

.data-center-page__notes-mode-shell {
  min-height: 36px;
}

.data-center-page__notes-filter-card {
  display: grid;
  gap: var(--ios-pms-space-3);
}

.data-center-page__notes-filter-card .mobile-section-title {
  margin-bottom: var(--ios-pms-space-1);
}

.data-center-page__notes-type-segment {
  margin-top: 0;
}

.data-center-page__notes-detail-card {
  display: grid;
  gap: 0;
}

.data-center-page__notes-detail-list {
  display: grid;
  gap: 0;
  border: 1px solid #e0e2e5;
  border-radius: var(--ios-pms-radius-input);
  overflow: hidden;
  background: #fff;
}

.data-center-page__notes-entry {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
}

.data-center-page__notes-entry + .data-center-page__notes-entry {
  border-top: 1px solid #eceef1;
}

.data-center-page__notes-empty {
  color: #999da4;
  line-height: 1.5;
}

.data-center-page__notes-detail-card .data-center-page__subtle-actions {
  margin-top: var(--ios-pms-space-2);
  padding-top: var(--ios-pms-space-2);
  border-top-color: #dedede;
}

.data-center-page__notes-detail-card .data-center-page__subtle-actions ion-button {
  min-height: 30px;
  margin: 0;
  --border-color: #d7d7d7;
  --border-radius: 7px;
  --color: #44464b;
  --padding-start: 11px;
  --padding-end: 11px;
  font-size: 12px;
}

.data-center-page__segment-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 6px;
}

.data-center-page__segment-grid ion-segment-button {
  --padding-start: 10px;
  --padding-end: 10px;
  grid-row: auto;
  min-width: 0;
  min-height: 48px;
  margin: 0;
  white-space: normal;
  font-size: 13px;
  line-height: 1.3;
}

.data-center-page__segment-grid ion-segment-button::before {
  display: none;
}

.data-center-page__segment-grid ion-label {
  white-space: normal;
  overflow: visible;
  text-overflow: initial;
  line-height: 1.3;
}

.data-center-page__segment-grid--detail ion-segment-button {
  min-height: 52px;
  font-size: 12px;
}

.data-center-page__detail-toolbar {
  flex-wrap: wrap;
  align-items: flex-start;
}

.data-center-page__detail-toolbar-copy {
  flex: 1 1 220px;
  min-width: 0;
}

.data-center-page__export-button {
  flex: 0 0 auto;
  min-height: 32px;
  margin: 0;
  --background: #168cf3;
  --background-activated: #0f7edc;
  --border-radius: 7px;
  --box-shadow: none;
  --color: #fff;
  --padding-start: 12px;
  --padding-end: 12px;
  font-size: 12px;
  white-space: nowrap;
}

.data-center-page__export-button::part(native) {
  white-space: nowrap;
}

.data-center-page__sales-searchbar {
  --background: #f5f7fa;
  --border-radius: 12px;
  --box-shadow: none;
  --color: #333438;
  --icon-color: #8b96a5;
  --padding-start: 0;
  --padding-end: 0;
  --placeholder-font-weight: 400;
  padding: 12px 0 4px;
}

.data-center-page__metric-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
  margin-top: 16px;
  border: 1px solid #dedede;
  border-radius: 2px;
  overflow: hidden;
  background: #fff;
}

.data-center-page__metric-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px 14px;
  min-height: 42px;
  padding: 7px 12px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.data-center-page__metric-card + .data-center-page__metric-card {
  border-top: 1px solid #dedede;
}

.data-center-page__metric-card.is-primary {
  grid-template-columns: minmax(0, 1fr);
  gap: 3px;
  min-height: 72px;
  padding: 9px 12px;
  background: #fff;
}

.data-center-page__metric-card:not(.is-primary) .data-center-page__metric-label {
  color: #3d3e42;
  font-size: 14px;
  font-weight: 500;
}

.data-center-page__metric-card:not(.is-primary) .data-center-page__metric-value {
  font-size: 17px;
  font-weight: 500;
  text-align: right;
}

.data-center-page__metric-label {
  color: #797d83;
  font-size: 13px;
}

.data-center-page__metric-value {
  color: #333438;
  font-size: 22px;
  line-height: 1.3;
}

.data-center-page__metric-card.is-primary .data-center-page__metric-value {
  font-size: 27px;
  font-weight: 700;
}

.data-center-page__summary-list,
.data-center-page__card-list {
  display: grid;
  gap: 0;
  margin-top: 14px;
  border: 1px solid #e0e2e5;
  border-radius: 15px;
  overflow: hidden;
  background: #fff;
}

.data-center-page__summary-item,
.data-center-page__detail-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
  padding: 14px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.data-center-page__summary-item > div {
  min-width: 0;
}

.data-center-page__summary-item > strong {
  flex: 0 0 auto;
  color: #333438;
  font-size: 17px;
  font-weight: 600;
  text-align: right;
}

.data-center-page__summary-item > div > strong {
  color: #333438;
  font-size: 15px;
  font-weight: 600;
}

.data-center-page__summary-item + .data-center-page__summary-item,
.data-center-page__detail-card + .data-center-page__detail-card {
  border-top: 1px solid #eceef1;
}

.data-center-page__summary-list--distribution {
  gap: 10px;
  border: 0;
  border-radius: 0;
  overflow: visible;
  background: transparent;
}

.data-center-page__summary-list--distribution .data-center-page__summary-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 7px;
  min-height: 92px;
  border: 1px solid #dedede;
  border-radius: 14px;
  background: #fff;
}

.data-center-page__summary-list--distribution .data-center-page__summary-item + .data-center-page__summary-item {
  border-top: 1px solid #dedede;
}

.data-center-page__summary-list--distribution .data-center-page__summary-item > strong {
  font-size: 24px;
  font-weight: 700;
  text-align: left;
}

.data-center-page__summary-list--distribution .mobile-note {
  color: #575b62;
}

.data-center-page__summary-list--table {
  border-radius: 12px;
}

.data-center-page__summary-list--table .data-center-page__summary-item {
  min-height: 42px;
  padding: 8px 12px;
}

.data-center-page__summary-list--table .data-center-page__summary-item > strong,
.data-center-page__summary-list--table .data-center-page__summary-item > div > strong {
  font-weight: 400;
}

.data-center-page__summary-list--revenue-table .data-center-page__summary-item > strong,
.data-center-page__summary-list--revenue-table .data-center-page__summary-item > div > strong {
  font-weight: 400;
}

.data-center-page__summary-item--stack,
.data-center-page__detail-card {
  align-items: flex-start;
}

.data-center-page__detail-card {
  display: grid;
  gap: 8px;
}

.data-center-page__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.data-center-page__amount {
  color: #333438;
  font-size: 16px;
  font-weight: 600;
}

.data-center-page__amount.is-income {
  color: var(--ion-color-success);
}

.data-center-page__amount.is-expense {
  color: var(--ion-color-danger);
}

.data-center-page__inline-actions,
.data-center-page__tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.data-center-page__type-tag {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.data-center-page__type-tag.is-income {
  background: rgba(34, 197, 94, 0.14);
  color: var(--ion-color-success);
}

.data-center-page__type-tag.is-expense {
  background: rgba(239, 68, 68, 0.14);
  color: var(--ion-color-danger);
}

.data-center-page__subtle-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e6e9ed;
}

.data-center-page__chart-title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px;
}

.data-center-page__chart-title span {
  color: #42464d;
  font-size: 14px;
  font-weight: 500;
}

.data-center-page__trend-chart {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 4px;
  min-height: 264px;
  margin-top: 16px;
  padding: 10px 7px 8px;
  border: 1px dashed #d4dce7;
  border-bottom-style: solid;
  background: #fff;
}

.data-center-page__trend-chart::after {
  position: absolute;
  inset-inline: 0;
  top: 50%;
  z-index: 0;
  border-top: 1px dashed rgba(154, 169, 187, 0.42);
  content: '';
  pointer-events: none;
}

.data-center-page__trend-chart--daily {
  grid-template-columns: repeat(auto-fit, minmax(36px, 1fr));
  gap: 2px;
  padding-inline: 4px;
}

.data-center-page__trend-column {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-rows: 30px 178px minmax(38px, auto);
  align-items: end;
  min-width: 0;
  text-align: center;
}

.data-center-page__trend-column--daily {
  grid-template-rows: 30px 164px 24px 18px;
}

.data-center-page__trend-value {
  align-self: center;
  min-width: 0;
  overflow: hidden;
  color: #4c535d;
  font-size: 10px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-center-page__trend-value--compact {
  font-size: 9px;
}

.data-center-page__trend-track {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 36%;
  max-width: 30px;
  height: 100%;
  margin: 0 auto;
  background: rgba(224, 235, 249, 0.48);
}

.data-center-page__trend-track::before {
  position: absolute;
  inset: 0;
  z-index: 0;
  border-inline: 1px dashed rgba(179, 191, 206, 0.4);
  content: '';
  pointer-events: none;
}

.data-center-page__trend-bar {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  border-radius: 999px;
  background: linear-gradient(180deg, #acd7ff 0%, #76b7fa 38%, #4f8ce9 100%);
  box-shadow: 0 7px 14px rgba(65, 132, 224, 0.18);
}

.data-center-page__trend-chart--daily .data-center-page__trend-track {
  width: 52%;
  max-width: 24px;
}

.data-center-page__trend-label {
  align-self: start;
  padding-top: 9px;
  color: #40444b;
  font-size: 11px;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.data-center-page__trend-column--daily .data-center-page__trend-label {
  padding-top: 7px;
  font-size: 10px;
  white-space: nowrap;
}

.data-center-page__trend-meta {
  align-self: start;
  color: #8b9098;
  font-size: 9px;
  line-height: 1.2;
  white-space: nowrap;
}

@media (max-width: 360px) {
  .data-center-page {
    --padding-start: 10px;
    --padding-end: 10px;
  }

  .data-center-page__toolbar-card,
  .data-center-page__overview-card {
    padding: 17px;
  }

  .data-center-page__tab-button {
    font-size: 11px;
  }

  .data-center-page__metric-card:not(.is-primary),
  .data-center-page__summary-item,
  .data-center-page__detail-header {
    display: grid;
  }

  .data-center-page__summary-list--distribution .data-center-page__summary-item {
    display: grid;
  }

  .data-center-page__trend-chart {
    padding-inline: 3px;
  }

  .data-center-page__trend-chart--daily {
    grid-template-columns: repeat(auto-fit, minmax(32px, 1fr));
    padding-inline: 2px;
  }

  .data-center-page__trend-track {
    width: 38%;
  }

  .data-center-page__trend-label {
    font-size: 10px;
  }

  .data-center-page__subtle-actions {
    justify-content: flex-start;
  }
}
</style>
