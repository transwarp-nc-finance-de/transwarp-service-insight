import { expect, test } from '@playwright/test'
import { loginAs } from './helpers'

test('@runtime-fault Embedding 运行中故障不停止 backend 并留下 FTS_ONLY 历史 Run', async ({
  page,
  request,
}) => {
  const health = await request.get('/api/v1/health')
  expect(health.ok()).toBeTruthy()

  await page.goto('/precheck-v2')
  await loginAs(page, 'mock-precheck-tdh', 'TDH 预诊用户（模拟数据）')
  const createSession = page.getByRole('button', { name: '创建 Session' })
  await expect(createSession).toBeVisible()
  await createSession.click()

  const latestRun = page.locator('article.reference').first()
  await expect(latestRun).toContainText('检索模式：FTS_ONLY')
  await expect(latestRun).toContainText('置信度：LOW')
  await expect(latestRun).toContainText('人工介入：')
  await expect(latestRun).toContainText('待补充：')
  await expect(latestRun).toContainText('不是最终根因、最终方案或正式复盘结论')
  await expect(latestRun.getByTestId('evidence-link').first()).toBeVisible()
  await expect(page.getByTestId('session-status')).toHaveText('ACTIVE')
  await expect(page.getByTestId('continue')).toBeEnabled()
})

test('@recovery 服务恢复后不改写 FTS_ONLY 历史 Run，下一轮重新使用 HYBRID', async ({ page }) => {
  await page.goto('/precheck-v2')
  await loginAs(page, 'mock-precheck-tdh', 'TDH 预诊用户（模拟数据）')
  await page.reload()

  await expect(page.getByTestId('session-status')).toHaveText('ACTIVE')
  const degradedRuns = page.locator('article.reference').filter({ hasText: '检索模式：FTS_ONLY' })
  const degradedRunCount = await degradedRuns.count()
  expect(degradedRunCount).toBeGreaterThan(0)
  const degradedRunHistory = await degradedRuns.allTextContents()

  await page.getByTestId('title').fill('模拟数据：Embedding 恢复后的 MOCK-1001 第二轮')
  await page.getByTestId('supplement').click()

  await expect(degradedRuns).toHaveCount(degradedRunCount)
  expect(await degradedRuns.allTextContents()).toEqual(degradedRunHistory)
  await expect(page.locator('article.reference').first()).toContainText('检索模式：HYBRID')
})
