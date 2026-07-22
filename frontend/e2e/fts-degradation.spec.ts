import { expect, test } from '@playwright/test'
import { loginAs } from './helpers'

test('@fts 浏览器用户在 Embedding 缺失时仍可核验依据并继续人工提交', async ({ page, request }) => {
  await page.goto('/evaluation')
  await loginAs(page, 'mock-admin', '本地管理员（模拟数据）')
  await page.getByTestId('evaluation-start').click()
  await expect
    .poll(
      async () => {
        await page.getByTestId('evaluation-refresh').click()
        return page.getByTestId('evaluation-run').first().textContent()
      },
      { timeout: 90_000 },
    )
    .toContain('SUCCEEDED')
  await expect(page.getByTestId('evaluation-run').first()).toContainText(
    '小样本工程评估，不代表生产效果',
  )

  // Evaluation deliberately creates persistent traces. Reset only the LOCAL
  // simulated dataset so the following browser path must create its own Session.
  await page.goto('/admin-reset')
  await page.getByTestId('reset-reason').fill('模拟数据：Issue #29 FTS-only 浏览器验收')
  await page.getByTestId('reset-confirmation').fill('RESET LOCAL MOCK DATA')
  await page.getByTestId('reset-submit').click()
  await expect(page.getByTestId('reset-latest')).toContainText('PENDING')
  await expect
    .poll(
      async () => {
        await request.post('/api/v2/auth-sessions', {
          data: { userCode: 'mock-admin', mockData: true },
        })
        const response = await request.get('/api/v2/admin/resets?size=20')
        if (!response.ok()) return 'PENDING'
        const resets = (await response.json()) as { items: Array<{ status: string }> }
        return resets.items[0]?.status ?? 'PENDING'
      },
      { timeout: 90_000 },
    )
    .toBe('SUCCEEDED')

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
  await expect(page.getByRole('button', { name: '跳过建议并继续人工提交' })).toBeEnabled()

  const evidenceLink = latestRun.getByTestId('evidence-link').first()
  await expect(evidenceLink).toBeVisible()
  await evidenceLink.click()
  await expect(page.getByTestId('evidence-detail')).toContainText('模拟数据')
  await expect(page.getByTestId('evidence-detail')).toContainText('内容哈希：sha256:')

  const feedback = page.getByTestId('feedback-submit')
  await expect(feedback).toBeEnabled()
  await page.getByTestId('feedback-adoption').selectOption('PARTIALLY_ADOPTED')
  await feedback.click()
  await expect(page.getByText(/反馈已独立保存/)).toBeVisible()

  await page.getByTestId('continue').click()
  await expect(page.getByTestId('session-status')).toHaveText('TERMINATED')
  await expect(page.getByText(/未创建 SLA、工单草稿、ticketId 或回执/)).toBeVisible()

  await loginAs(page, 'mock-admin', '本地管理员（模拟数据）')
  await page.goto('/audit')
  await expect(page.getByTestId('audit-event').first()).toContainText('模拟数据')
})
