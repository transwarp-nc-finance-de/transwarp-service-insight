# local-embedding 模型资格实测报告（Issue #39）

Status: ACTIVE

Owner: Service Insight Maintainers

Last reviewed: 2026-07-18

Source of truth for: Issue #39 的受控取件、供应链、离线安全、资源性能、`mock-eval-v1` 资格实测及提交给 Decision Gate #19 的人工决策建议

## 结论摘要

**Issue #39 总体建议：`PASS`。Decision Gate #19 状态：`AWAITING_HUMAN_CONFIRMATION`，不得自动标记为 PASS。**

固定候选 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f` 的五个批准文件共 `1,129,285,340` bytes，低于 2,000,000,000 bytes。隔离镜像在 `network=none`、CPU-only 和 4 GiB 内存限制下完成真实加载、768 维推理、五次冷启动、三轮查询、36 组批量基线和 `mock-eval-v1` 全集资格评估。没有 OOM、网络访问、批量失败、权限泄漏或引用错误；三轮热态单查询 P95 均低于 1,000 ms，Recall@5 为 96%。

模拟数据：小样本工程评估，不代表生产效果。

结论置信度为**高**，依据是固定 revision、实际交付字节 SHA-256、锁文件/SBOM/镜像 digest、独占运行记录、原始结果校验和和现有固定评估集。该结论只建议模型通过当前工程资格门禁，不是生产容量承诺、最终方案、最终根因或正式复盘结论。

人工介入建议：人工复核本报告、外置原始结果和校验和后，再在 Issue #19 决定 `PASS/FAIL/BLOCKED`。SLA 内容与提交仍由人工确认，资格建议不得阻断人工继续提交。

待补充信息：

- 生产批量索引可接受的完整重建窗口；
- 真实知识语料、真实生产负载和多并发容量；
- 是否将约 17 小时 34 分钟的全量资格矩阵保留为专项/夜间门禁，或拆分为快速与完整两层门禁。

## 固定取件与制品清单

受控取件仅发生一次，工具为 `curl 8.20.0 (Windows)`。固定来源前缀为：

`https://huggingface.co/intfloat/multilingual-e5-base/resolve/d13f1b27baf31030b7fd040960d60d909913633f/`

| 文件 | bytes | SHA-256 |
| --- | ---: | --- |
| `config.json` | 694 | `9dab198f24c8c0879e481cf7822005d5ecbceedbacb390ffafa594e28d31bac4` |
| `model.safetensors` | 1,112,201,288 | `a18a44fad1d0b46ded15928144138cff1135d5cc8233bdd90be5f18822de09a7` |
| `special_tokens_map.json` | 280 | `06e405a36dfe4b9604f484f6a1e619af1a7f7d09e34a8555eb0b77b66318067f` |
| `tokenizer.json` | 17,082,660 | `62c24cdc13d4c9952d63718d6c9fa4c287974249e16b7ade6d5a85e7bbb75626` |
| `tokenizer_config.json` | 418 | `efb5c0d09722e5fe59a462cd2a9976ee216d55b037597d997cd3fe833216da15` |

规范化 manifest hash 为 `8f58395f4bf0f613fd15ebbf3d5467193ad6717715c4167c764e6e91aae1810e`。仓库只保存 manifest 和聚合结果，不保存模型、tokenizer、缓存、wheel、OCI 导出或大型原始日志。

## 供应链绑定

| 对象 | 绑定值 |
| --- | --- |
| Python | `3.11.9` |
| 基础镜像 | `python:3.11.9-slim-bookworm@sha256:2856e6af...`（完整值见 Dockerfile） |
| 资格镜像 | `sha256:eeef52545a2646e03d7bbd719885672342bebae8c13c3afa4ff6264dae1121ce` |
| 镜像大小 | `325,025,308` bytes |
| 依赖锁 hash | `sha256:c6c8f2d503fe88179dc3b78d996ec5a12f83a409c42da9102020310532e3feb1` |
| SBOM hash | `sha256:3bfe4c86bc20f82e8fea29944850997718378c03bf45ef9bc0ff41e26ed68de9` |
| dataset | `mock-eval-v1` |
| dataset checksum | `sha256:315f364835d88023c6225c62b6feff95cf2df1fd3fe26fb4fe92048e26aa9229` |
| 测量 Git commit | `875b1bd77e2361434f8ff48e5fc877bcddbf14c1` |

