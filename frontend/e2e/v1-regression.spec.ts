import { expect, test } from '@playwright/test'

test('@v1 Sandbox 保留预诊、追问、反馈和人工继续提交行为', async ({ page }) => {
  await page.goto('/sandbox')
  await expect(
    page.getByText('本页面仅用于本地开发、联调和演示，不是正式 SLA 提交入口。'),
  ).toBeVisible()

  await page.getByRole('button', { name: '智能预诊' }).click()
  const result = page.getByRole('heading', { name: '预诊结果' }).locator('..')
  await expect(result).toContainText('置信度')
  await expect(result).toContainText('依据来源')
  await expect(result).toContainText('建议下一步')
  await expect(result).toContainText('允许操作：补充信息、继续人工提交')

  await page.getByRole('button', { name: '补充日志现象' }).click()
  await expect(page.getByText('人工补充')).toBeVisible()
  await page.getByRole('button', { name: '部分采纳' }).click()
  await expect(page.getByText(/反馈已记录|部分采纳/)).toBeVisible()

  await page.getByRole('button', { name: '继续提交 SLA' }).click()
  await expect(page.getByText('已进入人工确认步骤（模拟），未调用真实提交接口。')).toBeVisible()
  await page.getByRole('button', { name: '忽略建议并继续 AIOps 原有提交' }).click()
  await expect(
    page.getByText('模拟数据：已交还 Mock AIOps 人工确认，未调用真实提交接口。'),
  ).toBeVisible()
})

test('@v1 预诊失败仍不阻断人工继续提交', async ({ page }) => {
  await page.goto('/sandbox')
  await page.getByLabel('模拟预诊后端失败').check()
  await page.getByRole('button', { name: '智能预诊' }).click()
  await expect(page.getByText(/预诊后端不可用；仍可继续 AIOps 原有人工提交/)).toBeVisible()
  await expect(page.getByRole('button', { name: '继续提交 SLA' })).toBeEnabled()
  await expect(page.getByRole('button', { name: '忽略建议并继续 AIOps 原有提交' })).toBeEnabled()
})
