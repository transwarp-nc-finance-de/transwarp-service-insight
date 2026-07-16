# local-embedding 模型制品资格调查（Issue #19）

Status: ACTIVE

Owner: Service Insight Maintainers

Last reviewed: 2026-07-16

Source of truth for: Issue #19 的候选模型、供应链证据、资格实测协议、`mock-eval-v1` 评估口径与当前人工决策建议

## 结论摘要

**当前建议：`BLOCKED`。** 本文没有下载模型、没有修改 Compose，也不宣布模型门禁通过。

`intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f` 仍是范围文档已经确认的默认评估对象。官方固定 revision 页面显示模型采用 MIT 标识、支持中英在内的多语言、输出 768 维，并提供 Safetensors；严格 fast-tokenizer 方案中两个必要大文件的远端元数据合计约 1.129 GB，理论上低于 2 GB 制品上限。但下列门禁仍未取得可审计结论：

- 本任务禁止下载，因而小型配置文件的逐文件 SHA-256 尚未计算，完整制品清单不能封存；
- 最终运行时依赖版本、锁文件、SBOM、第三方 NOTICE 和容器基础镜像 digest 尚未形成；
- 目标 CPU 主机上的制品大小、峰值内存、查询 P95 和批量索引吞吐尚未实测；
- `mock-eval-v1` 只有固定数据资产与校验实现，EvaluationRun、真实 Embedding/混合检索执行路径仍未实现，无法产生模型资格结果。

2026-07-16，用户以本项目唯一开发者及全部责任角色身份，已确认内部非商用使用范围、许可证剩余风险、固定模型、安全边界、后续独立 Ticket 的受控下载授权、代表性测试机和资格门槛；该授权不允许 Issue #19 本身下载模型，也不等同于门禁通过。

该判断置信度为**高**：剩余阻塞项可以直接映射到 Issue #19 验收条件和仓库当前实现边界。人工介入建议见“人工批准项”。待补充信息为最终依赖锁、完整 SHA-256 manifest、原始测量数据与评估运行 ID。

## 人工确认记录（2026-07-16）

- `CONFIRMED`：项目由用户一人开发；用户可代表架构、安全、产品、SLA、维护、法务/开源合规和基础设施责任作出本次确认。
- `CONFIRMED`：模型仅用于公司内部系统，不商用、不对外分发、不公开发布或转授权；保留上游 LICENSE、版权声明、NOTICE 与 SBOM。
- `CONFIRMED`：接受 MIT 模型标识、Apache-2.0/BSD 依赖及模型训练数据权利链未逐项审计的剩余风险。
- `CONFIRMED`：默认模型固定为 `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f`，768 维、最大 512 Token，使用 `query:`/`passage:` 前缀；实测失败前不切换其他候选。
- `CONFIRMED`：允许后续独立 Ticket 从模型所有者的固定 revision 进行一次受控下载，用于逐文件 SHA-256、SBOM、离线运行和资格实测；本 Issue 不执行下载。
- `CONFIRMED`：当前开发机作为一期代表性测试机。2026-07-16 读取到 Intel Core i5-12490F、12 个逻辑处理器、31.85 GB 物理内存；当时可用内存约 11.86 GB，该瞬时值不作为门禁结果。
- `CONFIRMED`：硬门槛为模型制品不超过 2 GB、CPU-only、Embedding 服务峰值内存不超过 4 GB、并发 1 热态查询 P95 不超过 1 秒；批量索引只记录吞吐、峰值内存和总耗时，暂不设硬门槛。
- `CONFIRMED`：`mock-eval-v1` 门槛为权限泄漏率 0%、引用错误率 0%、Embedding 降级场景通过率 100%、Recall@5 不低于 80%，并声明“模拟数据：小样本工程评估，不代表生产效果”。
- `CONFIRMED`：`local-embedding` 仅允许内部网络通信、禁止互联网出站；Backend 后续按独立审批接入 AIOps，不受该容器边界影响。
- `CONFIRMED`：Issue #19 当前人工建议为 `BLOCKED`；待完整哈希、SBOM、资源性能和真实固定评估证据产生后，再人工决定 `PASS` 或 `FAIL`。