依赖已按 hash 锁定，仓库包含 CycloneDX SBOM 和第三方 NOTICE。SBOM 工具对部分 Python/Go catalog 关系给出非阻断告警，且部分包未自动识别许可证字段；因此 `THIRD_PARTY_NOTICE.md` 是人工复核入口，SBOM 不是正式法律意见。

## 离线与安全验证

- 模型目录只读挂载，运行镜像不含下载凭据、Git/LFS、curl、`hf` / `huggingface-cli` 下载入口或自定义模型 Python 文件。AutoTokenizer/AutoModel 必需的传递库 `huggingface-hub` 仍在 SBOM 中，但不提供 CLI，并由离线变量和 `network=none` 阻断远端能力。
- `local_files_only=true`、`trust_remote_code=false`、`use_safetensors=true`、`use_fast=true`。
- `HF_HUB_OFFLINE=1`、`TRANSFORMERS_OFFLINE=1`、`HF_DATASETS_OFFLINE=1`。
- 新空缓存目录下以 `network=none` 加载 tokenizer 和模型，完成真实 CPU 推理并输出 768 维归一化向量。
- 运行期 Docker 网络 I/O 为 `0B / 0B`，没有远程代码执行。
- 身份和产品线在候选召回前过滤；无权 Evidence 不进入候选集。

## 测量环境

| 项目 | 值 |
| --- | --- |
| CPU | Intel Core i5-12490F |
| 物理核 / 逻辑处理器 | 6 / 12 |
| 宿主内存 | 34,198,499,328 bytes |
| OS | Windows 11 Pro 10.0.26200 64-bit，Docker Linux/amd64 |
| Docker | 29.4.0 |
| 容器 CPU limit | 12 |
| 容器内存 limit | 4,294,967,296 bytes |
| 线程 | `OMP_NUM_THREADS=1`、`MKL_NUM_THREADS=1`、`OPENBLAS_NUM_THREADS=1` |

预检记录显示当时没有其他运行容器，可用内存 `10,382,561,280` bytes、CPU 负载 44%。最终性能期间未并行执行 Maven、npm、Compose、其他模型容器或 Agent 构建。

## 冷启动与内存

| 运行 | 加载秒数 | 首次推理 ms | 首次推理 RSS bytes | cgroup peak bytes |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 4.444 | 1,337.676 | 1,004,552,192 | 915,554,304 |
| 2 | 4.196 | 1,237.121 | 1,004,191,744 | 916,701,184 |
| 3 | 4.081 | 1,188.070 | 1,004,486,656 | 916,111,360 |
| 4 | 4.085 | 1,145.057 | 1,006,338,048 | 918,323,200 |
| 5 | 3.975 | 1,247.647 | 1,004,503,040 | 916,500,480 |

平均加载 4.156 秒，平均首次推理 1,231.114 ms。批量矩阵最大进程 RSS `2,843,578,368` bytes，cgroup 峰值 `2,800,455,680` bytes；均低于 4,000,000,000 bytes，未发生 OOM。

## 热态单查询延迟

固定并发 1、batch size 1、`query:` 前缀；三轮开始前统一预热 100 次，每轮再记录 1,000 次，使用单调时钟。

| 轮次 | P50 ms | P95 ms | P99 ms | max ms |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 125.651 | 159.685 | 182.550 | 235.174 |
| 2 | 123.458 | 153.533 | 168.070 | 202.687 |
| 3 | 127.489 | 162.682 | 183.343 | 207.165 |

三轮 P95 全部低于 1,000 ms，未用平均值掩盖单轮超限。

## 批量索引基线

36 组矩阵覆盖 100/1,000/10,000 chunks、32/128/512 Token 桶和 batch 1/8/16/32。每组同时记录 `minTokenCount` / `maxTokenCount`，三个桶均严格等于目标值；全部组 `failureCount=0`，最终向量数与输入一致。

- 总测量时间：`63,252.193` 秒；
- chunks/s 范围：`0.879–19.081`；
- tokens/s 范围：`316.307–610.584`；
- 最重组合：10,000 chunks、512 Token、batch 1，用时 `11,380.032` 秒；
- 未设置或推导未经人工确认的吞吐硬门槛。

这组结果只建立当前单核线程配置下的工程基线。全矩阵墙钟约 17 小时 34 分钟，不适合直接作为同步 CI；是否接受完整重建窗口仍需人工确认。

## `mock-eval-v1` 资格结果

