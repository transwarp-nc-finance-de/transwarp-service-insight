# Third-party notices

本资格 Harness 的最终第三方依赖与版本以 `requirements.lock` 和
`sbom.cdx.json` 为准。

- `multilingual-e5-base`：上游模型卡标记 MIT；仅按固定 revision 用于内部非商用
  资格评估。模型字节不提交 Git。
- `transformers`、`tokenizers`、`safetensors`、`huggingface-hub`：Apache-2.0。
- PyTorch：BSD-style 主许可证，并包含上游第三方组件声明。
- NumPy、packaging、PyYAML、requests、tqdm、filelock、fsspec、regex、
  typing-extensions、psutil 等传递依赖：许可证明细以 SBOM 和安装包元数据为准。

该汇总是工程供应链材料，不是正式法律意见。人工复核必须绑定模型 manifest
hash、依赖锁 hash、镜像 digest 与 SBOM hash。
