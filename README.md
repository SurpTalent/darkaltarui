# Dark Altar UI — 黑暗祭坛UI

[![MC Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Version](https://img.shields.io/badge/version-3.0.1-brightgreen)](https://github.com/SurpTalent/darkaltarui)

为 [Goety（诡厄巫法）](https://www.curseforge.com/minecraft/mc-mods/goety) 的黑暗祭坛提供 AUI 图形化操作界面，支持 JEI/EMI 配方一键自动填充。背包装备 / RS 网络存储联动。

## 功能

- 🕯️ **可视化祭坛管理** — 空手右键黑暗祭坛打开菱形布局界面，展示 12 底座 + 激活物品放祭坛
- ✨ **一键配方填充** — JEI/EMI 点击 + 从玩家背包 / Sophisticated Backpacks / RS 网络自动匹配材料
- 🎒 **背包联动** — 穿戴背包时 + 号搜索背包物品
- 📡 **RS 网络联动** — 佩戴灵魂便捷终端时 + 号从 RS 存储提取物品
- 💎 **灵魂便捷终端** — 右键 RS 设备绑定，右键打开无线网格（消耗灵魂）
- 🔗 **链接宝石无限距离** — 链接宝石放 RS 无线发射器升级槽，无视距离
- ⚡ **诅咒之笼供电** — 诅咒之笼输出 FE 给 RS 网络（1灵魂=1000FE，可配置）
- 📋 **NBT 宽松匹配** — 配方 NBT 是背包物品的子集即可匹配
- 🔒 **物品安全** — 打开界面时清空底座物品防止被拿走

## 依赖

| 模组 | 必需 | 说明 |
|------|:--:|------|
| [Goety](https://www.curseforge.com/minecraft/mc-mods/goety) | ✅ | 黑暗祭坛与仪式系统 |
| [ApricityUI](https://www.curseforge.com/minecraft/mc-mods/apricityui) | ✅ | UI 框架 |
| [EMI](https://modrinth.com/mod/emi) | 推荐 | 配方浏览与一键填充 |
| [Sophisticated Backpacks](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks) | 可选 | 背包物品自动填充 |
| [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) | 可选 | RS 网络物品自动填充 + 终端 |

## 使用

1. 黑暗祭坛下方放诅咒之笼并充能
2. 空手右键祭坛打开 AUI
3. JEI/EMI 查看仪式配方，点 + 自动填充底座
4. 激活物品自动放祭坛方块上
5. 右键祭坛取走激活物品再放回去开始仪式

## 灵魂便捷终端

- 右键 RS 控制器 / 磁盘驱动器绑定网络
- 右键空气打开无线网格（消耗灵魂能量）
- 链接宝石放无线发射器升级槽 → 无视距离

## 配置

`config/darkaltarui-common.toml`：

```toml
[soul_cost]
enabled = false     # 操控物品消耗灵魂（默认关）
per_item = 1        # 每物品灵魂消耗

[terminal]
soul_cost_per_open = 2  # 打开终端消耗

[rs_power]
fe_per_soul = 1000      # 1灵魂转化FE（0=禁用）
```

## 构建

```bash
./gradlew build
```

## 致谢

- [Goety](https://github.com/Polarice3/Goety-2) · [ApricityUI](https://github.com/Tower-of-Sighs/AUI) · [EMI](https://github.com/emilyploszaj/emi)
- [Sophisticated Backpacks](https://github.com/P3pp3rF1y/SophisticatedBackpacks) · [Refined Storage](https://github.com/refinedmods/refinedstorage)
- [RSInfinityBooster](https://github.com/Hexeption/RSInfinityBooster) · [Sophisticated-JEI-Index](https://github.com/AmicBeam/Sophisticated-JEI-Index)

## 许可

MIT License
