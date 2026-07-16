# Issue #39 Embedding Qualification Harness

Status: ACTIVE

Owner: Service Insight Maintainers

Last reviewed: 2026-07-16

Source of truth for: Issue #39 隔离模型取件、完整性校验、离线推理、资格评估与性能实测命令

本目录不是产品代码，不暴露 HTTP，不接入 Backend Port，不修改默认
`compose.yaml`，不实现 pgvector、IndexTask、知识发布或正式 Retrieval Adapter。
整个目录可以在 Issue #39 完成后整体删除。

所有输入均为 `模拟数据`。智能结论只提供依据、置信度、人工介入建议和待补充
信息，不是最终根因、最终方案或正式复盘结论，也不会阻断人工继续提交。

## 固定对象

- 模型：`intfloat/multilingual-e5-base`
- revision：`d13f1b27baf31030b7fd040960d60d909913633f`
- 模型目录：仓库外 Artifact Root 下的 `model/`
- allowlist：仅 `allowlist.json` 中 5 个文件
- 基础镜像：Dockerfile 中固定的 Linux/amd64 manifest digest
- 运行时：CPU-only、fast tokenizer、Safetensors、平均池化、L2 normalization、
  `query:` / `passage:`、最大 512 Token、768 维

## 安全边界

- 取件必须设置 `QUALIFICATION_CURL` 为已审核的绝对可执行文件路径。
- 运行镜像不包含 curl、Git、Git LFS、Hugging Face 下载工具或 Token。
- 模型目录以只读方式挂载到容器绝对路径 `/model`。
- 离线验证与资格运行使用 `--network none` 和全新空缓存目录。
- `verify` 会拒绝缺失、额外、字节数不符、符号链接或 manifest 不匹配。
- 若固定 allowlist 不能离线加载，立即停止；不得增加文件或切换 tokenizer。

## 测试

```powershell
$env:PYTHONPATH = "tools/embedding-qualification/src"
python -m unittest discover -s tools/embedding-qualification/tests -v
```

完整受控运行使用 `scripts/run-qualification.ps1`。脚本要求 Artifact Root 位于
Git worktree 外，并把大文件、缓存和原始性能记录留在仓库外。
