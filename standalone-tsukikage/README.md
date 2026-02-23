# standalone-tsukikage

独立 Forge 模组（MC 1.20.1），仅包含两项功能：

1. 新刀：`tsukikage:tsukikage` 与 `tsukikage:tsukikage_awakened`
2. 跟随实体：玩家主手持有觉醒态月影刀时，在玩家左前方显示跟随实体（ArmorStand）

## 关键实现

- 模组入口：`cn.adwadg.tsukikage.TsukikageMod`
- 跟随逻辑：`cn.adwadg.tsukikage.TsukikageMod` 内部静态类 `FollowerHandler`
- 数据驱动刀定义：
  - `data/tsukikage/slashblade/named_blades/tsukikage.json`
  - `data/tsukikage/slashblade/named_blades/tsukikage_awakened.json`

## 构建

在目录 `standalone-tsukikage/` 下执行：

```bash
gradle build  # 需要本机已安装 Gradle（仓库不包含二进制 wrapper jar）
```
