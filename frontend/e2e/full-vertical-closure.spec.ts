import { expect, test } from '@playwright/test'
import { loginAs } from './helpers'

test('@full 完整四服务完成一期核心纵向闭环', async ({ page }) => {
  await page.goto('/knowledge')
  await loginAs(page, 'mock-knowledge-editor', '知识编辑人员（模拟数据）')

  await page.getByTestId('title').fill('Issue 29 完整模式知识（模拟数据）')
  await page.getByTestId('file').setInputFiles({
    name: 'issue-29-full-mode.md',
    mimeType: 'text/markdown',
    buffer: Buffer.from('# 模拟数据\n\nMOCK-1001 Compose 完整模式排查依据。'),
  })
  await page.getByRole('button', { name: '上传并解析' }).click()
  await expect(page.getByText('任务状态：SUCCEEDED')).toBeVisible({ timeout: 30_000 })

  const versionId = (await page.getByTestId('knowledge-version-id').textContent())?.trim()
  expect(versionId).toMatch(/^[0-9a-f-]{36}$/)

  await page
    .getByTestId('cleaned-text')
    .fill('# 模拟数据\n\nMOCK-1001 Compose 完整模式修订后的排查依据。')
  await page.getByTestId('revise').click()
  await expect(page.getByText('任务状态：SUCCEEDED')).toBeVisible({ timeout: 30_000 })
  await page.getByTestId('submit-review').click()
  await expect(page.getByText('治理状态：IN_REVIEW')).toBeVisible()

  await page.getByTestId('approve').click()
  await expect(page.locator('p.error')).toBeVisible()
  await expect(page.getByText('治理状态：IN_REVIEW')).toBeVisible()

  await loginAs(page, 'mock-knowledge-reviewer', '知识审核人员（模拟数据）')
  await page.getByTestId('approve').click()
  await expect(page.getByText('治理状态：APPROVED')).toBeVisible()
  await page.getByTestId('publish').click()
  await expect(page.getByText('治理状态：PUBLISHED')).toBeVisible({ timeout: 90_000 })
  await expect(page.getByText(/FTS SUCCEEDED \/ 向量 SUCCEEDED/)).toBeVisible()

  await loginAs(page, 'mock-precheck-tdh', 'TDH 预诊用户（模拟数据）')
  await page.goto('/precheck-v2')
  const createSession = page.getByRole('button', { name: '创建 Session' })
  await expect(createSession).toBeVisible()
  await createSession.click()

  let latestRun = page.locator('article.reference').first()
  await expect(latestRun).toContainText('检索模式：HYBRID')
  await expect(latestRun.getByTestId('evidence-link').first()).toBeVisible()
  await latestRun.getByTestId('evidence-link').first().click()
  await expect(page.getByTestId('evidence-detail')).toContainText('内容哈希：sha256:')

  await page.getByTestId('title').fill('模拟数据：第二轮补充 MOCK-1001')
  await page.getByTestId('supplement').click()
  latestRun = page.locator('article.reference').first()
  await expect(latestRun).toContainText('第 2 次')
  await expect(latestRun).toContainText('检索模式：HYBRID')

  await page.getByTestId('feedback-adoption').selectOption('PARTIALLY_ADOPTED')
  await page.getByTestId('feedback-submit').click()
  await expect(page.getByText(/反馈已独立保存/)).toBeVisible()
  await page.getByTestId('continue').click()
  await expect(page.getByTestId('session-status')).toHaveText('TERMINATED')

  await loginAs(page, 'mock-admin', '本地管理员（模拟数据）')
  await page.goto('/audit')
  await expect(page.getByTestId('audit-event').first()).toContainText('模拟数据')

  await page.goto('/evaluation')
  const evaluationCreated = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v2/evaluation-runs') && response.request().method() === 'POST',
  )
  await page.getByTestId('evaluation-start').click()
  const evaluation = (await (await evaluationCreated).json()) as { taskId: string }
  await expect
    .poll(async () => {
      const response = await page.request.get(`/api/v2/evaluation-runs/${evaluation.taskId}`)
      if (!response.ok()) return 'PENDING'
      return ((await response.json()) as { status: string }).status
    })
    .toBe('SUCCEEDED')
  await page.reload()
  await expect(page.getByTestId('evaluation-run').first()).toContainText('SUCCEEDED')
  await expect(page.getByTestId('metrics')).toBeVisible()
})
