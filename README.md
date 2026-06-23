# Dark Altar UI — 黑暗祭坛UI

[![MC Version](https://img.shields.io/badge/Minecraft-1.20.1-blue)](https://minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange)](https://files.minecraftforge.net)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Version](https://img.shields.io/badge/version-3.7.7-brightgreen)](https://github.com/SurpTalent/darkaltarui)

为 [Goety（诡厄巫法）](https://www.curseforge.com/minecraft/mc-mods/goety) 的黑暗祭坛提供 AUI 图形化操作界面，支持 JEI/EMI 配方一键自动填充。背包装备 / RS 网络存储联动。

## 功能

- 🕯️ **可视化祭坛管理** — 空手右键黑暗祭坛打开菱形布局界面，展示 12 底座 + 激活物品放祭坛
- ✨ **一键配方填充** — JEI/EMI 点击 + 从玩家背包 / Sophisticated Backpacks / RS 网络自动匹配材料
- 🎒 **背包联动** — 穿戴背包时 + 号搜索背包物品
- 📡 **RS 网络联动** — 佩戴灵魂便捷终端时 + 号从 RS 存储提取物品
- 💎 **灵魂便捷终端** — 右键 RS 设备绑定，右键打开无线网格（消耗灵魂）
- 🔗 **链接宝石无限距离** — 链接宝石放 RS 无线发射器升级槽，无视距离
- ⚡ **诅咒之笼供电** — 诅咒之笼输出 FE 给 RS 网络（1灵魂=1000FE，可配置）
- 🌿 **智能配方预览** — 目光看祭坛时浮窗显示预测配方、仪式类型、材料匹配度
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

## 仪式预览（v3.1+）

目光看祭坛 → 左上角浮窗自动显示：

```
预合成: 尖牙盛宴聚晶
仪式: 魔法仪式 ✅
  缺少: 末影珍珠×3
  ✓ 饥饿核心 2个
  ✗ 末影珍珠 1/4个
  ✗ 金锭 0/2个
```

- 祭坛空 + 底座有物品 → 根据底座物品推断配方
- 祭坛空 + 底座空 → 显示当前支持的仪式列表
- 视线移开 → 浮窗消失（Jade 风格）
- **可拖动** — 按住顶部拖动条移动面板位置
- 开背包/界面时预览保持显示

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