## 候选模型与固定 revision

| 优先级 | 候选与固定 revision | 官方事实 | 适配判断 |
| --- | --- | --- | --- |
| 1 | `intfloat/multilingual-e5-base@d13f1b27baf31030b7fd040960d60d909913633f` | Hugging Face 固定 revision 标记 MIT、94 languages、Safetensors；权重约 1.11 GB。模型卡规定检索输入使用 `query:` / `passage:` 前缀，平均池化、归一化，最大 512 Token；配置为 768 维。 | **唯一默认资格候选。** 与 `docs/project/scope.md` 及 `docs/product/metrics.md` 已确认的 768 维、512 Token 和前缀一致。仍须完成本文全部门禁。 |
| 2 | `intfloat/multilingual-e5-small@e4ce9877abf3edfe10b0d82785e83bdcb973e22e` | 官方模型卡标记 MIT、支持多语言；固定评测论文记录该 revision。模型配置为 384 维、512 位置，Safetensors 权重约 471 MB。 | **资源失败时的重评候选，不是无缝替代。** 384 维会改变索引模型与向量列约束，须另行人工变更范围、模型版本和索引版本，再从头跑资格门禁。 |
| 3 | `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2@bf3bf13ab40c3157080a7ab344c831b9ad18b5eb` | 官方模型卡标记 Apache-2.0、50 languages、384 维，SentenceTransformer 配置最大长度为 128；固定评测论文记录该 revision。 | **不满足当前 768 维/512 Token 既定接口，不能作为一期默认制品。** 仅可在范围变更后作为资源基线对照。 |

不把 `main`、标签或短提交号作为制品 revision。固定值必须是 40 位提交哈希；模型变化必须产生新的模型版本与索引版本，不能复用旧评估结论。