运行状态为 `SUCCEEDED`，`gatePassed=true`。HYBRID 路径实际执行预过滤、确定性词法召回、真实向量召回、RRF `k=60` 和稳定 Top 5；FTS_ONLY/UNAVAILABLE 通过真实故障 seam 验证降级。

| 指标 | 门槛 | 实测 | 结果 |
| --- | ---: | ---: | --- |
| 权限泄漏率 | 0% | 0% | `PASS` |
| 引用错误率 | 0% | 0% | `PASS` |
| 降级场景通过率 | 100% | 100% | `PASS` |
| Recall@5 | >= 80% | 96% | `PASS` |

引用核验覆盖 `evidenceId`、`documentId`、`versionId`、`chunkId` 和 `contentHash`。多轮案例依序执行所有完整 Context 快照。

## 门禁表

| 门禁 | 结果 | 依据 |
| --- | --- | --- |
| 固定 revision 与批准 allowlist | `PASS` | 五文件实际 SHA-256、规范化 manifest |
| 必要制品 <= 2 GB | `PASS` | 1,129,285,340 bytes |
| 依赖锁、SBOM、NOTICE、镜像绑定 | `PASS` | 锁 hash、SBOM hash、NOTICE、镜像 digest |
| CPU-only、离线、无远程代码 | `PASS` | `network=none` 真实推理、0B 网络、加载参数 |
| 4 GiB 无 OOM、峰值 <= 4 GB | `PASS` | cgroup peak 2,800,455,680 bytes |
| 三轮热态查询 P95 <= 1,000 ms | `PASS` | 159.685 / 153.533 / 162.682 ms |
| 批量基线完整性 | `PASS` | 32/128/512 精确桶、36 组、0 失败；没有吞吐硬门槛 |
| `mock-eval-v1` 四项指标 | `PASS` | 0% / 0% / 100% / 96% |
| API、后端、前端、Compose 回归 | `PASS` | OpenAPI v1/v2、后端 91 项、前端 24 项及构建、默认 Compose 三服务健康检查均通过 |
| Decision Gate #19 人工签字 | `AWAITING_HUMAN_CONFIRMATION` | 必须由人工复核后决定，不自动更新 |

因此 Issue #39 当前总体建议为 `PASS`；Issue #19 仍等待人工确认。

## 原始证据与校验和

仓库外 Artifact Root：

`D:\workspace\transwarp-service-insight-issue39-artifacts`

原始结果目录：

`D:\workspace\transwarp-service-insight-issue39-artifacts\results`

原始结果 SHA-256 清单自身 hash：

`sha256:3347d2f4f6e581f06c909acad93c236aaf80682c05b15747d69bf37a34cd4caf`

仓库内脱敏副本见 `tools/embedding-qualification/evidence/`。Git 副本只把原始 Windows CRLF 行尾规范化为 LF，因此该副本自身 hash 为 `sha256:bb02d7236d3461ceb7b056638770f2598b2cffb21a5633b1c56864480989d0bf`；逐结果 SHA-256、字节数、文件名与顺序均未改变。逐请求原始结果仅保存 caseId、语言/场景标签、tokenCount、duration、status、degradation 和模型 manifest hash，不含正文。

## 建议回填 Issue #19 的评论

> Issue #39 已完成固定 revision 的受控资格实测。五个批准文件共 1,129,285,340 bytes，manifest hash 为 `8f58395f...`; 隔离镜像 digest 为 `sha256:eeef525...`。CPU-only、`network=none`、4 GiB 条件下无 OOM，cgroup 峰值 2,800,455,680 bytes；三轮热态查询 P95 为 159.685 / 153.533 / 162.682 ms。32/128/512 精确 Token 桶的 36 组批量基线全部成功。`mock-eval-v1` 运行 `SUCCEEDED` 且 `gatePassed=true`：权限泄漏率 0%、引用错误率 0%、降级通过率 100%、Recall@5 96%。模拟数据：小样本工程评估，不代表生产效果。建议人工复核报告、SBOM/NOTICE、模型 manifest 与原始结果 checksum 后决定 Issue #19 的 `PASS/FAIL/BLOCKED`；本评论不自动关闭或标记 Decision Gate。

## 回滚

该 Harness 与产品运行时隔离。回滚时删除 `tools/embedding-qualification/` 及本报告/导航增量，并删除仓库外 Artifact Root 与本地资格镜像；无需修改默认 Compose、API、Controller、检索 Port、数据库或产品依赖。删除外置模型和镜像属于破坏性操作，必须由人工明确授权。
