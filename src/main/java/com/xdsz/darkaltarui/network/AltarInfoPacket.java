package com.xdsz.darkaltarui.network;

import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.instance.element.Slot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：祭坛信息 + 可用配方列表。
 */
public class AltarInfoPacket {

    private final int count;
    private final int souls;
    private final List<String> ritualTypes;
    private final List<RecipeInfo> recipes;

    public AltarInfoPacket(int count, int souls, List<String> ritualTypes, List<RecipeInfo> recipes) {
        this.count = count;
        this.souls = souls;
        this.ritualTypes = ritualTypes;
        this.recipes = recipes;
    }

    public static void encode(AltarInfoPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.count);
        buf.writeVarInt(msg.souls);
        buf.writeVarInt(msg.ritualTypes.size());
        for (String s : msg.ritualTypes) buf.writeUtf(s);
        buf.writeVarInt(msg.recipes.size());
        for (RecipeInfo r : msg.recipes) {
            buf.writeResourceLocation(r.id);
            buf.writeUtf(r.resultName);
            buf.writeVarInt(r.ingredientStacks.size());
            for (int i = 0; i < r.ingredientStacks.size(); i++) {
                buf.writeVarInt(r.ingredientStacks.get(i).size());
                for (ItemStack stack : r.ingredientStacks.get(i)) {
                    buf.writeItem(stack);
                }
            }
            buf.writeItem(r.resultStack);
            buf.writeUtf(r.craftType);
            buf.writeVarInt(r.soulCost);
        }
    }

    public static AltarInfoPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        int souls = buf.readVarInt();
        int typeSize = buf.readVarInt();
        List<String> ritualTypes = new ArrayList<>();
        for (int i = 0; i < typeSize; i++) ritualTypes.add(buf.readUtf());

        int recipeSize = buf.readVarInt();
        List<RecipeInfo> recipes = new ArrayList<>();
        for (int r = 0; r < recipeSize; r++) {
            ResourceLocation id = buf.readResourceLocation();
            String resultName = buf.readUtf();
            int ingCount = buf.readVarInt();
            List<List<ItemStack>> ingStacks = new ArrayList<>();
            for (int i = 0; i < ingCount; i++) {
                int stackCount = buf.readVarInt();
                List<ItemStack> stacks = new ArrayList<>();
                for (int j = 0; j < stackCount; j++) {
                    stacks.add(buf.readItem());
                }
                ingStacks.add(stacks);
            }
            ItemStack result = buf.readItem();
            String craftType = buf.readUtf();
            int soulCost = buf.readVarInt();
            recipes.add(new RecipeInfo(id, resultName, ingStacks, result, craftType, soulCost));
        }
        return new AltarInfoPacket(count, souls, ritualTypes, recipes);
    }

    public static void handle(AltarInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<Document> docs = ApricityUI.getDocument("altar/index.html");
            for (Document doc : docs) {
                if (doc == null || doc.body == null) continue;

                // 更新祭坛数量
                Element countEl = findElementById(doc.body, "altar-count");
                if (countEl != null) {
                    countEl.innerText = "检测到 " + msg.count + " 个祭坛 | 魂力: " + msg.souls;
                }

                // 更新仪式信息
                Element ritualEl = findElementById(doc.body, "ritual-info");
                if (ritualEl != null) {
                    if (msg.ritualTypes.isEmpty()) {
                        ritualEl.innerText = "适用于：无";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < msg.ritualTypes.size(); i++) {
                            if (i > 0) sb.append(" · ");
                            sb.append(msg.ritualTypes.get(i).replaceAll("§[0-9a-fk-or]", ""));
                        }
                        ritualEl.innerText = sb.toString();
                    }
                }

                // 填充配方列表到特殊槽位
                Element recipeSlot = findElementById(doc.body, "recipe-slot-0");
                if (recipeSlot != null && !msg.recipes.isEmpty()) {
                    // 将第一个配方的结果物品显示在配方预览区
                    recipeSlot.innerText = msg.recipes.get(0).resultName;
                }

                // 存储配方数据到 data 属性供 JS 使用
                Element panel = findElementById(doc.body, "altar-panel");
                if (panel != null) {
                    StringBuilder sb = new StringBuilder();
                    for (RecipeInfo r : msg.recipes) {
                        if (sb.length() > 0) sb.append("||");
                        sb.append(r.id).append("|").append(r.resultName).append("|").append(r.craftType);
                    }
                    panel.setAttribute("data-recipes", sb.toString());
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static Element findElementById(Element parent, String id) {
        if (parent == null || id == null) return null;
        if (id.equals(parent.id)) return parent;
        for (Element child : parent.children) {
            Element found = findElementById(child, id);
            if (found != null) return found;
        }
        return null;
    }

    /** 配方数据 */
    public record RecipeInfo(
            ResourceLocation id,
            String resultName,
            List<List<ItemStack>> ingredientStacks,
            ItemStack resultStack,
            String craftType,
            int soulCost
    ) {}
}
