import { expect, type Page } from '@playwright/test'

export async function loginAs(page: Page, userCode: string, displayName: string) {
  const identity = page.getByLabel('本地模拟身份（模拟数据）')
  await identity.selectOption(userCode)
  await page.getByRole('button', { name: /登录|切换身份/ }).click()
  await expect(page.getByRole('region', { name: '本地模拟身份' }).locator('strong')).toBeVisible()
  await expect(page.getByRole('region', { name: '本地模拟身份' }).locator('strong')).toHaveText(
    displayName,
  )
}