来源：[默认模型固定 revision 与文件树](https://huggingface.co/intfloat/multilingual-e5-base/tree/d13f1b27baf31030b7fd040960d60d909913633f)、[默认模型卡](https://huggingface.co/intfloat/multilingual-e5-base)、[e5-small 固定 revision](https://huggingface.co/intfloat/multilingual-e5-small/tree/e4ce9877abf3edfe10b0d82785e83bdcb973e22e)、[MiniLM 固定 revision](https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/tree/bf3bf13ab40c3157080a7ab344c831b9ad18b5eb)、[MTEB 固定 revision 表](https://proceedings.iclr.cc/paper_files/paper/2025/file/fc0e3f908a2116ba529ad0a1530a3675-Paper-Conference.pdf)。

## 许可证与企业分发边界

### 模型与直接依赖

| 对象 | 上游许可证 | 初步权限判断 | 必须保留/复核 |
| --- | --- | --- | --- |
| `multilingual-e5-base`、`multilingual-e5-small` | 模型卡元数据为 MIT | MIT 文本授予使用、复制、修改、合并、发布、分发、再许可及销售副本的权限，原则上覆盖企业内部使用、装入内部镜像及离线复制。 | 版权与许可声明必须随所有副本或实质部分分发；模型仓库没有独立 LICENSE 文件的交付形式须由法务确认；训练数据权利链不能仅由模型卡的 MIT 标签替代审查。 |
| `paraphrase-multilingual-MiniLM-L12-v2` | Apache-2.0 | Apache-2.0 明确授予复制、衍生、再许可与分发，并包含有条件的专利授权，原则上可内部镜像和离线分发。 | 分发时附许可证、保留归属/NOTICE、标记修改；不得推定商标授权，专利诉讼终止条款由法务复核。 |
| `transformers`、`tokenizers`、`safetensors`、可选 `sentence-transformers` | Apache-2.0 | 可用于企业内部运行及镜像/离线分发，受 Apache-2.0 第 4 节条件约束。 | 最终锁定版本对应的 LICENSE/NOTICE、修改说明、版权/专利/商标归属。 |
| PyTorch CPU | BSD-style 主许可证，仓库还包含第三方组件声明 | 主许可证允许源代码/二进制再分发及商业使用。 | 不能只记录“BSD”；必须对最终 wheel/镜像生成 SBOM，并随包保留 PyTorch LICENSE 与第三方许可证清单。 |
| SentencePiece | Apache-2.0 | 允许内部使用和再分发。 | 如果最终运行时或 tokenizer fallback 实际包含该库，纳入 SBOM 与 NOTICE；否则不得虚报为运行依赖。 |
| `huggingface_hub` | Apache-2.0 | 仅建议存在于受控取件工具中，不进入离线运行时。 | 固定工具版本与许可证；运行镜像不得保留可联网取件逻辑。 |

来源：[Transformers LICENSE](https://github.com/huggingface/transformers/blob/main/LICENSE)、[Sentence Transformers LICENSE](https://github.com/huggingface/sentence-transformers/blob/main/LICENSE)、[PyTorch LICENSE](https://github.com/pytorch/pytorch/blob/main/LICENSE)、[Safetensors LICENSE](https://github.com/safetensors/safetensors/blob/main/LICENSE)、[Tokenizers LICENSE](https://github.com/huggingface/tokenizers/blob/main/LICENSE)、[SentencePiece LICENSE](https://github.com/google/sentencepiece/blob/master/LICENSE)。

以上是工程侧许可证条款映射，不是正式法律意见。最终 Python/系统依赖尚无锁文件，因此当前不能声明“依赖许可证兼容性已通过”。法务批准必须绑定模型 ID、40 位 revision、完整 SBOM、内部使用主体、镜像仓库、离线接收方范围与 NOTICE 包，而不是只批准模型名称。

## 受控来源、必要文件与 SHA-256

### 受控下载来源

唯一建议上游为 Hugging Face 模型所有者仓库的固定 revision `resolve` 端点，首次取件必须由获批主体在受控构建区执行，例如：

`https://huggingface.co/intfloat/multilingual-e5-base/resolve/d13f1b27baf31030b7fd040960d60d909913633f/<filename>`

不得使用 `main`、第三方网盘、个人缓存、未固定 revision 的 Hub URL 或运行时隐式拉取。批准后的一次性取件流程应为：固定 allowlist → 下载至隔离区 → 逐文件计算 SHA-256 → 与双人复核的 manifest 比对 → 恶意文件扫描 → 生成 SBOM/NOTICE → 写入内部不可变制品库 → 以后构建只从内部 digest 地址读取。Hugging Face 官方下载接口支持 `revision`、文件过滤和 `dry_run`，但 dry-run/远端 ETag 不能替代对实际交付字节计算 SHA-256。[官方下载接口](https://huggingface.co/docs/huggingface_hub/en/package_reference/file_download)

### 默认候选的最小文件清单

建议使用 Transformers 原生 `AutoTokenizer` + `AutoModel`、模型卡规定的平均池化与归一化，并强制 fast tokenizer，避免 SentenceTransformer 的额外模块配置和 SentencePiece 慢速回退。资格制品只允许下列文件：

| 文件 | 用途 | 固定 revision 官方元数据 | 当前 SHA-256 状态 |
| --- | --- | --- | --- |
| `model.safetensors` | 唯一权重文件 | 1,112,201,288 bytes | `a18a44fad1d0b46ded15928144138cff1135d5cc8233bdd90be5f18822de09a7`（官方 LFS OID） |
| `tokenizer.json` | 快速 tokenizer | 17,082,660 bytes | `62c24cdc13d4c9952d63718d6c9fa4c287974249e16b7ade6d5a85e7bbb75626`（官方 LFS OID） |
| `config.json` | XLM-R 模型架构、768 维等配置 | 694 bytes | `BLOCKED`：官方 API 仅给 Git blob ID，须批准下载后按实际字节计算 |
| `tokenizer_config.json` | tokenizer 类、长度等配置 | 418 bytes | `BLOCKED`：同上 |
| `special_tokens_map.json` | 特殊 Token 映射 | 280 bytes | `BLOCKED`：同上 |

两个必要大文件合计 1,129,283,948 bytes；这只是远端必要文件元数据之和，不是目标容器内实测，所以不能据此判定制品大小门禁通过。资格制品明确排除 `sentencepiece.bpe.model`、`pytorch_model.bin`、`onnx/`、`openvino/`、TensorFlow 权重、`.gitattributes` 及其他重复或回退格式；若离线烟测证明 fast-tokenizer 清单不足，应停止而不是自动回退，并经人工评审后产生新 manifest。`README.md` 不进入运行时模型目录，但其固定 revision 快照、模型卡许可证元数据、本文和人工批准记录必须进入审计包。

元数据来源：[固定 revision、含 blob 信息的官方 API](https://huggingface.co/api/models/intfloat/multilingual-e5-base/revision/d13f1b27baf31030b7fd040960d60d909913633f?blobs=true)、[固定 revision 文件树](https://huggingface.co/intfloat/multilingual-e5-base/tree/d13f1b27baf31030b7fd040960d60d909913633f)。

### SHA-256 固定方式

批准取件后的 manifest 每行使用 `sha256  bytes  relative/path`，路径按 UTF-8 字节序升序，拒绝重复路径、绝对路径、`..`、符号链接及未列出文件。流程必须：

1. 对服务器返回的最终字节计算小写 64 位 SHA-256，不只信任 URL、ETag、Git blob ID 或缓存名；
2. 核对文件数、相对路径、逐文件字节数和 SHA-256；
3. 再对规范化 manifest 本身计算 SHA-256，并将其与模型 ID、revision、下载时间、审批单号、取件工具版本、来源 URL、SBOM hash 和镜像 digest 绑定；
4. 构建前、镜像内和离线导出后各校验一次；任一差异立即失败，不允许自动更新 hash；
5. manifest 的变更只能通过新的人工评审和新制品版本完成。

## 禁止远程代码和联网的运行方式

只从容器内绝对路径加载，并同时设置：

```text
HF_HUB_OFFLINE=1
TRANSFORMERS_OFFLINE=1
HF_DATASETS_OFFLINE=1
```

加载器必须显式传入 `local_files_only=True`、`trust_remote_code=False`、`use_safetensors=True`，tokenizer 还须传入 `use_fast=True` 并断言实际类型为 fast tokenizer；模型和 tokenizer 参数均为本地目录，不得传 Hub ID。镜像中不得包含 `.py` 模型文件、Git/LFS 元数据、下载 token 或运行时下载工具；网络策略应默认拒绝出站，并用“清空缓存 + 阻断 DNS/出站后仍能启动和完成推理”的集成测试验证，而不是只检查环境变量。

Transformers 官方文档确认 `trust_remote_code` 默认是 `False`，启用它会执行 Hub 上的自定义代码；`local_files_only=True` 只读取本地文件；离线模式可用 `HF_HUB_OFFLINE=1`；当 Safetensors 存在时应优先加载它以避开 pickle 权重。[Auto Classes 参数](https://huggingface.co/docs/transformers/model_doc/auto)、[离线模式](https://huggingface.co/docs/transformers/v4.49.0/en/installation)、[模型加载与 Safetensors](https://huggingface.co/docs/transformers/models)。

## 资源与性能实测方案

所有结果必须绑定同一份：CPU 型号/指令集、物理与逻辑核数、宿主内存、OS/内核、容器引擎、CPU quota、内存上限、运行镜像 digest、依赖锁 hash、模型 manifest hash、线程环境变量、`mock-eval-v1` checksum 和 Git commit。只使用 `模拟数据`；不接生产服务或真实数据。

### 制品大小与内存

- 制品大小：在解包后的只读模型目录递归统计普通文件实际字节数，同时记录压缩归档大小和 OCI 模型层大小；门禁以解包后的必要文件总字节数 `<= 2,000,000,000` 为准，并确认不存在排除格式。
- 冷启动内存：清空进程后启动 5 次，记录加载前基线、加载完成、首次推理和稳定空闲 RSS。
- 稳态峰值：容器限制为 4 GB，在批量索引最大批准 batch 与查询循环期间按不高于 100 ms 间隔采样 RSS/cgroup `memory.current` 和 `memory.peak`；任何 OOM 或峰值 `> 4,000,000,000` bytes 为失败。
- CPU-only：隐藏 GPU 设备并验证运行时没有 CUDA/ROCm provider；报告 CPU 时间、墙钟时间和线程数。

### 查询 P95

- 使用 `mock-eval-v1` 30 个案例各自最后一轮的查询映射，并按中文、英文、中英混合及跨语言标签分层；映射规则必须是未来线上 Precheck→query 的同一确定性实现。
- 固定并发 1、batch size 1、512 Token 截断、`query:` 前缀；先做 100 次不计入的预热，再以固定顺序轮转至少 1,000 次计时，使用单调时钟记录每次 Embedding 延迟。
- 分别报告冷启动首请求、热态 P50/P95/P99/max；门禁为热态单查询 P95 `<= 1,000 ms`。另报端到端混合检索 P95，但不能用它替代 Embedding P95。
- 原始逐请求记录必须保存 caseId、语言标签、输入 token 数、开始/结束时间、状态和降级，不保存未授权正文；重复运行至少 3 轮，任何一轮越界均不得取平均掩盖。

### 批量索引

- `mock-eval-v1` 只有 14 条 Evidence fixture，适合正确性校验但不足以给出稳定吞吐。性能集应从这些 `模拟数据` 模板确定性扩展为 100/1,000/10,000 个唯一 chunk，保留来源模板 ID并标注 `模拟数据`，不得改变资格评估集本身。
- 对 32/128/512 Token 桶以及 batch size 1/8/16/32 做矩阵测试；每组重建空索引，记录总 chunks、总 tokens、墙钟时间、chunks/s、tokens/s、P95 batch latency、峰值 RSS、失败/重试数和最终向量数。
- 批量索引没有仓库硬吞吐阈值，因此只形成容量基线，不能借用“查询 P95 <= 1 秒”宣布通过；产品/架构负责人须另行确认可接受的完整重建窗口。

## 使用 `mock-eval-v1` 做资格评估

### 前置条件

先运行现有 `EvaluationDatasetValidationTest`，证明数据资产、Schema、覆盖矩阵和 checksum 未漂移。该测试通过只代表数据资产一致，不能代表检索或模型效果。随后必须把 `evidence-fixture-manifest.json` 的 14 条稳定 Evidence 作为唯一模拟语料，按产品线、身份和发布版本规则建立 FTS 与该候选模型的向量索引；不允许手写命中映射或在测试中直接返回期望 ID。

### 执行与指标

1. 按 `caseId` 升序运行 30 个案例；多轮案例按 `runSequence` 依次执行完整上下文快照，不得只跑最后一轮。
2. 对 HYBRID 案例使用相同的权限过滤、FTS、向量召回和固定融合实现；`query:` 用于查询，Evidence chunk 使用 `passage:`。精确错误码、版本号和配置键仍主要由 FTS/混合路径验证。
3. 对 FTS_ONLY/UNAVAILABLE 案例注入真实的 Embedding 不可用 seam，验证降级状态、置信度限制和人工继续提交不受阻断；不得用跳过模型调用的恒真桩冒充故障行为。
4. 保存模型 ID/revision/manifest hash、索引版本、策略版本、dataset checksum、每例 Top-5 Evidence ID/score、过滤与降级摘要、原始计时以及失败码；失败案例公共表示不得泄露无权 Evidence ID 或正文。
5. 计算并按语言/场景切片展示：
   - `Recall@5`：对有期望 Evidence 的案例，Top-5 与 `expectedEvidenceIds` 的召回；总值必须 `>= 80%`；
   - 权限泄漏率：返回任一 `forbiddenEvidenceIds` 或超出执行身份/允许产品线的 Evidence 即失败，必须为 `0%`；
   - 引用错误率：Evidence ID、versionId、chunkId 或 `contentHash` 任一与 manifest 不一致即错误，必须为 `0%`；
   - 降级场景通过率：预期模式、降级状态、置信度上限、待补字段和继续提交行为全部匹配才算通过，必须为 `100%`。
6. 正常执行但指标未过门槛应记录运行 `SUCCEEDED` 且 `gatePassed=false`；执行基础设施失败才是任务失败。报告必须声明“小样本工程评估，不代表生产效果”。

资格报告的智能输出必须包含依据来源、置信度、人工介入建议和待补充信息；不得写成最终根因、最终方案或正式复盘结论，也不得阻断人工继续提交。指标门槛来源：[产品与评估指标](../product/metrics.md)；数据边界来源：[固定模拟评估集维护指南](evaluation-dataset.md)。

## 决策状态机与当前建议

- `PASS`：且仅当许可证/分发、下载来源、完整文件 SHA-256、依赖 SBOM/NOTICE、安全离线运行、资源实测和 `mock-eval-v1` 四项指标均由责任人签字通过。
- `FAIL`：已取得可审计实测或评估结果，明确违反任一硬门槛，且责任人拒绝豁免/变更范围；失败模型不得进入默认镜像。
- `BLOCKED`：缺少批准、制品字节、运行实现或测量，无法作出 PASS/FAIL。缺证据不是 PASS，也不应伪装成 FAIL。

**Issue #19 当前建议：`BLOCKED`。** 建议保持 `ready-for-human`，不关闭门禁；已授权后续独立 Ticket 受控下载和实测，但本 Issue 不下载模型，证据齐备前不得打包或默认启用。

## 人工批准项

| 责任角色 | 需要明确批准的对象 | 当前状态 |
| --- | --- | --- |
| 法务/开源合规负责人 | 默认模型 MIT 标签及训练数据风险；企业内部非商用使用边界；LICENSE/NOTICE/SBOM 交付方式 | `CONFIRMED`；最终依赖锁与 SBOM 待生成 |
| 信息安全/供应链负责人 | Hugging Face 固定 revision、Safetensors-only、禁止远程代码、内部网络和运行时禁止互联网出站；后续独立 Ticket 受控下载 | `CONFIRMED`；完整 SHA-256 manifest 与扫描证据待生成 |
| 基础设施负责人 | 当前开发机为代表性主机；4 GB、CPU-only、查询 P95 门槛；批量索引只建基线 | `CONFIRMED`；实测报告待生成 |
| 产品负责人 / SLA 处理人 / 安全负责人 | `mock-eval-v1` 四项门槛及“小样本工程评估”声明 | `CONFIRMED`；真实评估结果待生成 |
| 架构负责人 | 默认模型 revision、768 维、512 Token、前缀/池化和模型/索引版本边界 | `CONFIRMED` |
| Service Insight Maintainers | 汇总上述签字、确认无事实冲突后，将 Issue #19 人工标为 PASS、FAIL 或继续 BLOCKED；SLA 内容及提交仍由人工确认 | `BLOCKED` |

## 审计来源

- [GitHub Issue #19](https://github.com/transwarp-nc-finance-de/transwarp-service-insight/issues/19)
- [默认模型固定 revision 文件树](https://huggingface.co/intfloat/multilingual-e5-base/tree/d13f1b27baf31030b7fd040960d60d909913633f)
- [默认模型固定 revision API（含 blobs）](https://huggingface.co/api/models/intfloat/multilingual-e5-base/revision/d13f1b27baf31030b7fd040960d60d909913633f?blobs=true)
- [Hugging Face 下载接口](https://huggingface.co/docs/huggingface_hub/en/package_reference/file_download)
- [Transformers 离线模式](https://huggingface.co/docs/transformers/v4.49.0/en/installation)
- [Transformers Auto Classes 安全参数](https://huggingface.co/docs/transformers/model_doc/auto)
- [Transformers 模型加载与 Safetensors](https://huggingface.co/docs/transformers/models)
- 仓库事实：`docs/project/scope.md`、`docs/product/metrics.md`、`docs/development/evaluation-dataset.md`、`backend/src/main/resources/evaluation/mock-eval-v1/`、`docs/api/openapi-v2.yaml`（DRAFT）。
