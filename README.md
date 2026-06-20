# Dark Altar UI — 黑暗祭坛UI

[![MC Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

为 [Goety（诡厄巫法）](https://www.curseforge.com/minecraft/mc-mods/goety) 的黑暗祭坛提供 AUI 图形化操作界面，支持 JEI/EMI 配方一键自动填充。

## 功能

- 🕯️ **可视化祭坛管理** — 空手右键黑暗祭坛打开菱形布局界面，展示周围 12 个底座物品
- ✨ **一键配方填充** — 在 JEI/EMI 查看仪式配方，点击 + 自动将材料填入底座槽位
- 🔄 **自动提交** — 关闭界面自动放置/退回物品，保留 NBT 数据
- 📋 **仪式类型检测** — 自动识别 15 种仪式类型（活化/死灵/锻造/魔法等）
- 🔒 **物品安全** — 打开界面时清空底座物品防止被其他玩家拿走
- ⚡ **可选灵魂消耗** — 配置文件启用后操控物品消耗诅咒之笼/玩家灵魂能量

## 依赖

| 模组 | 必需 | 说明 |
|------|:--:|------|
| [Goety](https://www.curseforge.com/minecraft/mc-mods/goety) | ✅ | 黑暗祭坛与仪式系统 |
| [ApricityUI](https://www.curseforge.com/minecraft/mc-mods/apricityui) | ✅ | UI 框架 |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 或 [EMI](https://modrinth.com/mod/emi) | 推荐 | 配方浏览与一键填充 |

## 使用

1. 在黑暗祭坛下方放置诅咒之笼并充能
2. 空手右键黑暗祭坛打开操作界面
3. 从背包拖拽物品或通过 JEI/EMI 点击 + 一键填充
4. 点击"魔力操控"关闭界面，物品自动放置到周围底座
5. 手动将激活物品放入祭坛开始仪式

## 配置

`config/darkaltarui-common.toml`：

```toml
[soul_cost]
enabled = false   # 启用灵魂消耗（默认关闭）
per_item = 1      # 每物品消耗点数
```

## 构建

```bash
./gradlew build
```

输出：`build/libs/darkaltarui-*.jar`

## 致谢

- [Goety](https://github.com/Polarice3/Goety-2) — 诡厄巫法
- [ApricityUI](https://github.com/Tower-of-Sighs/AUI) — 晴雪UI
- [JEI](https://github.com/mezz/JustEnoughItems) — Just Enough Items
- [EMI](https://github.com/emilyploszaj/emi) — Even More Items

## 许可

MIT License — 详见 [LICENSE](LICENSE)
